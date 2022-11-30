Feature: Debezium Connector Reify With Target Meta

  Background:
    Given Await configuration
      | atMost       | 60000   |
      | pollDelay    | 100     |
      | pollInterval | 500     |

  Scenario: reify with target meta
    Given a Connector with:
      | connector.type.id           | debezium-postgres-1.9.4.Final    |
      | desired.state               | ready                            |
      | kafka.bootstrap             | kafka.acme.com:443               |
      | operator.id                 | cos-fleetshard-operator-debezium |
      | operator.type               | debezium-connector-operator      |
      | operator.version            | [1.0.0,2.0.0)                    |
    And set connector annotation "foo/barAnnotation" to "baz"
    And set connector label "foo/barLabel" to "baz"
    And with a simple Debezium connector

    When deploy
    Then the connector exists
     And the connector secret exists

    Then the kc exists
     And the kc has annotations containing:
          | foo/barAnnotation  | baz |
     And the kc has labels containing:
          | foo/barLabel       | baz |

    Then the kctr exists
     And the kctr has annotations containing:
          | foo/barAnnotation  | baz |
     And the kctr has labels containing:
          | foo/barLabel       | baz |

