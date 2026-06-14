# syntax=docker/dockerfile:1.7

FROM eclipse-temurin:25-jdk AS build
ARG MAVEN_VERSION=3.9.9
WORKDIR /build

RUN apt-get update \
 && apt-get install -y --no-install-recommends wget \
 && wget -q https://archive.apache.org/dist/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz -O /tmp/maven.tar.gz \
 && tar -xzf /tmp/maven.tar.gz -C /opt \
 && ln -s /opt/apache-maven-${MAVEN_VERSION}/bin/mvn /usr/local/bin/mvn \
 && rm -rf /var/lib/apt/lists/* /tmp/maven.tar.gz

COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -ntp dependency:go-offline

COPY src ./src
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -ntp package -DskipTests

FROM eclipse-temurin:25-jre
WORKDIR /app
RUN useradd --uid 1001 --no-create-home --shell /bin/false spring
USER spring
COPY --from=build /build/target/api-*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
