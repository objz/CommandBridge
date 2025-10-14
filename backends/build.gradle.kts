plugins {
    `java-library`
}

java {
    toolchain { languageVersion.set(JavaLanguageVersion.of(21)) }
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    api(project(":core"))
    api("com.squareup.okhttp3:okhttp:4.12.0")
    compileOnly("org.spigotmc:spigot-api:1.21.1-R0.1-SNAPSHOT")
}
