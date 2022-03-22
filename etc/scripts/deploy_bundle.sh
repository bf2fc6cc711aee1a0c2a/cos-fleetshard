#!/bin/bash -ex

managedtenants --addon-name connectors-operator --addons-dir bundles bundles --quay-org $QUAY_ORG
