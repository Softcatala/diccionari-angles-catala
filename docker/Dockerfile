FROM maven:3-eclipse-temurin-17 as build

COPY server /tmp/server
COPY diccionari /tmp/diccionari

RUN mvn -f /tmp/server/pom.xml clean package

RUN mv /tmp/server/target/engcat-0.0.1-SNAPSHOT-jar-with-dependencies.jar /tmp/engcat.jar

FROM eclipse-temurin:17-jre as prod

COPY diccionari /diccionari
COPY server-cfg /srv

COPY --from=build /tmp/engcat.jar /srv/engcat.jar

#prebuild
#RUN java -jar /srv/engcat.jar --config /srv/engcat.cfg

ENTRYPOINT /srv/entrypoint.sh
