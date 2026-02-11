FROM amazoncorretto:21

RUN yum update -y && \
    yum install -y procps-ng vim iproute && \
    yum clean all

COPY ./moaticket.jar moaticket.jar
EXPOSE 8080
CMD ["java", "-jar", "moaticket.jar"]