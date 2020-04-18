# Sudoku@Cloud

Cloud Computing and Virtualization 2019-2020, 2nd semester project

## Dependencies

[Packer](https://packer.io/)

[Terraform](https://www.terraform.io/)

## How to run

### AWS - Setup Credentials

- Declare your AWS credentials using whichever method you prefer:

  [Setup your AWS credentials file](https://docs.aws.amazon.com/cli/latest/userguide/cli-configure-files.html)

  or

  Provide your credentials through environment variables:

      export AWS_ACCESS_KEY_ID="key-id"
      export AWS_SECRET_ACCESS_KEY="key-secret"

- You may also configure the credentials profile to use by setting the AWS_PROFILE environment variable:

      export AWS_PROFILE="profile"

### Packer - Building Images

- You need to specify an aws credentials file in an environment variable:

      export CREDENTIALS_FILE="$HOME/.aws/credentials"

    or use a command line argument: (needs an absolute path to the file)

      packer build -var 'credentials_file=/path/to/credentials' ami-base.json

- **Warning** The provided AWS credentials file will be copied to the AWS instances and used by them for their work.

  Also, if using AWS Educate credentials, pay attention to their expiration time.

- Validate packer ami configuration file: (Mostly for developers)

      packer validate ami-<module>.json

- Create the AWS AMI's using Packer:

      packer build ami-base.json
      packer build ami-webserver.json
      packer build ami-scaler.json
      packer build ami-balancer.json

### Terraform - Deploying the Infrastructure

- Download dependencies for any providers defined in the code

      terraform init

- Preview execution plan

      terraform plan

- Deploy resources

  For the next command, you will be asked to provide the name of an existing AWS ssh key pair

  If you have a **key_name.pem** file, then the value should be **key_name**

  It is recommended to export environment variable **TF_VAR_ssh_key_pair_name=key_name**

      terraform apply

## Usage and testing

When requesting from a **load balancer**, use port 80.

When requesting directly from a **server**, use port 8000.

- make a request

      curl <public_dns>:8000/sudoku?s=<strategy>&un=<max_unassigned_entries>&n1=<puzzle_lines>&n2=<puzzle_columns>&i=<puzzle_name>

    example:

      curl ec2-3-227-244-216.compute-1.amazonaws.com:8000/sudoku?s=BFS&un=81&n1=9&n2=9&i=SUDOKU_PUZZLE_9x19_101

- look at the server logs (from home directory)

      tail -f logs/server.log

## Shutdown

- Terminate all deployed resources

      terraform destroy

Other relevant commands:

- Preview resource termination

      terraform plan -destroy

- Destroy a specific resource

      terraform destroy -target <RESOURCE_TYPE.NAME>
