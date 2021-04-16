#/bin/bash

BASE=http://localhost:8000/api/managed-services-api/v1
KAFKA_BASE=${BASE}/kafkas

curl --oauth2-bearer $(ocm token) -S -s -D /dev/stderr ${KAFKA_BASE} 2>/dev/null | jq