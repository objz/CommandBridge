buildscript {
    repositories {
      mavenCentral()
    }
    dependencies {
        classpath("org.yaml:snakeyaml:2.3")

    }
}

import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml

plugins {
    id("java")
    id("com.gradleup.shadow") version "8.3.3"
}

val pversion: String by gradle.extra

group = "dev.consti"
version = pversion

repositories {
    mavenCentral()
    maven { url = uri("https://repo.papermc.io/repository/maven-public/") }
    maven { url = uri("https://repo.codemc.org/repository/maven-public/") }
    maven { url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")}
    maven { url = uri("https://repo.extendedclip.com/releases/")}
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
    // compileOnly("io.papermc.paper:paper-api:1.20-R0.1-SNAPSHOT")
    compileOnly("org.spigotmc:spigot-api:1.20-R0.1-SNAPSHOT")
    implementation("org.ow2.asm:asm:9.7")
    implementation("dev.consti:foundationlib:2.1.2")
    implementation("dev.jorel:commandapi-bukkit-shade:9.7.0")
    compileOnly("dev.jorel:commandapi-annotations:9.7.0")
    compileOnly("me.clip:placeholderapi:2.11.6")
    annotationProcessor("dev.jorel:commandapi-annotations:9.7.0")
}


tasks.register("modifyPaperPluginYaml") {
    doLast {
        
        val yamlFile = file("src/main/resources/paper-plugin.yml") 

        if (yamlFile.exists()) {
            println("Found paper-plugin.yml")

            val options = DumperOptions().apply {
                defaultFlowStyle = DumperOptions.FlowStyle.BLOCK
            }
            val yaml = Yaml(options)

            val yamlContent = yaml.load<Map<String, Any>>(yamlFile.reader()).toMutableMap()

            yamlContent["version"] = pversion

            yamlFile.writer().use { writer ->
                yaml.dump(yamlContent, writer)
            }

            println("paper-plugin.yml updated successfully with version ${pversion}")
        } else {
            println("paper-plugin.yml not found!")
        }
    }
}

tasks.register("modifyLegacyPluginYaml") {
    doLast {
        
        val yamlFile = file("src/main/resources/plugin.yml") 

        if (yamlFile.exists()) {
            println("Found legacy plugin.yml")

            val options = DumperOptions().apply {
                defaultFlowStyle = DumperOptions.FlowStyle.BLOCK
            }
            val yaml = Yaml(options)

            val yamlContent = yaml.load<Map<String, Any>>(yamlFile.reader()).toMutableMap()

            yamlContent["version"] = pversion

            yamlFile.writer().use { writer ->
                yaml.dump(yamlContent, writer)
            }

            println("legacy plugin.yml updated successfully with version ${pversion}")
        } else {
            println("legacy plugin.yml not found!")
        }
    }
}

tasks.named("processResources") {
    dependsOn("modifyPaperPluginYaml")
    dependsOn("modifyLegacyPluginYaml")
}

