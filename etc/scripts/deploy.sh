#!/bin/bash -ex

function print_exit() {
  echo $1
  exit 1
}

for env_var in IMAGE_REPO_PASSWORD IMAGE_REPO_USERNAME IMAGE_REPO_HOSTNAME; do
  if [ -z "${!env_var}" ]; then
    print_exit "Make sure to set the ${env_var} environment variable."
  fi
done

oc project ${OC_PROJECT} || print_exit "Unable to access openshift cluster."

oc get secrets cos-pull-secret || oc create secret docker-registry cos-pull-secret \
   --docker-server="${IMAGE_REPO_HOSTNAME}" \
   --docker-username="${IMAGE_REPO_USERNAME}" \
   --docker-password="${IMAGE_REPO_PASSWORD}" \
   --docker-email=mas-connectors@redhat.com

oc apply -f etc/kubernetes/managedconnectoroperators.cos.bf2.org-v1.yml
oc apply -f etc/kubernetes/managedconnectorclusters.cos.bf2.org-v1.yml
oc apply -f etc/kubernetes/managedconnectors.cos.bf2.org-v1.yml
#oc apply -f etc/kubernetes/kubernetes.yml
#oc rollout restart deployment/cos-fleetshard
