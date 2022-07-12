Feature: Debezium Connector Metrics

  Background:
    Given Await configuration
      | atMost       | 30000   |
      | pollDelay    | 100     |
      | pollInterval | 500     |

  Scenario: deploy
    Given a Connector with:
      | connector.type.id           | debezium-postgres-1.9.4.Final    |
      | desired.state               | ready                            |
      | kafka.bootstrap             | kafka.acme.com:443               |
      | operator.id                 | cos-fleetshard-operator-debezium |
      | operator.type               | debezium-connector-operator      |
      | operator.version            | [1.0.0,2.0.0)                    |
    And with a simple Debezium connector

    When deploy
    Then the connector exists
     And the connector secret exists
     And the connector is in phase "Monitor"
     And the meters has counter "cos.fleetshard.controller.connectors.reconcile.initialization.count" with value greater than or equal to 1
     And the meters has counter "cos.fleetshard.controller.connectors.reconcile.augmentation.count" with value greater than or equal to 1
     And the meters has counter "cos.fleetshard.controller.connectors.reconcile.monitor.count" with value greater than or equal to 1
     And the meters has timer with name "cos.fleetshard.controller.connectors.reconcile.initialization.time"
     And the meters has timer with name "cos.fleetshard.controller.connectors.reconcile.augmentation.time"
     And the meters has timer with name "cos.fleetshard.controller.connectors.reconcile.monitor.time"



