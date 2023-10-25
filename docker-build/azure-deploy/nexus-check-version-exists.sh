#!/bin/bash

set -euo pipefail

# Check that version in pom file is correct
pomVersion=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
if [[ "$pomVersion" != "$OTP_VERSION" ]]
then
  echo "Unexpected version in pom.xml. Found: $pomVersion, expected: $OTP_VERSION"
  exit 1
fi

nexusVersion=$(curl -u resesok:$MVN_PASSWORD -X GET "https://nexus-dev.skanetrafiken.se/service/rest/v1/search?repository=maven-releases&group=se.skanetrafiken&name=resesok-otp&version=$OTP_VERSION" | jq -r '.items[0].version')

if [[ "$nexusVersion" != "$OTP_VERSION" ]]
then
  echo "Version not found on nexus, setting versionExistsOnNexus to false"
  echo "##vso[task.setvariable variable=versionExistsOnNexus]false"
else
  echo "Version found on nexus, setting versionExistsOnNexus to true"
  echo "##vso[task.setvariable variable=versionExistsOnNexus]true"
fi
