plugins {
    id("java")
    id("com.github.johnrengelman.shadow") version "8.1.1"
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
    implementation("com.github.72-S:FoundationLib:master-SNAPSHOT")
    implementation("dev.jorel:commandapi-bukkit-shade:9.6.0")
    compileOnly("dev.jorel:commandapi-annotations:9.6.0")
    annotationProcessor("dev.jorel:commandapi-annotations:9.6.0")
}

tasks {
    shadowJar {
        archiveClassifier.set("shadowAPI")
        relocate("dev.jorel.commandapi", "dev.consti.commandapi")
    }

    build {
        dependsOn(shadowJar)
    }
}

configurations {
    create("shadowAPI")
}

artifacts {
    add("shadowAPI", tasks.shadowJar)
}

