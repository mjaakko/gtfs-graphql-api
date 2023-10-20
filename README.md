# gtfs-graphql-api [![Build project with Gradle](https://github.com/mjaakko/gtfs-graphql-api/actions/workflows/build.yml/badge.svg)](https://github.com/mjaakko/gtfs-graphql-api/actions/workflows/build.yml)
GraphQL API for GTFS and GTFS-RT data

# Running

Docker image is available from [DockerHub](https://hub.docker.com/r/mjaakko/gtfs-graphql-api) with name `mjaakko:gtfs-graphql-api`. Currently only available tag is `edge`, which is the latest build from the main branch.

## Enviroment variables

* `GTFSRT_VEHICLE_POSITION_URL`: URL to the vehicle position feed
* `GTFS_PATH`: Path to the GTFS file
* `GTFS_URL`: URL to the GTFS feed
* `GTFS_UPDATE_INTERVAL`: interval how often to update the GTFS feed from the URL

Only `GTFS_PATH` or `GTFS_URL` can be used at the same time