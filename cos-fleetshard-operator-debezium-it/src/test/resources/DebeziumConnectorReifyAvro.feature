Feature: Debezium Connector Reify

  Background:
    Given Await configuration
      | atMost       | 60000   |
      | pollDelay    | 100     |
      | pollInterval | 500     |

  Scenario: reify
    Given a Connector with:
      | connector.type.id           | debezium-postgres-1.9.4.Final    |
      | desired.state               | ready                            |
      | kafka.bootstrap             | kafka.acme.com:443               |
      | operator.id                 | cos-fleetshard-operator-debezium |
      | operator.type               | debezium-connector-operator      |
      | operator.version            | [1.0.0,2.0.0)                    |
    And with Debezium connector using "AVRO" datashape

    When deploy
    Then the connector exists
     And the connector secret exists
     And the connector is in phase "Monitor"

    Then the kc exists
     And the kc has labels containing:
       | cos.bf2.org/cluster.id           | ${cos.cluster.id}             |
       | cos.bf2.org/connector.id         | ${cos.connector.id}           |
       | cos.bf2.org/deployment.id        | ${cos.deployment.id}          |
       | app.kubernetes.io/managed-by     | ${cos.operator.id}            |
       | app.kubernetes.io/created-by     | ${cos.operator.id}            |
       | app.kubernetes.io/component      | cos-connector                 |
       | app.kubernetes.io/version        | 1                             |
       | app.kubernetes.io/part-of        | ${cos.cluster.id}             |
       | app.kubernetes.io/name           | ${cos.connector.id}           |
       | app.kubernetes.io/instance       | ${cos.deployment.id}          |

     And the kc has an entry at path "$.metadata.ownerReferences[0].apiVersion" with value "cos.bf2.org/v1alpha1"
     And the kc has an entry at path "$.metadata.ownerReferences[0].kind" with value "ManagedConnector"
     And the kc has an entry at path "$.spec.authentication.passwordSecret.secretName" with value "${cos.managed.connector.name}-config"
     And the kc has an entry at path "$.spec.authentication.passwordSecret.password" with value "_kafka.client.secret"
     And the kc has an entry at path "$.spec.image" with value "quay.io/rhoas/cos-connector-debezium-postgres@sha256:d64ee218a912c27b06350290fad003acf196b7739c5a3ba16426a3cdef0b21f2"
     And the kc has an entry at path "$.spec.template.pod.imagePullSecrets[0].name" with value "addon-pullsecret"
    And the kc has an entry at path "$.spec.metricsConfig.type" with value "jmxPrometheusExporter"
    And the kc has an entry at path "$.spec.metricsConfig.valueFrom.configMapKeyRef.name" with value "${cos.managed.connector.name}-metrics"
    And the kc has an entry at path "$.spec.metricsConfig.valueFrom.configMapKeyRef.key" with value "kafka_connect_metrics.yml"
    And the kc has the correct metrics config map
     And the kc has config containing:
       | config.providers                  | file,dir                             |
       | config.storage.replication.factor | -1                                   |
       | config.storage.topic              | ${cos.managed.connector.name}-config |
       | offset.storage.topic              | ${cos.managed.connector.name}-offset |
       | status.storage.topic              | ${cos.managed.connector.name}-status |
       | group.id                          | ${cos.managed.connector.name}        |
       | connector.secret.name             | ${cos.managed.connector.name}-config |
       | connector.secret.checksum         | ${cos.ignore}                        |
       | key.converter                     | io.apicurio.registry.utils.converter.AvroConverter                                                                |
       | value.converter                   | io.apicurio.registry.utils.converter.AvroConverter                                                                |
       | key.converter.apicurio.auth.client.id         |  ${kafka.client.id}      |
       | value.converter.apicurio.auth.client.id       |  ${kafka.client.id}      |
       | key.converter.apicurio.auth.client.secret     |  ${dir:/opt/kafka/external-configuration/connector-configuration:_kafka.client.secret}                    |
       | value.converter.apicurio.auth.client.secret   |  ${dir:/opt/kafka/external-configuration/connector-configuration:_kafka.client.secret}                    |

    Then the kctr exists
     And the kctr has labels containing:
       | cos.bf2.org/cluster.id           | ${cos.cluster.id}             |
       | cos.bf2.org/connector.id         | ${cos.connector.id}           |
       | cos.bf2.org/deployment.id        | ${cos.deployment.id}          |
       | strimzi.io/cluster               | ${cos.managed.connector.name} |
       | app.kubernetes.io/managed-by     | ${cos.operator.id}            |
       | app.kubernetes.io/created-by     | ${cos.operator.id}            |
       | app.kubernetes.io/component      | cos-connector                 |
       | app.kubernetes.io/version        | 1                             |
       | app.kubernetes.io/part-of        | ${cos.cluster.id}             |
       | app.kubernetes.io/name           | ${cos.connector.id}           |
       | app.kubernetes.io/instance       | ${cos.deployment.id}          |

     And the kctr has an entry at path "$.metadata.ownerReferences[0].apiVersion" with value "cos.bf2.org/v1alpha1"
     And the kctr has an entry at path "$.metadata.ownerReferences[0].kind" with value "ManagedConnector"
     And the kctr has an entry at path "$.spec.pause" with value false
     And the kctr has an entry at path "$.spec.tasksMax" with value 1
     And the kctr has an entry at path "$.spec.class" with value "io.debezium.connector.postgresql.PostgresConnector"
     And the kctr has config containing:
       | database.password           | ${file:/opt/kafka/external-configuration/connector-configuration/debezium-connector.properties:database.password} |
       | plugin.name                 | pgoutput                                                                                                          |
