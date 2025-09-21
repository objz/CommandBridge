plugins {
    // Apply the foojay-resolver plugin to allow automatic download of JDKs
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.10.0"
}

rootProject.name = "CommandBridge"
include("core")
include("velocity")

include("dist")

include("backends")                
include("backends:loader")         
include("backends:impl:bukkit")    
include("backends:impl:paper")     
include("backends:impl:folia")     
include("backends:bootstrap")      
