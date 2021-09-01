#!/bin/bash -ex

PROFILE="${KUSTOMIZE_PROFILE:-rh-fuse}"

oc apply -k etc/kubernetes/operator-camel/"${PROFILE}"
oc apply -k etc/kubernetes/operator-debezium/"${PROFILE}"
oc apply -k etc/kubernetes/sync/"${PROFILE}"
