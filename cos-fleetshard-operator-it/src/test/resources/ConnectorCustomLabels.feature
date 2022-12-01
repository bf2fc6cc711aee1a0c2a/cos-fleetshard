Feature: Metrics With Custom Tags

  Background:
    Given Await configuration
      | atMost       | 30000   |
      | pollDelay    | 100     |
      | pollInterval | 500     |

  Scenario: custom tags
    Given a Connector with:
      | connector.type.id           | log_sink_0.1                    |
      | desired.state               | ready                           |
      | kafka.bootstrap             | kafka.acme.com:443              |
      | operator.id                 | cos-fleetshard-operator-it      |
      | operator.type               | connector-operator-it           |
      | operator.version            | [1.0.0,2.0.0)                   |

    When set connector label "my.cos.bf2.org/connector-group" to "baz"
    When set connector annotation "cos.bf2.org/organisation-id" to "20000000"
    When set connector annotation "cos.bf2.org/pricing-tier" to "essential"

    When deploy
    Then the connector exists
     And the connector secret exists
     And the connector is in phase "Monitor"
     And the deployment is in phase "ready"

     And the meters has entries with name matching "cos.fleetshard.controller.connectors.reconcile\..*" and tags:
      | foo                       | bar       |
       | connector_group           | baz       |
       | organisation_id           | 20000000  |
       | pricing_tier              | essential |

     And the meters has entries with name matching "cos.fleetshard.controller.event.operators.operand\..*" and tags:
       | foo                       | bar       |
       | connector_group           | baz       |
       | organisation_id           | 20000000  |
       | pricing_tier              | essential |

