FROM openjdk:11.0.5-jre-slim-buster

COPY target/mygiftlistrocks.jar /mygiftlistrocks.jar

ENV PORT ""
ENV JWK_ENDPOINT ""
ENV DATABASE_URL ""

CMD java -cp mygiftlistrocks.jar clojure.main -m rocks.mygiftlist.main
