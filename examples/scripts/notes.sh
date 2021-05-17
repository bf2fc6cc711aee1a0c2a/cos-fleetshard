
export BASE=http://localhost:8000/api/connector_mgmt/v1

curl --oauth2-bearer $(ocm token) -S -s -D /dev/stderr http://localhost:8000/api/connector_mgmt/v1/kafka-connectors?async=true -d '{
 "kind": "Connector",
    "metadata": {
        "name": "My Injector 2",
        "kafka_id": "1sfB6ebiHOjJy4BlruFDRDnqNEo"
    },
    "deployment_location": {
        "kind": "addon",
        "cluster_id": "1sfB9loRB3oh5IDUtVm8fHNMqVu"
    },
    "connector_type_id": "twitter-timeline-source-1.0",
    "connector_spec": {
        "connector.accessToken": "accessToken",
        "connector.accessTokenSecret": "accessTokenSecret",
        "connector.apiKey": "apiKey",
        "connector.apiKeySecret": "apiKeySecret",
        "connector.user": "user",
        "kafka.topic": "topic"
    }
}' | jq


curl -XPATCH -H "Content-Type: application/merge-patch+json" --oauth2-bearer $(ocm token) -S -s -D /dev/stderr ${BASE}/kafka-connectors/1rstfBqFmXSIwdduYeZ0ju5hpE4 -d '{
    "connector_spec": {
        "delay": "1s",
        "multiLine": true
    }
}' | jq


curl -XDELETE --oauth2-bearer $(ocm token) -S -s -D /dev/stderr ${BASE}/kafka-connectors/1rstfBqFmXSIwdduYeZ0ju5hpE4
