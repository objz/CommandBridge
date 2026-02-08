plugins {
    id("com.gradleup.shadow") version "9.2.2"
    id("com.modrinth.minotaur") version "2.+"
    java
}

java {
    toolchain { languageVersion.set(JavaLanguageVersion.of(21)) }
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.william278.net/releases/")
}

dependencies {
    implementation(project(":core"))
    implementation(project(":velocity"))
    implementation(project(":backends"))
    implementation(project(":backends:bukkit"))
    implementation(project(":backends:paper"))
    implementation(project(":backends:folia"))
    implementation(project(":backends:velocity"))
    implementation("org.bouncycastle:bcprov-jdk18on:1.82")
    implementation("org.bouncycastle:bcpkix-jdk18on:1.82")
    implementation("org.spongepowered:configurate-yaml:4.2.0")
    implementation("org.spongepowered:configurate-core:4.2.0")
    implementation("org.yaml:snakeyaml:2.5")
    implementation("org.snakeyaml:snakeyaml-engine:2.10")
    implementation("com.google.code.gson:gson:2.13.2")
    implementation("net.kyori:adventure-text-minimessage:4.17.0")
    implementation("org.bstats:bstats-velocity:3.1.0")
}

val pluginVersion: Provider<String> = providers.gradleProperty("pluginVersion")

modrinth {
    token.set(System.getenv("MODRINTH_TOKEN"))
    projectId.set("commandbridge")
    versionNumber.set(pluginVersion)
    versionName.set(pluginVersion.map { "CommandBridge $it" })
    changelog.set(rootProject.file("CHANGELOG.md").readText())
    versionType.set("beta")
    uploadFile.set(tasks.shadowJar)
    gameVersions.addAll("1.20", "1.20.1", "1.20.2", "1.20.3", "1.20.4", "1.20.5", "1.20.6", "1.21", "1.21.1", "1.21.2", "1.21.3", "1.21.4", "1.21.5", "1.21.6", "1.21.7", "1.21.8", "1.21.9", "1.21.10", "1.21.11")
    loaders.addAll("folia", "paper", "bukkit", "spigot", "purpur", "velocity")
    dependencies {
        required.project("commandapi")
        optional.project("packetevents")
        optional.project("papiproxybridge")
        optional.project("placeholderapi")
    }
}


tasks {
    jar { enabled = false }

    val processPluginResources by registering(Copy::class) {
        val version = pluginVersion
        from(project(":backends").layout.projectDirectory.dir("src/main/resources")) {
            include("plugin.yml", "paper-plugin.yml")
        }
        from(project(":velocity").layout.projectDirectory.dir("src/main/resources")) {
            include("velocity-plugin.json")
        }
        into(layout.buildDirectory.dir("plugin-resources"))
        filter { it.replace("@version@", version.get()) }
    }

    val generateVersionFile by registering {
        val version = pluginVersion
        val outputDir = layout.buildDirectory.dir("plugin-resources")
        val versionFile = outputDir.map { it.file("version") }
        outputs.file(versionFile)
        doLast {
            outputDir.get().asFile.mkdirs()
            versionFile.get().asFile.writeText(version.get())
        }
    }

    shadowJar {
        dependsOn(processPluginResources, generateVersionFile)

        archiveVersion.set(
            if (project.hasProperty("buildVersion")) project.property("buildVersion") as String
            else pluginVersion.get()
        )

        archiveBaseName.set("CommandBridge")
        archiveClassifier.set("all")

        relocate("com.fasterxml.jackson", "dev.objz.libs.jackson")
        relocate("io.undertow", "dev.objz.libs.undertow")
        relocate("org.xnio", "dev.objz.libs.xnio")
        relocate("org.jboss.threads", "dev.objz.libs.jboss.threads")
        relocate("org.spongepowered.configurate", "dev.objz.libs.configurate")
        relocate("org.yaml.snakeyaml", "dev.objz.libs.snakeyaml")
        relocate("org.bstats", "dev.objz.libs.bstats")
        mergeServiceFiles()

        from(layout.buildDirectory.dir("plugin-resources")) {
            include("velocity-plugin.json", "plugin.yml", "paper-plugin.yml", "version")
        }
    }

    val copyToPaperPlugins by registering(Copy::class) {
        dependsOn(shadowJar)
        from(shadowJar.get().outputs.files)
        // into("/home/consti/code-test/Paper/plugins")
        into("/mnt/storage/Server-TEST/CB-v2/Paper/plugins")
    }

    val copyToVelocityPlugins by registering(Copy::class) {
        dependsOn(shadowJar)
        from(shadowJar.get().outputs.files)
        // into("/home/consti/code-test/Velocity/plugins")
        into("/mnt/storage/Server-TEST/CB-v2/Velocity/plugins")
    }

    register("dev") { dependsOn(copyToVelocityPlugins, copyToPaperPlugins) }
    build { dependsOn(shadowJar) }
}
