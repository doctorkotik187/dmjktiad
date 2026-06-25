FROM clojure:temurin-21-tools-deps AS build
COPY . /app
WORKDIR /app
RUN clj -T:build uber

FROM eclipse-temurin:21-jre-jammy
COPY --from=build /app/target/dmjktiad-standalone.jar /app/dmjktiad.jar
EXPOSE 3000
ENTRYPOINT ["java", "-jar", "/app/dmjktiad.jar"]
