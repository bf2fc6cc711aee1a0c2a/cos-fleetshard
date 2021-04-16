#/bin/bash

BASE=http://localhost:8000/api/managed-services-api/v1
KAFKA_BASE=${BASE}/kafkas

curl --oauth2-bearer $(ocm token) -S -s -D /dev/stderr ${KAFKA_BASE}?async=true -d '{
    "region": "us-east-1",
    "cloud_provider": "aws",
    "name": "cos",
    "multi_az":true
}' | jq
