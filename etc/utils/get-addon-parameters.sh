#/bin/bash

BASE=http://localhost:8000/api/managed-services-api/v1
CLUSTER_BASE=${BASE}/kafka-connector-clusters
CLUSTER_ID=$(curl --oauth2-bearer $(ocm token) -S -s -D /dev/stderr ${BASE}/kafka-connector-clusters 2>/dev/null | jq -r '.items[0].id')

curl --oauth2-bearer $(ocm token) -S -s -D /dev/stderr ${CLUSTER_BASE}/${CLUSTER_ID}/addon-parameters | jq
