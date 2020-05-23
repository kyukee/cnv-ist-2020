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

- Before building the webserver make sure you have the the necessary dependencies in webserver/org/json and webserver/org/apache. These are provided in the course page but can also be downloaded loacally by running the following:

      cd webserver
      mvn clean compile

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

### Terminal setup - Quickstart

What variables do you need if you already have a credentials file:

    export CREDENTIALS_FILE="$HOME/.aws/credentials"
    export TF_VAR_ssh_key_pair_name=CNV-2020-project-educate.pem

## Usage and testing

When requesting from a **load balancer**, use port 80.

When requesting directly from a **server**, use port 8000.

- make a request

      curl <public_dns>:8000/sudoku?s=<strategy>&un=<max_unassigned_entries>&n1=<puzzle_lines>&n2=<puzzle_columns>&i=<puzzle_name>

    example:

      curl http://ec2-3-227-244-216.compute-1.amazonaws.com:8000/sudoku?s=BFS&un=81&n1=9&n2=9&i=SUDOKU_PUZZLE_9x19_101

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

## Architecture description

This project uses terraform to automatically deploy AWS infrastructure. All the infrastructure configuration can be seen in at **infrastructure/aws-setup.tf**.

For the servers, we build a base AWS AMI with all the commom dependencies between the webserver, load balancer and auto scaler modules. Then each module gets its own AMI with its own code module.
Some modules are used by several AMI's like the DynamoDB client.

At this stage, for the checkpoint delivery, the load balancer and auto scaler being used are the default AWS ones.

There is some code written for the load balancer module but it is not functional or being used by any servers.

### Project structure

This project follow the guide specification.

The instrumentation we are using is the number of basic blocks.

The instrumented classes write metrics to a Database class (in a Map) in the server and then the server reads the content of that class and writes it to DynamoDB.

The load balancer can then read those metrics from DynamoDB and make its estimates.

The DynamoDB client has been tested and is known to be able to write to DynamoDB, but there haven't been any reading tests yet, even though the code implements that feature.

### Load balancer pseudocode

    DEFAULT_ESTIMATE = ...
    MAX_SERVER_LOAD = ...

    List<Server> servers_list

    Server {
        total_load
        url
        Map<id, ServerRequest>
    }

    ServerRequest {
        query
        query_estimate
        start_time
    }

    Estimate {
        load
        duration
    }

    Estimate getEstimateFromMetrics(metrics_list):

        load = average load from metrics_list
        duration = average duration from metrics_list

        estimate = new Estimate(load, duration)
        return estimate

    Estimate estimateCost( Query query ):

        metrics_list = MSS.get(query)

        if (metrics_list is empty):
            return DEFAULT_ESTIMATE

        return getEstimateFromMetrics(metrics_list)

    Server getServerWithLowestLoad( Request request ):
        min_load_server = server_list.getFirst
        min_load = MAX_SERVER_LOAD

        for each server in servers_list:
            load = 0

            for each server_request in server:

                // check if request is finished, according to its estimate
                start_time = server_request.start_time
                current_time = get_current_time()
                duration = server_request.estimate.duration

                time_left = start_time + duration - current_time

                if ( time_left > 0):
                    load += server_request.estimate.load

            if (load < min_load_server):
                min_load = load
                min_load_server = server

        return min_load_server

    void receiveRequest( Request client_request ):

        // get query
        query = client_request.getQuery()

        estimate = estimateCost(query)

        min_load_server = getServerWithLowestLoad()

        // increase estimate if there already are queries running on the server
        // given n = number of already running queries, the penalty increases by a factor of n squared
        num_running_queries = min_load_server.requests.size()
        estimate += num_running_queries * ESTIMATE_MULTI_QUERIES_PENALTY

        // save request data in the load balancer
        new_server_request = new ServerRequest(query, estimate, get_current_time())
        min_load_server.requests.put(client_request.id, new_server_request)

        // forward query to a server
        response = server.url.send(query)

        min_load_server.requests.delete(client_request.id)

        return response

### Auto scaler pseudocode

see report
