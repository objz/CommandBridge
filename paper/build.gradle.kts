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
    maven { url = uri("https://jitpack.io") }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
    implementation("org.ow2.asm:asm:9.7")
    implementation("com.github.72-S:FoundationLib:-SNAPSHOT")
    implementation("dev.jorel:commandapi-bukkit-shade-mojang-mapped:9.7.0")
    compileOnly("dev.jorel:commandapi-annotations:9.7.0")
    annotationProcessor("dev.jorel:commandapi-annotations:9.7.0")
}

tasks {
    build {
        dependsOn(shadowJar)
    }
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

tasks.named("processResources") {
    dependsOn("modifyPaperPluginYaml")
}

