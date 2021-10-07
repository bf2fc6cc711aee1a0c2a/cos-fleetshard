#!/bin/bash

rm -f "${PWD}/kind/kube/config"
kind delete clusters connectors

kind create cluster \
    --name=connectors \
    --image="kindest/node:v1.20.7" \
    --kubeconfig="${PWD}/kind/.kube/config" \
    --config="${PWD}/kind/kind-config.yaml"

kubectl --kubeconfig="${PWD}/kind/.kube/config" create namespace "${KUBERNETES_NAMESPACE}"

sed -e 's/^/    /' "${PWD}/kind/.kube/config" | cat "${PWD}/cluster-template.yaml" - | kubectl apply -f -