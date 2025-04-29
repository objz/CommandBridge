---
description: >-
  CommandBridge shines in scenarios where you need cross-server communication to
  execute commands seamlessly. Below are practical scenarios where CommandBridge
  can be a game-changer.
---

# 🤖 Use cases

### <mark style="color:orange;">Example 1:</mark> Global `/hub` Command with Additional Actions

**Scenario**:\
You want to implement a global `/hub` command that works on **any server in your network**. When a player types `/hub`, they are:

1. **Teleported to the hub server** (handled by Velocity).
2. Once in the hub, a **Paper plugin plays background music** using the `/music` command.

**Problem**:

* The `/hub` command isn’t natively available on Paper servers.
* The `/music` command can only be executed on the hub server.

**Solution with CommandBridge**:\
CommandBridge lets you define the `/hub` command as follows:

1. The **player types `/hub`** on any server (Paper or Velocity).
2. **CommandBridge triggers the teleport** to the hub server using Velocity’s `/server hub` command.
3. Once the player is on the hub server, **CommandBridge automatically executes `/music`** as the player.

| Step                   | Action                                                 | Executed On        |
| ---------------------- | ------------------------------------------------------ | ------------------ |
| **Player runs `/hub`** | CommandBridge receives and processes the command.      | Velocity           |
| **Teleport to hub**    | Executes Velocity's `/server hub` command to teleport. | Velocity           |
| **Play music**         | Executes `/music` for the player after teleporting.    | Hub (Paper plugin) |

**Why Use CommandBridge?**

* **Global availability**: `/hub` works from any server in your network.
* **Multi-step execution**: Executes commands in sequence, even across servers.
* **Automation**: Automatically triggers the `/music` command without user intervention.

***

### <mark style="color:orange;">**Example 2:**</mark> Timed Gameplay with Return to Lobby

**Scenario**:\
You have a **minigame server** where players have **15 minutes** to play. After their time is up, they must:

1. **Teleport back to the main lobby** (handled by Velocity).
2. A message should appear announcing their return, triggered on the minigame server.

**Problem**:

* Paper doesn’t support Velocity’s `/server` commands natively.
* The minigame server needs to trigger global actions without direct access to Velocity commands.

**Solution with CommandBridge**:

1. A **Paper plugin** on the minigame server triggers a CommandBridge-defined command, e.g., `/end-session`.
2. CommandBridge teleports the player to the lobby using Velocity’s `/server lobby`.
3. CommandBridge executes a follow-up `/broadcast` command on the minigame server, announcing their departure.

| Step                       | Action                                              | Executed On      |
| -------------------------- | --------------------------------------------------- | ---------------- |
| **Trigger `/end-session`** | Minigame plugin triggers the CommandBridge command. | Minigame (Paper) |
| **Teleport to lobby**      | Executes Velocity’s `/server lobby` command.        | Velocity         |
| **Announce return**        | Broadcasts a farewell message to players.           | Minigame (Paper) |

**Why Use CommandBridge?**

* **Seamless teleportation**: Coordinates actions across Velocity and Paper.
* **Custom workflows**: Chains actions like teleporting and messaging.

***

### <mark style="color:orange;">**Example 3:**</mark> Centralized Rewards with Conditional Actions

**Scenario**:\
You’re running a **network-wide event** where players can earn rewards. After claiming their reward:

1. They are **teleported to a “rewards room” server**.
2. The server **gives them a diamond** automatically.
3. An announcement is broadcast **only on the rewards server**.

**Problem**:

* Bukkit/Paper plugins can’t trigger Velocity’s `/server` commands directly.
* Reward actions need to be automated and localized per server.

**Solution with CommandBridge**:

1. Define a CommandBridge command, e.g., `/claim-reward`.
2. The command:
   * Teleports the player to the “rewards room” using Velocity’s `/server rewards`.
   * Executes `/give %cb_player% diamond` on the rewards server (Paper) to give a diamond to the player.
   * Sends a local announcement using `/say %cb_player% claimed their reward!` on the rewards server.

| Step                            | Action                                       | Executed On     |
| ------------------------------- | -------------------------------------------- | --------------- |
| **Player runs `/claim-reward`** | CommandBridge triggers the sequence.         | Any server      |
| **Teleport to rewards server**  | Executes `/server rewards`.                  | Velocity        |
| **Give diamond**                | Runs `/give {player} diamond`.               | Rewards (Paper) |
| **Local announcement**          | Sends `/say {player} claimed their reward!`. | Rewards (Paper) |

**Why Use CommandBridge?**

* **Cross-server coordination**: Automates actions across multiple servers.
* **Localized commands**: Keeps announcements server-specific while managing rewards globally.

***

### **When Should You Use CommandBridge?**&#x20;

CommandBridge is ideal if you:

* **Need global commands** that work on every server in your network.
* **Automate multi-step workflows**, such as teleportation and follow-up actions.
* **Coordinate cross-server actions** seamlessly between Velocity and Paper.
