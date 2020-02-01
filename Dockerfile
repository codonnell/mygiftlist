FROM openjdk:11.0.5-jre-slim-buster

COPY ca-certificate.crt /root/.postgresql/root.crt

RUN apt update && apt -y install --no-install-recommends curl && rm -rf /var/cache/apt/archives/*

COPY docker-healthcheck /usr/local/bin

HEALTHCHECK --start-period=60s CMD ["docker-healthcheck"]

COPY target/mygiftlistrocks.jar /mygiftlistrocks.jar

CMD java ${JVM_OPTS} -cp /mygiftlistrocks.jar clojure.main -m rocks.mygiftlist.main
