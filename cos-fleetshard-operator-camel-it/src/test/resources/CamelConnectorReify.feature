Feature: Camel Connector Reify

  Background:
    Given Await configuration
      | atMost       | 30000   |
      | pollDelay    | 100     |
      | pollInterval | 500     |

  Scenario: reify
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
          | trait.camel.apache.org/container.image | quay.io/lburgazzoli/mci:0.1.2-log-sink-0.1 |
     And the klb has labels containing:
          | cos.bf2.org/cluster.id                |                                                           |
          | cos.bf2.org/connector.id              |                                                           |
          | cos.bf2.org/deployment.id             |                                                           |
          | app.kubernetes.io/managed-by          | camel-connector-operator-cos-fleetshard-operator-camel    |
          | app.kubernetes.io/created-by          | camel-connector-operator-cos-fleetshard-operator-camel    |
          | app.kubernetes.io/component           | connector                                                 |
          | app.kubernetes.io/version             | 1                                                         |
          | app.kubernetes.io/part-of             | ${cos.cluster.id}                                         |
          | app.kubernetes.io/name                | ${cos.connector.id}                                       |
          | app.kubernetes.io/instance            | ${cos.deployment.id}                                      | 
     And the klb has an array at path "$.spec.integration.configuration" containing:
          | { "type":"secret" , "value": "${json-unit.ignore}" }            |
          | { "type":"property" , "value": "camel.main.route-controller-backoff-delay=2s" }            |
          | { "type":"property" , "value": "camel.main.route-controller-initial-delay=1s" }            |
          | { "type":"property" , "value": "camel.main.route-controller-backoff-multiplier=2" }        |

    And the klb has an entry at path "$.metadata.ownerReferences[0].apiVersion" with value "cos.bf2.org/v1alpha1"
    And the klb has an entry at path "$.metadata.ownerReferences[0].kind" with value "ManagedConnector"

    Then the klb secret exists
     And the klb secret contains:
          | camel.kamelet.log-sink.multiLine                    | true                       |
          | camel.kamelet.log-sink.showAll                      | true                       |
          | camel.kamelet.managed-kafka-source.bootstrapServers | kafka.acme.com:443         |
          | camel.kamelet.managed-kafka-source.password         |                            |
          | camel.kamelet.managed-kafka-source.user             |                            |
          | camel.kamelet.managed-kafka-source.topic            | dbz_pg.inventory.customers |
          | camel.main.route-controller-supervise-enabled       | true                       |
     And the klb secret has labels containing:
          | cos.bf2.org/cluster.id                |                                                           |
          | cos.bf2.org/connector.id              |                                                           |
          | cos.bf2.org/deployment.id             |                                                           |
          | app.kubernetes.io/managed-by          | camel-connector-operator-cos-fleetshard-operator-camel    |
          | app.kubernetes.io/created-by          | camel-connector-operator-cos-fleetshard-operator-camel    |
          | app.kubernetes.io/component           | connector                                                 |
          | app.kubernetes.io/version             | 1                                                         |
          | app.kubernetes.io/part-of             | ${cos.cluster.id}                                         |
          | app.kubernetes.io/name                | ${cos.connector.id}                                       |
          | app.kubernetes.io/instance            | ${cos.deployment.id}                                      | 
    And the klb secret has an entry at path "$.metadata.ownerReferences[0].apiVersion" with value "cos.bf2.org/v1alpha1"
    And the klb secret has an entry at path "$.metadata.ownerReferences[0].kind" with value "ManagedConnector"

