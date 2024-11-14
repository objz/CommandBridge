plugins {
    id("java")
}

group = "dev.consti"
version = "1.0.0"

repositories {
    mavenCentral()
    maven { url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots") }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:1.21.1-R0.1-SNAPSHOT")
}

