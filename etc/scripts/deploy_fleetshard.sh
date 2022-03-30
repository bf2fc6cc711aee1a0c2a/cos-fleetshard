#!/bin/bash -e

PROFILE="${KUSTOMIZE_PROFILE:-dev}"

for app in cos-fleetshard-operator-camel cos-fleetshard-operator-debezium cos-fleetshard-sync; do
  oc apply -k etc/kubernetes/manifests/overlays/"${PROFILE}"/"${app}"
  oc rollout restart deployment/"${app}"
done
