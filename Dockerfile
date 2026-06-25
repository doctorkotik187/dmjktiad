FROM clojure:temurin-21-tools-deps AS build
COPY . /app
WORKDIR /app
RUN make uberjar

FROM eclipse-temurin:21-jre-jammy
COPY --from=build /app/target/dmjktiad-standalone.jar /app/dmjktiad.jar
EXPOSE 3000
ENTRYPOINT ["java", "-jar", "/app/dmjktiad.jar"]
