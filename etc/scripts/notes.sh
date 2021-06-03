
curl --oauth2-bearer $(ocm token) -S -s -D /dev/stderr http://localhost:8000/api/connector_mgmt/v1/kafka-connectors?async=true -d '{
 "kind": "Connector",
    "metadata": {
        "name": "My Example",
        "kafka_id": "1tQfNM9XLHLJGvs33N2DDiXhvD1"
    },
    "deployment_location": {
        "kind": "addon",
        "cluster_id": "1tQfNtClMdp00m3VBafDbF3r5ln"
    },
    "kafka": {
      "bootstrap_server": "kafka.hostname",
      "client_id": "myclient",
      "client_secret": "test"
    },
    "connector_type_id": "example-source-0.1",
    "connector_spec": {
        "connector": {
          "delay": "1s"
        },
        "kafka": {
          "multiLine": true
        }
    }
}' | jq


curl -XPATCH -H "Content-Type: application/merge-patch+json" --oauth2-bearer $(ocm token) -S -s -D /dev/stderr  http://localhost:8000/api/connector_mgmt/v1/kafka_connectors/1tQsRdmyHvkukdatExUhBAKv0eA -d '{
    "connector_spec": {
        "connector": {
          "delay": "4s"
        }
    }
}' | jq


curl -XDELETE --oauth2-bearer $(ocm token) -S -s -D /dev/stderr  http://localhost:8000/api/connector_mgmt/kafka-connectors/1rstfBqFmXSIwdduYeZ0ju5hpE4
