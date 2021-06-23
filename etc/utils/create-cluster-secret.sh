#!/bin/bash


BASE=${BASE_PATH}/api/connector_mgmt/v1
CLUSTER_BASE=${BASE}/kafka_connector_clusters/"${1}"/addon_parameters

CLIENT_ID=$(curl --insecure --oauth2-bearer "$(ocm token)" -S -s -D /dev/stderr "${CLUSTER_BASE}" 2>/dev/null | jq -r '.[] | select(.id == "client-id") | .value')
CLIENT_SECRET=$(curl --insecure --oauth2-bearer "$(ocm token)" -S -s -D /dev/stderr "${CLUSTER_BASE}"  2>/dev/null | jq -r '.[] | select(.id == "client-secret") | .value')
SSO_REALM=$(curl --insecure --oauth2-bearer "$(ocm token)" -S -s -D /dev/stderr "${CLUSTER_BASE}" 2>/dev/null | jq -r '.[] | select(.id == "mas-sso-realm") | .value')
SSO_URL=$(curl --insecure --oauth2-bearer "$(ocm token)" -S -s -D /dev/stderr "${CLUSTER_BASE}" 2>/dev/null | jq -r '.[] | select(.id == "mas-sso-base-url") | .value')

#echo "client-id:     ${CLIENT_ID}"
#echo "client-secret: ${CLIENT_SECRET}"
#echo "sso-realm:     ${SSO_REALM}"
#echo "sso_url:       ${SSO_URL}"

tmp_dir=$(mktemp -d -t ci-XXXXXXXXXX)

echo "mas-sso-base-url=${SSO_URL}" > "${tmp_dir}"/application.properties
echo "mas-sso-realm=${SSO_REALM}" >> "${tmp_dir}"/application.properties
echo "client-id=${CLIENT_ID}" >> "${tmp_dir}"/application.properties
echo "client-secret=${CLIENT_SECRET}" >> "${tmp_dir}"/application.properties
echo "control-plane-base-url=${BASE_PATH}" >> "${tmp_dir}"/application.properties
echo "cluster-id=${1}" >> "${tmp_dir}"/application.properties

cat "${tmp_dir}"/application.properties

kubectl create secret generic addon-cos-fleetshard-operator-parameters --from-file="${tmp_dir}"/application.properties
