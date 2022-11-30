Feature: Camel Connector Reify With Annotations

  Background:
    Given Await configuration
      | atMost       | 60000   |
      | pollDelay    | 100     |
      | pollInterval | 500     |

  Scenario: reify with annotations
    Given a Connector with:
      | connector.type.id           | log_sink_0.1                    |
      | desired.state               | ready                           |
      | kafka.bootstrap             | kafka.acme.com:443              |
      | operator.id                 | cos-fleetshard-operator-camel   |
      | operator.type               | camel-connector-operator        |
      | operator.version            | [1.0.0,2.0.0)                   |
    And with sample camel connector

    When deploy
    Then the connector exists
     And the connector secret exists
     And the connector is in phase "Monitor"

    Then the klb exists
     And the klb has annotations containing:
          | camel.apache.org/operator.id                    | ${cos.operator.id}     |
          | trait.camel.apache.org/container.image          | quay.io/foo/bar:latest |
          | trait.camel.apache.org/jvm.enabled              | false                  |
          | trait.camel.apache.org/logging.json             | false                  |
          | trait.camel.apache.org/owner.target-labels      | ${cos.ignore}          |
          | trait.camel.apache.org/kamelets.enabled         | true                   |
          | trait.camel.apache.org/affinity.enabled         | true                   |
     And the klb has labels containing:
          | cos.bf2.org/cluster.id    |      |
          | cos.bf2.org/connector.id  |      |
          | cos.bf2.org/deployment.id |      |
     And the klb has an array at path "$.spec.integration.configuration" containing:
          | { "type":"secret" , "value": "${json-unit.ignore}" }            |

