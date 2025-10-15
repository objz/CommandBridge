import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("com.gradleup.shadow") version "8.3.8"
    java
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
    implementation(project(":backends:paper"))
    implementation(project(":backends:folia"))
    implementation("org.bouncycastle:bcprov-jdk18on:1.78.1")
    implementation("org.bouncycastle:bcpkix-jdk18on:1.78.1")
    implementation("org.spongepowered:configurate-yaml:4.2.0")
    implementation("org.spongepowered:configurate-core:4.2.0")
    implementation("org.yaml:snakeyaml:2.2")
    implementation("org.snakeyaml:snakeyaml-engine:2.7")
}

val libsProj = project(":libs")
val paperExtract    = libsProj.layout.buildDirectory.dir("extracted/libs/paper")
val spigotExtract   = libsProj.layout.buildDirectory.dir("extracted/libs/spigot")
val velocityExtract = libsProj.layout.buildDirectory.dir("extracted/libs/velocity")

tasks {
    jar { enabled = false }

    shadowJar {
        archiveBaseName.set("CommandBridge")
        archiveClassifier.set("all")

        relocate("com.fasterxml.jackson", "dev.objz.libs.jackson")
        relocate("io.undertow", "dev.objz.libs.undertow")
        relocate("org.xnio", "dev.objz.libs.xnio")
        relocate("org.jboss.threads", "dev.objz.libs.jboss.threads")
        relocate("org.spongepowered.configurate", "dev.objz.libs.configurate")
        relocate("org.yaml.snakeyaml", "dev.objz.libs.snakeyaml")
        mergeServiceFiles()

        from(project(":velocity").layout.projectDirectory.dir("src/main/resources")) { include("velocity-plugin.json") }
        from(project(":backends").layout.projectDirectory.dir("src/main/resources")) { include("plugin.yml", "paper-plugin.yml") }

        dependsOn(":libs:fetchAll")

        from(paperExtract)    { into("libs/paper");    exclude("META-INF/**") }
        from(spigotExtract)   { into("libs/spigot");   exclude("META-INF/**") }
        from(velocityExtract) { into("libs/velocity"); exclude("META-INF/**") }

        duplicatesStrategy = DuplicatesStrategy.INCLUDE
    }

    val copyToPaperPlugins by registering(Copy::class) {
        dependsOn(shadowJar)
        from(shadowJar.get().outputs.files)
        into("/mnt/storage/Server-TEST/CB-v2/Paper/plugins")
    }

    val copyToVelocityPlugins by registering(Copy::class) {
        dependsOn(shadowJar)
        from(shadowJar.get().outputs.files)
        into("/mnt/storage/Server-TEST/CB-v2/Velocity/plugins")
    }

    register("dev") { dependsOn(":libs:fetchAll", copyToVelocityPlugins, copyToPaperPlugins) }
    build { dependsOn(shadowJar) }
}
