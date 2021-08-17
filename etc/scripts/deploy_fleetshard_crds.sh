#!/bin/bash -ex

kubectl apply -f etc/kubernetes/managedconnectorclusters.cos.bf2.org-v1.yml
kubectl apply -f etc/kubernetes/managedconnectoroperators.cos.bf2.org-v1.yml
kubectl apply -f etc/kubernetes/managedconnectors.cos.bf2.org-v1.yml
