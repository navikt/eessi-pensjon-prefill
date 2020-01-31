FROM navikt/java:8-appdynamics

COPY build/libs/eessi-fagmodul-*.jar /app/app.jar

ENV USER.TIMEZONE 'Europe/Oslo'
ENV APPD_NAME eessi-pensjon
ENV APPD_TIER fagmodul
ENV APPD_ENABLED true
