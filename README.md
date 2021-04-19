# cos-fleetshard


## operator

```shell
kubectl apply -f cos-fleetshard-api/src/main/generated/resources/connectorclusters.cos.bf2.org-v1.yml
kubectl apply -f cos-fleetshard-api/src/main/generated/resources/connectors.cos.bf2.org-v1.yml

# build
./mvnw install

# run the control plane mock
./mvnw -pl cos-fleetshard-mock quarkus:dev

# run the operator
./mvnw -pl cos-fleetshard-operator quarkus:dev

# create the cluster
kubectl apply -f examples/my-cluster.yaml

# create a sample connector
curl -XPOST \
  -H "Content-Type: application/json" \
  http://localhost:9090/api/managed-services-api/v1/kafka-connector-clusters/test/connectors \
  -d @examples/my-connector-1.json
```