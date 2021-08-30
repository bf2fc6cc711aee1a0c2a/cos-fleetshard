#!/bin/bash -ex

kubectl apply -k etc/kubernetes/operator-camel/rh-fuse
kubectl apply -k etc/kubernetes/operator-debezium/rh-fuse
kubectl apply -k etc/kubernetes/sync/rh-fuse
