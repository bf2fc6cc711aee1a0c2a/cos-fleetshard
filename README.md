# cos-fleetshard

```shell

#
# install CRDs
#
kubectl apply -f etc/kubernetes/managedconnectoroperators.cos.bf2.org-v1.yml
kubectl apply -f etc/kubernetes/managedconnectors.cos.bf2.org-v1.yml

#
# install connector operator
#
kubectl apply -f etc/examples/camel-connector-operator-1.yaml
kubectl apply -f etc/examples/debezium-connector-operator-1.yaml
```

[sink](cos-fleetshard-sync/README.md)

[operator](cos-fleetshard-operator/README.md)
