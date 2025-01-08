.PHONY: clean jacoco badua pitest

clean:
	mvn clean

jacoco:
	mvn verify -P isw2-jacoco

badua:
	mvn verify -P isw2-ba-dua -Dmaven.test.failure.ignore=true

pitest:
	mvn verify -P isw2-pitest

all:
	mvn clean
	mvn verify -P 'isw2-jacoco, isw2-pitest'