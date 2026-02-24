well this is a big version jump because it has major rewrites to support
a second backend. what do I mean by that? CommandBridge was(or still is)
being powered by Websockets. but some users asked for the feature to implement
redis. So here it is: 

- full redis support as transport layer
- you can either use websockets or redis depends on what you select in the config

so big version because the config has major changes and the plugin architecture

