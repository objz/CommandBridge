import com.google.gson.JsonParser
import java.nio.file.Files

plugins {
    id("java")
}

repositories {
    maven { url = uri("https://repo.papermc.io/repository/maven-public/") }
    maven { url = uri("https://repo.william278.net/releases/") }
}

dependencies {
    implementation("io.netty:netty-all:4.2.2.Final")
    implementation("org.json:json:20250517")
    compileOnly("com.velocitypowered:velocity-api:3.4.0-SNAPSHOT")
    annotationProcessor("com.velocitypowered:velocity-api:3.4.0-SNAPSHOT")
    compileOnly("net.william278:papiproxybridge:1.7.2")
    implementation("org.bstats:bstats-velocity:3.1.0")
    implementation("com.github.luben:zstd-jni:1.5.7-4")

    implementation(project(":core"))
}

tasks.register("modifyVelocityPluginJson") {
    doLast {
        val jsonFile = layout.buildDirectory.file("classes/java/main/velocity-plugin.json").get().asFile
        if (jsonFile.exists()) {
            println("Found velocity-plugin.json")

            val jsonContent = Files.readString(jsonFile.toPath())
            val jsonObject = JsonParser.parseString(jsonContent).asJsonObject

            jsonObject.addProperty("version", version.toString())

            Files.writeString(jsonFile.toPath(), jsonObject.toString())
            println("velocity-plugin.json updated successfully with version $version")
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
        propertiesFile.writeText("plugin.version=$version")

        println("Successfully generated plugin.properties file")
    }
}

tasks.named("processResources") {
    dependsOn("generatePluginProperties")
    finalizedBy("modifyVelocityPluginJson")
}
