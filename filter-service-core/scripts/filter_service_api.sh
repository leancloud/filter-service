#!/bin/bash

create_filter() {
    local name=$1
    local expected_insertions=$2
    local fpp=$3
    local valid_period=${4:-0}

    curl -s -XPUT \
    -H 'content-type: application/json; charset=utf-8' \
    "http://localhost:8080/v1/bloomfilter/$name" \
    -d@- > /dev/null <<EOF 
    {
        "expectedInsertions": $expected_insertions,
        "fpp": $fpp,
        "validPeroid": $valid_period
    }
EOF
    echo "Filter: \"$name\" created."
}

set_value() {
    local name=$1
    local value=$2

    curl -s -XPOST \
    -H 'content-type: application/json; charset=utf-8' \
    "http://localhost:8080/v1/bloomfilter/$name/set" \
    -d@- > /dev/null <<EOF 
    {
        "value": $value
    }
EOF
}

delete_filter() {
    local name=$1
    curl -s -XDELETE "http://localhost:8080/v1/bloomfilter/$name" > /dev/null

    echo "Filter: \"$name\" deleted."
}

