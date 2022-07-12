Feature: Debezium Connector Lifecycle

  Background:
    Given Await configuration
      | atMost       | 30000   |
      | pollDelay    | 100     |
      | pollInterval | 500     |

  Scenario: delete
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
    Then the connector secret exists
    Then the kc exists
    Then the kctr exists

    When the connector desired status is set to "deleted"
    Then the connector is in phase "Deleted"
    Then the connector secret exists
    Then the kc does not exists
    Then the kctr does not exists