Feature: Camel Connector ReSync

  Background:
    Given Await configuration
      | atMost       | 5000    |
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

    When the klb with conditions:
      | message   | reason   | status     | type     | lastTransitionTime        |
      | a message | a reason | True       | Ready    | 2021-06-12T12:35:09+02:00 |
    Then the connector is in phase "Monitor"
     And the deployment is in phase "ready"

    When deploy
    Then wait till the connector has entry in history with phase "Augmentation" and conditions:
      | type         | status | reason    |
      | Resync       | True   | Resync    |
      | Augmentation | True   | Resync    |
      | Ready        | False  | Resync    |
     And wait till the connector has entry in history with phase "Monitor" and conditions:
      | type         | status | reason            |
      | Resync       | False  | Resync            |
      | Augmentation | True   | Augmentation      |
