#!/bin/bash

docker run \
    --rm \
    -ti \
    edenhill/kafkacat:1.6.0 \
        kafkacat -b ${KAFKA_URL} \
        -X security.protocol=SASL_SSL \
        -X sasl.mechanism=PLAIN \
        -X sasl.username=${KAFKA_USR} \
        -X sasl.password=${KAFKA_PWD} \
        "$@"