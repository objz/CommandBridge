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

        relocate("com.fasterxml.jackson", "dev.objz.shaded.jackson")
        relocate("io.undertow", "dev.objz.libs.undertow")
        relocate("org.xnio", "dev.objz.libs.xnio")
        relocate("org.jboss.threads", "dev.objz.libs.jboss.threads")
	mergeServiceFiles()

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


    val copyToPaperPlugins by registering(Copy::class) {
        dependsOn(shadowJar)
        from(shadowJar.get().outputs.files)
        into("/mnt/Storage/Server-TEST/CB-v2/Paper/plugins")
    }

    val copyToVelocityPlugins by registering(Copy::class) {
        dependsOn(shadowJar)
        from(shadowJar.get().outputs.files)
        into("/mnt/Storage/Server-TEST/CB-v2/Velocity/plugins")
    }

    register("dev") { dependsOn(copyToVelocityPlugins, copyToPaperPlugins) }

    build { dependsOn(shadowJar) }
}
