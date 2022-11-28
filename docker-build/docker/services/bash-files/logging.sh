#!/bin/bash

function log_info {
  command echo "[$(date +'%Y-%m-%d %H:%M:%S.%3N'])[INFO]"  ${FUNCNAME[1]} "$@" 2>&1 | tee -a /var/log/entrypoint.log
}

function log_error {
  command echo "[$(date +'%Y-%m-%d %H:%M:%S.%3N'])[ERROR]"  ${FUNCNAME[1]} "$@" 2>&1 | tee -a /var/log/entrypoint.log
}
