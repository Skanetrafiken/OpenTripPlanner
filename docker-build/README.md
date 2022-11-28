# Skanetrafiken build configurations

This folder will contain configurations neccessary only for Skanetrafiken not EnTur

# Azure
The folder `azure` contains the Azure Pipelines configuration files

    * The deployment.yaml file will build the project, create docker image and manage Helm charts. Then it will trigger a deploy job.
    * The ci-deployment.yaml will trigger a build with all Unit tests. Good for pull requests
    * The swagger.json file is to be uploaded with the deployment for the API
    
# Docker
In the `docker` folder there is a Docker file to be built in the Azure Pipeline and deployed to Azure container repository for deployment
in Kubernetes.

In the `docker/docker-local` folder there is an docker-compose.yml to run the docker image locally for testing.

The files to include in Docker image are in the `docker/services` folder.

Before building Docker image it's important to run the `copy-docker-data.sh` that will fetch the latest built jar. Of course the jar must 
exist, so first do a maven build. 

# Helm
The Helm charts are for the Kubernetes deploy.
