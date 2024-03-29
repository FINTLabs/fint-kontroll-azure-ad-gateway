# FINT Kontroll - Azure AD Gateway

## Start docker-containers

docker-compose -f docker-compose.yaml up

## Look at data

localhost:

## Using kafkacat/kcat on localhost
*Debian 11: Install tool with: _apt-get install kafkacat_*

    kafkacat -P -b localhost:9092 -t fintlabs-no.kontroll.entity.resource-group -K!

## Run on localhost

cd k8s/manifests/local
kaf fint-kontroll-localhost.yaml

### Known problems

#### Dev iterations

    # Iterate/develop/re-test
    # Finish debugging - take down
    kdelf <filename>

    # Investigate running pods
    kgp <containername>
    # Error from server: container <container> is waiting to start: ContainerCreating
    # Check events
    kubectl get events
    # Rerun kaf <filename> to test-deploy
    kaf <filename>

#### Kubernetes cheat sheet

    # List all resources in current namespace
    kga
    # kga --namespace kubernetes-dashboard
    # List namespaces - make sure kubernetes-dashboard is there
    kgns
    # List secrets - assert kubernetes-dashboard-certs is created
    kgseca
    # Delete stale deployment
    kdeld --namespace kubernetes-dashboard flyt-fint-resource-gateway
    # Delete stale service
    kdels --namespace kubernetes-dashboard <service>
    # Follow specific log
    klf --namespace kubernetes-dashboard <pod> 