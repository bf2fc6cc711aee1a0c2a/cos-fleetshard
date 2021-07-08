#!/bin/bash

BASE=${BASE_PATH}/api/connector_mgmt/v1
CONNECTORS_BASE=${BASE}/kafka_connectors

curl --insecure --oauth2-bearer "$(ocm token)" -S -s "${CONNECTORS_BASE}" \
    | jq -r '
            ([
                "ID",
                "TYPE",
                "CHANNEL",
                "OWNER",
                "NAME",
                "CLUSTER_ID",
                "KAFKA_ID",
                "CREATED_AT",
                "DESIRED_STATE",
                "STATUS"] 
            | (., map(length*"-"))), 
            (.items[]? | [
                .id, 
                .connector_type_id, 
                .channel,
                .metadata.owner, 
                .metadata.name, 
                .deployment_location.cluster_id, 
                .metadata.kafka_id, 
                .metadata.created_at,
                .desired_state, 
                .status
            ]) 
            | @tsv
        ' \
    | column -t -s $'\t' 
