# ESDuels v1.0 — Lightweight Minecraft Duels Plugin for Paper

**ESDuels v1.0** is a high-performance, production-ready duel plugin designed for live Paper servers. Built with the same lightweight philosophy, branding, and code quality as **ESCore** and **eShards**, ESDuels replicates the beloved **DonutSMP duels experience**: no kits, real player inventories, a DonutSMP-inspired queue confirmation GUI, a 60-second loot collection period, and reliable, disk-backed arena restoration.

---

## ✨ Features

- **DonutSMP-Inspired Queue Confirmation GUI**:
  - Recreates the exact layout from the DonutSMP reference screenshots:
    - Dark gray/black glass pane backdrop.
    - **Red stained-glass pane** on the left to close/cancel.
    - **Green stained-glass pane** on the right to enter matchmaking.
    - **Compass** displaying waiting players, estimated queue time, and search status.
    - **Gray Dye** displaying real-time player statistics (Wins, Losses, Win Streak).
    - **Feather** displaying current region (`NA East`) and player latency (`0ms`).
  - Dynamic live inventory updates without menu flickering.
- **Pure Player Inventory Combat (No Kits)**:
  - Players fight with their own equipment, inventory, armor, and offhand.
  - Zero kits, zero predefined loadouts, zero combat item gifts.
  - Pre-duel inventory backups prevent item loss during exceptional server crashes.
- **60-Second Post-Duel Loot Period**:
  - When the loser dies, their items drop into the arena as legitimate spoils of war.
  - Loser is safely teleported to `/duels setspawn`.
  - The winner remains in the locked arena with an animated Action Bar & BossBar countdown.
  - Native Minecraft loot music plays during the countdown.
  - When the timer expires, the winner is safely teleported to duels spawn.
- **High-Performance Asynchronous Arena Restoration**:
  - Disk-backed YAML snapshot format (`arenas/snapshots/<arena>.yml`). Zero permanent RAM consumption for snapshots.
  - Snapshot captures and file I/O run completely asynchronously.
  - World blocks are restored in bounded main-thread batches (default `250` blocks/tick) to prevent server lag spikes.
  - Intelligent diffing: untouched blocks during combat are skipped, reducing block mutations to only changed blocks.
  - Dropped items and lingering entities in the bounding box are automatically cleared before the arena returns to `IDLE`.
- **Direct Challenges & Queue Matchmaking**:
  - `/duel send <player>`, `/duel accept <player>`, `/duel deny <player>`.
  - Thread-safe matchmaking queue with automatic pairing and arena reservation.
  - Native Minecraft music disc tracks play while waiting in queue and stop immediately when matched or leaving.
- **PlaceholderAPI Integration**:
  - Soft-dependency support: `%esduels_wins%`, `%esduels_losses%`, `%esduels_streak%`, `%esduels_best_streak%`, `%esduels_win_rate%`, `%esduels_in_duel%`, `%esduels_in_queue%`, `%esduels_queued_players%`.

---

## 📋 Commands & Permissions

### Player Commands
| Command | Permission | Description |
|---|---|---|
| `/duel` | `esduels.use` | Opens the DonutSMP Queue Confirm GUI |
| `/duel send <player>` | `esduels.use` | Sends a direct duel challenge to a player |
| `/duel accept <player>` | `esduels.use` | Accepts a pending duel challenge |
| `/duel deny <player>` | `esduels.use` | Declines a pending duel challenge |
| `/duel leave` / `/duels leave` | `esduels.use` | Leaves the matchmaking queue, cancels requests, or forfeits active duel |

### Administrator Commands
| Command | Permission | Description |
|---|---|---|
| `/duels setspawn` | `esduels.admin` | Sets the global duels spawn point |
| `/duels arena create <name>` | `esduels.admin` / `esduels.arena` | Creates a new duel arena configuration |
| `/duels arena set <name> pos1` | `esduels.admin` / `esduels.arena` | Sets Player 1's spawn location |
| `/duels arena set <name> pos2` | `esduels.admin` / `esduels.arena` | Sets Player 2's spawn location |
| `/duels arena set <name> corner1`| `esduels.admin` / `esduels.arena` | Sets Region Corner 1 |
| `/duels arena set <name> corner2`| `esduels.admin` / `esduels.arena` | Sets Region Corner 2 |
| `/duels arena save <name>` | `esduels.admin` / `esduels.arena` | Captures and persists the arena's original block snapshot |
| `/duels arena delete <name>` | `esduels.admin` / `esduels.arena` | Deletes an arena and its snapshot file |
| `/duels arena list` | `esduels.admin` / `esduels.arena` | Lists all arenas and their readiness states |
| `/duels reload` | `esduels.admin` | Reloads `config.yml`, `messages.yml`, and arena files |

