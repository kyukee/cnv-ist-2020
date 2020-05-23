provider "aws" {
    region     = "us-east-1"
}

variable "ssh_key_pair_name" {
    description = "The name of the key you will use for ssh access. If you have a <key_name.pem> file, then the value should be <key_name> \nIt is recommended to export environment variable TF_VAR_ssh_key_pair_name=key_name"
}

resource "aws_security_group" "security-group" {
    name = "CNV-project-security-group"

    ingress {
        from_port   = 8000
        to_port     = 8000
        protocol    = "tcp"
        cidr_blocks = ["0.0.0.0/0"]
    }

    ingress {
        from_port   = 8080
        to_port     = 8080
        protocol    = "tcp"
        cidr_blocks = ["0.0.0.0/0"]
    }

    ingress {
        from_port   = 80
        to_port     = 80
        protocol    = "tcp"
        cidr_blocks = ["0.0.0.0/0"]
    }

    // ssh access
    ingress {
        from_port   = 22
        to_port     = 22
        protocol    = "tcp"
        cidr_blocks = ["0.0.0.0/0"]
    }

    // terraform removes the default aws outbound rule
    egress {
        from_port   = 0
        to_port     = 0
        protocol    = "-1"
        cidr_blocks = ["0.0.0.0/0"]
    }
}

resource "aws_dynamodb_table" "dynamodb-table-cnv-performance-metrics" {
  name           = "CNV-project-metrics"
  read_capacity  = 5
  write_capacity = 5
  hash_key       = "threadID"
  range_key      = "startTimeMillis"

  attribute {
    name = "threadID"
    type = "N"
  }

  attribute {
    name = "startTimeMillis"
    type = "N"
  }
}

data "aws_ami" "webserver-ami" {  // Based on: Amazon Linux 2 AMI (HVM), SSD Volume Type
    owners = ["self"]

    filter {
        name   = "state"
        values = ["available"]
    }

    filter {
        name   = "tag:Name"
        values = ["Packer-CNV-Webserver-*"] // defined in the corresponding packer ami definition file
    }

    most_recent = true
}

data "aws_ami" "loadbalancer-ami" {
    owners = ["self"]

    filter {
        name   = "state"
        values = ["available"]
    }

    filter {
        name   = "tag:Name"
        values = ["Packer-CNV-LoadBalancer-*"] // defined in the corresponding packer ami definition file
    }

    most_recent = true
}

resource "aws_elb" "load_balancer_classic" {
  name               = "CNV-project-load-balancer"
  availability_zones = ["us-east-1a"]

  listener {
    instance_port     = 8000
    instance_protocol = "http"
    lb_port           = 80
    lb_protocol       = "http"
  }

  health_check {
    healthy_threshold   = 2
    unhealthy_threshold = 5
    timeout             = 5
    target              = "HTTP:8000/health"
    interval            = 30
  }

  security_groups = ["${aws_security_group.security-group.id}"]
  cross_zone_load_balancing   = true
  connection_draining         = true
  connection_draining_timeout = 400

  tags = {
    Name = "CNV-project-elb"
  }
}

resource "aws_launch_configuration" "auto_scaler_config" {
  name_prefix       = "CNV-project-autoscaler-"
  image_id          = data.aws_ami.webserver-ami.id
  instance_type     = "t2.micro"
  enable_monitoring = true
  security_groups   = ["${aws_security_group.security-group.id}"]
  key_name          = var.ssh_key_pair_name

  lifecycle {
    create_before_destroy = true
  }
}

resource "aws_autoscaling_group" "auto_scaler" {
  // If we reference the launch configuration name in the name of the Auto Scaling group we
  // can force the ASG resource to be inextricably tied to the launch configuration.
  // This follows a blue/green deployment strategy.
  name                      = "${aws_launch_configuration.auto_scaler_config.name}-asg"
  launch_configuration      = aws_launch_configuration.auto_scaler_config.name
  min_size                  = 1
  max_size                  = 2
  availability_zones        = ["us-east-1a"]
  load_balancers            = [aws_elb.load_balancer_classic.id]
  health_check_type         = "ELB"
  health_check_grace_period = 60
  default_cooldown          = 60
  enabled_metrics           = [ "GroupMinSize", "GroupMaxSize", "GroupInServiceInstances", "GroupPendingInstances",
                                "GroupStandbyInstances", "GroupTerminatingInstances", "GroupTotalInstances" ]

  lifecycle {
    create_before_destroy = true
  }
}

resource "aws_autoscaling_policy" "scale_up_policy" {
  name                   = "CNV-project-scale-up-policy"
  scaling_adjustment     = 1
  adjustment_type        = "ChangeInCapacity"
  cooldown               = 60
  autoscaling_group_name = aws_autoscaling_group.auto_scaler.name
}

resource "aws_autoscaling_policy" "scale_down_policy" {
  name                   = "CNV-project-scale-down-policy"
  scaling_adjustment     = -1
  adjustment_type        = "ChangeInCapacity"
  cooldown               = 60
  autoscaling_group_name = aws_autoscaling_group.auto_scaler.name
}

resource "aws_cloudwatch_metric_alarm" "scale_up_metric" {
  alarm_name          = "scaler-alarm-high-cpu"
  alarm_description = "This metric monitors ec2 cpu utilization"
  alarm_actions     = ["${aws_autoscaling_policy.scale_up_policy.arn}"]

  namespace           = "AWS/EC2"
  period              = "60"  // measured in seconds
  evaluation_periods  = "1"
  metric_name         = "CPUUtilization"
  statistic           = "Average"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  threshold           = "60"

  dimensions = {
    AutoScalingGroupName = "${aws_autoscaling_group.auto_scaler.name}"
  }
}

resource "aws_cloudwatch_metric_alarm" "scale_down_metric" {
  alarm_name          = "scaler-alarm-low-cpu"
  alarm_description = "This metric monitors ec2 cpu utilization"
  alarm_actions     = ["${aws_autoscaling_policy.scale_down_policy.arn}"]

  namespace           = "AWS/EC2"
  period              = "60"  // measured in seconds
  evaluation_periods  = "1"
  metric_name         = "CPUUtilization"
  statistic           = "Average"
  comparison_operator = "LessThanOrEqualToThreshold"
  threshold           = "40"

  dimensions = {
    AutoScalingGroupName = "${aws_autoscaling_group.auto_scaler.name}"
  }
}

resource "aws_instance" "loadbalancer-instance" {
    count                  = 1
    ami                    = data.aws_ami.loadbalancer-ami.id
    instance_type          = "t2.micro"
    availability_zone      = "us-east-1a"
    monitoring             = true
    vpc_security_group_ids = ["${aws_security_group.security-group.id}"]
    key_name               = var.ssh_key_pair_name

    tags = {
        Name = format("loadbalancer-%03d", count.index + 1)
    }
}

output "public_dns" {
    value       = aws_elb.load_balancer_classic.dns_name
    description = "The public DNS of the application entry point (AWS)"
}

output "public_dns_custom" {
    value       = "${aws_instance.loadbalancer-instance[0].public_dns}"
    description = "The public DNS of custom load balancer instance"
}
