this version brings some nice additions mostly around target-required
and player tracking across proxies. the wiki has been fully rewritten for this version so make sure to check that out.

so whats new:

- target-required per command option. this makes sure the player is actually on the target server before dispatching the command. also works when a proxy is in client mode so it checks across all connected proxies not just the local one
- per-command cooldowns, the old pipeline cooldown stage is gone
- player presence tracking. backends and client mode proxies now sync their player lists to the proxy. the full list only gets sent once on auth and after that only join/leave deltas get sent so you dont have to worry about huge packets even with like 30k players
- added PLAYERS argument type for velocity
- scripts now require version 3

breaking changes:
- removed Server.timeout from the config
- removed some unused config values
- script version 3 is now required, older versions wont work anymore

latest commit: 37b29f5
