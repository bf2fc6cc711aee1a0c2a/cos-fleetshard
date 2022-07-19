#!/bin/bash -e

kubectl apply -f etc/kubernetes/manifests/overlays/local/observability/crds/observabilities.observability.redhat.com-v1.yml
kubectl apply -f etc/kubernetes/manifests/overlays/local/observability/crds/customresourcedefinition-subscriptions.operators.coreos.com.yaml