---

## 🛠️ Arena Setup Guide

Setting up an arena takes less than 30 seconds:

1. **Set the main duels spawn** (where players go after a match):
   ```
   /duels setspawn
   ```
2. **Create the arena**:
   ```
   /duels arena create forest
   ```
3. **Set spawn points**:
   - Stand where Player 1 should spawn and run:
     ```
     /duels arena set forest pos1
     ```
   - Stand where Player 2 should spawn and run:
     ```
     /duels arena set forest pos2
     ```
4. **Set bounding box corners**:
   - Stand at the lower or first corner and run:
     ```
     /duels arena set forest corner1
     ```
   - Stand at the opposite diagonal corner and run:
     ```
     /duels arena set forest corner2
     ```
5. **Capture and save snapshot**:
   ```
   /duels arena save forest
   ```
   *The plugin will capture all block states, asynchronously persist `arenas/snapshots/forest.yml`, and mark the arena `[READY]`!*

---

## ⚙️ Configuration (`config.yml`)

```yaml
# Duration of the post-duel loot collection timer in seconds
loot-timer-seconds: 60

# Expiration time for direct duel challenges in seconds
request-expiration-seconds: 60

# Countdown duration inside the arena before combat begins (seconds)
countdown-seconds: 5

# Matchmaking queue configuration
queue:
  refresh-interval-ticks: 20
  region-name: "NA East"
  default-estimated-wait: "15s"

# Queue Waiting Music (Native Minecraft music discs)
queue-music:
  enabled: true
  volume: 1.0
  pitch: 1.0
  tracks:
    - "MUSIC_DISC_CAT"
    - "MUSIC_DISC_WAIT"
    - "MUSIC_DISC_STRAD"
    - "MUSIC_DISC_CHIRP"
    - "MUSIC_DISC_MELLOHI"
    - "MUSIC_DISC_FAR"

# Post-Duel Loot Period Music
loot-music:
  enabled: true
  volume: 1.0
  pitch: 1.0
  tracks:
    - "MUSIC_DISC_PIGSTEP"
    - "MUSIC_DISC_OTHERSIDE"
    - "MUSIC_DISC_RELIC"

# Bounded restoration batching
restoration:
  batch-size: 250
  log-restoration: true
```

---

## 📁 File Structure

```
plugins/ESDuels/
├── config.yml
├── messages.yml
├── spawn.yml
├── arenas/
│   ├── arena1.yml
│   └── snapshots/
│       └── arena1.yml
└── data/
    ├── stats/
    │   └── <uuid>.yml
    └── backups/
        └── <uuid>.yml
```

---

## 🛡️ Disaster Recovery & Safety Boundaries

- **Combat-Logging & Disconnect Handling**:
  - If a player disconnects or quits while inside an arena duel (during countdown or active combat), they immediately die at their disconnect coordinates.
  - All inventory contents, armor, and offhand items drop naturally on the arena floor as legitimate spoils of war.
  - The opponent is immediately awarded victory, fanfare, and enters the 60-second loot period in the arena to collect all the dropped items.
  - The leaver is persistently logged in `combat_logs.yml`. When they reconnect to the server:
    - They are confirmed dead and instantly placed at the main duels spawn (never inside an arena).
    - Their inventory is wiped completely clean with zero loot.
    - They receive the defeat screen/message notifying them of their combat-log penalty.
- **Inventory Safety**:
  - Pre-duel inventories (including full armor, off-hand, levels, and food) are cached before combat.
  - Normal match outcomes (death, surrender) delete backup snapshots to maintain genuine risk and DonutSMP-style loot mechanics.
  - Backups are never blindly restored over legitimate player progression.
- **Path Traversal Security**:
  - Arena identifiers are validated against `^[a-zA-Z0-9_-]{1,32}$` and canonical path verification to prevent filesystem escape.
- **Lag Prevention & Bounded Mutexing**:
  - Arena restoration uses spatial bounding boxes and chunks blocks into configurable tick batches (default `250` blocks/tick).
  - Entity clearance uses local spatial queries (`world.getNearbyEntities`) rather than scanning all entities across the world.
  - Concurrent snapshot writes are locked per arena name with atomic temporary-file writes to ensure snapshot files on disk are never corrupted.

---

## 🚀 Building from Source

```bash
mvn clean package
```
Output JAR: `target/ESDuels-1.0.jar`
Target Server: Paper 1.21.x (Java 21+)

#   E S D u e l s  
 #   E S D u e l s  
 