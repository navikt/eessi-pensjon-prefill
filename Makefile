DOCKER  := docker
GRADLE  := ./gradlew
NAIS    := nais
GIT     := git
VERSION := $(shell git describe --abbrev=0)
REGISTRY:= repo.adeo.no:5443

.PHONY: all build test docker docker-push release manifest

all: build test docker
release: tag docker-push

build:
	$(GRADLE) assemble

test:
	$(GRADLE) test

docker:
	$(NAIS) validate
	$(DOCKER) build --pull -t $(REGISTRY)/eessi-fagmodul .

docker-push:
	$(DOCKER) tag $(REGISTRY)/eessi-fagmodul $(REGISTRY)/eessi-fagmodul:$(VERSION)
	$(DOCKER) push $(REGISTRY)/eessi-fagmodul:$(VERSION)

tag:
	$(eval VERSION=$(shell echo $$(($(VERSION) + 1))))
	$(GIT) tag -a $(VERSION) -m "auto-tag from Makefile"

manifest:
	$(NAIS) upload --app eessi-fagmodul -v $(VERSION)
