FROM maven:3.6-jdk-11-slim
MAINTAINER Stian Soiland-Reyes <stain@apache.org>


RUN mkdir -p /usr/src/app
WORKDIR /usr/src/app

# Top-level files (ignoring .git etc)
ADD pom.xml LICENSE.txt NOTICE.txt README.md /usr/src/app/

# add src/ (which often change)
ADD src /usr/src/app/src
# Skip tests while building as that requires a local mongodb
RUN mvn clean package -DskipTests && cp target/research-object-service-*.jar /usr/lib/research-object-service.jar && rm -rf target

# NOTE: ~/.m2/repository is a VOLUME and so will be deleted anyway
# This also means that every docker build downloads all of it..

#WORKDIR /tmp

EXPOSE 8080
ENV LC_ALL C.UTF-8
#CMD ["/usr/bin/java", "-jar", "/usr/lib/research-object-service.jar"]

# For now need to run from source tree as ResearchObjectServiceApplication
# reads profile documents as files
CMD ["mvn", "spring-boot:run"]
