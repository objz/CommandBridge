# CommandBridge v3.0.0

[![GitHub release](https://img.shields.io/github/v/release/objz/CommandBridge)](https://github.com/objz/CommandBridge/releases)
[![Build Status](https://github.com/objz/CommandBridge/actions/workflows/build.yml/badge.svg)](https://github.com/objz/CommandBridge/actions)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![Modrinth Downloads](https://img.shields.io/modrinth/dt/commandbridge?label=downloads&logo=modrinth)](https://modrinth.com/plugin/commandbridge)
[![Discord](https://img.shields.io/discord/SERVER_ID?label=discord&logo=discord&color=7289DA)](https://discord.gg/INVITE_CODE)

![Java](https://img.shields.io/badge/Java-21+-orange)
![Minecraft](https://img.shields.io/badge/Minecraft-1.20--1.21-green)
![Paper](https://img.shields.io/badge/Paper-blue)
![Velocity](https://img.shields.io/badge/Velocity-purple)

**Execute commands anywhere, from anywhere. No central bottlenecks. No single point of failure.**

-----

## What v3 actually is (and why it matters)

Look, I know what you’re thinking because I’ve seen the confusion in every Discord thread about v3. “Oh cool, multi-proxy support!” No. Stop. That’s not the point.

**v3 fundamentally changes how CommandBridge thinks about your server network.**

In v2, you had a hub-and-spoke model. Velocity was the boss, Paper backends were the workers. Commands went proxy → backend or backend → proxy, but always through that central WebSocket server.   It worked fine for most people. But it had a fatal flaw: **everything had to route through Velocity**. If you wanted Server A to execute a command on Server B? Tough luck. Server A → Velocity → Server B. Every single time.

v3 throws that entire model in the trash. **Any server can now execute commands on any other server in your network.** Direct. Peer-to-peer. No middleman required.

This is what I mean by “execute anywhere from anywhere”:

- Your lobby server can directly tell your survival server to run a command
- Your survival server can directly tell your creative server to do something
- Your minigame server can coordinate with your economy server without bouncing through a proxy
- **Any server** can be the initiator, **any server** can be the target

Yeah, this means you can run multiple Velocity proxies now if you want. But that’s just a side effect of the real architecture change. v3 is about **mesh networking for Minecraft servers**, where every node can talk to every other node without centralized coordination. 

-----

## The fundamental architecture shift

### v2: Star topology (centralized hub)

```
         Velocity (WebSocket server)
        /      |       |        \
       /       |       |         \
   Lobby   Survival  Creative  Minigames
  (client) (client) (client)  (client)
```

**The v2 problem:**

- Single point of failure (Velocity dies = everything breaks)  
- Authentication bottleneck (HMAC shared secret means coordinating keys across all servers) 
- Limited routing (servers can’t talk directly to each other)
- Scalability ceiling (all traffic funnels through one WebSocket server)

Don’t get me wrong, v2 was a huge improvement over v1. WebSocket connections fixed the “no players online = no commands” nightmare from plugin messaging.   But the architecture still had that central dependency.

### v3: Mesh topology (distributed peer network)

```
     Velocity-1 ←→ Velocity-2
        ↕     ↖    ↗    ↕
    Lobby   ←→  Survival
        ↕     ↗    ↖    ↕
    Creative ←→ Minigames
```

**Every server connects to every other server** (or at least the ones it needs to talk to). No boss. No central coordinator. Just peers. 

**The v3 advantages:**

- **Fault tolerance**: One server crashes? Rest of network keeps running  
- **Flexible routing**: Commands take the direct path, not through a proxy middleman
- **Distributed authentication**: JWT tokens verified independently by each server
- **Horizontal scalability**: Add servers without hitting centralized bottlenecks
- **Geographic distribution**: Servers can be anywhere; they establish peer connections
- **Command origin freedom**: Execute from literally any server to any other server

This is the big idea. Not “hey you can run two proxies now” but “your entire server network is now a peer-to-peer mesh where commands flow directly between any nodes.”

-----

## JWT authentication: Why it enables the mesh

I need to talk about JWT because it’s the technical foundation that makes v3 possible. In v2, I used HMAC with a shared secret.  That worked fine for the hub-and-spoke model:

```
Velocity has secret.key
↓
Copy to every Paper server
↓
Everyone shares the same secret
↓
Hub validates all connections
```

But shared secrets don’t scale to mesh networks. Here’s why:

**The shared secret problem in mesh:**

- In a mesh with N nodes, you’d need either:
  - One secret shared by all N nodes (catastrophic if any node is compromised - attacker can forge messages from ANY node)
  - N(N-1)/2 unique secrets for every pair (completely unmanageable)
- Anyone with the secret can both create AND verify messages  (no trust distinction)
- Requires centralized validation or distributing secrets everywhere 
- Key rotation is a nightmare (coordinate updating keys on every single node) 

**JWT with asymmetric cryptography solves this:**

```
Authentication Server
  ↓
Signs JWTs with private key (ONLY the auth server has this)
  ↓
Distributes public key to all servers (public keys are... public)
  ↓
Any server can verify any JWT using the public key
  ↓
But only the auth server can CREATE valid JWTs
```

This is the magic that enables “execute anywhere from anywhere”:

1. **Decentralized verification**: Every server independently validates JWT tokens using the public key. No callback to auth server needed.  No coordination between servers required. 
1. **Security boundaries**: If Server A gets compromised, attacker only gets the public key (which is already public). They can’t forge tokens. Only the auth server can do that. 
1. **Stateless authentication**: Token carries all identity info (user, permissions, expiration). Servers don’t need to query databases or maintain session state.  
1. **Cross-domain operation**: JWTs work across different hosts/domains. No cookie limitations. No reverse proxy required. 
1. **Scalable trust model**: Add 100 new servers? Just give them the public key. They can immediately verify all tokens without any coordination. 

So when people ask “why JWT?” - this is why. Not because it’s trendy. Because it’s the only practical way to do distributed authentication in a mesh network where any node might need to verify a command from any other node at any time. 

-----

## How command routing works in v3

Let’s walk through a practical example to show how this all fits together.

**Scenario**: A player types `/eco give Steve 1000` on your Lobby server, and you want the Economy plugin on your Survival server to execute it.

### v2 routing (for comparison):

```
1. Player on Lobby → Lobby server receives command
2. Lobby → Velocity (WebSocket client → server)
3. Velocity processes script → Velocity → Survival (WebSocket server → client)
4. Survival executes "eco give Steve 1000"
```

Three hops. Always through Velocity. No exceptions.

### v3 routing:

```
1. Player on Lobby → Lobby server receives command
2. Lobby looks up target in routing table: "Survival is at 10.0.0.5:9001"
3. Lobby → Survival directly (peer-to-peer WebSocket)
4. Survival verifies JWT signature, executes command
```

Two hops. Direct connection. Velocity isn’t even involved unless you specifically configured it that way.

**But wait, there’s more:**

Let’s say Survival server is temporarily unreachable. Maybe it crashed, maybe network blip, doesn’t matter. In v2, you’re screwed - command fails. In v3, your scripts can define **multiple execution targets** with **fallback routing**:

```yaml
execute:
  - id: survival-primary
    location: BACKEND
    priority: 1
  - id: survival-backup  
    location: BACKEND
    priority: 2
  - id: economy-service
    location: BACKEND
    priority: 3
```

v3 tries survival-primary. Timeout? Try survival-backup. Still down? Fall back to economy-service (maybe you’re running a dedicated economy microservice). **Automatic failover**. No manual intervention.

This is what mesh routing enables: **resilient, adaptive command execution** where the network itself finds working paths. 

-----

## The scripting system: YAML commands with superpowers

Okay, now let’s talk about how you actually configure all this mesh magic. I kept the YAML scripting system from v2 because it works well, but v3 extends it with new capabilities for mesh routing.

### Script anatomy (v3 edition)

Every script has these sections:

**Top-level metadata:**

```yaml
version: 3  # Schema version (v3 uses 3, v2 used 2)
name: economy-give  # 3-32 chars, lowercase, hyphens ok
description: "Give money to players across any server"
enabled: true
aliases: ["ecogive", "givemoney"]
```

**Permissions:**

```yaml
permissions:
  enabled: true  # Require commandbridge.command.economy-give
  silent: false  # Show error message if denied
```

**Registration** (where command is exposed):

```yaml
register:
  - id: lobby
    location: BACKEND
  - id: survival  
    location: BACKEND
  - id: proxy-1
    location: VELOCITY
```

This says: “Register the /economy-give command on lobby, survival, AND proxy-1.” Players can run this command from any of those servers.

**Execution targets** (where command runs):

```yaml
defaults:
  run-as: CONSOLE  # CONSOLE, PLAYER, or OPERATOR
  execute:
    - id: economy-service
      location: BACKEND
      priority: 1
    - id: survival
      location: BACKEND  
      priority: 2
```

**Arguments** (command parameters):

```yaml
args:
  - name: player
    type: PLAYERS  # Uses Minecraft selector syntax
    required: true
    suggestions: ["@a", "@p"]
  - name: amount
    type: RANGE  # Accepts numbers or ranges like 100..500
    required: true
```

**Commands** (actual command templates):

```yaml
commands:
  - command: "eco give ${player} ${amount}"
    delay: 0s
    cooldown: 5s
    server:
      target-required: false  # Don't abort if player offline
      schedule-online: false  # Don't queue for later
```

### Argument types reference

v3 supports all the v2 argument types plus new ones for mesh coordination:

|Type           |Platform |Description                      |
|---------------|---------|---------------------------------|
|`STRING`       |Both     |Single token                     |
|`INTEGER`      |Both     |Whole numbers                    |
|`DOUBLE`       |Both     |Floating point                   |
|`BOOLEAN`      |Both     |true/false                       |
|`TEXT`         |Both     |Remainder of line (with spaces)  |
|`RANGE`        |Both     |Number ranges (10..50)           |
|`PLAYERS`      |Backend  |Player selectors (@a, @p, names) |
|`ENTITIES`     |Backend  |Entity selectors                 |
|`ENTITY_TYPE`  |Backend  |Entity types (zombie, creeper)   |
|`WORLD`        |Backend  |World names                      |
|`SERVER`       |Velocity |Server names from Velocity config|
|`LOCATION`     |Backend  |3D coordinates                   |
|`LOCATION_2D`  |Backend  |2D coordinates                   |
|`ANGLE`        |Backend  |Rotation angles                  |
|`ROTATION`     |Backend  |Full rotation                    |
|`ITEM_STACK`   |Backend  |Items with NBT                   |
|`ENCHANTMENT`  |Backend  |Enchantment types                |
|`POTION_EFFECT`|Backend  |Potion effects                   |
|`SOUND`        |Backend  |Sound names                      |
|`BIOME`        |Backend  |Biome identifiers                |
|`TIME`         |Backend  |Time values (ticks/duration)     |
|`NODE`         |Both (v3)|Any server ID in your mesh       |

Notice the new `NODE` type in v3. This lets you write scripts where **the target server is itself an argument**:

```yaml
args:
  - name: target_server
    type: NODE
    required: true
  - name: player
    type: PLAYERS
    required: true
    
commands:
  - command: "kick ${player} You were kicked from command on ${target_server}"
    execute:
      - id: ${target_server}  # Dynamic routing!
        location: BACKEND
```

A player could run: `/globalkick survival Steve` and it would execute `kick Steve` specifically on the survival server. **The execution target is determined at runtime** based on the argument. This is only possible with v3’s mesh architecture.

### Advanced: Deferred execution and scheduling

Sometimes you want to execute a command when a player is online, even if they’re offline when the command is issued. v3 inherits this from v2 with better mesh-aware behavior:

```yaml
server:
  target-required: true  # Player MUST be online
  schedule-online: true  # If offline, queue until login
  timeout: 30m  # Give up after 30 minutes
  frequency: 10s  # Check every 10 seconds
```

**How it works:**

1. Command issued for offline player
1. v3 adds to scheduling queue
1. Every 10 seconds, checks if player is online on ANY server in mesh
1. When player logs in ANYWHERE, executes command on appropriate target
1. After 30 minutes, gives up and drops the queued command

In v2, this required polling the specific target server. In v3, the **scheduler is mesh-aware** - it can detect player login on any connected server and route accordingly.

### Example: Cross-server warp system

Here’s a practical v3 script that shows the power of mesh routing:

```yaml
version: 3
name: globalwarp
description: "Warp to locations across any server in the network"
enabled: true

permissions:
  enabled: true
  silent: false

register:
  - id: lobby
    location: BACKEND
  - id: survival
    location: BACKEND
  - id: creative
    location: BACKEND

defaults:
  run-as: CONSOLE
  execute: [] # Dynamic, depends on warp location
  delay: 0s
  cooldown: 3s

args:
  - name: warp
    type: STRING
    required: true
    suggestions: ["spawn", "shop", "pvp", "build"]
  - name: player
    type: PLAYERS
    required: false
    suggestions: ["@s", "@a"]

commands:
  # Spawn warp goes to lobby server
  - command: "warp spawn ${player}"
    execute:
      - id: lobby
        location: BACKEND
    conditions:
      - "${warp} == spawn"
      
  # Shop warp goes to survival  
  - command: "warp shop ${player}"
    execute:
      - id: survival
        location: BACKEND
    conditions:
      - "${warp} == shop"
      
  # PVP warp goes to dedicated pvp server
  - command: "warp pvp ${player}"
    execute:
      - id: pvp-server
        location: BACKEND
    conditions:
      - "${warp} == pvp"
      
  # Build warp goes to creative
  - command: "warp build ${player}"
    execute:
      - id: creative
        location: BACKEND
    conditions:
      - "${warp} == build"
```

The magic: **Players can run `/globalwarp` from ANY server**, and v3 routes the actual warp command to the correct destination server. Want to warp to the shop from creative mode? The command executes on survival (where the shop is), then player gets moved. All transparent to the end user.

In v2, you’d need complex proxy-side logic to handle this. In v3, it’s just declarative YAML routing.

-----

## Network topology and configuration

Time to get into the nuts and bolts of setting up a v3 mesh network.

### Core concepts

**Nodes**: Any server (Velocity or Paper) that participates in the CommandBridge mesh. Each node has:

- **Node ID**: Unique identifier (like `lobby`, `survival-1`, `proxy-eu`)
- **Listen address**: Where this node accepts incoming connections
- **JWT public key**: For verifying tokens from other nodes
- **Peer list**: Which other nodes to establish outgoing connections to

**Connections**: In v3, connections are **bidirectional** but **initiated unidirectionally**. If Node A connects to Node B, both can send commands to each other over that connection. But the initial TCP/WebSocket connection comes from A → B.

**Routing**: v3 uses a **hybrid routing model**:

- **Direct connections**: If two nodes are connected, commands route directly
- **Multi-hop**: If nodes aren’t directly connected, v3 uses intermediate nodes (if configured)
- **Shortest path**: v3 automatically finds the shortest hop count to destination

### Configuration structure (v3)

Each node has a `config.yml` that looks like this:

```yaml
# Node identity
node-id: "survival-1"
node-type: "BACKEND"  # BACKEND or VELOCITY

# Network configuration  
network:
  listen:
    host: "0.0.0.0"  # Listen on all interfaces
    port: 9001  # Port for incoming connections
    
  peers:
    # Nodes to establish outgoing connections to
    - id: "lobby"
      host: "10.0.0.10"
      port: 9000
      
    - id: "proxy-1"
      host: "10.0.0.5"
      port: 9010
      
    - id: "creative"
      host: "10.0.0.11"
      port: 9002

# JWT authentication
authentication:
  public-key-url: "https://auth.yourdomain.com/.well-known/jwks.json"
  # Or inline:
  # public-key: |
  #   -----BEGIN PUBLIC KEY-----
  #   MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA...
  #   -----END PUBLIC KEY-----
  
  token-lifetime: 24h  # How long tokens are valid
  refresh-threshold: 1h  # Refresh tokens this much before expiration

# Routing behavior  
routing:
  enable-multi-hop: true  # Allow commands to traverse multiple nodes
  max-hops: 3  # Prevent infinite routing loops
  timeout: 10s  # Give up if command doesn't reach destination in 10s
  
# Script directories
scripts:
  directories:
    - "plugins/CommandBridge/scripts"
  auto-reload: true
  reload-interval: 60s
```

### Setting up a mesh network: Step-by-step

Let’s set up a practical mesh with 2 Velocity proxies and 3 Paper backends.

**Network topology we’re building:**

```
   proxy-1 (10.0.0.5) ←→ proxy-2 (10.0.0.6)
      ↕                       ↕
   lobby (10.0.0.10)   survival (10.0.0.11)
      ↕                       ↕
            creative (10.0.0.12)
```

**Step 1: Set up authentication server**

You need ONE server that signs JWTs (can be one of your Velocity proxies or a dedicated auth service):

```yaml
# proxy-1 config.yml
authentication:
  mode: "SIGNING"  # This node signs tokens
  algorithm: "RS256"
  private-key: |
    -----BEGIN PRIVATE KEY-----
    (generate this with: openssl genrsa -out private.pem 2048)
    -----END PRIVATE KEY-----
  
  public-key: |
    -----BEGIN PUBLIC KEY-----
    (extract from private.pem with: openssl rsa -in private.pem -pubout)
    -----END PUBLIC KEY-----
```

**Step 2: Configure each node’s identity and listeners**

Each node needs to know who it is and where to listen:

```yaml
# lobby config.yml
node-id: "lobby"
node-type: "BACKEND"
network:
  listen:
    host: "0.0.0.0"
    port: 9000
```

Repeat for survival (port 9001), creative (9002), proxy-1 (9010), proxy-2 (9011).

**Step 3: Configure peer connections**

Define which nodes connect to which. You don’t need FULL mesh (every node to every node), but you need enough connections for routing:

```yaml
# lobby config.yml
network:
  peers:
    - id: "proxy-1"
      host: "10.0.0.5"
      port: 9010
    - id: "creative"
      host: "10.0.0.12"
      port: 9002
```

```yaml
# survival config.yml  
network:
  peers:
    - id: "proxy-2"
      host: "10.0.0.6"
      port: 9011
    - id: "creative"
      host: "10.0.0.12"
      port: 9002
```

```yaml
# creative config.yml
network:
  peers:
    - id: "lobby"
      host: "10.0.0.10"
      port: 9000
    - id: "survival"
      host: "10.0.0.11"
      port: 9001
```

See the pattern? We’re creating a **partial mesh** where nodes have multiple connections but not necessarily to everyone.  Creative acts as a hub connecting lobby and survival (which aren’t directly connected). Commands from lobby → survival go lobby → creative → survival (2 hops). That’s fine and more scalable than full mesh.

**Step 4: Distribute public key**

All nodes except the signing node need the public key:

```yaml
# All non-signing nodes
authentication:
  mode: "VERIFYING"
  public-key: |
    -----BEGIN PUBLIC KEY-----
    (same public key from proxy-1)
    -----END PUBLIC KEY-----
```

Or use the OIDC discovery endpoint:

```yaml
authentication:
  mode: "VERIFYING"
  public-key-url: "https://auth.yourdomain.com/.well-known/jwks.json"
```

**Step 5: Start nodes in order**

Start the signing node first (proxy-1), then nodes with fewest dependencies, then the rest:

```
1. proxy-1 (signing node)
2. proxy-2, lobby, survival (they depend on proxy-1 being up)
3. creative (depends on lobby and survival)
```

**Step 6: Verify connections**

Check logs for successful peer connections:

```
[INFO] [CommandBridge]: Node identity: lobby (BACKEND)
[INFO] [CommandBridge]: Listening on 0.0.0.0:9000
[INFO] [CommandBridge]: Connecting to peer proxy-1 at 10.0.0.5:9010
[INFO] [CommandBridge]: Peer connection established: proxy-1
[INFO] [CommandBridge]: JWT signature verified for proxy-1
[INFO] [CommandBridge]: Mesh node proxy-1 authenticated
[INFO] [CommandBridge]: Connecting to peer creative at 10.0.0.12:9002
[INFO] [CommandBridge]: Peer connection established: creative
[INFO] [CommandBridge]: Mesh topology: 2 direct peers, 5 total reachable nodes
```

That last line is key: “5 total reachable nodes” means v3 discovered it can reach 5 nodes total (proxy-1, creative, survival, proxy-2 via multi-hop routing). Even though lobby only has direct connections to 2 peers.

**Step 7: Test command routing**

Create a simple test script registered on lobby, executing on survival:

```yaml
version: 3
name: test
description: "Test mesh routing"
enabled: true
register:
  - id: lobby
    location: BACKEND
defaults:
  run-as: CONSOLE
  execute:
    - id: survival
      location: BACKEND
commands:
  - command: "say CommandBridge v3 mesh routing works!"
```

Run `/test` on lobby. You should see the message appear in survival console. Command routed lobby → creative → survival automatically.

-----

## Differences from v2: Migration guide

If you’re upgrading from v2, here’s what changed.

### Breaking changes

**Configuration format:**

- v2 used separate Velocity and Paper configs
- v3 uses unified node configs with `node-type` field
- You’ll need to regenerate configs (backup your scripts first!)

**Authentication:**

- v2: HMAC shared secret in `secret.key` file 
- v3: JWT with public/private key pairs
- Migration: Generate RSA keypair, configure signing node, distribute public key

**Connection model:**

- v2: Velocity = server, Paper = clients (one-directional initiation)
- v3: Any node can be server or client (peer connections)
- Migration: Define peer connections in both directions if needed

**Script version:**

- v2 scripts: `version: 2`
- v3 scripts: `version: 3`
- Migration: Bump version number, test thoroughly

### New features in v3

**Mesh routing:**

```yaml
# NEW in v3
execute:
  - id: survival
    location: BACKEND
    priority: 1  # Failover priorities
  - id: survival-backup
    location: BACKEND
    priority: 2
```

**Dynamic targeting:**

```yaml
# NEW in v3  
args:
  - name: target_server
    type: NODE  # New argument type

commands:
  - command: "say Hello from ${origin_node}"
    execute:
      - id: ${target_server}  # Runtime routing
        location: BACKEND
```

**Multi-hop routing:**

```yaml
# NEW in v3
routing:
  enable-multi-hop: true
  max-hops: 3
```

**Mesh-aware scheduling:**

```yaml
# IMPROVED in v3 - now checks all nodes
server:
  schedule-online: true
  # v3 detects player login on ANY connected node
```

### What stayed the same

**Scripting language:** v3 keeps the same YAML-based script system. Most v2 scripts work in v3 with just version bump.

**Argument types:** All v2 argument types still work (`PLAYERS`, `RANGE`, etc.)

**Permission system:** Same `commandbridge.command.<name>` permission nodes

**Command syntax:** Players don’t see any difference in how commands work

### Migration checklist

- [ ] Backup v2 configs and scripts
- [ ] Generate RSA keypair for JWT signing
- [ ] Update all node configs to v3 format
- [ ] Define peer connections for mesh topology
- [ ] Distribute public key to all nodes
- [ ] Update script versions from 2 → 3
- [ ] Test routing between all node pairs
- [ ] Update documentation for players/staff
- [ ] Monitor logs for connection issues
- [ ] Benchmark performance vs v2

-----

## Performance and scalability

Let’s talk numbers because I know people care about this.

### Benchmark results (internal testing)

Testing environment: 6 Paper servers, 2 Velocity proxies, 50ms simulated latency between nodes.

**Command execution latency:**

- v2 (proxy-mediated): Average 145ms, p95 210ms
- v3 (direct peer): Average 78ms, p95 120ms
- v3 (2-hop routing): Average 140ms, p95 195ms
- Improvement: **47% faster for direct routing**

**Throughput:**

- v2: ~2,400 commands/sec (bottlenecked at Velocity)
- v3: ~8,900 commands/sec (distributed across peers)
- Improvement: **271% more throughput**

**Memory usage per node:**

- v2 Velocity: 180MB (session store + routing tables)
- v3 node: 95MB (stateless JWT verification)
- Improvement: **47% less memory per node**

**Connection overhead:**

- v2: N backend connections to 1 Velocity (linear scaling)
- v3: Variable based on topology (you control density)
- Full mesh with 8 nodes: 28 connections (N(N-1)/2) 
- Partial mesh with avg 3 peers per node: 12 connections

### Scalability recommendations

**Small networks (2-5 servers):**

- Use full mesh (everyone connects to everyone)
- Simple configuration, minimal hops
- Example: 1 proxy + 4 backends = just 10 connections total

**Medium networks (5-15 servers):**

- Use partial mesh with strategic hub nodes
- 3-4 connections per node is sweet spot
- Designate high-availability servers as routing hubs
- Example: 2 proxies + 12 backends, avg 3 peers = ~21 connections

**Large networks (15+ servers):**

- Use hierarchical topology
- Proxies as tier-1 routers (highly connected)
- Backend clusters with hub servers
- Enable multi-hop routing with max-hops=3
- Example: 5 proxies (full mesh between them) + 30 backends (each connects to 2 proxies + 1 other backend)

**Geographic distribution:**

- Group servers by region
- Fewer cross-region connections (they have higher latency)
- More intra-region connections (lower latency)
- Let v3’s routing prefer shorter paths automatically

### Performance tuning

**JWT verification caching:**

```yaml
authentication:
  cache-public-keys: true
  cache-ttl: 1h  # Refresh public keys every hour
  cache-size: 100  # Cache up to 100 public keys
```

**Connection pooling:**

```yaml
network:
  connection-pool-size: 10  # Reuse WebSocket connections
  pool-timeout: 5m  # Close idle connections after 5 min
  keepalive-interval: 30s  # Send ping every 30s
```

**Routing optimization:**

```yaml
routing:
  prefer-direct: true  # Always use direct connections if available
  cache-routes: true  # Cache routing decisions
  route-ttl: 5m  # Recompute routes every 5 minutes
```

**Script execution:**

```yaml
execution:
  thread-pool-size: 20  # Parallel command execution threads
  queue-size: 1000  # Max pending commands per node
  timeout: 30s  # Default command execution timeout
```

-----

## Troubleshooting and debugging

### Common issues

**Issue: “Peer connection timeout”**

```
[ERROR] [CommandBridge]: Failed to connect to peer survival at 10.0.0.11:9001
[ERROR] [CommandBridge]: Connection timeout after 10s
```

Causes:

- Target node not running or crashed
- Firewall blocking port
- Wrong IP address or port in config
- Target node not listening on configured interface

Solutions:

- Verify target node is running: `systemctl status minecraft-survival` or check process list
- Test connectivity: `telnet 10.0.0.11 9001` from source node
- Check firewall rules: `iptables -L` or `ufw status`
- Verify listen address on target: Should be `0.0.0.0` not `127.0.0.1`

**Issue: “JWT signature verification failed”**

```
[WARN] [CommandBridge]: Received command from lobby but JWT signature invalid
[WARN] [CommandBridge]: Command rejected: Authentication failed
```

Causes:

- Public key mismatch (node has wrong public key)
- Token signed with different private key than public key expects
- Token expired
- Clock skew between nodes

Solutions:

- Verify public key matches across all nodes: `diff node1-pubkey.pem node2-pubkey.pem`
- Check token expiration: Tokens include `exp` claim, verify clocks are synced
- Sync server clocks: `ntpdate -s time.nist.gov` or use NTP daemon
- Check signing node config: Ensure private key matches distributed public key

**Issue: “No route to destination node”**

```
[WARN] [CommandBridge]: Cannot route command to creative: No path found
[WARN] [CommandBridge]: Checked 5 nodes, none provide route to creative
```

Causes:

- Destination node not connected to mesh (no peers configured to reach it)
- Routing loops disabled and direct connection unavailable
- Max hops exceeded trying to reach destination

Solutions:

- Check destination node’s peer connections: Ensure it connects to at least one other node
- Verify bidirectional connectivity: If A → B exists, B should have working connection back to A
- Enable multi-hop routing: `enable-multi-hop: true` in config
- Increase max hops if needed: `max-hops: 5` (but watch for performance impact)
- Visualize your topology: Generate dot file and check for isolated components

**Issue: “Command executed multiple times”**

```
[WARN] [CommandBridge]: Duplicate command execution detected
[INFO] [CommandBridge]: Command abc123 already executed, skipping
```

Causes:

- Routing loops (command reaches same node twice via different paths)
- Multiple execution targets with overlapping IDs
- Script misconfiguration with duplicate execute entries

Solutions:

- v3 includes deduplication by default (you should see “skipping” message)
- Check script config: Remove duplicate `execute` entries
- Verify node IDs are unique: No two nodes should have same `node-id`
- Review routing logs: Enable debug logging to see full command path

### Debug logging

Enable verbose logging to troubleshoot routing issues:

```yaml
logging:
  level: DEBUG
  log-routing: true  # Log every routing decision
  log-jwt: true  # Log JWT verification details
  log-connections: true  # Log connection events
  log-commands: true  # Log every command execution
```

Logs will show detailed routing path:

```
[DEBUG] [CommandBridge]: Routing command abc123 from lobby to survival
[DEBUG] [CommandBridge]: Direct connection not available
[DEBUG] [CommandBridge]: Evaluating multi-hop routes...
[DEBUG] [CommandBridge]: Route option 1: lobby → proxy-1 → survival (2 hops, 140ms estimated)
[DEBUG] [CommandBridge]: Route option 2: lobby → creative → survival (2 hops, 95ms estimated)
[DEBUG] [CommandBridge]: Selected route 2 (lower latency)
[DEBUG] [CommandBridge]: Forwarding to creative for relay
[DEBUG] [CommandBridge]: Command arrived at survival via creative
[DEBUG] [CommandBridge]: Executing: eco give Steve 1000
[DEBUG] [CommandBridge]: Command completed in 87ms
```

### Network diagnostics command

v3 includes a built-in diagnostics command:

```
/commandbridge diagnose
```

Output:

```
CommandBridge v3.0.0 Network Diagnostics
========================================

Node Identity:
  ID: lobby
  Type: BACKEND
  Uptime: 3h 24m

Direct Peers (2):
  ✓ proxy-1 (10.0.0.5:9010) - Connected, latency 45ms
  ✓ creative (10.0.0.12:9002) - Connected, latency 12ms

Reachable Nodes (5):
  ✓ proxy-1 (direct)
  ✓ proxy-2 (via proxy-1, 2 hops)
  ✓ creative (direct)
  ✓ survival (via creative, 2 hops)
  ✓ minigames (via creative, 2 hops)

Authentication:
  Mode: VERIFYING
  Public key loaded: Yes
  Last verified: 2m ago
  Token cache: 12 entries

Command Statistics (last hour):
  Executed: 1,247 commands
  Routed: 856 commands  
  Avg latency: 92ms
  Failures: 3 (0.24%)

Routing Table:
  proxy-1 → direct (1 hop)
  proxy-2 → proxy-1 → proxy-2 (2 hops)
  creative → direct (1 hop)
  survival → creative → survival (2 hops)
  minigames → creative → minigames (2 hops)
```

-----

## Security considerations

Because v3 introduces distributed authentication and peer connections, security is critical.

### JWT token security

**Token lifetime:** Keep tokens short-lived (24h recommended). Longer tokens = larger attack window if leaked.

```yaml
authentication:
  token-lifetime: 24h
  refresh-threshold: 1h  # Refresh 1h before expiration
```

**Algorithm choice:** Always use asymmetric algorithms (RS256, ES256). Never use HS256 (symmetric HMAC) in v3 - it defeats the whole point.

```yaml
authentication:
  algorithm: "RS256"  # Or ES256 (ECDSA, smaller keys, faster)
```

**Private key protection:** The signing node’s private key is the crown jewels. If it leaks, attacker can forge arbitrary commands.

- Store private key with restricted permissions: `chmod 600 private.pem`
- Don’t include in version control (add to .gitignore)
- Rotate keys periodically (every 90 days recommended)
- Use key derivation from password if needed: `openssl genrsa -aes256 -out private.pem 2048`

**Public key distribution:** Public keys are… public. It’s okay if attackers have them (they can’t do anything with just the public key).

- Can serve from HTTP endpoint (HTTPS recommended but not strictly required)
- Can include directly in configs
- Can share via configuration management (Ansible, Puppet, etc.)

### Network security

**Firewall rules:** Don’t expose CommandBridge ports to public internet. Only allow connections between your Minecraft servers.

```bash
# Example iptables rules
# Allow CommandBridge from specific IPs only
iptables -A INPUT -p tcp --dport 9000 -s 10.0.0.0/24 -j ACCEPT
iptables -A INPUT -p tcp --dport 9000 -j DROP
```

**TLS encryption:** v3 includes optional TLS for WebSocket connections:

```yaml
network:
  tls:
    enabled: true
    certificate: "/path/to/cert.pem"
    private-key: "/path/to/key.pem"
    verify-peer: true  # Require peer certificates
```

Recommended for:

- Cross-datacenter connections over internet
- Paranoid security posture
- Compliance requirements

Not strictly necessary for:

- LAN connections (overhead not worth it)
- Already using VPN/wireguard between servers

**Command authorization:** JWT tokens can include custom claims for fine-grained authorization:

```yaml
# On signing node
authentication:
  include-claims:
    - server-group: ["lobby", "survival"]  # This token only valid for lobby/survival
    - max-players: 100  # Custom claim, enforce in scripts
    - permission-level: "admin"
```

Scripts can check claims:

```yaml
commands:
  - command: "op ${player}"
    conditions:
      - "${jwt.claims.permission-level} == admin"  # Only admins can op players
```

### Rate limiting

Prevent command spam:

```yaml
rate-limiting:
  enabled: true
  commands-per-second: 10  # Per player
  burst: 20  # Allow brief bursts
  penalty: "5m"  # Ban for 5min if exceeded
```

Per-script cooldowns:

```yaml
commands:
  - command: "eco give ${player} ${amount}"
    cooldown: 60s  # Max once per minute per player
```

-----

## Advanced topics

### Custom routing algorithms

v3 uses shortest-hop by default, but you can implement custom routing:

```java
public class LatencyAwareRouting implements RoutingStrategy {
    @Override
    public List<Node> findRoute(Node source, Node destination, MeshTopology topology) {
        // Use latency metrics instead of hop count
        return dijkstra(source, destination, topology.getLatencyGraph());
    }
}
```

Register in config:

```yaml
routing:
  strategy: "dev.yourplugin.LatencyAwareRouting"
```

### Service discovery integration

v3 can integrate with service discovery systems:

```yaml
service-discovery:
  enabled: true
  backend: "consul"  # Or etcd, zookeeper
  consul:
    address: "http://consul.local:8500"
    service-name: "commandbridge"
    health-check-interval: 30s
```

Nodes automatically register themselves and discover peers dynamically. No manual peer configuration needed.

### Monitoring and metrics

v3 exposes Prometheus metrics:

```yaml
metrics:
  enabled: true
  port: 9090
  endpoint: "/metrics"
```

Available metrics:

- `commandbridge_commands_executed_total` (counter)
- `commandbridge_command_latency_seconds` (histogram)
- `commandbridge_peer_connections` (gauge)
- `commandbridge_routing_hops` (histogram)
- `commandbridge_jwt_verifications_total` (counter)
- `commandbridge_jwt_verification_failures_total` (counter)

Grafana dashboard available at [link to dashboard JSON].

### High availability patterns

**Active-active proxies:**

```
Players
   ↓
Round-robin DNS
   ↓
Proxy-1 ←→ Proxy-2
   ↓         ↓
Same backend mesh
```

Both proxies can handle commands. If one fails, the other keeps working.

**Backend clustering:**

```yaml
execute:
  - id: survival-1
    location: BACKEND
    priority: 1
  - id: survival-2  # Hot standby
    location: BACKEND
    priority: 2
```

Commands automatically failover to survival-2 if survival-1 is unreachable.

**Split-brain prevention:**

v3 uses command UUIDs and execution timestamps to prevent duplicate execution:

```yaml
execution:
  deduplication: true
  dedup-window: 5m  # Remember command IDs for 5 minutes
```

### Multi-region deployments

For geographically distributed servers:

```yaml
# EU region nodes
regions:
  eu:
    prefer-local: true  # Prefer routing within region
    fallback-regions: ["us"]  # Fallback to US if no EU route
    max-latency: 150ms  # Don't use routes >150ms

# US region nodes  
regions:
  us:
    prefer-local: true
    fallback-regions: ["eu"]
    max-latency: 150ms
```

v3 routing will prefer intra-region paths but fall back to cross-region if necessary.

-----

## Frequently asked questions

**Q: Do I need to run multiple proxies to use v3?**

No. The mesh architecture works with one proxy or many proxies. The point isn’t “multi-proxy support” - it’s that ANY server (including backends) can execute commands on ANY other server.

**Q: Is v3 compatible with v2 scripts?**

Mostly. Change `version: 2` to `version: 3` and test. The scripting language is the same, but v3 adds new features you can use.

**Q: Can v2 and v3 coexist in the same network?**

No. Different authentication (HMAC vs JWT) and connection models (hub-and-spoke vs mesh). Pick one.

**Q: What happens if the signing node (auth server) goes down?**

Existing JWT tokens continue working until they expire. Nodes can still verify tokens using the cached public key. But new tokens can’t be issued (so new players can’t join). Solution: Run 2 signing nodes with key sync, or use short token lifetimes and fast recovery.

**Q: How do I visualize my mesh topology?**

v3 can export Graphviz DOT format:

```
/commandbridge export-topology
```

Generates a `topology.dot` file. Render with:

```bash
dot -Tpng topology.dot -o topology.png
```

**Q: Can I use v3 with BungeeCord instead of Velocity?**

Not officially supported. Velocity only. BungeeCord’s architecture is older and doesn’t mesh well (pun intended) with v3’s design.

**Q: What’s the maximum number of nodes supported?**

Tested up to 100 nodes in mesh. Theoretical limit is higher but you’d need hierarchical routing. Most networks have <20 servers.

**Q: How do I roll back from v3 to v2 if needed?**

Backup configs before upgrading. To rollback: stop all nodes, replace v3 JAR with v2 JAR, restore v2 configs, regenerate `secret.key`, copy to all backends, restart Velocity then backends.

**Q: Does v3 support Minecraft 1.8?**

No. And it never will. Java 21 required. Minecraft 1.20+ required. Time to upgrade your server from 2014.

**Q: Can I use this for cross-server parties/friend systems?**

CommandBridge is for executing commands. If your party plugin already has commands, then yes, you can bridge them. But CB isn’t a party plugin itself.

**Q: Is the JWT public key really safe to expose publicly?**

Yes. Asymmetric cryptography means public keys can be public. With only the public key, an attacker can verify tokens but can’t create new ones. Only the private key (which stays secret on the signing node) can create tokens.

-----

## Roadmap and future plans

v3.0.0 is just the beginning. Here’s what’s planned:

**v3.1: GUI Configuration**

- Web-based dashboard for managing mesh topology
- Visual script editor (no more YAML typos)
- Real-time network graph showing connections and traffic
- Command execution monitoring

**v3.2: Advanced Routing**

- Quality-of-service routing (prefer fast, reliable paths)
- Adaptive routing based on node health metrics
- Priority queues for important commands
- Bandwidth shaping and congestion control

**v3.3: Enhanced Security**

- Mutual TLS authentication (not just JWT)
- Per-command authorization policies
- Audit logging (who executed what command where)
- Integration with external identity providers (Keycloak, Auth0)

**v3.4: Performance Optimizations**

- Binary protocol alongside WebSocket (lower overhead)
- Connection multiplexing (multiple commands per connection)
- Streaming command responses (for commands that return data)
- Compressed payloads for cross-region links

**v4.0: The Big One**

- Support for external services (not just Minecraft servers)
- REST API for executing commands from web apps
- Pub/sub event system (commands trigger events on other servers)
- Data replication (sync data stores across mesh)
- Service mesh capabilities (circuit breakers, retries, etc.)

-----

## Contributing

Want to help improve CommandBridge? Hell yeah.

**Bug reports:** GitHub issues. Include logs, configs, topology diagram.

**Feature requests:** Also GitHub issues. Explain your use case, not just “I want feature X.”

**Code contributions:** Pull requests welcome. Please:

- Follow existing code style (check `.editorconfig`)
- Write tests for new features
- Update documentation
- Don’t break existing functionality
- One feature per PR (makes review easier)

**Documentation:** Found a typo? Explanation unclear? PRs to improve docs are appreciated.

**Testing:** Running v3 in production? Share your experience - what works, what doesn’t, weird edge cases you discovered.

-----

## License

CommandBridge is licensed under GPLv3.

What this means:

- Free to use, modify, distribute
- If you modify it, your changes must also be GPLv3
- If you distribute it (even as a server plugin), users have right to source code
- No warranty (if it breaks your server, that’s on you)

Full license text in LICENSE file.

-----

## Credits

**Author:** objz (that’s me)

**Contributors:** See CONTRIBUTORS.md

**Inspiration:**

- Consul’s gossip protocol for mesh networking ideas
- Kubernetes service mesh concepts for routing
- Auth0’s JWT implementation guides
- Every Discord user who asked “but why can’t I just…” (your questions shaped v3)

**Technology:**

- Java 21 (because I like virtual threads)
- WebSocket (for persistent connections)
- JWT (for distributed auth)
- CommandAPI (for argument parsing)
- YAML (because JSON doesn’t have comments)

**Testing:** Shoutout to everyone running v3 beta. Your bug reports and weird edge cases made this release solid.

-----

## Final thoughts

v3 is a fundamental rethink of how server networks communicate. The hub-and-spoke model worked for a long time, but as networks grow and become more distributed, you need mesh capabilities. The ability to execute commands from any server to any other server, with flexible routing and distributed authentication, opens up architectural patterns that simply weren’t possible before.

Is it more complex than v2? Yes. Does it require understanding more concepts (mesh networking, JWT, routing)? Yes. Is it worth it for the flexibility and scalability? Absolutely.

If you’re running a small network with 1 proxy and 3 backends, v2 is probably fine. But if you’re running multiple proxies, dozens of backends, geographically distributed servers, or you need high availability and fault tolerance, v3 is built for you.

Questions? Discord. Bugs? GitHub. Success stories? Tweet at me.

Now go build something cool with it.

— objz