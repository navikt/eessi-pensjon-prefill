FROM navikt/java:17-appdynamics

COPY build/libs/eessi-pensjon-prefill.jar /app/app.jar

ENV APPD_NAME eessi-pensjon
ENV APPD_TIER prefill
ENV APPD_ENABLED true
