rootProject.name = "CommandBridge"
include("paper", "velocity")


gradle.extra["pversion"] = "2.1.3"


gradle.extra["pluginType"] = "release"

gradle.extra["pluginVersions"] = listOf("1.20.5", "1.20.6", "1.21", "1.21.1", "1.21.2", "1.21.3", "1.21.4")

gradle.extra["pluginLoaders"] = listOf("folia", "paper", "purpur", "velocity")

