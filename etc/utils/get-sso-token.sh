#!/bin/bash

BASE=${BASE_PATH}/api/connector_mgmt/v1
CLUSTER_ID="${1}"
CLUSTER_BASE=${BASE}/kafka_connector_clusters/${CLUSTER_ID}

typeset -A myarray

while IFS=: read -r key value; do
    myarray["$key"]="$value"
done < <(curl --insecure --oauth2-bearer "$(ocm token)" -S -s "${CLUSTER_BASE}/addon-parameters" | jq -r 'map("\(.id):\(.value|tostring)")|.[]' )

curl \
  -S -s -D /dev/stderr \
  --insecure \
  --header 'content-type: application/x-www-form-urlencoded' \
  --data grant_type=client_credentials \
  --data client_id="${myarray[client-id]}" \
  --data client_secret="${myarray[client-secret]}" \
  "${myarray[mas-sso-base-url]}/auth/realms/${myarray[mas-sso-realm]}/protocol/openid-connect/token" 2>/dev/null | jq -r '.access_token'

