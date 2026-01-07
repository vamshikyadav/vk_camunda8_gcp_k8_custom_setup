## Camunda documentation
https://unsupported.docs.camunda.io/8.1/docs/self-managed/platform-deployment/helm-kubernetes/platforms/google-gke/

## Make sure GKE’s PD CSI + SSD class is available

kubectl get sc

If you don’t see premium-rwo, create it (SSD PD via the PD CSI driver) --> checkout `gke-sc-premium-rwo.yaml ` file

kubectl apply -f gke-sc-premium-rwo.yaml

## Camunda 8 (Self-Managed) on GKE

SSD PVCs for Zeebe, Operate, Tasklist, and Elasticsearch (if you run the bundled one)
Connector pods enabled & aligned with the platform version
Scheduling preference for n1 nodes (optional)
Reasonable defaults that match Camunda’s production guide structure (tweak sizes/limits to your load)
align Connectors images with that platform release to avoid “can’t start” issues

Sample local values file --> camunda-gke-values.yaml

## Confirm image tags align with your platform version:

kubectl -n camunda get deploy,statefulset,pod | grep connector -i
kubectl -n camunda describe pod deploy/camunda-connectors-...  --> look for Image and Events
kubectl -n camunda logs deploy/camunda-connectors-... --tail=200

## SSD PVCs don’t bind
kubectl get sc
kubectl -n camunda get pvc

## want a UI but still avoid all the disk / SSD issues, we can do a “UI but no PVCs” dev setup:

Zeebe broker + gateway
Elasticsearch in-memory (no persistence / no PD)
Operate + Tasklist enabled
All persistence disabled → no PV / PD attach errors
Checkout --> bare-minimum-with-ui.yml

## Access UI 

http://camunda.example.com/operate
http://camunda.example.com/tasklist


ZEEBE_ADDRESS=camunda.example.com:26500 for workers
