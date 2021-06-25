#!/bin/bash

BASE=${BASE_PATH}/api/connector_mgmt/v1
CONNECTORS_BASE=${BASE}/kafka_connector_types

curl --insecure --oauth2-bearer "$(ocm token)" -S -s "${CONNECTORS_BASE}" \
    | jq -r '(["ID","NAME","VERSION"] | (., map(length*"-"))), (.items[]? | [.id, .name, .version]) | @tsv' \
    | column -t -s $'\t' 