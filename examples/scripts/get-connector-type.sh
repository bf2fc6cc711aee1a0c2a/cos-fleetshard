#/bin/bash

BASE=http://localhost:8000/api/managed-services-api/v1
CONNECTORS_BASE=${BASE}/kafka-connector-types

curl --oauth2-bearer $(ocm token) -S -s -D /dev/stderr ${CONNECTORS_BASE}/${1} | jq