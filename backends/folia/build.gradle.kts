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
    implementation(project(":backends"))
    compileOnly("dev.folia:folia-api:1.21.6-R0.1-SNAPSHOT")
    compileOnly("dev.jorel:commandapi-paper-core:11.0.0")
}
