#!/bin/bash

BASE=${BASE_PATH}/api/kafkas_mgmt/v1
KAFKA_BASE=${BASE}/kafkas

curl --insecure --oauth2-bearer "$(ocm token)" -S -s "${KAFKA_BASE}" \
    | jq -r '(["ID","NAME","OWNER","STATUS"] | (., map(length*"-"))), (.items[]? | [.id, .name, .owner, .status]) | @tsv' \
    | column -t -s $'\t' 