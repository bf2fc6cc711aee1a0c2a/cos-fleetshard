Feature: Debezium Connector Reify

  Background:
    Given Await configuration
      | atMost       | 30000   |
      | pollDelay    | 100     |
      | pollInterval | 500     |

  Scenario: reify
    Given a Connector with:
      | connector.type.id           | debezium-postgres-1.9.4.Final    |
      | desired.state               | ready                            |
      | kafka.bootstrap             | kafka.acme.com:443               |
      | operator.id                 | cos-fleetshard-operator-debezium |
      | operator.type               | debezium-connector-operator      |
      | operator.version            | [1.0.0,2.0.0)                    |

     And with Debezium connector using "AVRO" datashape
     And set connector label "cos.bf2.org/organisation-id" to "20000000"
     And set connector label "cos.bf2.org/pricing-tier" to "essential"
     And set connector annotation "my.cos.bf2.org/connector-group" to "baz"

    When deploy
    Then the connector exists
     And the connector secret exists
     And the connector is in phase "Monitor"

    Then the kc exists
     And the kc has the correct metrics config map
     And the kc has an entry at path '$.metadata.labels.["cos.bf2.org/pricing-tier"]' with value "essential"
     And the kc has an entry at path '$.metadata.labels.["cos.bf2.org/organisation-id"]' with value "20000000"
     And the kc has an entry at path '$.metadata.annotations.["my.cos.bf2.org/connector-group"]' with value "baz"

    Then the kctr exists
     And the kctr has an entry at path '$.metadata.labels.["cos.bf2.org/pricing-tier"]' with value "essential"
     And the kctr has an entry at path '$.metadata.labels.["cos.bf2.org/organisation-id"]' with value "20000000"
     And the kctr has an entry at path '$.metadata.annotations.["my.cos.bf2.org/connector-group"]' with value "baz"

    Then the meters has entries with name matching "cos.fleetshard.controller.connectors.reconcile\..*" and tags:
      | foo                       | bar       |
      | connector_group           | baz       |
      | organisation_id           | 20000000  |
      | pricing_tier              | essential |

    Then the meters has entries with name matching "cos.fleetshard.controller.event.operators.operand\..*" and tags:
      | foo                       | bar       |
      | connector_group           | baz       |
      | organisation_id           | 20000000  |
      | pricing_tier              | essential |