plugins {
    id("java")
    id("com.gradleup.shadow") version "8.3.3"
}

group = "dev.consti"
version = "2.1.1"

repositories {
    mavenCentral()
    maven { url = uri("https://repo.papermc.io/repository/maven-public/") }
    maven { url = uri("https://repo.codemc.org/repository/maven-public/") }
    maven { url = uri("https://jitpack.io") }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
    implementation("org.ow2.asm:asm:9.7")
    implementation("com.github.72-S:FoundationLib:-SNAPSHOT")
    implementation("dev.jorel:commandapi-bukkit-shade-mojang-mapped:9.7.0")
    compileOnly("dev.jorel:commandapi-annotations:9.7.0")
    annotationProcessor("dev.jorel:commandapi-annotations:9.7.0")
}

tasks {
    build {
        dependsOn(shadowJar)
    }
}

