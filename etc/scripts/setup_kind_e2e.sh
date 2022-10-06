#!/usr/bin/env bash

kind load docker-image "quay.io/rhoas/cos-fleetshard-operator-camel:latest"
kind load docker-image "quay.io/rhoas/cos-fleetshard-operator-debezium:latest"
kind load docker-image "quay.io/rhoas/cos-fleetshard-sync:latest"

kubectl apply \
  --server-side=true \
  --kustomize=./etc/kubernetes/manifests/overlays/e2e

kubectl apply -f etc/kubernetes/manifests/overlays/local/camel-k/integration-platform.yaml \
  --server-side \
  --force-conflicts \
  --namespace=redhat-openshift-connectors
