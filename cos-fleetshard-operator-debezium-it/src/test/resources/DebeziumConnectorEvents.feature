Feature: Debezium Connector Status

  Background:
    Given Await configuration
      | atMost       | 30000   |
      | pollDelay    | 100     |
      | pollInterval | 500     |

  Scenario: kctr becomes ready
    Given a Connector with:
      | connector.type.id           | debezium-postgres-1.9.0.Alpha2    |
      | desired.state               | ready                            |
      | kafka.bootstrap             | kafka.acme.com:443               |
      | operator.id                 | cos-fleetshard-operator-debezium |
      | operator.type               | debezium-connector-operator      |
      | operator.version            | [1.0.0,2.0.0)                    |
    And with a simple Debezium connector

    When deploy
    Then the connector exists
    Then the connector secret exists
    Then the kc exists
    Then the kctr exists

    When the kctr has conditions:
      | message   | reason   | status     | type     | lastTransitionTime        |
      | a message | a reason | True       | Ready    | 2021-06-12T12:35:09+02:00 |
    Then the connector is in phase "Monitor"
    Then the deployment is in phase "ready"
