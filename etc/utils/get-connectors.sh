#!/bin/bash

BASE=${BASE_PATH}/api/connector_mgmt/v1
CONNECTORS_BASE=${BASE}/kafka_connectors

curl --insecure --oauth2-bearer "$(ocm token)" -S -s -D /dev/stderr "${CONNECTORS_BASE}" | jq

