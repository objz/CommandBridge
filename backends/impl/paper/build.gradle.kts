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
    implementation(project(":backends:loader"))
    compileOnly("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")
    compileOnly("dev.jorel:commandapi-bukkit-shade:10.0.1")
}
