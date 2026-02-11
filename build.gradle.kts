import com.google.protobuf.gradle.*

plugins {
    java
    id("org.springframework.boot") version "3.4.1"
    id("io.spring.dependency-management") version "1.1.7"
    id("com.google.protobuf") version "0.9.4"
}

group = "com.pension"
version = "1.0.0"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

repositories {
    mavenCentral()
}

val grpcVersion = "1.62.2"
val protobufVersion = "3.25.3"

dependencies {
    // WebFlux
    implementation("org.springframework.boot:spring-boot-starter-webflux")

    // Jackson Blackbird
    implementation("com.fasterxml.jackson.module:jackson-module-blackbird")

    // JSON Patch
    implementation("com.flipkart.zjsonpatch:zjsonpatch:0.4.16")

    // gRPC Spring Boot Starter
    implementation("net.devh:grpc-server-spring-boot-starter:3.1.0.RELEASE")

    // Protobuf utilities (for Struct/Value)
    implementation("com.google.protobuf:protobuf-java-util:$protobufVersion")

    // javax.annotation for gRPC generated code
    implementation("javax.annotation:javax.annotation-api:1.3.2")
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:$protobufVersion"
    }
    plugins {
        create("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:$grpcVersion"
        }
    }
    generateProtoTasks {
        all().forEach { task ->
            task.plugins {
                create("grpc")
            }
        }
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}
