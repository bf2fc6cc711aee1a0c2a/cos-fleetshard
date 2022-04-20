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
      | kafka.client.id             | kcid                            |
      | kafka.client.secret         | ${cos.uid}                      |
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
     And the klb has an entry at path "$.spec.errorHandler.sink.endpoint.ref.kind" with value "Kamelet"
     And the klb has an entry at path "$.spec.errorHandler.sink.endpoint.ref.apiVersion" with value "camel.apache.org/v1alpha1"
     And the klb has an entry at path "$.spec.errorHandler.sink.endpoint.ref.name" with value "cos-kafka-sink"
     And the klb has an entry at path "$.spec.errorHandler.sink.endpoint.properties.topic" with value "dlq"
     And the klb has an entry at path "$.spec.errorHandler.sink.endpoint.properties.bootstrapServers" with value "${kafka.bootstrap}"
     And the klb has an entry at path "$.spec.errorHandler.sink.endpoint.properties.user" with value "{{sa_client_id}}"
     And the klb has an entry at path "$.spec.errorHandler.sink.endpoint.properties.password" with value "{{sa_client_secret}}"

    Then the klb secret exists
     And the klb secret contains:
      | sa_client_id      | ${kafka.client.id}         |
      | sa_client_secret  | ${kafka.client.secret}     |

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

  Scenario: reify default error handler
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
    And the connector secret exists
    And the connector is in phase "Monitor"

    Then the klb exists
    And the klb has an entry at path "$.spec.errorHandler.sink.endpoint.uri" with value "rc:fail?routeId=current"

    When the klb phase is "error"
    Then the connector is in phase "Monitor"
    And the connector operand status is in phase "failed"
