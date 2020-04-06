# Sudoku@Cloud

Cloud Computing and Virtualization 2019-2020, 2nd semester project

## Dependencies

[Packer](https://packer.io/)

[Terraform](https://www.terraform.io/)

Terraform

## How to run

### AWS - Setup Credentials

- Declare your AWS credentials using whichever method you prefer:

  [Setup your AWS credentials file](https://docs.aws.amazon.com/cli/latest/userguide/cli-configure-files.html)

  or

  Provide your credentials through environment variables:

      export AWS_ACCESS_KEY_ID="key-id"
      export AWS_SECRET_ACCESS_KEY="key-secret"

### Packer - Building Images

- Validate packer ami configuration file: (Only necessary for developers)

      packer validate ami-webserver.json

- Create the AWS AMI's using Packer:

      packer build ami-webserver.json

### Terraform - Deploying the Infrastructure

- Download dependencies for any providers defined in the code

      terraform init

- Preview execution plan

      terraform plan

  For the next step, you will be asked to provide the name of an existing AWS ssh key pair name

  If you have a **key_name.pem** file, then the value should be **key_name**

  It is recommended to export environment variable **TF_VAR_ssh-key-pair-name=key_name**

- Deploy resources

      terraform apply

## Usage and testing

- make a request to a server

      curl <server_public_dns>:8000/test

    example:

      curl ec2-3-227-244-216.compute-1.amazonaws.com:8000/test

## Shutdown

- Terminate all deployed resources

      terraform destroy

Other relevant commands:

- Preview resource termination

      terraform plan -destroy

- Destroy a specific resource

      terraform destroy -target <RESOURCE_TYPE.NAME>
