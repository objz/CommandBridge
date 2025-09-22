plugins {
    `java-library`
    id("com.gradleup.shadow") version "8.3.8"
}

java {
    toolchain { languageVersion.set(JavaLanguageVersion.of(21)) }
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://oss.sonatype.org/content/repositories/snapshots")
}

dependencies {
    implementation(project(":backends:loader"))
    runtimeOnly(project(":backends:impl:bukkit"))
    runtimeOnly(project(":backends:impl:paper"))
    runtimeOnly(project(":backends:impl:folia"))
    compileOnly("org.spigotmc:spigot-api:1.21.1-R0.1-SNAPSHOT")
}
