Feature: Camel Connector Metrics

  Background:
    Given Await configuration
      | atMost       | 30000   |
      | pollDelay    | 100     |
      | pollInterval | 500     |

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
     And the connector is in phase "Monitor"

     And the meters has counter "cos.fleetshard.controller.connectors.reconcile.initialization.count" with value greater than or equal to 1
     And the meters has counter "cos.fleetshard.controller.connectors.reconcile.augmentation.count" with value greater than or equal to 1
     And the meters has counter "cos.fleetshard.controller.connectors.reconcile.monitor.count" with value greater than or equal to 1
     And the meters has counter with name "cos.fleetshard.controller.connectors.reconcile.monitor.count" and tags:
       | cos.operator.id   | ${cos.operator.id}   |
       | cos.connector.id  | ${cos.connector.id}  |
       | cos.deployment.id | ${cos.deployment.id} |

     And the meters has timer with name "cos.fleetshard.controller.connectors.reconcile.initialization.time"
     And the meters has timer with name "cos.fleetshard.controller.connectors.reconcile.augmentation.time"
     And the meters has timer with name "cos.fleetshard.controller.connectors.reconcile.monitor.time" and tags:
       | cos.operator.id   | ${cos.operator.id}   |
       | cos.connector.id  | ${cos.connector.id}  |
       | cos.deployment.id | ${cos.deployment.id} |

