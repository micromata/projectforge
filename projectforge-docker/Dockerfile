FROM debian:jessie
MAINTAINER Micromata
ENV http_proxy=http://proxy.micromata.priv:3128
ENV https_proxy=http://proxy.micromata.priv:3128

## Install requirements ( oracle-java7-installer )
RUN echo "deb http://ppa.launchpad.net/webupd8team/java/ubuntu precise main" | tee /etc/apt/sources.list.d/webupd8team-java.list && \
    echo "deb-src http://ppa.launchpad.net/webupd8team/java/ubuntu precise main" | tee -a /etc/apt/sources.list.d/webupd8team-java.list && \
    apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys EEA14886 && \
    apt-get update && \
    apt-get install -y debconf-utils --no-install-recommends --no-install-suggests && \
    echo "oracle-java8-installer shared/accepted-oracle-license-v1-1 boolean true" | debconf-set-selections && \
    apt-get install -y oracle-java8-installer oracle-java8-set-default --no-install-recommends --no-install-suggests && \
    rm -rf /var/lib/apt/lists/* /var/cache/oracle-jdk8-installer

EXPOSE 8080

WORKDIR /usr/share

ENTRYPOINT ["/usr/bin/java", "-jar", "/usr/share/projectforge-application-6.13.0-SNAPSHOT.jar"]

# Add the service itself
ADD ./target/extra-resources/target/projectforge-application-6.13.0-SNAPSHOT.jar /usr/share/projectforge-application-6.13.0-SNAPSHOT.jar

# Add the properties
ADD ./target/extra-resources/application.properties /usr/share/application.properties