plugins {
    `java-library`
    `checkstyle`
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
    compileOnly("com.velocitypowered:velocity-api:3.4.0-SNAPSHOT")
    compileOnly("dev.jorel:commandapi-velocity-core:11.1.0")

    testImplementation(libs.junit.jupiter)
    testImplementation("com.velocitypowered:velocity-api:3.4.0-SNAPSHOT")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}
