#!/bin/bash

# Info - this script is meant for local development purposes and easier setup/re-setup.
# Please note that it will try to remove previous container, which can lead for example to removal of database data.
# That is not a problem on local setup because we are using Liquibase (and db will be created on application startup).
# But additional data created by you locally will be lost.
# On Production environment due to a different setup running this script will not do anything.

# Standard output that is being echoed at the end is the docker container id where mysql image is now running

output="$(docker run --name mysql-encounterhandlerpf2e -p 3306:3306 -e MYSQL_ROOT_PASSWORD=PASSWORD -d mysql 2>&1)"

if [[ $output == *"docker: error during connect: This error may indicate that the docker daemon is not running"* ]]; then
  echo "You need to run your docker daemon/manager/desktop first. Full error below:"
  echo "$output"
  exit 0
fi

if [[ $output == *"Error response from daemon: Conflict. The container name"* ]]; then
  container_id=$(echo "$output" | sed -n 's/.*container \"\([^"]*\)\".*/\1/p')
  echo "Stopping and removing previous container with id '$container_id'."
  output="$(docker stop "$container_id" 2>&1)"
  if [[ "$output" != "$container_id" ]]; then
    echo "Cannot stop running container. Full error below:"
    echo "$output"
    exit 0
  fi
  output="$(docker rm "$container_id" 2>&1)"
  if [[ "$output" != "$container_id" ]]; then
    echo "Container stopped, but could not be removed. Full error below:"
    echo "$output"
    exit 0
  fi
  output="$(docker run --name mysql-encounterhandlerpf2e -p 3306:3306 -e MYSQL_ROOT_PASSWORD=PASSWORD -d mysql 2>&1)"
fi

echo "$output"