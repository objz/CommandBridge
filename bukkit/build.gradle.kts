plugins {
    id("java")
    id("com.gradleup.shadow") version "8.3.3"
}

group = "dev.consti"
version = "2.0.0"

repositories {
    mavenCentral()
    maven { url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots") }
    maven { url = uri("https://repo.codemc.org/repository/maven-public/") }
    maven { url = uri("https://jitpack.io") }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:1.20-R0.1-SNAPSHOT")
    implementation("org.ow2.asm:asm:9.4")
    implementation("com.github.72-S:FoundationLib:master-SNAPSHOT")
    implementation("dev.jorel:commandapi-bukkit-shade:9.6.0")
    compileOnly("dev.jorel:commandapi-annotations:9.6.0")
    annotationProcessor("dev.jorel:commandapi-annotations:9.6.0")
}

tasks {
    shadowJar {
    }

    build {
        dependsOn(shadowJar)
    }
}

