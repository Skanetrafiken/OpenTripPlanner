#!/bin/bash

. /code/bash-files/logging.sh

function downloadFromAzureStorage {
  account_name=$1
  container_name=$2
  name=$3
  file=$4
  if ! az storage blob download \
    --account-name $account_name \
    --container-name $container_name \
    --name $name \
    --file $file \
    --auth-mode key; then

      log_error "Failed to download file $name for account name $account_name"
      return 1
  fi
}

function uploadToAzureStorage {
  account_name=$1
  container_name=$2
  file=$3
  name=$4
  echo "Uploading file $file to azure storage"
  if ! az storage blob upload \
    --account-name $account_name \
    --container-name $container_name \
    --name $name \
    --file $file \
    --auth-mode key; then
      log_error "WARNING: Upload file $file to azure storage failed"
      return 1
  fi
}

function getKeyVaultValue {
  vault=$1
  key=$2

  # Get value from KeyVault
  if ! applicationInsightsKey=$(az keyvault secret show \
    --vault-name "$vault" \
    --name "$key" \
    --query value); then

    log_error "Error fetching value for KeyVault key $key in keyvault $vault"
    return 1
  fi

  echo $applicationInsightsKey
}
