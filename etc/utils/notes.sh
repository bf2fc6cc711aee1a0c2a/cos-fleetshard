
curl --insecure --oauth2-bearer "$(ocm token)" -S -s -D /dev/stderr "${BASE_PATH}"/api/connector_mgmt/v1/kafka_connectors?async=true -d '{
 "kind": "Connector",
    "metadata": {
        "name": "My Example 4",
        "kafka_id": "1uKsx0Xo6KAtIyNHF0QQbI9rEDh"
    },
    "deployment_location": {
        "kind": "addon",
        "cluster_id": "1uJKvbvqtIWtS39Tft7i7c6dWCa"
    },
    "kafka": {
      "bootstrap_server": "kafka.hostname",
      "client_id": "myclient",
      "client_secret": "test"
    },
    "connector_type_id": "example_source_0.1",
    "connector_spec": {
        "connector": {
          "delay": "1s"
        },
        "kafka": {
          "topic": "inject"
        }
    }
}' | jq


curl --insecure --oauth2-bearer "$(ocm token)" -S -s -D /dev/stderr "${BASE_PATH}"/api/connector_mgmt/v1/kafka_connectors/1tnuaWgFxq9vEKPkR3yplgItVru -XPATCH -H "Content-Type: application/merge-patch+json" -d '{
    "connector_spec": {
        "connector": {
          "delay": "4s"
        }
    }
}' | jq

curl --insecure --oauth2-bearer "$(ocm token)" -XDELETE -S -s -D /dev/stderr  "${BASE_PATH}"/api/connector_mgmt/v1/kafka_connectors/1uKtXmgwdLGq0ZyczRkFbvsQ8oq
