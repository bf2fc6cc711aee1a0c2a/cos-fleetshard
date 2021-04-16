
export BASE=http://localhost:8000/api/managed-services-api/v1

curl --oauth2-bearer $(ocm token) -S -s -D /dev/stderr ${BASE}/kafka-connectors?async=true -d '{
 "kind": "Connector",
    "metadata": {
        "name": "My Injector",
        "kafka_id": "1s6Sr7jwJJAl4KkKMadp6QsiO4u"
    },
    "deployment_location": {
        "kind": "addon",
        "cluster_id": "1s6St85gmiruUzqARdJGi2lLscz"
    },
    "connector_type_id": "injector-source-v1alpha1",
    "connector_spec": {
        "delay": "15s",
        "loggerName": "cos",
        "multiLine": false,
        "showAll": false
    }
}' | jq


curl -XPATCH -H "Content-Type: application/merge-patch+json" --oauth2-bearer $(ocm token) -S -s -D /dev/stderr ${BASE}/kafka-connectors/1rstfBqFmXSIwdduYeZ0ju5hpE4 -d '{
    "connector_spec": {
        "delay": "1s",
        "multiLine": true
    }
}' | jq


curl -XDELETE --oauth2-bearer $(ocm token) -S -s -D /dev/stderr ${BASE}/kafka-connectors/1rstfBqFmXSIwdduYeZ0ju5hpE4
