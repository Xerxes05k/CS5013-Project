# Image for the course submission (report §4). It runs the app with the default (dev)
# profile and its seed data, so a grader can log in straight away as the demo resident,
# who is also the GS:  ce24b128@smail.iitm.ac.in / cycle1234
#
# Build (on any machine, including Apple Silicon, build for amd64 so graders can run it):
#   docker buildx build --platform linux/amd64 -t jamuna-wheels:<commit-sha> --load .
# Run:
#   docker run --rm -p 8080:8080 jamuna-wheels:<commit-sha>     then open http://localhost:8080
#
# The live server does NOT use this image: it runs the jar under systemd (ADR 0006/0007,
# docs/DEPLOY.md). The database here lives inside the container and is reset each run.

# 1. Build the jar with the project's own Maven wrapper.
FROM eclipse-temurin:25-jdk AS build
WORKDIR /src
COPY app/.mvn .mvn
COPY app/mvnw app/pom.xml ./
# A Windows checkout can give mvnw CRLF line endings, which break it on Linux.
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw && ./mvnw -q -B dependency:go-offline
COPY app/src src
# Tests run in CI and on our laptops (./mvnw test); the image only needs the jar.
RUN ./mvnw -q -B package -DskipTests

# 2. Run it on a JRE with Tesseract for the ID-card scan. The -dev packages add the
#    plain libtesseract.so / libleptonica.so names that Tess4j and Lept4j load.
FROM eclipse-temurin:25-jre
RUN apt-get update \
    && apt-get install -y --no-install-recommends tesseract-ocr tesseract-ocr-eng libtesseract-dev libleptonica-dev \
    && rm -rf /var/lib/apt/lists/*
ENV TESSDATA_PREFIX=/usr/share/tesseract-ocr/5/tessdata
WORKDIR /app
COPY --from=build /src/target/cyclebooking-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-Xmx512m", "-jar", "app.jar"]
