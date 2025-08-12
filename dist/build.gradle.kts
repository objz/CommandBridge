plugins {
    id("com.gradleup.shadow") version "8.3.8"
    `java`
}

java {
    toolchain { languageVersion.set(JavaLanguageVersion.of(21)) }
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    implementation(project(":core"))
    implementation(project(":velocity"))
    implementation(project(":backends"))
    implementation(project(":backends:bukkit"))
    implementation(project(":backends:folia"))
}

tasks {
    jar { enabled = false }

    shadowJar {
        archiveBaseName.set("CommandBridge")
        archiveClassifier.set("all") // produce CommandBridge-<version>.jar

        mergeServiceFiles()
        relocate("com.fasterxml.jackson", "dev.objz.shaded.jackson")

        dependencies {
            exclude(dependency("com.google.guava:.*"))
            exclude(dependency("com.google.guava:listenablefuture:.*"))
            exclude(dependency("com.google.j2objc:j2objc-annotations:.*"))
            exclude(dependency("com.google.code.findbugs:jsr305:.*"))
            exclude(dependency("org.slf4j:.*"))
            exclude(dependency("net.kyori:.*"))
            exclude(dependency("org.spongepowered:configurate-.*"))
            exclude(dependency("com.velocitypowered:velocity-.*"))
            exclude(dependency("com.mojang:brigadier:.*"))
        }

        from(project(":velocity").layout.projectDirectory.dir("src/main/resources")) {
            include("velocity-plugin.json")
        }
        from(project(":backends").layout.projectDirectory.dir("src/main/resources")) {
            include("plugin.yml", "paper-plugin.yml")
        }
    }

    build { dependsOn(shadowJar) }
}
