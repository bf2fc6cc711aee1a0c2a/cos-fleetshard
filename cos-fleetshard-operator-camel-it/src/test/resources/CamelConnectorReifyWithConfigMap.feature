Feature: Camel Connector Reify With ConfigMap

  Background:
    Given Await configuration
      | atMost       | 30000 |
      | pollDelay    | 100   |
      | pollInterval | 500   |

  Scenario: reify config map
    Given a Connector with:
      | connector.type.id   | log_sink_0.1                  |
      | desired.state       | ready                         |
      | kafka.bootstrap     | kafka.acme.com:443            |
      | kafka.client.id     | kcid                          |
      | kafka.client.secret | ${cos.uid}                    |
      | operator.id         | cos-fleetshard-operator-camel |
      | operator.type       | camel-connector-operator      |
      | operator.version    | [1.0.0,2.0.0)                 |
    And with sample camel connector

    When deploy

    Then the connector exists
    And the connector secret exists
    And the connector configmap exists with labels:
      | cos.bf2.org/operator.type | camel-connector-operator |
    And the connector is in phase "Monitor"
    And the klb exists
    And the klb secret exists

    When set configmap to:
      | quarkus.log.level                           | ALL       |
      | quarkus.log.min-level                       | ALL       |
      | quarkus.log.category."org.apache".level     | DEBUG     |
      | quarkus.log.category."org.apache".min-level | DEBUG     |
      | bad.property                                | any-value |

    Then the klb secret contains:
      | quarkus.log.level                           | ALL       |
      | quarkus.log.min-level                       | ALL       |
      | quarkus.log.category."org.apache".level     | DEBUG     |
      | quarkus.log.category."org.apache".min-level | DEBUG     |
      | bad.property                                | any-value |