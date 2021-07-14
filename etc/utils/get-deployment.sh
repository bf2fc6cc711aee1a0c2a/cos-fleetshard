#!/bin/bash

BASE=${BASE_PATH}/api/connector_mgmt/v1
CLUSTER_ID="${1}"
CLUSTER_BASE=${BASE}/kafka_connector_clusters/${CLUSTER_ID}

curl --insecure --oauth2-bearer $(get-sso-token.sh "${CLUSTER_ID}") -S -s "${CLUSTER_BASE}/deployments/${2}" | jq
