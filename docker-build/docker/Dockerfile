FROM openjdk:17.0.2-slim-buster

RUN apt update > /dev/null 2>&1
RUN apt -y install jq ca-certificates curl apt-transport-https lsb-release gnupg > /dev/null 2>&1
RUN curl -sL https://aka.ms/InstallAzureCLIDeb | bash
RUN apt-get -y install zip unzip

# install application insights
RUN mkdir /code

WORKDIR /code

COPY target/otp-*-shaded.jar /code/otp-shaded.jar
COPY docker-build/docker/services/ /code

RUN curl -LO https://github.com/microsoft/ApplicationInsights-Java/releases/download/3.2.0-BETA.2/applicationinsights-agent-3.2.0-BETA.2.jar
RUN mv applicationinsights-agent-3.2.0-BETA.2.jar /code/appInsights/applicationinsights-agent.jar

RUN chmod a+x /code/*.sh

EXPOSE 8080

COPY docker-build/docker/scuttle/scuttle /bin/scuttle
ENV ENVOY_ADMIN_API="http://127.0.0.1:15000"
ENV ISTIO_QUIT_API="http://127.0.0.1:15020"
ENV APPLICATIONINSIGHTS_ROLE_NAME="OpenTripPlanner"
ENTRYPOINT ["scuttle", "bash", "/code/entrypoint.sh"]
