#/bin/bash

BASE=http://localhost:8000/api/managed-services-api/v1
CONNECTORS_BASE=${BASE}/kafka-connectors
KAFKA_BASE=${BASE}/kafkas
KAFKA_ID=$(curl --oauth2-bearer $(ocm token) -S -s -D /dev/stderr ${KAFKA_BASE} 2>/dev/null | jq -r '.items[0].id')

curl --oauth2-bearer $(ocm token) -S -s -D /dev/stderr ${CONNECTORS_BASE}?kafka_id=${KAFKA_ID} | jq
