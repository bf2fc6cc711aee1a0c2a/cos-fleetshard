Feature: Connector Secret UoW Mismatch

  Background:
    Given Await configuration
      | atMost       | 30000   |
      | pollDelay    | 100     |
      | pollInterval | 500     |

  Scenario: augmentation because secret is missing
    Given a Connector with:
      | connector.type.id           | log_sink_0.1                    |
      | desired.state               | ready                           |
      | kafka.bootstrap             | kafka.acme.com:443              |
      | operator.id                 | cos-fleetshard-operator-it      |
      | operator.type               | connector-operator-it           |
      | operator.version            | [1.0.0,2.0.0)                   |

    When deploy connector
    Then the connector exists
     And the connector secret does not exists
     And the connector is in phase "Augmentation"
     And the meters does not have any counter with name "cos.fleetshard.controller.connectors.reconcile.monitor.count"

  Scenario: augmentation because secret UoW is out of sync
    Given a Connector with:
      | connector.type.id           | log_sink_0.1                    |
      | desired.state               | ready                           |
      | kafka.bootstrap             | kafka.acme.com:443              |
      | operator.id                 | cos-fleetshard-operator-it      |
      | operator.type               | connector-operator-it           |
      | operator.version            | [1.0.0,2.0.0)                   |

    When deploy connector
     And deploy secret
     And the connector path ".spec.deployment.connectorResourceVersion" is set to 100
    Then the connector exists
     And the connector secret exists
     And the connector is in phase "Augmentation"
     And the meters does not have any counter with name "cos.fleetshard.controller.connectors.reconcile.monitor.count"