FROM java:8-jdk as builder
# Multi stage build - https://docs.docker.com/engine/userguide/eng-image/multistage-build/

# install maven
RUN apt-get update && apt-get install -y --no-install-recommends maven

# build and extract deegree (build twice due to dependency download issues)
RUN mkdir /build && mkdir /target
COPY ./ /build/
RUN cd /build/ && \
  mvn clean install -DskipTests;mvn clean install -DskipTests && \
  cp /build/deegree-services/deegree-webservices/target/deegree-webservices-*.war /build/deegree-webservices.war && \
  unzip -o /build/deegree-webservices.war -d /target

# add to image...
FROM tomcat:8.5-jre8
ENV LANG en_US.UTF-8

# add build info - see hooks/build and https://github.com/opencontainers/image-spec/blob/master/annotations.md
ARG BUILD_DATE
ARG VCS_REF
ARG VCS_URL
LABEL org.opencontainers.image.created=$BUILD_DATE \
  org.opencontainers.image.source=$VCS_URL \
  org.opencontainers.image.revision=$VCS_REF

EXPOSE 8080

# set default secrets ( override for production use! )
# consoleSecretKey="deegree"
ENV consoleSecretKey=000001544E797221:564344F65B8F9DDBA6A410E461E7801E10955F56D8679284966F400C68B6CEAB
ENV apiUser=deegree
ENV apiPass=deegree

RUN mkdir /root/.deegree && \
  rm -r /usr/local/tomcat/webapps/ROOT

COPY --from=builder /target /usr/local/tomcat/webapps/ROOT

#cmd:
# 1. configure schemas directory mapping
# 2. configure deegreeapi access
# 3. configure console access
# 4. start tomcat

CMD sed -i 's/.*<\/Host>/<Context docBase="\/root\/.deegree\/ws_production\/appschemas" path="\/schemas"\/>  <\/Host>/' /usr/local/tomcat/conf/server.xml \
     && sed -i '44i <user username="'"$apiUser"'" password="'"$apiPasswd"'" roles="deegree" \/> /' /usr/local/tomcat/conf/tomcat-users.xml \
     && echo $consoleSecretKey >/root/.deegree/console.pw \
     && mkdir -p /root/.deegree/ws_production/appschemas \
     && /usr/local/tomcat/bin/catalina.sh run