#!/bin/bash -ex

kubectl apply -k etc/kubernetes/operator-camel/base
kubectl apply -k etc/kubernetes/operator-debezium/base
kubectl apply -k etc/kubernetes/sync/base
