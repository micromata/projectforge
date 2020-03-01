FROM postgres:11.2


# This is a Debian system, update system packages (if needed)
RUN apt-get update && apt-get -y upgrade

MAINTAINER Micromata

environment:
      - POSTGRES_USER:'foo'
      - POSTGRES_PASSWORD:'foobar'
      - POSTGRES_DB:'baz'
