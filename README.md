# cos-fleetshard


## operator

```shell

#
# install CRDs
#
kubectl apply -f etc/kubernetes/managedconnectoroperators.cos.bf2.org-v1.yml
kubectl apply -f etc/kubernetes/managedconnectorclusters.cos.bf2.org-v1.yml
kubectl apply -f etc/kubernetes/managedconnectors.cos.bf2.org-v1.yml

#
# install connector operator
#
kubectl apply -f etc/examples/camel-connector-operator-1.yaml
kubectl apply -f etc/examples/debezium-connector-operator-1.yaml

#
# create operator config
#
kubectl create configmap cos-fleetshard-config \
  --from-file=etc/kubernetes/app-config-map/application.properties

#
# create operator secret
#  
# NOTE: the file in etc/kubernetes/app-secret/application.properties is
#       only a template, copy it somewhere and adapt the command below
#       and remember not to commit it
#      
kubectl create configmap addon-cos-fleetshard-operator-parameters \
  --from-file=etc/kubernetes/app-secret/application.properties
               
# build
./mvnw install

# export a variable to define the namespace
export KUBERNETES_NAMESPACE=$(oc project -q)

# run the operator locally
./mvnw -pl cos-fleetshard-operator -Plocal
```