#/bin/bash

KAS_ETC=$HOME/work/etc/kas-fleetshard

OCM_ENV=integration go run cmd/kas-fleet-manager/main.go \
    serve \
        --enable-connectors=true \
        --enable-ocm-mock=true \
        --ocm-mock-mode=emulate-server \
        --allow-list-config-file=${KAS_ETC}/allow-list-configuration.yaml \
        --image-pull-docker-config-file=${KAS_ETC}/image-pull.dockerconfigjson \
        --connector-types=${KAS_ETC}/connector-types
