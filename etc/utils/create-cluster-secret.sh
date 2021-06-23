#!/bin/bash


BASE=${BASE_PATH}/api/connector_mgmt/v1
CLUSTER_BASE=${BASE}/kafka_connector_clusters/"${1}"/addon_parameters

CLIENT_ID=$(curl --oauth2-bearer "$(ocm token)" -S -s -D /dev/stderr "${CLUSTER_BASE}" 2>/dev/null | jq -r '.[] | select(.id == "client-id") | .value')
CLIENT_SECRET=$(curl --oauth2-bearer "$(ocm token)" -S -s -D /dev/stderr" ${CLUSTER_BASE}"  2>/dev/null | jq -r '.[] | select(.id == "client-secret") | .value')
SSO_REALM=$(curl --oauth2-bearer "$(ocm token)" -S -s -D /dev/stderr "${CLUSTER_BASE}" 2>/dev/null | jq -r '.[] | select(.id == "mas-sso-realm") | .value')
SSO_URL=$(curl --oauth2-bearer "$(ocm token)" -S -s -D /dev/stderr "${CLUSTER_BASE}" 2>/dev/null | jq -r '.[] | select(.id == "mas-sso-base-url") | .value')

echo "client-id:     ${CLIENT_ID}"
echo "client-secret: ${CLIENT_SECRET}"
echo "sso-realm:     ${SSO_REALM}"
echo "sso_url:       ${SSO_URL}"

kubectl create secret generic addon-cos-fleetshard-operator-parameters \
    --from-literal=mas-sso-base-url="${SSO_URL}" \
    --from-literal=mas-sso-realm="${SSO_REALM}" \
    --from-literal=client-id="${CLIENT_ID}" \
    --from-literal=client-secret="${CLIENT_SECRET}" \
    --from-literal=control-plane-base-url="${BASE_PATH}" \
    --from-literal=cluster-id="${CLUSTER_ID}"
