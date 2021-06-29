#!/bin/bash

BASE=${BASE_PATH}/api/kafkas_mgmt/v1
KAFKA_BASE=${BASE}/kafkas

curl --insecure --oauth2-bearer "$(ocm token)" -S -s -D /dev/stderr "${KAFKA_BASE}"/"${1}" | jq