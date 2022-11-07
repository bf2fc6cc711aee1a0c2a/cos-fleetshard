Feature: Connector Deleted And Resynced

  Background:
    Given Await configuration
      | atMost       | 30000   |
      | pollDelay    | 100     |
      | pollInterval | 500     |

  Scenario: Deleted And Resynced
    Given a Connector with:
      | connector.type.id           | log_sink_0.1                    |
      | desired.state               | ready                           |
      | kafka.bootstrap             | kafka.acme.com:443              |
      | operator.id                 | cos-fleetshard-operator-it      |
      | operator.type               | connector-operator-it           |
      | operator.version            | [1.0.0,2.0.0)                   |

    When deploy
    Then the connector exists
     And the connector secret exists
     And the connector configmap exists with labels:
       | cos.bf2.org/operator.type | connector-operator-it |
     And the connector is in phase "Monitor"
     And the deployment is in phase "ready"

    When the connector desired status is set to "stopped"
    Then the connector is in phase "Stopped"

    When deploy
     And wait till the connector has entry in history with phase "Stopped" and conditions:
      | type         | reason      | status | message  |
      | Resync       | Resync      | True   | Resync   |
      | Stopping     | Stopped     | False  | Stopped  |
      | Stop         | Stopped     | True   | Stopped  |
     And the connector is in phase "Stopped"
     And the deployment is in phase "stopped"

