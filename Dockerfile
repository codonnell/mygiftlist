FROM openjdk:11.0.5-jre-slim-buster

COPY target/mygiftlistrocks.jar /mygiftlistrocks.jar

CMD java ${JVM_OPTS} -cp /mygiftlistrocks.jar clojure.main -m rocks.mygiftlist.main
