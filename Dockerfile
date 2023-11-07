FROM tomcat:jdk14-openjdk-oracle

ADD ../rhies-client-registry/rhies-client-registry-server/target/rhies-client-registry-server-1.0.0.war /usr/local/tomcat/webapps/clientregistry.war

EXPOSE 8080

