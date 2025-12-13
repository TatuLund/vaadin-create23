#!/bin/sh
export JAVA_OPTS="-javaagent:/otel/opentelemetry-javaagent.jar -Dvaadin.productionMode=true -Dhibernate.config=hibernate-prod.cfg.xml -Dgenerate.data=false -Dorg.apache.catalina.connector.RECYCLE_FACADES=false"
