#!/bin/bash

BASE=${BASE_PATH}/api/connector_mgmt/v1
CLUSTER_BASE=${BASE}/kafka_connector_clusters

curl --insecure --oauth2-bearer "$(ocm token)" -S -s "${CLUSTER_BASE}" \
    | jq -r '(["ID","NAME","OWNER","STATUS"] | (., map(length*"-"))), (.items[]? | [.id, .metadata.name, .metadata.owner, .status]) | @tsv' \
    | column -t -s $'\t' 