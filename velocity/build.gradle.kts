plugins {
    `java-library`
}

java {
    toolchain { languageVersion.set(JavaLanguageVersion.of(21)) }
    withSourcesJar()
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") 
}

dependencies {
    implementation(project(":core"))

    compileOnly("com.velocitypowered:velocity-api:3.4.0-SNAPSHOT")
    annotationProcessor("com.velocitypowered:velocity-api:3.4.0-SNAPSHOT")

    implementation("net.kyori:adventure-text-minimessage:4.17.0")
    compileOnly("net.kyori:adventure-api:4.17.0")

    compileOnly("org.spongepowered:configurate-yaml:4.2.0")
    compileOnly("org.spongepowered:configurate-core:4.2.0")
    compileOnly("dev.jorel:commandapi-velocity-core:11.1.0")

    testImplementation(libs.junit.jupiter)
}

tasks.test {
    useJUnitPlatform()
}

tasks.jar { }

