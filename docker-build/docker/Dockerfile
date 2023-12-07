FROM tjpcrprod.azurecr.io/temurin.jre21.jammy:2023-10-24

ARG APPLICATION_VERSION="UnknownVersion"

RUN apt-get update && \
    apt-get -y install jq ca-certificates curl apt-transport-https lsb-release gnupg zip unzip && \
    curl -sL https://aka.ms/InstallAzureCLIDeb | bash && \
    apt-get autoremove -y && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/* && \
    mkdir /code

COPY target/resesok-otp-*-shaded.jar /code/otp-shaded.jar
COPY docker-build/docker/services/ /code
COPY docker-build/docker/scuttle/scuttle /bin/scuttle

RUN curl -L -o /code/appInsights/applicationinsights-agent.jar \
    "https://github.com/microsoft/ApplicationInsights-Java/releases/download/3.4.18/applicationinsights-agent-3.4.18.jar"

WORKDIR /code

EXPOSE 8080

ENV APPLICATION_VERSION=$APPLICATION_VERSION
ENV ENVOY_ADMIN_API="http://127.0.0.1:15000"
ENV ISTIO_QUIT_API="http://127.0.0.1:15020"
ENV APPLICATIONINSIGHTS_ROLE_NAME="OpenTripPlanner"

ENTRYPOINT ["scuttle", "bash", "/code/entrypoint.sh"]
