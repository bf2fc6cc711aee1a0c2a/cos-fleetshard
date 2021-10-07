#!/bin/bash

"${CAMEL_K_ROOT}"/kamel install \
    --skip-registry-setup \
    --skip-operator-setup \
    --olm=false \
    --namespace "${KUBERNETES_NAMESPACE}"

export WATCH_NAMESPACE="${KUBERNETES_NAMESPACE}"

exec "${CAMEL_K_ROOT}"/kamel operator
