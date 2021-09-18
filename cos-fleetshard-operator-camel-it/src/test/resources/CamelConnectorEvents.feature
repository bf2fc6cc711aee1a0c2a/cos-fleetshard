Feature: Camel Connector Status

  Background:
    Given Await configuration
      | atMost       | 30000   |
      | pollDelay    | 100     |
      | pollInterval | 500     |

  Scenario: klb becomes ready
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

    When the klb phase is "Ready" with conditions:
      | message   | reason   | status     | type     | lastTransitionTime        |
      | a message | a reason | the status | the type | 2021-06-12T12:35:09+02:00 |
    Then the connector is in phase "Monitor"
    Then the deployment is in phase "ready"