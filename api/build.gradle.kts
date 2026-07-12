import org.gradle.plugins.signing.SigningExtension
plugins {
    `java-library`
    `checkstyle`
    id("com.vanniktech.maven.publish") version "0.36.0"
}

java {
    toolchain { languageVersion.set(JavaLanguageVersion.of(21)) }
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(libs.junit.jupiter)
}

tasks.test {
    useJUnitPlatform()
}

plugins.withId("signing") {
    extensions.configure<SigningExtension>("signing") {
        useGpgCmd()
    }
}

mavenPublishing {
    publishToMavenCentral(automaticRelease = true)
    signAllPublications()

    coordinates("dev.objz", "commandbridge-api", providers.gradleProperty("pluginVersion").get())

    pom {
        name.set("CommandBridge API")
        description.set("Developer API for CommandBridge - cross-server command execution for Minecraft networks")
        url.set("https://cb.objz.dev")
        licenses {
            license {
                name.set("GPL-3.0-only")
                url.set("https://www.gnu.org/licenses/gpl-3.0.html")
            }
        }
        developers {
            developer {
                id.set("objz")
                name.set("objz")
                url.set("https://github.com/objz")
            }
        }
        scm {
            url.set("https://github.com/objz/CommandBridge")
            connection.set("scm:git:git://github.com/objz/CommandBridge.git")
            developerConnection.set("scm:git:ssh://git@github.com/objz/CommandBridge.git")
        }
    }
}
