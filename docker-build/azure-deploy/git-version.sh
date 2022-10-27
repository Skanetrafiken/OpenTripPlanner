#!/bin/bash

version=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
versionTag="$version.RELEASE"
echo "New version : $versionTag"

gitUser=$(git config user.name)
gitEmail=$(git config user.email)
[[ -z "$gitUser" ]] && git config user.name =  "resesok-adapter Azure Pipelines"
[[ -z "$gitEmail" ]] && git config user.email = "noreply@resesok.adapter.se"

# Get rid of all local tags
git -c http.extraheader="AUTHORIZATION: bearer $DEVOPS_ACCESSTOKEN" fetch --prune  origin --tags -f

nexusVersion=$(curl -u resesok:$MVN_PASSWORD -X GET "https://nexus-dev.skanetrafiken.se/service/rest/v1/search?repository=maven-releases&group=se.skanetrafiken&name=resesok-otp&version=$version" | python -c '
import json,sys;
obj=json.load(sys.stdin);
if obj["items"]:
  print obj["items"][0]["version"]
')

if [[ "$nexusVersion" != "$version" ]]
then
  echo "Version not found on nexus, setting SKIP_DEPLOY to false"
  echo "##vso[task.setvariable variable=SKIP_DEPLOY]false"
else
  echo "Version was found on nexus, setting SKIP_DEPLOY flag true."
  echo "##vso[task.setvariable variable=SKIP_DEPLOY]true"
fi

echo "##vso[task.setvariable variable=OTP_VERSION]$version"

if [ $(git tag -l $versionTag) ]; then
  echo "Tag already exists, skipping."
  exit 0
fi

# tag the master branch
echo "# Create local tag $versionTag"
if ! git tag -a $versionTag -m "New RELEASE $new_version"; then
  echo "# Error while creating tag $versionTag"
  exit 1
fi

echo "# Push tag to origin"
if ! git -c http.extraheader="AUTHORIZATION: bearer $DEVOPS_ACCESSTOKEN" push origin "$versionTag"; then
  echo "# Error push tag $versionTag to origin"
  exit 1
fi
