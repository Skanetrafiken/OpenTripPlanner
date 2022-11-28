#!/bin/bash

. /code/bash-files/logging.sh

GRAPH_CONTAINER="resesok-graph"
NETEX_CONTAINER="resesok-netex"
NETEX_FILE_NAME="ST_netex.zip"
GTFS_DK_FILE_NAME="GTFS-filtered.zip"

function downloadNetexFiles {
  file_path=$1
  log_info "Downloading netex files"
  downloadFromAzureStorage $SA_NAME $NETEX_CONTAINER $NETEX_FILE_NAME $file_path/$NETEX_FILE_NAME
}

function downloadDkGtfsFiles {
  file_path=$1
  log_info "Downloading DK GTFS file"
  downloadFromAzureStorage $SA_NAME $GRAPH_CONTAINER $GTFS_DK_FILE_NAME $file_path/$GTFS_DK_FILE_NAME
}

function downloadOSMFile {
  file_path=$1
  OSM_FILE_NAME=$2
  log_info "Downloading OSM file"
  downloadFromAzureStorage $SA_NAME $GRAPH_CONTAINER $OSM_FILE_NAME $file_path/$OSM_FILE_NAME
}

function buildGraphFromNetexData {
  otp_jar_path=$1
  file_path=$2
  graph_path=$3
  log_info "Building Graph"
  if ! java -Xmx10500m -jar $otp_jar_path --save --build $graph_path; then
    log_error "Build OpenTripPlanner Graph database failed in bash $0"
    return 1
  fi

}

# From OTP command line version extract all version info and send to stdout in format major.minor.patch-commitHash
function getVersionString {
  versionString="$1"

  version=$(echo "$versionString" | sed 's/.*version: \([^ ]*\),.*/\1/')
  commit=$(echo "$versionString" | sed 's/.*commit: \([^ ]*\),.*/\1/')

  echo "$version-$commit"

}
