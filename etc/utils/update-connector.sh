#!/bin/bash

BASE=${BASE_PATH}/api/connector_mgmt/v1
CONNECTORS_BASE=${BASE}/kafka_connectors

echo $1
echo $2

curl \
    --insecure \
    --oauth2-bearer "$(ocm token)" \
    -S -s \
    -D /dev/stderr \
    "${CONNECTORS_BASE}/${1}" \
    -XPATCH \
    -H "Content-Type: application/merge-patch+json" \
    -d "${2}" | jq
