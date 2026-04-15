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
    api(project(":api"))
    api(project(":core"))
    api("com.squareup.okhttp3:okhttp:4.12.0")
    api("redis.clients:jedis:7.1.0")
    compileOnly("com.velocitypowered:velocity-api:3.4.0-SNAPSHOT")
    compileOnly("org.spigotmc:spigot-api:1.21.1-R0.1-SNAPSHOT")
    compileOnly("dev.jorel:commandapi-spigot-core:11.2.0")
    compileOnly("org.bstats:bstats-bukkit:3.2.0")
}

// plugin.yml and paper-plugin.yml contain @version@ placeholders.
// The :dist module handles replacing them and packaging into the shadow JAR.
tasks.jar {
    exclude("plugin.yml", "paper-plugin.yml")
}

dependencies {
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}
