#!/bin/bash -e

./mvnw ${MAVEN_ARGS} clean install -Psourcecheck "$@"