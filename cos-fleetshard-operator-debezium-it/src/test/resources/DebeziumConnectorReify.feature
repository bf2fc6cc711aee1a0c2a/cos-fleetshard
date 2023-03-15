Feature: Debezium Connector Reify

  Background:
    Given Await configuration
      | atMost       | 30000   |
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

     And with a simple Debezium connector
     And set connector label "cos.bf2.org/organisation-id" to "20000000"
     And set connector label "cos.bf2.org/pricing-tier" to "essential"
     And set connector annotation "my.cos.bf2.org/connector-group" to "baz"

    When deploy
     And the kc deployment exists
     And the kc secret exists
     And the kc configmap exists
     And the kc svc exists
     And the kc pvc exists

     And the kc deployment satisfy expression '.metadata.labels."cos.bf2.org/pricing-tier" == "essential"'
     And the kc deployment satisfy expression '.metadata.labels."cos.bf2.org/organisation-id" == "20000000"'
     And the kc deployment satisfy expression '.metadata.annotations."my.cos.bf2.org/connector-group" == "baz"'
     And the kc deployment satisfy expression '.spec.template.metadata.labels."cos.bf2.org/pricing-tier" == "essential"'
     And the kc deployment satisfy expression '.spec.template.metadata.labels."cos.bf2.org/organisation-id" == "20000000"'
     And the kc deployment satisfy expression '.spec.template.metadata.annotations."my.cos.bf2.org/connector-group" == "baz"'
     And the kc deployment satisfy expression '.metadata.ownerReferences[0].apiVersion == "cos.bf2.org/v1alpha1"'
     And the kc deployment satisfy expression '.metadata.ownerReferences[0].kind == "ManagedConnector"'
     And the kc deployment satisfy expression '.spec.template.spec.imagePullSecrets[0].name == "addon-pullsecret"'
     And the kc deployment satisfy expression '.spec.template.spec.containers[0] | any(.env[]; .name == "CONNECTOR_PARAMS_CHECKSUM")'
     And the kc deployment satisfy expression '.spec.template.spec.containers[0] | any(.env[]; .name == "CONNECTOR_RESOURCES_CHECKSUM")'

     And the kc has configmap containing logging config
     And the kc has configmap containing metrics config

     And the kc has config containing:
       | admin.sasl.mechanism                               | PLAIN                                |
       | admin.sasl.jaas.config                             | org.apache.kafka.common.security.plain.PlainLoginModule required username="${kafka.client.id}" password="${kafka.client.secret}";  |
       | admin.security.protocol                            | SASL_SSL                             |
       | producer.sasl.mechanism                            | PLAIN                                |
       | producer.sasl.jaas.config                          | org.apache.kafka.common.security.plain.PlainLoginModule required username="${kafka.client.id}" password="${kafka.client.secret}";  |
       | producer.security.protocol                         | SASL_SSL                             |
       | sasl.mechanism                                     | PLAIN                                |
       | sasl.jaas.config                                   | org.apache.kafka.common.security.plain.PlainLoginModule required username="${kafka.client.id}" password="${kafka.client.secret}";  |
       | security.protocol                                  | SASL_SSL                             |
       | key.converter                                      | org.apache.kafka.connect.json.JsonConverter |
       | value.converter                                    | org.apache.kafka.connect.json.JsonConverter |
       | offset.flush.interval.ms                           | 10000                             |
       | offset.storage.file.filename                       | /etc/rhoc/data/connector.offsets  |
       | offset.storage.replication.factor                  | -1                                |
       | topic.creation.default.replication.factor          | -1                                |
       | topic.creation.default.partitions                  | -1                                |
       | topic.creation.default.cleanup.policy              | compact                           |
       | topic.creation.default.delete.retention.ms         | 2678400000                        |

    And the kctr has config containing:
      | name                   | ${cos.connector.id}                                  |
      | tasks.max              | 1                                                    |
      | tasks                  | 1                                                    |
      | connector.class        | io.debezium.connector.postgresql.PostgresConnector   |

    And the kctr has config from connector

    Then the meters has entries with name matching "cos.fleetshard.controller.connectors.reconcile\..*" and tags:
      | foo                       | bar       |
      | connector_group           | baz       |
      | organisation_id           | 20000000  |
      | pricing_tier              | essential |

    Then the meters has entries with name matching "cos.fleetshard.controller.event.operators.operand\..*" and tags:
      | foo                       | bar       |
      | connector_group           | baz       |
      | organisation_id           | 20000000  |
      | pricing_tier              | essential |