# cos-fleetshard


## operator

```shell
./mvnw install
kubectl apply -f cos-fleetshard-api-camel/target/classes/META-INF/fabric8/camelconnectors.cos.bf2.org-v1.yml
./mvnw -pl cos-fleetshard-operator quarkus:dev
```