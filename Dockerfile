FROM clojure:openjdk-11-tools-deps-1.10.1.502 AS builder
RUN curl -sL https://deb.nodesource.com/setup_12.x | bash - && apt install nodejs
COPY resources/public/index.html ./resources/public/index.html
COPY package.json package-lock.json ./
RUN npm install
COPY deps.edn ./
RUN clojure -A:dev:depstar -Stree
RUN mkdir -p resources/public/js
COPY resources/public/css ./resources/public/css
COPY resources/config.edn ./resources/config.edn
COPY shadow-cljs.edn ./
COPY src ./src
RUN ./node_modules/.bin/shadow-cljs release prod
RUN clojure -A:depstar:uberjar
RUN mv target/mygiftlistrocks.jar /mygiftlistrocks.jar

FROM openjdk:11.0.6-jre-slim-buster
COPY ca-certificate.crt /root/.postgresql/root.crt
RUN apt update && apt -y install --no-install-recommends curl && rm -rf /var/cache/apt/archives/*
COPY docker-healthcheck /usr/local/bin
HEALTHCHECK --start-period=60s CMD ["docker-healthcheck"]
COPY --from=builder /mygiftlistrocks.jar /mygiftlistrocks.jar
CMD java ${JVM_OPTS} -cp /mygiftlistrocks.jar clojure.main -m rocks.mygiftlist.main
