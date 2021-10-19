Feature: Connector Multiple Operators

  Background:
    Given Await configuration
      | atMost       | 30000   |
      | pollDelay    | 100     |
      | pollInterval | 500     |

  Scenario: create an available operator
    Given a Connector with:
      | connector.type.id           | log_sink_0.1                    |
      | desired.state               | ready                           |
      | kafka.bootstrap             | kafka.acme.com:443              |
      | operator.id                 | cos-fleetshard-operator-it      |
      | operator.type               | connector-operator-it           |
      | operator.version            | [1.0.0,2.0.0)                   |

    When deploy
    Then the connector exists
    Then the connector secret exists
    Then the ManagedConnectorOperator with name "cos-fleetshard-operator-it" exists
    Then the connector's assignedOperator exists with:
      | operator.id      | cos-fleetshard-operator-it |
      | operator.type    | connector-operator-it      |
      | operator.version | 1.5.0                      |
    Then the connector's availableOperator does not exist

    When deploy a ManagedConnectorOperator with:
      | operator.id      | cos-fleetshard-operator-2-it |
      | operator.type    | connector-operator-it        |
      | operator.version | 1.6.0                        |
      | operator.runtime | none                         |
    Then the connector's assignedOperator exists with:
      | operator.id      | cos-fleetshard-operator-it |
      | operator.type    | connector-operator-it      |
      | operator.version | 1.5.0                      |
    Then the connector's availableOperator exists with:
      | operator.id      | cos-fleetshard-operator-2-it |
      | operator.type    | connector-operator-it        |
      | operator.version | 1.6.0                        |