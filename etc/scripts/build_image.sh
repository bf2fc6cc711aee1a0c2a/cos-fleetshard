#!/bin/bash -ex

if [ -z "${IMAGE_REPO_USERNAME}" ]; then
  echo "The quay.io push user name hasn't been provided."
  echo "Make sure to set the IMAGE_REPO_USERNAME environment variable."
  exit 1
fi

if [ -z "${IMAGE_REPO_PASSWORD}" ]; then
  echo "The quay.io push token hasn't been provided."
  echo "Make sure to set the IMAGE_REPO_PASSWORD environment variable."
  exit 1
fi

if [ -z "${IMAGE_REPO_NAMESPACE}" ]; then
  echo "The quay.io org hasn't been provided."
  echo "Make sure to set the IMAGE_REPO_NAMESPACE environment variable."
  exit 1
fi

export CONTAINER_REGISTRY_USR="${IMAGE_REPO_USERNAME}"
export CONTAINER_REGISTRY_PWD="${IMAGE_REPO_PASSWORD}"
export MAVEN_ARGS="-V -ntp -Dhttp.keepAlive=false -e"

# CONTAINER_VERSION can be set to the release tag so an extra image is pushed
# for the current build. When in CI, we also want a separate image for each commit
[ -z "${CI}" ] || ADDITIONAL_TAGS="$(git log --pretty=format:'%h' -n 1)"

if [ ! -z "${CONTAINER_VERSION}" ]; then
  [ -z ${ADDITIONAL_TAGS} ] && ADDITIONAL_TAGS="${CONTAINER_VERSION}" \
    || ADDITIONAL_TAGS="${CONTAINER_VERSION},${ADDITIONAL_TAGS}"
fi

./mvnw ${MAVEN_ARGS} \
  clean package \
  -Dquarkus.container-image.username="${CONTAINER_REGISTRY_USR}" \
  -Dquarkus.container-image.password="${CONTAINER_REGISTRY_PWD}" \
  -Dquarkus.container-image.tag=latest \
  -Dquarkus.container-image.additional-tags="${ADDITIONAL_TAGS}" \
  -Dquarkus.container-image.group=${IMAGE_REPO_NAMESPACE} \
  -Pcontainer-push \
  -pl :"${1}"