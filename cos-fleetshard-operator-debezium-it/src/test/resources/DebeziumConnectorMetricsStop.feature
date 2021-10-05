Feature: Camel Connector Metrics

  Background:
    Given Await configuration
      | atMost       | 30000   |
      | pollDelay    | 100     |
      | pollInterval | 500     |

  Scenario: stop
    Given a Connector with:
      | connector.type.id           | debezium-postgres-1.5.0.Final    |
      | desired.state               | ready                            |
      | kafka.bootstrap             | kafka.acme.com:443               |
      | operator.id                 | cos-fleetshard-operator-debezium |
      | operator.type               | debezium-connector-operator      |
      | operator.version            | [1.0.0,2.0.0)                    |
    And with sample debezium connector

    When deploy
    Then the connector exists
     And the connector secret exists
     And the connector is in phase "Monitor"

    When the connector desired status is set to "stopped"
     And the kctr status is set to "PAUSED"
    Then the connector is in phase "Stopped"
     And the meters has counter "cos.fleetshard.controller.connectors.reconcile.initialization.count" with value greater than or equal to 1
     And the meters has counter "cos.fleetshard.controller.connectors.reconcile.stopping.count" with value greater than or equal to 1
     And the meters has counter "cos.fleetshard.controller.connectors.reconcile.stopped.count" with value greater than or equal to 1
     And the meters has timer with name "cos.fleetshard.controller.connectors.reconcile.initialization.time"
     And the meters has timer with name "cos.fleetshard.controller.connectors.reconcile.stopping.time"
     And the meters has timer with name "cos.fleetshard.controller.connectors.reconcile.stopped.time"



