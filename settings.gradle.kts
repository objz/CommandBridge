plugins {
    // Apply the foojay-resolver plugin to allow automatic download of JDKs
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "CommandBridge"
include("core")
include("velocity")

include("dist")

include("backends")                
include("backends:bukkit")    
include("backends:paper")     
include("backends:folia")     
include("backends:velocity")     
