# My Gift List Rocks!

## Migrations

Run them using

    docker service create -e POSTGRES_HOSTNAME=postgres -e POSTGRES_PORT=5432 -e POSTGRES_DB=mygiftlistrocks --network mygiftlistrocks_backend-private --secret db_user --secret db_password --restart-condition none --name db-migrator cpodonnell/mygiftlistrocks:migrate-latest

on the swarm host. Once finished, remove the service with

    docker service rm db-migrator
