#/bin/bash

BASE=http://localhost:8000/api/managed-services-api/v1
CLUSTER_ID=$(curl --oauth2-bearer $(ocm token) -S -s -D /dev/stderr ${BASE}/kafka-connector-clusters 2>/dev/null | jq -r '.items[0].id')
CLUSTER_BASE=${BASE}/kafka-connector-clusters/${CLUSTER_ID}/addon-parameters

CLIENT_ID=$(curl --oauth2-bearer $(ocm token) -S -s -D /dev/stderr ${CLUSTER_BASE} 2>/dev/null | jq -r '.[] | select(.id == "client-id") | .value')
CLIENT_SECRET=$(curl --oauth2-bearer $(ocm token) -S -s -D /dev/stderr ${CLUSTER_BASE}  2>/dev/null | jq -r '.[] | select(.id == "client-secret") | .value')
SSO_REALM=$(curl --oauth2-bearer $(ocm token) -S -s -D /dev/stderr ${CLUSTER_BASE} 2>/dev/null | jq -r '.[] | select(.id == "mas-sso-realm") | .value')
SSO_URL=$(curl --oauth2-bearer $(ocm token) -S -s -D /dev/stderr ${CLUSTER_BASE} 2>/dev/null | jq -r '.[] | select(.id == "mas-sso-base-url") | .value')

echo "client-id:     ${CLIENT_ID}"
echo "client-secret: ${CLIENT_SECRET}"
echo "sso-realm:     ${SSO_REALM}"
echo "sso_url:       ${SSO_URL}"

kubectl create secret generic addon-cos-fleetshard-operator-parameters \
    --from-literal=mas-sso-base-url=${SSO_URL} \
    --from-literal=mas-sso-realm=${SSO_REALM} \
    --from-literal=client-id=${CLIENT_ID} \
    --from-literal=client-secret=${CLIENT_SECRET} \
    --from-literal=control-plane-base-url=http://localhost:8000 \
    --from-literal=cluster-id=${CLUSTER_ID}
