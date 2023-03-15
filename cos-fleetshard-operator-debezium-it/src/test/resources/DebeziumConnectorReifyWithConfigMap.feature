Feature: Debezium Connector Reify

  Background:
    Given Await configuration
      | atMost       | 30000   |
      | pollDelay    | 100     |
      | pollInterval | 500     |

  Scenario: reify

    Given a Connector with:
      | connector.type.id           | debezium_postgres_v1    |
      | desired.state               | ready                            |
      | kafka.bootstrap             | kafka.acme.com:443               |
      | operator.id                 | cos-fleetshard-operator-debezium |
      | operator.type               | debezium-connector-operator      |
      | operator.version            | [1.0.0,2.0.0)                    |

    And with a simple Debezium connector

    When deploy
    Then the connector exists
    Then the connector secret exists
    And the kc deployment exists
    And the kc secret exists
    And the kc configmap exists
    And the kc svc exists
    And the kc pvc exists

    When create kc pod

    When set kc detail to yaml:
      """
      name: "foo"
      type: "bar"
      connector:
        state: "RUNNING"
        trace: ""
      tasks:
      - state: "RUNNING"
        trace: ""
      """

    When set the kc pod to have conditions:
      | type            | status | reason                   | message           |
      | Ready           | True   | Ready                    | Ready             |
      | ContainersReady | True   | ContainersReady          | ContainersReady   |
      | PodScheduled    | True   | PodScheduled             | PodScheduled      |

    When set the kc pod property "phase" at path "$.status" to "Running"

    When set the kc deployment to have conditions:
      | type         | status | reason                   | message                                        |
      | Progressing  | True   | NewReplicaSetAvailable   | ReplicaSet "foo" has successfully progressed.  |
      | Available    | True   | MinimumReplicasAvailable | Deployment has minimum availability            |

    When set the kc deployment property "replicas" at path "$.status" to 1
    When set the kc deployment property "readyReplicas" at path "$.status" to 1

    Then the connector is in phase "Monitor"

    When set configmap to:
      | io.debezium              | DEBUG     |
      | org.apache.kafka.connect | DEBUG     |
      | bad.property             | any-value |

     And wait till the kc has configmap containing logger "io.debezium" with level "DEBUG"
     And wait till the kc has configmap containing logger "org.apache.kafka.connect" with level "DEBUG"
     And wait till the kc has configmap containing logger "bad.property" with level "any-value"