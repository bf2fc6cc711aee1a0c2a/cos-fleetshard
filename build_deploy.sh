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
# QUAY_USER - The name of the robot account used to push images to
# 'quay.io', for example 'openshift-unified-hybrid-cloud+jenkins'.
#
# QUAY_TOKEN - The token of the robot account used to push images to
# 'quay.io'.
#
# The machines that run this script need to have access to internet, so that
# the built images can be pushed to quay.io.

QUAY_ORG="${QUAY_ORG:-lgarciaac}"

if [ -z "${VERSION}" ]; then
  # The version should be the short hash from git. This is what the deployment
  # process expects.
  VERSION="$(git log --pretty=format:'%h' -n 1)"
fi

if [ -z "${IMAGE}" ]; then
  IMAGE="quay.io/${QUAY_ORG}/cos-fleetshard-operator:${VERSION}"
fi

# Set the directory for docker configuration:
export DOCKER_CONFIG="${PWD}/.docker"

# Log in to the image registry:
if [ -z "${QUAY_USER}" ]; then
  echo "The quay.io push user name hasn't been provided."
  echo "Make sure to set the QUAY_USER environment variable."
  exit 1
fi
if [ -z "${QUAY_TOKEN}" ]; then
  echo "The quay.io push token hasn't been provided."
  echo "Make sure to set the QUAY_TOKEN environment variable."
  exit 1
fi

# Set up the docker config directory
mkdir -p "${DOCKER_CONFIG}"

export CONTAINER_REGISTRY=quay.io
export CONTAINER_REGISTRY_USR="${QUAY_USER}"
export CONTAINER_REGISTRY_PWD="${QUAY_TOKEN}"
export CONTAINER_IMAGE="${IMAGE}"

./mvnw clean install
./mvnw clean package -Pcontainer-push -pl :cos-fleetshard-operator
