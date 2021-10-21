#!/bin/bash

rm -rf "${PWD}/.kcp"

exec "${KCP_ROOT}"/bin/kcp start
