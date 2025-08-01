rootProject.name = "CommandBridge"
include("paper", "velocity", "core")

gradle.extra["pversion"] = "2.3.0"
gradle.extra["pluginType"] = "release"
gradle.extra["pluginVersions"] = listOf("1.20", "1.20.1", "1.20.2", "1.20.3", "1.20.4", "1.20.5", "1.20.6", "1.21", "1.21.1", "1.21.2", "1.21.3", "1.21.4", "1.21.5", "1.21.6", "1.21.7", "1.21.8")
gradle.extra["pluginLoaders"] = listOf("folia", "paper", "purpur", "velocity", "bukkit", "spigot", "waterfall")
