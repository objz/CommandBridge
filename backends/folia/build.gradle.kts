plugins {
    `java-library`
    `checkstyle`
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
    compileOnly("dev.jorel:commandapi-paper-core:11.2.0")
    compileOnly("org.spongepowered:configurate-yaml:4.2.0")
}
