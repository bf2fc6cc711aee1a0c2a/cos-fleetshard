#!/bin/bash

BASE=${BASE_PATH}/api/connector_mgmt/v1
CLUSTER_BASE=${BASE}/kafka_connector_clusters

curl --insecure --oauth2-bearer "$(ocm token)" -S -s "${CLUSTER_BASE}"/"${1}"/addon_parameters \
    | jq -r '(["ID","VALUE"] | (., map(length*"-"))), (.[] | [.id, .value]) | @tsv' \
    | column -t -s $'\t' 
