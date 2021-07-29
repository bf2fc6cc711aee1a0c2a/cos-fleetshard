#!/bin/bash

BASE=${BASE_PATH}/api/connector_mgmt/v1/kafka_connector_clusters

tmp_dir=$(mktemp -d -t ci-XXXXXXXXXX)

curl --insecure --oauth2-bearer "$(ocm token)" -S -s "${BASE}"/"${1}"/addon_parameters \
    | jq -r 'map("\(.id)=\(.value|tostring)")|.[]' \
    > "${tmp_dir}"/application.properties

cat "${tmp_dir}"/application.properties

kubectl create secret generic cos-fleetshard-sync-config --from-file="${tmp_dir}"/application.properties
