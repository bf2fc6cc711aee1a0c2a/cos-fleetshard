Feature: Debezium Connector Reify

  Background:
    Given Await configuration
      | atMost       | 30000   |
      | pollDelay    | 100     |
      | pollInterval | 500     |

  Scenario: reify with configmap
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
     And the connector configmap exists with labels:
      | cos.bf2.org/operator.type | debezium-connector-operator |
     And the connector is in phase "Monitor"

    Then the kc exists
    Then the kctr exists

    When set configmap to:
      | io.debezium              | DEBUG     |
      | org.apache.kafka.connect | DEBUG     |
      | bad.property             | any-value |

    And the connector is in phase "Monitor"

    And the kc has an entry at path '$.spec.logging.type' with value "inline"
    And the kc has an entry at path '$.spec.logging.loggers.["io.debezium"]' with value "DEBUG"
    And the kc has an entry at path '$.spec.logging.loggers.["org.apache.kafka.connect"]' with value "DEBUG"
    And the kc has an entry at path '$.spec.logging.loggers.["bad.property"]' with value "any-value"