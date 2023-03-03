## Tutorial on how to setup a local installation of RHOC

This tutorial will show how to create a local installation of RHOC with a catalog of only Camel connectors (excluding therefore Debezium) and deploy a simple Slack connector.

We'll use the `kas-fleet-manager` repository, ignoring `cos-fleet-manager` because most of the code it's in this repository
and it's easier to setup a system by using a single control plane repository. 
You can consider `cos-fleet-manager` the midstream repository of the control plane project.

`cd kas-fleet-manager`

## Setup the DB

`make db/teardown`

This will fail if the DB doesn't exist

`make db/setup`

We can check a docker container called `kas-fleet-manager-db` has been created with

```shell
docker container ls
make db/login
```

This will login inside the database. With `\d` you can see there are no tables.
We should run the migration to populate the DB with the RHOC tables

To run the RHOC migrations we can use the `internal/connector/test/main/main.go` main instead of the "normal" `cmd/main.go`, which is used for `kas`. 
This `main.go` let us run a RHOC control plane inside the `kas-fleet-manager` repository.

To run the migrations: 

`go run internal/connector/test/main/main.go migrate`

or use the debug action from GoLand specifying `migrate` as a command line argument

We can check the tables are created accordingly with

```shell
make db/login
\d
```

## Setup SSO

We'll setup a local keycloak. This will let us interact with the fleet manager by using the `cos` tool and will handle authentication and authorization between the fleet manager and its agents.

First, let's create a few configuration files

```shell
make keycloak/setup 
make ocm/setup
```

If you want details check the internal Makefile, but this just creates a few empty files that are needed to run the fleet manager.

