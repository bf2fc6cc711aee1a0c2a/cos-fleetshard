# cos-fleetshard-sync


## configure

- create application config

```shell
kubectl create configmap cos-fleetshard-operator-config \
  --from-file=../etc/kubernetes/operator/app-config-map/application.properties
```

- create application secret (note that the file in etc/kubernetes/sync/app-secret/application.properties
  is only a template, copy it somewhere and adapt the command below

```shell
kubectl create secret generic cos-fleetshard-operator-config \
  --from-file=../etc/kubernetes/operator/app-secret/application.properties
```

## local profile

Start Quarkus in dev mode and read the application configuration from the current namespace.

```shell
mvn -Dlocal
```

By default, the application searches for
- **Secret**: cos-fleetshard-operator-config
- **ConfigMap**: cos-fleetshard-operator-config

To change the default values, use the following system properties:
- quarkus.kubernetes-config.namespace
- quarkus.kubernetes-config.secrets
- quarkus.kubernetes-config.config-maps

## configuration options

| Property                                  | Default              | Description                                                 |
|-------------------------------------------|----------------------|-------------------------------------------------------------|
| cos.connectors.resync.interval            | 60s                  | the interval between forces reconcile loop                  |
