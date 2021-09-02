#!/bin/bash -ex

PROFILE="${KUSTOMIZE_PROFILE:-rh-fuse}"

for app in operator-camel operator-debezium sync; do
  oc apply -k etc/kubernetes/"${app}"/"${PROFILE}"
  oc rollout restart deployment/cos-fleetshard-"${app}"
done
