
curl --oauth2-bearer $(ocm token) -S -s -D /dev/stderr http://localhost:8000/api/connector_mgmt/v1/kafka-connectors?async=true -d '{
 "kind": "Connector",
    "metadata": {
        "name": "My Example 2",
        "kafka_id": "1tnuMzfoUJpXisas7aaZ81etMwT"
    },
    "deployment_location": {
        "kind": "addon",
        "cluster_id": "1tnuO1tkF7QfxhhuqH4xNvZfmTX"
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
          "multiLine": true
        }
    }
}' | jq


curl -XPATCH -H "Content-Type: application/merge-patch+json" --oauth2-bearer $(ocm token) -S -s -D /dev/stderr  http://localhost:8000/api/connector_mgmt/v1/kafka_connectors/1tnuaWgFxq9vEKPkR3yplgItVru -d '{
    "connector_spec": {
        "connector": {
          "delay": "4s"
        }
    }
}' | jq

curl --oauth2-bearer $(ocm token) -S -s -D /dev/stderr http://localhost:8000/api/connector_mgmt/v1/kafka-connectors?async=true -d '{
 "kind": "Connector",
    "metadata": {
        "name": "My Example 5",
        "kafka_id": "1tnuMzfoUJpXisas7aaZ81etMwT"
    },
    "deployment_location": {
        "kind": "addon",
        "cluster_id": "1tnuO1tkF7QfxhhuqH4xNvZfmTX"
    },
    "kafka": {
      "bootstrap_server": "kafka.hostname",
      "client_id": "myclient",
      "client_secret": "test"
    },
    "connector_type_id": "debezium-postgres-1.5.0.Final",
    "connector_spec": {
        "database.server.name": "server-name",
        "database.hostname": "host",
        "database.user": "usr",
        "database.dbname": "dbname"
    }
}' | jq


curl -XDELETE --oauth2-bearer $(ocm token) -S -s -D /dev/stderr  http://localhost:8000/api/connector_mgmt/kafka-connectors/1rstfBqFmXSIwdduYeZ0ju5hpE4
