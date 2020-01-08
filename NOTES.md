# Infrastructure

In development, we use docker-compose for a postres database.

In production, we'll also use docker to host postgres. This will be fine because we'll be running a one node swarm. If we add more nodes, we'll need to add node affinity to the postgres container. We'll use a JRE container to host the server. We'll use a traefik container to manage SSL termination. This would also need node affinity with a larger-than-one-node cluster, since it will need a volume to hold its SSL cert.
