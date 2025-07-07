FROM jenkins/jenkins:latest

USER root

RUN apt-get update -qq && \
    apt-get install -qq -y apt-transport-https ca-certificates curl gnupg2 software-properties-common lsb-release && \
    curl -fsSL https://download.docker.com/linux/debian/gpg | apt-key add - && \
    echo "deb [arch=amd64] https://download.docker.com/linux/debian $(lsb_release -cs) stable" \
        > /etc/apt/sources.list.d/docker.list && \
    apt-get update -qq && \
    apt-get install -qq -y docker-ce-cli docker-compose-plugin docker-buildx-plugin && \
    groupadd docker && \
    usermod -aG docker jenkins

USER jenkins
