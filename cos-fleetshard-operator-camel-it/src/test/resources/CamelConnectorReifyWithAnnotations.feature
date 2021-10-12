Feature: Camel Connector Reify With Annotations

  Background:
    Given Await configuration
      | atMost       | 30000   |
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
    And with shard meta:
      """
      {
          "connector_image": "quay.io/lburgazzoli/mci:0.1.2-log-sink-0.1",
          "connector_type": "sink",
          "kamelets": {
            "connector": "log-sink",
            "kafka": "managed-kafka-source"
          },
          "annotations": {
            "trait.camel.apache.org/container.image": "quay.io/foo/bar",
            "trait.camel.apache.org/container.request-memory": "256Mi"
          }
      }
      """

    When deploy
    Then the connector exists
     And the connector secret exists
     And the connector is in phase "Monitor"

    Then the klb exists
     And the klb has annotations containing:
          | trait.camel.apache.org/container.image          | quay.io/foo/bar |
          | trait.camel.apache.org/container.request-memory | 256Mi           |
          | trait.camel.apache.org/kamelets.enabled         | false           |
          | trait.camel.apache.org/jvm.enabled              | false           |
          | trait.camel.apache.org/logging.json             | false           |
          | trait.camel.apache.org/owner.target-labels      | ${cos.ignore}   |
     And the klb has labels containing:
          | cos.bf2.org/cluster.id    |      |
          | cos.bf2.org/connector.id  |      |
          | cos.bf2.org/deployment.id |      |
     And the klb has an array at path "$.spec.integration.configuration" containing:
          | { "type":"secret" , "value": "${json-unit.ignore}" }            |

