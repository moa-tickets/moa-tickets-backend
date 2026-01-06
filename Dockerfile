FROM amazoncorretto:21
COPY ./moaticket.jar moaticket
EXPOSE 8080
CMD ["java", "-jar", "moaticket.jar"]