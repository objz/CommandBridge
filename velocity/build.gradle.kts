import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.nio.file.Files

plugins {
    id("java")
}

val pversion: String by gradle.extra

group = "dev.consti"
version = pversion

repositories {
    mavenCentral()
    maven { url = uri("https://repo.papermc.io/repository/maven-public/") }
    maven { url = uri("https://repo.william278.net/releases/") }
    maven {
        name = "GitHubPackages"
        url = uri("https://maven.pkg.github.com/72-S/FoundationLib")
        credentials {
            username = "72-S"
            password = System.getenv("GITHUB_TOKEN")
        }
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

dependencies {
    compileOnly("com.velocitypowered:velocity-api:3.4.0-SNAPSHOT")
    annotationProcessor("com.velocitypowered:velocity-api:3.4.0-SNAPSHOT")
    implementation("dev.consti:foundationlib:2.2.1")
    compileOnly("net.william278:papiproxybridge:1.7.2")
}


tasks.register("modifyVelocityPluginJson") {
    doLast {
        val jsonFile = layout.buildDirectory.file("classes/java/main/velocity-plugin.json").get().asFile
        if (jsonFile.exists()) {
            println("Found velocity-plugin.json")

            val jsonContent = Files.readString(jsonFile.toPath())
            val jsonObject = JsonParser.parseString(jsonContent).asJsonObject

            jsonObject.addProperty("version", pversion)

            Files.writeString(jsonFile.toPath(), jsonObject.toString())
            println("velocity-plugin.json updated successfully with version ${pversion}")
        } else {
            println("velocity-plugin.json not found")
        }
    }
}

tasks.register("generatePluginProperties") {
    doLast {
        println("Generating plugin.properties file")

        val propertiesFile = layout.buildDirectory.file("resources/main/plugin.properties").get().asFile
        propertiesFile.parentFile.mkdirs()
        propertiesFile.writeText("""
            plugin.version=${pversion}
        """.trimIndent())

        println("Successfully generated plugin.properties file")
    }
}


tasks.named("processResources") {
    dependsOn("generatePluginProperties")
    finalizedBy("modifyVelocityPluginJson")
}
