Feature: Camel Connector Reify with processors

  Background:
    Given Await configuration
      | atMost       | 30000   |
      | pollDelay    | 100     |
      | pollInterval | 500     |

  Scenario: reify with processors
    Given a Connector with:
      | connector.type.id           | log_sink_0.1                    |
      | desired.state               | ready                           |
      | kafka.bootstrap             | kafka.acme.com:443              |
      | operator.id                 | cos-fleetshard-operator-camel   |
      | operator.type               | camel-connector-operator        |
      | operator.version            | [1.0.0,2.0.0)                   |
    And with sample camel connector
    And with processors:
      """
        [
          {
            "choice": {
              "when": [
                {
                  "jq": ".destination == \"NA\"",
                  "steps": [
                    {
                      "setHeader": {
                        "name": "kafka.OVERRIDE_TOPIC",
                        "constant": "TOPIC.NA"
                      }
                    }
                  ]
                },
                {
                  "jq": ".destination == \"EMEA\"",
                  "steps": [
                    {
                      "setHeader": {
                        "name": "kafka.OVERRIDE_TOPIC",
                        "constant": "TOPIC.EMEA"
                      }
                    }
                  ]
                },
                {
                  "jq": ".destination == \"LATAM\"",
                  "steps": [
                    {
                      "setHeader": {
                        "name": "kafka.OVERRIDE_TOPIC",
                        "constant": "TOPIC.LATAM"
                      }
                    }
                  ]
                },
                {
                  "jq": ".destination == \"APAC\"",
                  "steps": [
                    {
                      "setHeader": {
                        "name": "kafka.OVERRIDE_TOPIC",
                        "constant": "TOPIC.APAC"
                      }
                    }
                  ]
                }
              ]
            }
          }
        ]
      """

    When deploy
    Then the connector exists
     And the connector secret exists
     And the connector is in phase "Monitor"

    Then the klb exists
    And the klb has an entry at path "$.spec.steps[0].uri" with value "direct:cos-transform"
    And the klb has an entry at path "$.spec.integration.flows[0].from.steps[0].choice.when[0].jq" with value ".destination == \"NA\""
    And the klb has an entry at path "$.spec.integration.flows[0].from.steps[0].choice.when[1].steps[0].setHeader.name" with value "kafka.OVERRIDE_TOPIC"
    And the klb has an entry at path "$.spec.integration.flows[0].from.steps[0].choice.when[2].steps[0].setHeader.constant" with value "TOPIC.LATAM"

