FROM navikt/java:8

COPY build/libs/eessi-fagmodul-*.jar /app/app.jar

ENV JAVA_OPTS -Dspring.profiles.active=prod
