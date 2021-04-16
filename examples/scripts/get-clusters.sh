#/bin/bash

BASE=http://localhost:8000/api/managed-services-api/v1
CLUSTER_BASE=${BASE}/kafka-connector-clusters

curl --oauth2-bearer $(ocm token) -S -s -D /dev/stderr ${CLUSTER_BASE} 2>/dev/null | jq