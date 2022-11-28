#!/bin/bash
. /code/bash-files/graph-build.sh
. /code/bash-files/azure-utils.sh
. /code/bash-files/logging.sh

FILE_TMP_PATH=/code/otpdata/malmo/
FILE_ZIP_PATH=/code/otpdata/malmo/Graph.zip
GRAPH_DATA_PATH=/code/otpdata/malmo
OTP_JAR_PATH=/code/otp-shaded.jar

GRAPH_CONTAINER="resesok-graph"
NETEX_FILENAME="ST_netex.zip"
OSM_FILENAME="sweden-filtered.osm.pbf"
OSM_DK_FILENAME="denmark-oresund.osm.pbf"
SA_NAME="ressa$ENVIRONMENT"

log_info "Running Entrypoint.sh.."

# If graph name not provided as env variable
# Set name based on version and hash
if [[ -z "${GRAPH_NAME}" ]]; then
  otpVersion=$(java -jar /code/otp-shaded.jar --version)
  if ! VERSION_HASH=$(getVersionString "$otpVersion"); then
    log_error "Failed to get OTP version or hash"
    exit 1
  fi
  GRAPH_NAME=GRAPH-$VERSION_HASH.zip
  UPLOAD_TO_AZURE=true
else
  log_info "Using custom graph file: $GRAPH_NAME. If file is missing no graph will be build."
  UPLOAD_TO_AZURE=false
fi

keyvault=$OtpKeyVaultName

log_info "Logging into azure.."

if ! az login --identity; then
  log_error "Login to Azure using assigned identity failed"
  exit 1
fi

# Get value from KeyVault
if ! applicationInsightsKey=$(getKeyVaultValue $keyvault "applicationInsightsKey"); then
  # Warn about not finding this key, but don't stop the startup
  log_error "Error fetching value for KeyVault key $applicationInsightsKey in keyvault $keyvault"
  exit 1
fi

# Cut of " chars and set as environment variable
export applicationInsightsKey=${applicationInsightsKey:1:-1}
export APPLICATIONINSIGHTS_CONNECTION_STRING="InstrumentationKey=$applicationInsightsKey;"

#applicationinsights.json needs to be in the same folder as insights agent
mv /code/appInsights/applicationinsights.json /code/appInsights/applicationinsights.json

# Attempt to download graph from azure storage
downloadFromAzureStorage $SA_NAME $GRAPH_CONTAINER $GRAPH_NAME $FILE_ZIP_PATH

if [ -s $FILE_ZIP_PATH ] ;
then
  log_info "Found $FILE_ZIP_PATH, unzipping.."
  unzip $FILE_ZIP_PATH -d $FILE_TMP_PATH
elif [ "$UPLOAD_TO_AZURE" = true ]; then
  log_info "** WARNING: Downloaded file ($FILE_ZIP_PATH) is empty or not present**"
  # Download netex + OSM data from azure storage in background
  downloadNetexFiles $GRAPH_DATA_PATH &
  downloadDkGtfsFiles $GRAPH_DATA_PATH &
  downloadOSMFile $GRAPH_DATA_PATH $OSM_FILENAME &
  downloadOSMFile $GRAPH_DATA_PATH $OSM_DK_FILENAME &
  # Wait for download of Netex/OSM to finish
  wait
  if [ -s $GRAPH_DATA_PATH/$NETEX_FILENAME ] && [ -s $GRAPH_DATA_PATH/$OSM_FILENAME ] ;
  then

    if ! buildGraphFromNetexData $OTP_JAR_PATH $GRAPH_DATA_PATH $FILE_TMP_PATH; then
      log_error "Building graph failed"
      exit 1
    fi
    rm $GRAPH_DATA_PATH/$NETEX_FILENAME $GRAPH_DATA_PATH/$OSM_FILENAME
    zip -j /code/${GRAPH_NAME} ${FILE_TMP_PATH}graph.obj
    uploadToAzureStorage $SA_NAME $GRAPH_CONTAINER /code/${GRAPH_NAME} $GRAPH_NAME
  else
    ls -al $GRAPH_DATA_PATH
    log_error "ERROR: Empty netex or OSM file, quitting."
    exit 1
  fi
else
  log_error "$GRAPH_NAME does not exist"
  exit 1
fi

cd /code || exit 1

log_info "Start java OTP jar"

exec java -javaagent:/code/appInsights/applicationinsights-agent.jar -Xms5120m -Xmx10500m -jar $OTP_JAR_PATH --load $GRAPH_DATA_PATH
