plugins {
    id("java")
    id("com.gradleup.shadow") version "8.3.3"
    id("maven-publish")
    id("com.modrinth.minotaur") version "2.+"
}

val pversion: String by gradle.extra
val pluginType: String by gradle.extra
val pluginVersions: List<String> by gradle.extra
val pluginLoaders: List<String> by gradle.extra

group = "dev.consti"

version = pversion

repositories {
    mavenCentral()
    maven {
        name = "GitHubPackages"
        url = uri("https://maven.pkg.github.com/objz/FoundationLib")
        credentials {
            username = "objz"
            password = System.getenv("GITHUB_TOKEN")
        }
    }
    maven { url = uri("https://repo.william278.net/releases/") }
    maven { url = uri("https://repo.extendedclip.com/releases/") }
}

dependencies {
    implementation(project(":paper"))
    implementation(project(":velocity"))
}

java { toolchain { languageVersion.set(JavaLanguageVersion.of(21)) } }

tasks {
    // Configure the existing shadowJar task, don't register a new one
    shadowJar {
        dependsOn(":paper:shadowJar")
        manifest { attributes["paperweight-mappings-namespace"] = "spigot" }

        relocate("dev.jorel.commandapi", "dev.consti.commandbridge.commandapi")
        relocate("org.bstats", "dev.consti.commandbridge.bstats")

        // Include the compiled outputs of core, paper, and velocity
        from(
                project(":paper")
                        .takeIf { it.plugins.hasPlugin("java") }
                        ?.sourceSets
                        ?.main
                        ?.get()
                        ?.output
                        ?: files()
        )
        from(
                project(":velocity")
                        .takeIf { it.plugins.hasPlugin("java") }
                        ?.sourceSets
                        ?.main
                        ?.get()
                        ?.output
                        ?: files()
        )

        configurations = listOf(project.configurations.runtimeClasspath.get())
        mergeServiceFiles()
    }

    val copyToPaperPlugins by
            registering(Copy::class) {
                dependsOn(shadowJar)
                from(shadowJar.get().outputs.files)
                into("/mnt/Storage/Server-TEST/CommandBridge/Paper/plugins")
            }

    val copyToVelocityPlugins by
            registering(Copy::class) {
                dependsOn(shadowJar)
                from(shadowJar.get().outputs.files)
                into("/mnt/Storage/Server-TEST/CommandBridge/Velocity/plugins")
            }

    register("dev") { dependsOn(copyToPaperPlugins, copyToVelocityPlugins) }
}

afterEvaluate {
    modrinth {
        token.set(System.getenv("MODRINTH_TOKEN"))
        projectId.set("wIuI4ru2")
        versionName.set("CommandBridge $pversion")
        changelog.set(rootProject.file("CHANGELOG.md").readText())
        versionNumber.set(pversion)
        versionType.set(pluginType)
        uploadFile.set(tasks.shadowJar)
        gameVersions.set(pluginVersions)
        loaders.set(pluginLoaders)
        debugMode.set(false)
    }
}