To setup the local keycloak run these commands (the first command will fail if it's the first time you run this tutorial) 

```shell
make sso/teardown 
make sso/setup
make sso/config
```

Check now with `docker container ls` there should be a container named `mas-sso` based on a keycloak image `quay.io/keycloak/keycloak`.

The script will setup two realms as well called `rhoas` and `rhoas-sre` and a client for each realm with client id `kas-fleet-manager` and as client secret `kas-fleet-manager` in both cases.

We can navigate to the local installation of keycloak by going with a browser to http://localhost:8180/auth/ and insert the default login username: `admin` password: `admin`

Let's edit the client called `kas-fleet-manager` inside the `rhoas` realm by adding a few claims to the token. 
This is currently needed for the local installation but it could be automated in the future in keycloak setup scripts.

Navigate in the UI under the `Rhoas` realm, select `Clients` in the left bar, then the `kas-fleet-manager` client, `Mappers` tab and create with the `Create` button. 
Create two claims of type `Hardcoded claim`

Name: Org Id
Token Claim Name: org_id
Claim Value: organization_id
Claim JSON Type: string

Name: Is Org Admin
Token Claim Name: is_org_admin
Claim Value: true
Claim JSON Type: boolean

These are two hardcoded values that will be added to the login token, and they're essential to interact with control plane. 
If you don't add such values, the control plane will return 401 Unauthorized to their call.

After having set the claims, update the control plane to make sure it's using the created user. 
Write `kas-fleet-manager` as clientId and clientSecret inside `secrets/keycloak-service.clientId` and `secrets/keycloak-service.clientSecret` 

To verify authentication works fine, we'll use the [cos-tools](https://github.com/bf2fc6cc711aee1a0c2a/cos-tools) suite tool.

Start the control plane by running

`go run internal/connector/test/main/main.go serve`

If you try to run some of the commands of `cos-tools` such as

```shell
bin/get-clusters
```
You'll see the messages:

```
Error: Failed to create OCM connection: Not logged in, credentials aren't set, run the 'login' command
curl: (3) URL using bad/illegal format or missing URL
```

`cos-tools` needs some configuration: it needs the user to be authenticated with `ocm`

```shell
ocm login --url http://localhost:8180\
          --token-url=http://localhost:8180/auth/realms/rhoas/protocol/openid-connect/token\
          --client-id=kas-fleet-manager\
          --client-secret=kas-fleet-manager
```

`cos-tools` also needs these environment variables

```shell
export COS_BASE_PATH=localhost:8000
export KAS_BASE_PATH=localhost:8000
```

Where `localhost:8000` is the hostname of the control plane running locally.

Setting these values will authenticate the calls and make `cos-tools` to point to the local control plane.

Start then the control plane either run it from the local action from GoLand specifying `serve` as a command line argument or use

`go run internal/connector/test/main/main.go serve`

And then try run 

`bin/get-clusters`

It should return an empty list of clusters without errors


## Create a cluster

Now we're going to create a local minikube cluster and register it inside the RHOC control plane.
Let's leave `cos-tools` in the terminal with the variables set and move to another window.

To create the cluster, checkout the fleet-shard repository and login inside 

`cd cos-fleetshard`

Create a minikube local install. This will create a local minikube profile called `cos`

```shell
./etc/scripts/start_minikube.sh
```

We can check the profile is installed correctly with 

`minikube profile list`

Install with this command all the CRD and the services needed by RHOC

```shell
kubectl apply -k etc/kubernetes/manifests/overlays/local --server-side --force-conflicts
kubectl apply -f etc/kubernetes/manifests/overlays/local/camel-k/integration-platform.yaml --server-side  --force-conflicts
```

To verify everything was installed correctly, you can run

`kubectl get namespaces`

and verify the `redhat-openshift-connectors` is created.

Inside this namespace, `camel-k` and `strimzi` will be running, you can check them with:

`kubectl -n redhat-openshift-connectors get pods`

After having created the minikube install, we should register it inside the control plane.
Move to the `cos-tools` terminal

```shell
export SUFFIX=$(uuidgen | tr -d '-')
bin/create-cluster "$USER-$SUFFIX"
```

This will return something similar

```json
{
  "id": "cg10fpruns2rbjn4ka20",
  "kind": "ConnectorCluster",
  "href": "/api/connector_mgmt/v1/kafka_connector_clusters/cg10fpruns2rbjn4ka20",
  "owner": "lmolteni",
  "created_at": "2023-03-03T15:44:56.505522+01:00",
  "modified_at": "2023-03-03T15:44:56.505522+01:00",
  "name": "lmolteni-6225FC431494432",
  "annotations": {
    "cos.bf2.org/organisation-id": "432432"
  },
  "status": {
    "state": "disconnected"
  }
}
```

The creation of the cluster also creates a service account with the name of the cluster, if you want to check the created service-account you can use the keycloak
UI and navigate inside the `rhoas` realms and check there will be a new client with the name of the cluster.

The cluster is in `disconnected` state. This means the control plane and the cluster aren't actively communicating.
We'll fix it soon, but first let's generate a few secrets and insert them in the local cluster. 

Gather the id from the previous request (in this case `cg10fpruns2rbjn4ka20`) and run

```shell
kubectl config set-context --current --namespace=redhat-openshift-connectors

bin/create-cluster-secret <CLUSTER_ID>`
```

The first line is needed as `create-cluster-secret` use a global context to know the namespace where to create the secret. 
In this secret there will be a few parameters such as the `clientId` and the `clientSecret` of the service account user
created during the cluster creation. Such parameters will be used by `cos-fleetshard-sync` to communicate with the control plane. 

To verify the secret was created accordingly, run

`kubectl -n redhat-openshift-connectors get secret -o yaml`

In another tab, run the `cos-fleetshard-sync` module. Before running the commands, make sure the local minikube is reachable, by doing

```shell
kubectl config set-context --current --namespace=redhat-openshift-connectors
kubectl get pods
```

You should see all the pods inside the `redhat-openshift-connectors` namespace. 

To run the sync use:

```shell
cd cos-fleetshard/cos-fleetshard-sync

mvn -Dlocal\
    -Dcontrol-plane-base-url="http://localhost:8000"\
    -Dcos.manager.sso-uri="http://127.0.0.1:8180/auth/realms/rhoas"
```

The `control-plane-base-url` should be taken from the secrets created previously, but due to a bug it's being set to `localhost` instead. 
Force it to `http://localhost:8000` to use the local control plane running on the environment.
The `cos.manager.sso-uri` has to use `127.0.0.1` instead of `localhost` due to another bug. It points to the local keycloak running

On the `cos-tools` tab, now check the the cluster is in `ready` state. There should be no errors in the `cos-fleetshard-sync` mvn run.

```shell
❯ ./get-clusters
ID             NAME                    OWNER                STATUS
--             ----                    -----                ------
cg10fpruns2rb  lmolteni-6225FC4314944  lmolteni             ready
```

# Create a local catalogue

To use camel connectors, RHOC control plane needs to know the existing available connectors.  

Make a folder in `kas-fleet-manager` called `catalog` (it will be ignored by `.gitgnore` eventually, otherwise add the folder to `.gitignore`)

Get the connectors metadata

Create a new file in `kas-fleet-manager/catalog/cos-fleet-catalog-metadata/metadata.yaml`
Copy [this part of the file](https://github.com/bf2fc6cc711aee1a0c2a/cos-fleet-manager/blob/main/templates/connector-metadata-camel-template.yaml#L21-L485) into the file. 
Make sure to format it accordingly.

Get the connector definitions

`git clone https://github.com/bf2fc6cc711aee1a0c2a/cos-manifests`

Copy `cos-manifests/connectors/cos-fleet-catalog-camel` to the `kas-fleet-manager/catalog` folder

Delete the `kas-fleet-manager/catalog/cos-fleet-catalog-camel/connectors.json` file

Edit your Goland control plane debug configuration or your `go run` command by adding

```shell
serve \
--connector-catalog=./catalog/cos-fleet-catalog-camel \
--connector-metadata=./catalog/cos-fleet-catalog-metadata
```

i.e.
```shell
go run internal/connector/test/main/main.go serve --connector-catalog=./catalog/cos-fleet-catalog-camel --connector-metadata=./catalog/cos-fleet-catalog-metadata
```

Restart the control plane

# Run the RHOC Camel operator

In another shell window, navigate to `cos-fleetshard/cos-fleetshard-operator-camel` and set the following environment variables

```shell
export COS_OPERATOR_ID=camel-k
export COS_OPERATOR_VERSION=1.0.0
export KUBERNETES_NAMESPACE=redhat-openshift-connectors
```

Now run the operator locally with 

`mvn -Plocal`

The operator will register in the cluster with the name and the namespace provided. Verify it with

```shell
kubectl get managedconnectoroperators.cos.bf2.org -A
NAMESPACE                     NAME      RUNTIME   TYPE                       VERSION
redhat-openshift-connectors   camel-k   camel-k   camel-connector-operator   1.0.0
```

## Create the Slack connector

To create the kafka connector you're going to need a managed kafka cluster first. 

Either user

`rhoas kafka create` 

if you want to create a new one or

```shell
rhoas kafka list
rhoas kafka use
````

if you have an already running Kafka cluster you want to use.

Create the needed kafka topic and the kafka service account first. 
Navigate to a different folder as these commands will create a `./service-acct-credentials.json` file you should avoid putting inside any GitHub repository. 
You'll need to provide a TOPIC_NAME and a SHORT_DESCRIPTION

```shell

cd ~

rhoas kafka topic create --name=<TOPIC_NAME>

rhoas service-account list

rhoas service-account create --output-file=./service-acct-credentials.json --file-format=json --overwrite --short-description=<SHORT_DESCRIPTION>

rhoas service-account list | grep $USER

export SERVICEACCOUNT=$(cat ./service-acct-credentials.json | jq -r '.clientID')

rhoas kafka acl grant-access --consumer --producer --service-account $SERVICEACCOUNT --topic-prefix <TOPIC_NAME>  --group all

```

In the `cos-tools` tab, save this payload in a file called `slack-example-connector.json`, and edit
* Kafka id and URL
* namespace_id (use `cos-tools` `./get-namespaces`)
* topic name, 
* service account client_id and client_secret 
* slack webhook accordingly.
* slack channel

```json
{
  "name": "local-slack-sink",
  "kind": "ConnectorType",
  "channels": [
    "stable"
  ],
  "connector_type_id": "slack_sink_0.1",
  "desired_state": "ready",
  "kafka": {
    "id": "KAFKA_ID",
    "url": "KAFKA_URL"
  },
  "namespace_id": "NAMESPACE_ID",
  "service_account": {
    "client_id": "CLIENT_ID",
    "client_secret": "CLIENT_SECRET"
  },
  "connector": {
    "data_shape": {
      "consumes": {
        "format": "application/octet-stream"
      }
    },
    "kafka_topic": "KAFKA_TOPIC",
    "slack_channel": "SLACK_CHANNEL",
    "slack_webhook_url": "SLACK_WEBHOOK_URL",
    "error_handler": {
      "stop": {}
    }
  }
}

```

Create the connector with 

```shell
./create-connector "$(cat slack-example-connector.json)"
```

You should see no logs inside the operator and the `cos-fleetshard-sync`
And verify it goes to ready state with

```shell
./get-connectors
```


Now you can access the kafka topic with the following command (it needs `kcat` installed, [here's the instructions on how to install](https://github.com/edenhill/kcat#install))

```shell
kcat -t "$KAFKA_TOPIC" -b "$KAFKA_BROKER_URL" \
-X security.protocol=SASL_SSL -X sasl.mechanisms=PLAIN \
-X sasl.username="$KAFKA_CLIENT_ID" \
-X sasl.password="$KAFKA_CLIENT_SECRET" -P
```

Write something, press CTRL-D and you should see the slack message on your provided channel. 



