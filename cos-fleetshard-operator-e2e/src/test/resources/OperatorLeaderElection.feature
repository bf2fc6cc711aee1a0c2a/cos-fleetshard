Feature: LeaderElection

  Background:
    Given E2E configuration
      | atMost       | 60000                       |
      | pollDelay    | 100                         |
      | pollInterval | 500                         |
      | namespace    | redhat-openshift-connectors |

  Scenario: Operator Leader Election
    Given a configmap "cos-fleetshard-operator-camel-config-override" with key "override.properties" and data:
      | cos.lease.enabled | true |

    When scale component "cos-fleetshard-operator-camel" to 1 replicas
    Then the component "cos-fleetshard-operator-camel" has 1 replicas
     And the component "cos-fleetshard-operator-camel" own a lease and store the leader identity as "leader.id.1"

     And the component pod "${leader.id.1}" has condition "ContainersReady" with status "True"
     And the component pod "${leader.id.1}" has condition "Ready" with status "True"
     And the component pod "${leader.id.1}" has condition "PodScheduled" with status "True"

    When scale component "cos-fleetshard-operator-camel" to 2 replicas
    Then the component "cos-fleetshard-operator-camel" has 2 replicas

     And the component pod "${leader.id.1}" has condition "ContainersReady" with status "True"
     And the component pod "${leader.id.1}" has condition "Ready" with status "True"
     And the component pod "${leader.id.1}" has condition "PodScheduled" with status "True"

    When delete the pod "${leader.id.1}"
    Then the component "cos-fleetshard-operator-camel" own a lease and store the leader identity as "leader.id.2"

     And property "${leader.id.1}" != "${leader.id.2}"
     And the component pod "${leader.id.2}" has condition "ContainersReady" with status "True"
     And the component pod "${leader.id.2}" has condition "Ready" with status "True"
     And the component pod "${leader.id.2}" has condition "PodScheduled" with status "True"

    When scale component "cos-fleetshard-operator-camel" to 0 replicas
    Then the component "cos-fleetshard-operator-camel" has 0 replicas

    Then delete configmap "cos-fleetshard-operator-camel-config-override"