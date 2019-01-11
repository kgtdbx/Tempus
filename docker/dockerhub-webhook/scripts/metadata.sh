#!/bin/bash

kubectl --kubeconfig /src/scripts/kubeconfig get pods -oname |grep redtail-metadata-api |xargs kubectl --kubeconfig /src/scripts/kubeconfig delete >> ./webhook_log

echo "last updated at: "$(date -u) >> ./webhook_log





