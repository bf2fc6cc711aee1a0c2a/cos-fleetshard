Feature: Connector Multiple Operators

  Background:
    Given Await configuration
      | atMost       | 30000   |
      | pollDelay    | 100     |
      | pollInterval | 500     |

  Scenario: The Operator stop managing a connector in order to migrate it to another operator
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
    Then the connector operatorSelector id is "cos-fleetshard-operator-it"
    Then the connector's assignedOperator exists with:
      | operator.id      | cos-fleetshard-operator-it |
      | operator.type    | connector-operator-it      |
      | operator.version | 1.5.0                      |

    When the connector path "spec.operatorSelector.id" is set to "cos-fleetshard-operator-it-new"
    Then the connector is in phase "Initialization"
    And the connector's availableOperator does not exist
    And the connector's assignedOperator does not exist
    And the meters has counter "cos.fleetshard.controller.event.operators.operand.stop.count" with value equal to 1

  Scenario: The Operator start managing a connector that has been migrated from another operator
    Given a Connector with:
      | connector.type.id           | log_sink_0.1                    |
      | desired.state               | ready                           |
      | kafka.bootstrap             | kafka.acme.com:443              |
      | operator.id                 | cos-fleetshard-operator-it-old  |
      | operator.type               | connector-operator-it           |
      | operator.version            | [1.0.0,2.0.0)                   |

    When deploy
    Then the connector exists
    Then the connector secret exists
    Then the connector operatorSelector id is "cos-fleetshard-operator-it-old"
    And the connector's availableOperator does not exist
    And the connector's assignedOperator does not exist

    When the connector path "spec.operatorSelector.id" is set to "cos-fleetshard-operator-it"
    Then the connector's assignedOperator exists with:
      | operator.id      | cos-fleetshard-operator-it |
      | operator.type    | connector-operator-it      |
      | operator.version | 1.5.0                      |
    Then the connector is in phase "Monitor"
