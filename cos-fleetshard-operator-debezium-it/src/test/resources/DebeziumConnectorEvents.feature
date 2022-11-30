Feature: Debezium Connector Status

  Background:
    Given Await configuration
      | atMost       | 60000   |
      | pollDelay    | 100     |
      | pollInterval | 500     |

  Scenario: kctr becomes ready
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

    When the kctr status is set to "RUNNING"
    And the kctr has conditions:
      | message   | reason             | status     | type     | lastTransitionTime        |
      | a message | a connector reason | True       | Ready    | 2021-06-12T12:35:09+02:00 |
    And the kc has conditions:
      | message   | reason                 | status     | type     | lastTransitionTime        |
      | a message | a kafka connect reason | True       | Ready    | 2021-06-12T12:36:09+02:00 |
    Then the connector is in phase "Monitor"
    And the deployment is in phase "ready"
