# cos-fleetshard-sync


## configure

- create application config

```shell
kubectl create configmap cos-fleetshard-sync-config \
  --from-file=../etc/kubernetes/sync/app-config-map/application.properties
```
 
- create application secret (note that the file in etc/kubernetes/sync/app-secret/application.properties 
  is only a template, copy it somewhere and adapt the command below

```shell
kubectl create secret generic cos-fleetshard-sync-config \
  --from-file=../etc/kubernetes/sync/app-secret/application.properties
```

## local profile

Start Quarkus in dev mode and read the application configuration from the current namespace.

```shell
mvn -Dlocal
```

To use a different set of configmaps/secrets:

```shell
mvn quarkus:dev -Plocal \
  -Dquarkus.kubernetes-config.secrets=cos-fleetshard-sync-config-1 \
  -Dquarkus.kubernetes-config.config-maps=cos-fleetshard-sync-config-1
```

By default, the application searches for
- **Secret**: cos-fleetshard-sync-config
- **ConfigMap**: cos-fleetshard-sync-config

To change the default values, use the following system properties:
- quarkus.kubernetes-config.namespace
- quarkus.kubernetes-config.secrets
- quarkus.kubernetes-config.config-maps

## configuration options

| Property                                  | Default              | Description                                                 |
|-------------------------------------------|----------------------|-------------------------------------------------------------|
| cos.connectors.namespace                  | KUBERNETES_NAMESPACE | the namespace where connectors should be created            |
| cos.connectors.poll.interval              | 15s                  | the interval between pool for new deployments               |
| cos.connectors.poll.resync.interval       | 60s                  | the interval between full deployments re-sync               |
| cos.connectors.provisioner.queue.timeout  | 15s                  | the wait timeout for the internal event queue               |
| cos.connectors.status.sync.observe        | true                 | enable/disable observing resources to trigger status update |
| cos.connectors.status.resync.interval     | 60s                  | the interval between full connector status re-sync          |
| cos.connectors.status.queue.timeout       | 15s                  | the wait timeout for the internal event queue               |
| cos.cluster.status.sync.interval          | 60s                  | the interval between cluster status sync                    |
| mas-sso-base-url                          |                      | sso base url                                                |
| mas-sso-realm                             |                      | sso realm                                                   |
| client-id                                 |                      | client id to access control plane                           |
| client-secret                             |                      | client secret to access control plane                       |
| control-plane-base-url                    |                      | control plane base url                                      |
| cluster-id                                |                      | the connectors cluster id                                   |

