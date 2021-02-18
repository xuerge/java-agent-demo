#!/usr/bin/env sh
java -javaagent:agent-jar-with-dependencies.jar -jar demo.jar | tee output.log