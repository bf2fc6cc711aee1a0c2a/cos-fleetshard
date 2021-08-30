#!/bin/bash -ex

oc apply -k etc/kubernetes/operator-camel/rh-fuse
oc apply -k etc/kubernetes/operator-debezium/rh-fuse
oc apply -k etc/kubernetes/sync/rh-fuse
