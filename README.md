# cos-fleetshard


## operator

```shell
kubectl apply -f cos-fleetshard-operator/src/main/kubernetes/managedconnectoroperators.cos.bf2.org-v1.yml
kubectl apply -f cos-fleetshard-operator/src/main/kubernetes/managedconnectorclusters.cos.bf2.org-v1.yml
kubectl apply -f cos-fleetshard-operator/src/main/kubernetes/managedconnectors.cos.bf2.org-v1.yml


kubectl apply -f etc/examples/camel-connector-operator.yaml

# build
./mvnw install

# run the operator
./mvnw -pl cos-fleetshard-operator quarkus:dev

```