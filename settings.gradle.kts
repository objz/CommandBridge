rootProject.name = "CommandBridge"
include("paper", "velocity")


gradle.extra["pversion"] = "2.1.1"


gradle.extra["versionType"] = "beta"

gradle.extra["gameVersions"] = listOf("1.20", "1.21")

gradle.extra["loaders"] = listOf("Folia", "Paper", "Purpur", "Velocity")

