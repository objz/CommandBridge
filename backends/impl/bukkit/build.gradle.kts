plugins {
    `java-library`
}

java {
    toolchain { languageVersion.set(JavaLanguageVersion.of(21)) }
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}
dependencies {
    implementation(project(":backends"))          // WsClient, MessageRouter, etc.
    implementation(project(":backends:loader"))   // PlatformAdapter API
    compileOnly("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")
}
