Feature: Camel Connector Reify

  Background:
    Given Await configuration
      | atMost       | 30000   |
      | pollDelay    | 100     |
      | pollInterval | 500     |

  Scenario: reify log error handler
    Given a Connector with:
      | connector.type.id           | log_sink_0.1                    |
      | desired.state               | ready                           |
      | kafka.bootstrap             | kafka.acme.com:443              |
      | operator.id                 | cos-fleetshard-operator-camel   |
      | operator.type               | camel-connector-operator        |
      | operator.version            | [1.0.0,2.0.0)                   |
    And with sample camel connector
    And with error handling configuration:
      | type  | log |

    When deploy
    Then the connector exists
     And the connector secret exists
     And the connector is in phase "Monitor"

    Then the klb exists
     And the klb has an empty object at path "$.spec.errorHandler.log"


  Scenario: reify dlq error handler
    Given a Connector with:
      | connector.type.id           | log_sink_0.1                    |
      | desired.state               | ready                           |
      | kafka.bootstrap             | kafka.acme.com:443              |
      | operator.id                 | cos-fleetshard-operator-camel   |
      | operator.type               | camel-connector-operator        |
      | operator.version            | [1.0.0,2.0.0)                   |
    And with sample camel connector
    And with error handling configuration:
      | type  | dead_letter_queue |
      | topic | dlq               |

    When deploy
    Then the connector exists
     And the connector secret exists
     And the connector is in phase "Monitor"

    Then the klb exists
     And the klb has an entry at path "$.spec.errorHandler.sink.endpoint.uri" with value "kamelet://cos-kafka-sink/error"

    Then the klb secret exists
     And the klb secret contains:
      | camel.kamelet.log-sink.multiLine                        | true                       |
      | camel.kamelet.log-sink.showAll                          | true                       |
      | camel.kamelet.managed-kafka-source.bootstrapServers     | kafka.acme.com:443         |
      | camel.kamelet.managed-kafka-source.password             |                            |
      | camel.kamelet.managed-kafka-source.user                 |                            |
      | camel.kamelet.managed-kafka-source.topic                | dbz_pg.inventory.customers |
      | camel.kamelet.cos-kafka-sink.error.topic                | dlq                        |
      | camel.kamelet.cos-kafka-sink.error.bootstrapServers     | kafka.acme.com:443         |
      | camel.kamelet.cos-kafka-sink.error.password             |                            |
      | camel.kamelet.cos-kafka-sink.error.user                 |                            |

  Scenario: reify stop error handler
    Given a Connector with:
      | connector.type.id           | log_sink_0.1                    |
      | desired.state               | ready                           |
      | kafka.bootstrap             | kafka.acme.com:443              |
      | operator.id                 | cos-fleetshard-operator-camel   |
      | operator.type               | camel-connector-operator        |
      | operator.version            | [1.0.0,2.0.0)                   |
    And with sample camel connector
    And with error handling configuration:
      | type  | stop |

    When deploy
    Then the connector exists
     And the connector secret exists
     And the connector is in phase "Monitor"

    Then the klb exists
     And the klb has an entry at path "$.spec.errorHandler.sink.endpoint.uri" with value "rc:fail?routeId=current"
     
    When the klb phase is "error"
    Then the connector is in phase "Monitor"
     And the connector operand status is in phase "failed"
