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

    And with Debezium connector using "JSON" datashape
    And set connector label "cos.bf2.org/organisation-id" to "20000000"
    And set connector label "cos.bf2.org/pricing-tier" to "essential"
    And set connector annotation "my.cos.bf2.org/connector-group" to "baz"

    When deploy
    And the kc deployment exists
    And the kc secret exists
    And the kc configmap exists
    And the kc svc exists
    And the kc pvc exists

    And the kc has config containing:
      | key.converter                     | org.apache.kafka.connect.json.JsonConverter |
      | value.converter                   | org.apache.kafka.connect.json.JsonConverter |
      | key.converter.schemas.enable      | true                                        |
      | value.converter.schemas.enable    | true                                        |
