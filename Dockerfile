FROM openjdk:17-alpine
VOLUME /tmp
ADD ./servicio-operaciones.jar servicio-operaciones.jar
ENTRYPOINT ["java","-jar","/servicio-operaciones.jar"]