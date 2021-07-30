#!/bin/bash -ex
#
# Copyright (c) 2018 Red Hat, Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# This script builds and deploys the OCM Sendgrid Service. In order to
# work, it needs the following variables defined in the CI/CD configuration of
# the project:
#
# IMAGE_REPO_USERNAME - The name of the robot account used to push images to
# 'quay.io', for example 'openshift-unified-hybrid-cloud+jenkins'.
#
# IMAGE_REPO_PASSWORD - The token of the robot account used to push images to
# 'quay.io'.
#
# The machines that run this script need to have access to internet, so that
# the built images can be pushed to quay.io.
#


# Log in to the image registry:
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
  -pl :cos-fleetshard-operator