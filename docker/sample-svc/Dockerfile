# Example consumer of the PCIS Java 21 base image.
# Build base first: IMAGE_TAG=local ./docker/base/build.sh
ARG BASE_IMAGE=pcis-base-java21:local
FROM ${BASE_IMAGE}

COPY fixtures/sample-app.jar /app/app.jar
USER nonroot
CMD ["/app/app.jar"]
