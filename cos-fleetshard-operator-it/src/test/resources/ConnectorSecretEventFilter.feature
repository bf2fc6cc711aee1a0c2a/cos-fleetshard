Feature: Connector Event Filter

  Background:
    Given Await configuration
      | atMost       | 30000   |
      | pollDelay    | 100     |
      | pollInterval | 500     |

  Scenario: filter events on a monitored resource
    Given a Connector with:
      | connector.type.id           | log_sink_0.1                    |
      | desired.state               | ready                           |
      | kafka.bootstrap             | kafka.acme.com:443              |
      | operator.id                 | cos-fleetshard-operator-it      |
      | operator.type               | connector-operator-it           |
      | operator.version            | [1.0.0,2.0.0)                   |

    When deploy
    Then the connector exists
    Then the connector secret exists
    Then wait till meters has counter "cos.fleetshard.controller.event.secrets.count" with value equal to 1
     And the connector is in phase "Monitor"
     And save the meters value of counter "cos.fleetshard.controller.connectors.reconcile.monitor.count"

    When the connector secret has labels:
      | foo | bar |
    Then wait till meters has counter "cos.fleetshard.controller.event.secrets.count" with value equal to 2
     And the meters value of counter "cos.fleetshard.controller.connectors.reconcile.monitor.count" has not changed