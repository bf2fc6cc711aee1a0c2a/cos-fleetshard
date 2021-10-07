#!/bin/bash

kubectl apply -f "${KCP_ROOT}/config/"
kubectl create namespace "${KUBERNETES_NAMESPACE}"

exec "${KCP_ROOT}"/bin/cluster-controller \
    -kubeconfig="${KUBECONFIG}" \
    -push_mode=true \
    -pull_mode=false \
    -auto_publish_apis=true \
    deployments.apps pods replicasets cronjobs secrets configmaps
