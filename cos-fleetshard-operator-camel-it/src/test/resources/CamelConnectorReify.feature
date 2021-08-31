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
    Then the connector secret exists

    Then the klb exists
     And the klb has annotations containing:
          | cos.bf2.org/deployment.resource.version | 1 |
     And the klb has labels containing:
          | cos.bf2.org/watch         | true |
          | cos.bf2.org/cluster.id    |      |
          | cos.bf2.org/connector.id  |      |
          | cos.bf2.org/deployment.id |      |
     And the klb has an array at path "$.spec.integration.configuration" containing:
          | { "type":"secret" , "value": "${json-unit.ignore}" }            |
          | { "type":"env"    , "value": "QUARKUS_LOG_CONSOLE_JSON=false" } |
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
     And the klb secret has annotations containing:
          | cos.bf2.org/deployment.resource.version | 1 |
     And the klb secret has labels containing:
          | cos.bf2.org/watch         | true |
          | cos.bf2.org/cluster.id    |      |
          | cos.bf2.org/connector.id  |      |
          | cos.bf2.org/deployment.id |      |
    And the klb secret has an entry at path "$.metadata.ownerReferences[0].apiVersion" with value "cos.bf2.org/v1alpha1"
    And the klb secret has an entry at path "$.metadata.ownerReferences[0].kind" with value "ManagedConnector"

