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
    compileOnly("org.spongepowered:configurate-yaml:4.2.0")
    implementation("org.snakeyaml:snakeyaml-engine:2.7")
    api("org.slf4j:slf4j-api:2.0.13")
    api("io.undertow:undertow-core:2.3.12.Final")
    api("io.undertow:undertow-websockets-jsr:2.3.12.Final")
    
    compileOnly("org.bouncycastle:bcprov-jdk18on:1.78.1")
    compileOnly("org.bouncycastle:bcpkix-jdk18on:1.78.1")

    implementation(libs.guava)

    testImplementation(libs.junit.jupiter)
}

tasks.test {
    useJUnitPlatform()
}
