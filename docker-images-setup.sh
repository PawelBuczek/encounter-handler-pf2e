#!/bin/bash

# Info - this script is meant for local development purposes and easier setup/re-setup.
# Please note that it will try to remove previous container, which can lead for example to removal of database data.
# That is not a problem on local setup because we are using Liquibase (and db will be created on application startup).
# But additional data created by you locally will be lost.

# Standard output that is being echoed at the end is the docker container id where mysql image is now running

output="$(docker images -q mysql 2>&1)"

if [[ $output == *"error during connect"* ]]; then
  echo "You need to run your docker daemon/manager/desktop first. Full error below:"
  echo "$output"
  exit 0
fi

if [[ $output == "" ]]; then
  echo "It seems that mysql image is not pulled."
  echo "Please pull it using 'docker pull mysql' command and then run this script again"
  exit 0
fi

container_name="mysql-encounterhandlerpf2e"

output="$(docker container inspect -f '{{.ID}}' $container_name 2>&1)"

if [[ $output != *"No such container"* ]]; then
  container_id=$output

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
fi

output="$(docker run --name $container_name -p 3306:3306 -e MYSQL_ROOT_PASSWORD=root -d mysql 2>&1)"

if [[ $output == *"error"* ]]; then
  echo "Cannot run container. Full error below:"
  echo "$output"
  exit 0
fi

for _ in {1..7} ; do
  if [ "$( docker container inspect -f '{{.State.Running}}' $container_name )" = "true" ]; then
    echo "docker container with name '$container_name' and id '$output' is now running."
    echo "it still may need some time (~5 seconds on my machine) before it can be used."
    break
  else
    sleep 1
  fi
done
