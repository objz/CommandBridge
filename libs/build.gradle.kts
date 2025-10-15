import org.gradle.api.tasks.Sync

plugins {
    java
}

java {
    toolchain { languageVersion.set(JavaLanguageVersion.of(21)) }
}

repositories {
    mavenCentral()
}

val paper    = "dev.jorel:commandapi-paper-shade:11.0.0"
val spigot   = "dev.jorel:commandapi-spigot-shade:11.0.0"
val velocity = "dev.jorel:commandapi-velocity-shade:11.0.0"

val sourcesLibsRoot = layout.projectDirectory.dir("libs")
val paperOut    = sourcesLibsRoot.dir("paper")
val spigotOut   = sourcesLibsRoot.dir("spigot")
val velocityOut = sourcesLibsRoot.dir("velocity")

val paperExtract    = layout.buildDirectory.dir("extracted/libs/paper")
val spigotExtract   = layout.buildDirectory.dir("extracted/libs/spigot")
val velocityExtract = layout.buildDirectory.dir("extracted/libs/velocity")

fun registerBinaryConf(name: String) =
    configurations.create(name) {
        isCanBeConsumed = false
        isCanBeResolved = true
        isTransitive = false
        attributes.attribute(
            org.gradle.api.attributes.Category.CATEGORY_ATTRIBUTE,
            objects.named(org.gradle.api.attributes.Category.LIBRARY)
        )
        attributes.attribute(
            org.gradle.api.attributes.LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE,
            objects.named(org.gradle.api.attributes.LibraryElements.JAR)
        )
    }

val paperBin    = registerBinaryConf("paperBin")
val spigotBin   = registerBinaryConf("spigotBin")
val velocityBin = registerBinaryConf("velocityBin")

dependencies {
    add(paperBin.name, "$paper@jar")
    add(spigotBin.name, "$spigot@jar")
    add(velocityBin.name, "$velocity@jar")
}

val fetchPaper = tasks.register<Sync>("fetchPaper") {
    from(provider { paperBin.resolve() })
    into(paperOut)
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}
val fetchSpigot = tasks.register<Sync>("fetchSpigot") {
    from(provider { spigotBin.resolve() })
    into(spigotOut)
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}
val fetchVelocity = tasks.register<Sync>("fetchVelocity") {
    from(provider { velocityBin.resolve() })
    into(velocityOut)
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

fun singleJar(conf: Configuration) = providers.provider {
    val files = conf.resolve()
    require(files.size == 1) { "Expected exactly one file for ${conf.name}, got ${files.size}" }
    files.first()
}

val extractPaper = tasks.register<Sync>("extractPaper") {
    dependsOn(fetchPaper)
    from(provider { zipTree(singleJar(paperBin).get()) })
    into(paperExtract)
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}
val extractSpigot = tasks.register<Sync>("extractSpigot") {
    dependsOn(fetchSpigot)
    from(provider { zipTree(singleJar(spigotBin).get()) })
    into(spigotExtract)
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}
val extractVelocity = tasks.register<Sync>("extractVelocity") {
    dependsOn(fetchVelocity)
    from(provider { zipTree(singleJar(velocityBin).get()) })
    into(velocityExtract)
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

val fetchAll = tasks.register("fetchAll") {
    dependsOn(fetchPaper, fetchSpigot, fetchVelocity, extractPaper, extractSpigot, extractVelocity)
}

tasks.register<Delete>("pruneSourcesLibs") {
    delete(sourcesLibsRoot)
}

fetchAll.configure {
    finalizedBy("pruneSourcesLibs")
}

tasks.register<Delete>("cleanLibs") {
    delete(sourcesLibsRoot, layout.buildDirectory.dir("extracted"))
}
