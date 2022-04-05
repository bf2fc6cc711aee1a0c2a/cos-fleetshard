#!/bin/bash -e

function print_exit() {
    echo "$1"
    exit 1
}

[ "$#" -eq 1 ] || print_exit "This script needs one parameter."

for env_var in IMAGE_REPO_USERNAME IMAGE_REPO_PASSWORD IMAGE_VERSION; do
  [ -z "${!env_var}" ] && print_exit "Make sure to set the ${env_var} environment variable."
done

./mvnw ${MAVEN_ARGS} \
  clean package \
  -Dquarkus.container-image.username="${IMAGE_REPO_USERNAME}" \
  -Dquarkus.container-image.password="${IMAGE_REPO_PASSWORD}" \
  -Dquarkus.container-image.tag="${IMAGE_VERSION}" \
  -Pcontainer-push \
  -pl :"${1}"
