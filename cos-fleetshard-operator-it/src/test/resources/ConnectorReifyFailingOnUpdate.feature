Feature: Connector Reify Failing On Update

  Background:
    Given Await configuration
      | atMost       | 30000   |
      | pollDelay    | 100     |
      | pollInterval | 500     |

  Scenario: augmentation failed on update
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
     And the connector is in phase "Monitor"
     And the deployment is in phase "ready"

    When deploy with secret data:
      | reify.fail | failure |
    Then the connector exists
     And the connector secret exists
     And the connector is in phase "Error"
     And the deployment is in phase "failed"
     And the connector has conditions:
       | type         | reason      | status | message  |
       | Resync       | Resync      | True   | Resync   |
       | Augmentation | ReifyFailed | False  | failure  |
       | Stopping     | Stopped     | False  | Stopped  |
       | Stop         | Stopped     | True   | Stopped  |
