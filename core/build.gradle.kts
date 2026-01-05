plugins {
    `java-library`
}

java {
    toolchain { languageVersion.set(JavaLanguageVersion.of(21)) }
    withSourcesJar()
    withJavadocJar()
}

repositories {
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
}

dependencies {
    api("com.fasterxml.jackson.core:jackson-databind:2.18.2")
    api("com.fasterxml.jackson.core:jackson-annotations:2.18")
    api("com.fasterxml.jackson.core:jackson-core:2.18.2")

    // used for <?> and Optional
    api("com.fasterxml.jackson.datatype:jackson-datatype-jdk8:2.18.2")
    api("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.18.2")

    compileOnly("org.spongepowered:configurate-yaml:4.2.0")
    compileOnly("org.snakeyaml:snakeyaml-engine:2.10")
    api("org.slf4j:slf4j-api:2.0.17")
    api("io.undertow:undertow-core:2.3.12.Final")
    api("io.undertow:undertow-websockets-jsr:2.3.12.Final")
    
    compileOnly("org.bouncycastle:bcprov-jdk18on:1.82")
    compileOnly("org.bouncycastle:bcpkix-jdk18on:1.82")

    compileOnly("com.google.code.gson:gson:2.13.1")

    compileOnly("dev.jorel:commandapi-spigot-core:11.1.0")

    implementation(libs.guava)

    testImplementation(libs.junit.jupiter)
}

tasks.test {
    useJUnitPlatform()
}
