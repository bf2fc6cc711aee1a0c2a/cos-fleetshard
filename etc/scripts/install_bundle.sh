#!/bin/bash -ex

INDEX_TAG=${GITHUB_SHA::7}
NS=redhat-openshift-connectors
kubectl create ns $NS

kubectl create secret -n $NS docker-registry --docker-server=quay.io --docker-username=$QUAY_USER --docker-password="$QUAY_APIKEY" addon-pullsecret

echo "$CLUSTER_SECRET" | kubectl create -n $NS -f -

kubectl create -f - <<EOF
apiVersion: operators.coreos.com/v1alpha1
kind: CatalogSource
metadata:
  name: redhat-openshift-connectors
  namespace: olm
spec:
  sourceType: grpc
  image: quay.io/$QUAY_ORG/connectors-operator-index:$INDEX_TAG
EOF

kubectl create -f - <<EOF
kind: OperatorGroup
apiVersion: operators.coreos.com/v1
metadata:
  name: connectors-addon
  namespace: $NS
spec:
  targetNamespaces:
  - $NS
EOF


kubectl create -f - <<EOF
apiVersion: operators.coreos.com/v1alpha1
kind: Subscription
metadata:
  name: cos-fleetshard-sync
  namespace: $NS
spec:
  channel: alpha
  installPlanApproval: Automatic
  name: cos-fleetshard-sync
  source: redhat-openshift-connectors
  sourceNamespace: olm
EOF

