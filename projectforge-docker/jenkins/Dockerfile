FROM jenkins:latest

USER root
RUN apt-get update

RUN apt-get install -y sudo software-properties-common apt-transport-https ca-certificates curl gnupg2 software-properties-common \
      && rm -rf /var/lib/apt/lists/* \
      && curl -fsSL https://download.docker.com/linux/debian/gpg | sudo apt-key add - \
      && add-apt-repository "deb [arch=amd64] https://download.docker.com/linux/debian $(lsb_release -cs) stable" \
	  && apt -qqqq update \
	  && apt install  -y -qqqqq docker-ce 

RUN groupadd docker || true
RUN groupadd staff || true

RUN gpasswd -a jenkins docker
RUN gpasswd -a jenkins staff

COPY .ssh /var/jenkins_home/.ssh

USER jenkins
COPY plugins.txt /usr/share/jenkins/plugins.txt
RUN /usr/local/bin/plugins.sh /usr/share/jenkins/plugins.txt
