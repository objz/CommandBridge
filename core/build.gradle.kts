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
}

dependencies {
    api("com.fasterxml.jackson.core:jackson-databind:2.17.2")
    api("com.fasterxml.jackson.core:jackson-annotations:2.17.2")
    api("com.fasterxml.jackson.core:jackson-core:2.17.2")
    implementation("org.spongepowered:configurate-yaml:4.2.0")
implementation("com.guicedee.services:slf4j:1.2.2.1")
    implementation("io.undertow:undertow-core:2.3.12.Final")
    implementation("io.undertow:undertow-websockets-jsr:2.3.12.Final")
    implementation("org.bouncycastle:bcprov-jdk18on:1.78.1")
    implementation("org.bouncycastle:bcpkix-jdk18on:1.78.1")

    implementation(libs.guava)

    testImplementation(libs.junit.jupiter)
}

tasks.test {
    useJUnitPlatform()
}
