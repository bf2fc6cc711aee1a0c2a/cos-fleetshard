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

oc get secrets addon-pullsecret || oc create secret docker-registry addon-pullsecret \
   --docker-server="${IMAGE_REPO_HOSTNAME}" \
   --docker-username="${IMAGE_REPO_USERNAME}" \
   --docker-password="${IMAGE_REPO_PASSWORD}" \
   --docker-email=mas-connectors@redhat.com

