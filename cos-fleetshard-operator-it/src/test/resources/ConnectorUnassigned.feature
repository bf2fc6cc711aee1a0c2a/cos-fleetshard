Feature: Deleted Stopped And Resynced

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

    When the connector desired status is set to "unassigned"
    Then the connector is in phase "Deleted"

    When deploy
     And wait till the connector has entry in history with phase "Deleted" and conditions:
      | type         | reason      | status | message  |
      | Deleting     | Deleted     | False  | Deleted  |
      | Deleted      | Deleted     | True   | Deleted  |
     And the connector is in phase "Deleted"
     And the deployment is in phase "deleted"

