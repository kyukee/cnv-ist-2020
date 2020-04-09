provider "aws" {
    region     = "us-east-1"
}

variable "ssh_key_pair_name" {
    description = "The name of the key you will use for ssh access. If you have a <key_name.pem> file, then the value should be <key_name> \nIt is recommended to export environment variable TF_VAR_ssh_key_pair_name=key_name"
}

resource "aws_instance" "webserver-instance" {
    count                  = 1
    ami                    = data.aws_ami.webserver-ami.id
    instance_type          = "t2.micro"
    availability_zone      = "us-east-1a"
    monitoring             = true
    vpc_security_group_ids = ["${aws_security_group.webserver-security.id}"]
    key_name               = var.ssh_key_pair_name

    tags = {
        Name = format("webserver-%03d", count.index + 1)
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
        values = ["Packer-CNV-Webserver"] // defined in the corresponding packer ami definition file
    }

    most_recent = true
}

resource "aws_security_group" "webserver-security" {
    name = "webserver"

    ingress {
        from_port   = 8000
        to_port     = 8000
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

output "public_ip" {
    value       = "${aws_instance.webserver-instance[0].public_ip}"
    description = "The public IP of the web server"
}

output "public_dns" {
    value       = "${aws_instance.webserver-instance[0].public_dns}"
    description = "The public DNS of the web server"
}
