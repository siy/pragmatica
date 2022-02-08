FROM ubuntu:21.04
ENV LANG C.UTF-8

ARG DEBIAN_FRONTEND=noninteractive

RUN apt-get update && apt-get upgrade -qy && \
    apt-get install -qy \
        build-essential \
        git \
        openjdk-17-jdk \
        make \
        mc \
        zip \
        unzip \
        xz-utils \
        bzip2 && \
    apt-get autoclean -y && \
    apt-get autoremove -y && \
    apt-get clean

WORKDIR /root

CMD ["/bin/bash"]
