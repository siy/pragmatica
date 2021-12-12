#!/bin/sh

#docker run --ulimit memlock=16777216:16777216 --rm -ti -v "$(realpath .):/root" pragmatica-builder:latest
docker run --privileged --rm -ti -v "$(realpath .):/root" pragmatica-builder:latest

