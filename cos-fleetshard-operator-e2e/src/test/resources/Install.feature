Feature: Install

  Background:
    Given E2E configuration
      | atMost       | 90000                       |
      | pollDelay    | 100                         |
      | pollInterval | 500                         |
      | namespace    | redhat-openshift-connectors |

  Scenario: Camel K
    When scale component "camel-k-operator" to 1 replicas
    Then the component "camel-k-operator" has 1 replicas
     And the component "camel-k-operator" has condition "Available" with status "True"
     And scale component "camel-k-operator" to 0 replicas
     And the component "camel-k-operator" has 0 replicas

  Scenario: Fleet Shard Camel
    When scale component "cos-fleetshard-operator-camel" to 1 replicas
    Then the component "cos-fleetshard-operator-camel" has 1 replicas
     And the component "cos-fleetshard-operator-camel" has condition "Available" with status "True"
     And scale component "cos-fleetshard-operator-camel" to 0 replicas
     And the component "cos-fleetshard-operator-camel" has 0 replicas

  Scenario: Fleet Shard Debezium
    When scale component "cos-fleetshard-operator-debezium" to 1 replicas
    Then the component "cos-fleetshard-operator-debezium" has 1 replicas
     And the component "cos-fleetshard-operator-debezium" has condition "Available" with status "True"
     And scale component "cos-fleetshard-operator-debezium" to 0 replicas
     And the component "cos-fleetshard-operator-debezium" has 0 replicas

  Scenario: Fleet Shard Sync

    Given a configmap "cos-fleetshard-sync-config-override" with key "override.properties" and data:
      | cos.resources.update-interval      | disabled |
      | cos.resources.poll-interval        | disabled |
      | cos.resources.resync-interval      | disabled |
      | cos.resources.housekeeper-interval | disabled |

    Given a secret "addon-connectors-operator-parameters" with data:
      | control-plane-base-url | http://localhost:8080 |
      | cluster-id             | ${cos.uid}            |
      | client-id              | ${cos.uid}            |
      | client-secret          | ${cos.uid}            |

    When scale component "cos-fleetshard-sync" to 1 replicas
    Then the component "cos-fleetshard-sync" has 1 replicas
     And the component "cos-fleetshard-sync" has condition "Available" with status "True"
     And scale component "cos-fleetshard-sync" to 0 replicas
     And the component "cos-fleetshard-sync" has 0 replicas