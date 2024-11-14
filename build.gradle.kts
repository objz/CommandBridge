plugins {
    id("java")
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("maven-publish")
}

group = "dev.consti"
version = "2.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":bukkit"))
    implementation(project(":velocity"))
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

tasks {
    // Configure the existing shadowJar task, don't register a new one
    shadowJar {

        // Include the compiled outputs of core, bukkit, and velocity
        from(project(":bukkit").takeIf { it.plugins.hasPlugin("java") }?.sourceSets?.main?.get()?.output ?: files())
        from(project(":velocity").takeIf { it.plugins.hasPlugin("java") }?.sourceSets?.main?.get()?.output ?: files())

        configurations = listOf(project.configurations.runtimeClasspath.get())
        mergeServiceFiles()
    }

    val copyToBukkitPlugins by creating(Copy::class) {
        dependsOn(shadowJar)
        from(shadowJar.get().outputs.files)
        into("/mnt/FastStorage/Server-TEST/CommandBridge/Bukkit/plugins")
    }

    val copyToVelocityPlugins by creating(Copy::class) {
        dependsOn(shadowJar)
        from(shadowJar.get().outputs.files)
        into("/mnt/FastStorage/Server-TEST/CommandBridge/Velocity/plugins")
    }

    register("dev") {
        dependsOn(copyToBukkitPlugins, copyToVelocityPlugins)
    }
}

