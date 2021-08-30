#!/bin/bash -ex

export MAVEN_ARGS="-V -ntp -Dhttp.keepAlive=false -e"

./mvnw ${MAVEN_ARGS} clean install -Psourcecheck