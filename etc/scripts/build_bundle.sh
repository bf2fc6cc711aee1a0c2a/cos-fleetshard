#!/bin/bash -e

function print_exit() {
    echo "$1"
    exit 1
}

[ "$#" -eq 1 ] || print_exit "This script needs one parameter."


kustomize build "etc/kubernetes/manifests/overlays/bundles/$1" \
  | operator-sdk generate bundle \
    --package "$1" \
    --kustomize-dir "etc/kubernetes/manifests/overlays/bundles/$1" \
    --output-dir "etc/kubernetes/bundles/$1" \
    --overwrite
