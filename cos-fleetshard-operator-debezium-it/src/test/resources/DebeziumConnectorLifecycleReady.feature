Feature: Ready Debezium Connector

  Background:
    Given Await configuration
      | atMost       | 30000   |
      | pollDelay    | 100     |
      | pollInterval | 500     |

  Scenario: ready
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
     And the deployment is in phase "ready"

     And the meters has counter "cos.fleetshard.controller.connectors.reconcile.initialization.count" with value greater than or equal to 1
     And the meters has counter "cos.fleetshard.controller.connectors.reconcile.augmentation.count" with value greater than or equal to 1
     And the meters has counter "cos.fleetshard.controller.connectors.reconcile.monitor.count" with value greater than or equal to 1
     And the meters has timer with name "cos.fleetshard.controller.connectors.reconcile.initialization.time"
     And the meters has timer with name "cos.fleetshard.controller.connectors.reconcile.augmentation.time"
     And the meters has timer with name "cos.fleetshard.controller.connectors.reconcile.monitor.time"
