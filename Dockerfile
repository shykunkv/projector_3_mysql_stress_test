FROM openjdk:8
ADD build/libs/mysql-stress-test.jar mysql-stress-test.jar
EXPOSE 9000
ENTRYPOINT ["java", "-jar", "mysql-stress-test.jar"]