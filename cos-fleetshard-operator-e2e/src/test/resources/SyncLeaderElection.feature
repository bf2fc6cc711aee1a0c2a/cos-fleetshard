Feature: LeaderElection

  Background:
    Given E2E configuration
      | atMost       | 60000                       |
      | pollDelay    | 100                         |
      | pollInterval | 500                         |
      | namespace    | redhat-openshift-connectors |

  Scenario: Sync Leader Election

    Given a configmap "cos-fleetshard-sync-config-override" with key "override.properties" and data:
      | cos.lease.enabled                  | true     |
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
     And the component "cos-fleetshard-sync" own a lease and store the leader identity as "leader.id.1"

     And the component pod "${leader.id.1}" has condition "ContainersReady" with status "True"
     And the component pod "${leader.id.1}" has condition "Ready" with status "True"
     And the component pod "${leader.id.1}" has condition "PodScheduled" with status "True"

    When scale component "cos-fleetshard-sync" to 2 replicas
    Then the component "cos-fleetshard-sync" has 2 replicas

     And the component pod "${leader.id.1}" has condition "ContainersReady" with status "True"
     And the component pod "${leader.id.1}" has condition "Ready" with status "True"
     And the component pod "${leader.id.1}" has condition "PodScheduled" with status "True"

    When delete the pod "${leader.id.1}"
    Then the component "cos-fleetshard-sync" own a lease and store the leader identity as "leader.id.2"

     And property "${leader.id.1}" != "${leader.id.2}"
     And the component pod "${leader.id.2}" has condition "ContainersReady" with status "True"
     And the component pod "${leader.id.2}" has condition "Ready" with status "True"
     And the component pod "${leader.id.2}" has condition "PodScheduled" with status "True"

    When scale component "cos-fleetshard-sync" to 0 replicas
    Then the component "cos-fleetshard-sync" has 0 replicas


    Then delete configmap "cos-fleetshard-sync-config-override"
    Then delete secret "ddon-connectors-operator-parameters"