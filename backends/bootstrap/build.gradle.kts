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
    implementation(project(":backends:loader"))
    runtimeOnly(project(":backends:impl:bukkit"))
    runtimeOnly(project(":backends:impl:paper"))
    runtimeOnly(project(":backends:impl:folia"))
    compileOnly("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")
}
