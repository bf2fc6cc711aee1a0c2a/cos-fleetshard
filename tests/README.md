# Running the test suite

Integration and e2e test are done with The KUbernetes Test TooL (KUTTL), which provides a declarative approach to testing production-grade Kubernetes operators.

To manually run the test suite, make sure to have kuttl installed, follow https://kuttl.dev/docs/#pre-requisites.

## Running the test suite

```shell
kubectl kuttl test 
```

This will spawn a local kubernetes cluster using [KIND](https://github.com/kubernetes-sigs/kind) and run the tests against it.