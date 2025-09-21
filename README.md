settings.gradle.kts
build.gradle.kts

core/
  build.gradle.kts
  src/
    main/
      java/
        dev/
          objz/
            commandbridge/
              ...                                  

backends/
  src/
    main/
      java/
        dev/
          objz/
            commandbridge/
                #stays the same          
    
  loader/
    build.gradle.kts
    src/
      main/
        java/
          dev/
            objz/
              commandbridge/
                platform/
                  PlatformAdapter.java
                  PlatformDetector.java
                  PlatformLauncher.java

  impl/
    bukkit/
      build.gradle.kts
      src/
        main/
          java/
            dev/
              objz/
                commandbridge/
                  backends/
                    impl/
                      bukkit/
                        BukkitAdapter.java
                         #later the cmd impl
    paper/
      build.gradle.kts
      src/
        main/
          java/
            dev/
              objz/
                commandbridge/
                  backends/
                    impl/
                      paper/
                        PaperAdapter.java
    folia/
      build.gradle.kts
      src/
        main/
          java/
            dev/
              objz/
                commandbridge/
                  backends/
                    impl/
                      folia/
                        FoliaAdapter.java


  bootstrap/                               
    build.gradle.kts
    src/
      main/
        java/
          dev/
            objz/
              commandbridge/
                backends/
                  bootstrap/
                    BukkitMain.java        # org.bukkit.plugin.java.JavaPlugin entrypoint
                    PaperMain.java         # optional separate main if you want
                    FoliaMain.java         # optional separate main if you want
                    FabricMain.java        # net.fabricmc.api.ModInitializer entrypoint
                    ForgeMain.java         # @Mod entrypoint with listeners
        resources/
          plugin.yml                       # Bukkit/Paper entry (main: dev.objz.commandbridge.backends.bootstrap.BukkitMain)
          paper-plugin.yml                 # Paper/Folia metadata (main can still be BukkitMain)
          fabric.mod.json                  # Fabric metadata (entrypoint -> FabricMain)
          META-INF/
            mods.toml                      # Forge metadata (modId -> ForgeMain)
