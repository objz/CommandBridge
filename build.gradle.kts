plugins {
    id("java")
    id("com.gradleup.shadow") version "8.3.3"
    id("maven-publish")
    id("com.modrinth.minotaur") version "2.+"
}

val pversion: String by gradle.extra
val versionType: String by gradle.extra
val gameVersions: List<String> by gradle.extra
val loaders: List<String> by gradle.extra
val loadersStr = loaders.joinToString(", ")

group = "dev.consti"
version = pversion

repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    implementation(project(":paper"))
    implementation(project(":velocity"))
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks {
    // Configure the existing shadowJar task, don't register a new one
    shadowJar {
        dependsOn(":paper:shadowJar")

        relocate("dev.jorel.commandapi", "dev.consti.commandbridge.commandapi")


        // Include the compiled outputs of core, paper, and velocity
        from(project(":paper").takeIf { it.plugins.hasPlugin("java") }?.sourceSets?.main?.get()?.output ?: files())
        from(project(":velocity").takeIf { it.plugins.hasPlugin("java") }?.sourceSets?.main?.get()?.output ?: files())

        configurations = listOf(project.configurations.runtimeClasspath.get())
        mergeServiceFiles()
    }

    val copyToPaperPlugins by registering(Copy::class) {
        dependsOn(shadowJar)
        from(shadowJar.get().outputs.files)
        into("/mnt/FastStorage/Server-TEST/CommandBridge/Bukkit/plugins")
    }

    val copyToVelocityPlugins by registering(Copy::class) {
        dependsOn(shadowJar)
        from(shadowJar.get().outputs.files)
        into("/mnt/FastStorage/Server-TEST/CommandBridge/Velocity/plugins")
    }

    register("dev") {
        dependsOn(copyToPaperPlugins, copyToVelocityPlugins)
    }
}

modrinth {
    token.set(System.getenv("MODRINTH_TOKEN"))
    projectId.set("wIuI4ru2")
    versionNumber.set(pversion)
    versionType.set(versionType)
    uploadFile.set(tasks.shadowJar)
    gameVersions.addAll(gameVersions)
    loaders.add(loadersStr)
}

