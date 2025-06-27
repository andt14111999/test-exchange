#!/bin/bash

# Get the directory where the script is located
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# Đặt các biến môi trường
ENV="development"
MAVEN_CACHE_VOLUME="maven-repo-cache"

# Kiểm tra xem base image đã tồn tại chưa
if [[ "$(docker images -q exchangeengine/rocksdb-base:latest-9.7.3 2> /dev/null)" == "" ]]; then
  echo "===== Building RocksDB base image (version 9.7.3) ====="
  docker build -t exchangeengine/rocksdb-base:latest-9.7.3 -f "$SCRIPT_DIR/Dockerfile-rocksdb-base" "$SCRIPT_DIR"
  echo "===== RocksDB base image built successfully ====="
fi

# Kiểm tra xem volume Maven cache đã tồn tại chưa
if [[ "$(docker volume ls -q -f name=$MAVEN_CACHE_VOLUME 2> /dev/null)" == "" ]]; then
  docker volume create $MAVEN_CACHE_VOLUME
fi

# Kiểm tra xem các container đã tồn tại chưa
if [[ "$(docker ps -q -f name=Exchange-kafka)" == "" ]] || [[ "$(docker ps -q -f name=Exchange-kafka-ui)" == "" ]]; then
  docker compose -f "$SCRIPT_DIR/docker-compose-dev.yml" up -d kafka kafka-ui
  sleep 10
fi

# Chỉ build và restart app container
docker compose -f "$SCRIPT_DIR/docker-compose-dev.yml" build --build-arg BUILDKIT_INLINE_CACHE=1 app

# Stop app container nếu đang chạy
if [[ "$(docker ps -q -f name=Exchange-engine)" != "" ]]; then
  docker stop Exchange-engine
  docker rm Exchange-engine
fi

# Start app container với volume Maven cache
docker compose -f "$SCRIPT_DIR/docker-compose-dev.yml" up -d app
