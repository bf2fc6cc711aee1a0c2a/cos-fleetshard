#!/bin/bash -ex

function print_exit() {
    echo "Make sure to set the $1 environment variable."
    exit 1
}

for env_var in IMAGE_REPO_USERNAME IMAGE_REPO_PASSWORD IMAGE_REPO_NAMESPACE; do
  [ -z "${!env_var}" ] && print_exit $env_var
done

export MAVEN_ARGS="-V -ntp -Dhttp.keepAlive=false -e"

# CONTAINER_VERSION can be set to the release tag so an extra image is pushed
# for the current build. When in CI, we also want a separate image for each commit
[ -z "${CI}" ] || ADDITIONAL_TAGS="$(git log --pretty=format:'%h' -n 1)"

# shellcheck disable=SC2236
if [ ! -z "${CONTAINER_VERSION}" ]; then
  [ -z "${ADDITIONAL_TAGS}" ] && ADDITIONAL_TAGS="${CONTAINER_VERSION}" \
    || ADDITIONAL_TAGS="${CONTAINER_VERSION},${ADDITIONAL_TAGS}"
fi

QUARKUS_BASE_IMAGE="${QUARKUS_BASE_IMAGE:-adoptopenjdk/openjdk11:ubi-minimal}"
QUARKUS_PLATFORM="${QUARKUS_PLATFORM:-linux/amd64}"

./mvnw ${MAVEN_ARGS} \
  clean package \
  -Dquarkus.jib.base-jvm-image="${QUARKUS_BASE_IMAGE}" \
  -Dquarkus.jib.platforms="${QUARKUS_PLATFORM}" \
  -Dquarkus.container-image.username="${IMAGE_REPO_USERNAME}" \
  -Dquarkus.container-image.password="${IMAGE_REPO_PASSWORD}" \
  -Dquarkus.container-image.tag=latest \
  -Dquarkus.container-image.additional-tags="${ADDITIONAL_TAGS}" \
  -Dquarkus.container-image.group="${IMAGE_REPO_NAMESPACE}" \
  -Pcontainer-push \
  -pl :"${1}"
