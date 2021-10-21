Feature: Camel Connector Reify With Target Meta

  Background:
    Given Await configuration
      | atMost       | 30000   |
      | pollDelay    | 100     |
      | pollInterval | 500     |

  Scenario: reify with target meta
    Given a Connector with:
      | connector.type.id           | log_sink_0.1                    |
      | desired.state               | ready                           |
      | kafka.bootstrap             | kafka.acme.com:443              |
      | operator.id                 | cos-fleetshard-operator-camel   |
      | operator.type               | camel-connector-operator        |
      | operator.version            | [1.0.0,2.0.0)                   |
    And set connector annotation "foo/barAnnotation" to "baz"
    And set connector label "foo/barLabel" to "baz"
    And with sample camel connector

    When deploy
    Then the connector exists
     And the connector secret exists

    Then the klb exists
     And the klb has annotations containing:
          | foo/barAnnotation  | baz |
     And the klb has labels containing:
          | foo/barLabel       | baz |
     And the klb secret has annotations containing:
          | foo/barAnnotation  | baz |
     And the klb secret has labels containing:
          | foo/barLabel       | baz |

