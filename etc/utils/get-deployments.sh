#!/bin/bash

BASE=${BASE_PATH}/api/connector_mgmt/v1
CLUSTER_ID="${1}"
CLUSTER_BASE=${BASE}/kafka_connector_clusters/${CLUSTER_ID}

curl --insecure --oauth2-bearer $(get-sso-token.sh "${CLUSTER_ID}") -S -s "${CLUSTER_BASE}/deployments?gv=0" \
    | jq -r '
            ([
                "DEPLOYMENT_ID",
                "DEPLOYMENT_RV",
                "CONNECTOR_ID",
                "CONNECTOR_TYPE_ID",
                "CONNECTOR_RV",
                "STATE",
                "DESIRED_STATE"]
            | (., map(length*"-"))),
            (.items[]? | [
                .id,
                .metadata.resource_version,
                .spec.connector_id,
                .spec.connector_type_id,
                .spec.connector_resource_version,
                .status.phase,
                .spec.desired_state
            ])
            | @tsv
        ' \
    | column -t -s $'\t'

