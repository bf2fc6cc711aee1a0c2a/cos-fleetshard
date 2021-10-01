Feature: Camel Connector Metrics

  Background:
    Given Await configuration
      | atMost       | 30000   |
      | pollDelay    | 100     |
      | pollInterval | 500     |

  Scenario: deploy
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
     And the meters has counter "cos.fleetshard.controller.connectors.reconcile.initialization.count" with value greater than or equal to 1
     And the meters has counter "cos.fleetshard.controller.connectors.reconcile.augmentation.count" with value greater than or equal to 1
     And the meters has counter "cos.fleetshard.controller.connectors.reconcile.monitor.count" with value greater than or equal to 1
     And the meters has timer "cos.fleetshard.controller.connectors.reconcile.initialization.time"
     And the meters has timer "cos.fleetshard.controller.connectors.reconcile.augmentation.time"
     And the meters has timer "cos.fleetshard.controller.connectors.reconcile.monitor.time"

  Scenario: stop
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

    When the connector desired status is set to "stopped"
    Then the connector is in phase "Stopped"
     And the meters has counter "cos.fleetshard.controller.connectors.reconcile.initialization.count" with value greater than or equal to 1
     And the meters has counter "cos.fleetshard.controller.connectors.reconcile.augmentation.count" with value greater than or equal to 1
     And the meters has counter "cos.fleetshard.controller.connectors.reconcile.stopping.count" with value greater than or equal to 1
     And the meters has counter "cos.fleetshard.controller.connectors.reconcile.stopped.count" with value greater than or equal to 1
     And the meters has timer "cos.fleetshard.controller.connectors.reconcile.initialization.time"
     And the meters has timer "cos.fleetshard.controller.connectors.reconcile.augmentation.time"
     And the meters has timer "cos.fleetshard.controller.connectors.reconcile.stopping.time"
     And the meters has timer "cos.fleetshard.controller.connectors.reconcile.stopped.time"

  Scenario: delete
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
    Then the klb secret exists

    When the connector desired status is set to "deleted"
    Then the connector is in phase "Deleted"
    Then the connector secret exists
    Then the klb does not exists
    Then the klb secret does not exists
     And the meters has counter "cos.fleetshard.controller.connectors.reconcile.initialization.count" with value greater than or equal to 1
     And the meters has counter "cos.fleetshard.controller.connectors.reconcile.augmentation.count" with value greater than or equal to 1
     And the meters has counter "cos.fleetshard.controller.connectors.reconcile.deleting.count" with value greater than or equal to 1
     And the meters has counter "cos.fleetshard.controller.connectors.reconcile.deleted.count" with value greater than or equal to 1
     And the meters has timer "cos.fleetshard.controller.connectors.reconcile.initialization.time"
     And the meters has timer "cos.fleetshard.controller.connectors.reconcile.augmentation.time"
     And the meters has timer "cos.fleetshard.controller.connectors.reconcile.deleting.time"
     And the meters has timer "cos.fleetshard.controller.connectors.reconcile.deleted.time"

