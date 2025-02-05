rootProject.name = "CommandBridge"
include("paper", "velocity")


gradle.extra["pversion"] = "2.1.1"


gradle.extra["pluginType"] = "beta"

gradle.extra["pluginVersions"] = listOf("1.20", "1.21")

gradle.extra["pluginLoaders"] = listOf("folia", "paper", "purpur", "velocity")

