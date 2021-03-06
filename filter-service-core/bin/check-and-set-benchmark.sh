#!/bin/bash

BASE_DIR=$(dirname $0)/..
. $BASE_DIR/scripts/filter_service_api.sh --source-only

create_filter check-set-bench 100000000 0.001

wrk --threads 4 --connections 20 --duration 30s --latency --script $BASE_DIR/scripts/check-and-set-benchmark.lua http://127.0.0.1:8080/

delete_filter check-set-bench 
