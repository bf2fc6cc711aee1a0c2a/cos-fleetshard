#/bin/bash

OCM_ENV=integration go run cmd/cos-fleet-manager/main.go \
    serve \
        --enable-connectors=true \
        --enable-ocm-mock=true \
        --ocm-mock-mode=emulate-server \
        --allow-list-config-file=${KAS_ETC}/allow-list-configuration.yaml \
        --image-pull-docker-config-file=${KAS_ETC}/image-pull.dockerconfigjson \
        --connector-catalog=${KAS_ETC}/connector-catalog/connector-catalog-camel-aws \
        --connector-catalog=${KAS_ETC}/connector-catalog/connector-catalog-camel-misc \
        --connector-catalog=${KAS_ETC}/connector-catalog/connector-catalog-debezium-mongodb \
        --connector-catalog=${KAS_ETC}/connector-catalog/connector-catalog-debezium-mysql \
        --connector-catalog=${KAS_ETC}/connector-catalog/connector-catalog-debezium-postgres