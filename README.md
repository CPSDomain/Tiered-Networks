# Tiered Networks

Wireless storage and autocrafting for Forge 1.20.1, by BobRoflza. No cables, no channels: every block joins a network with the Network Linker, and tiered Wireless Access Points decide how far the network reaches.

## How it works

1. Place a **Network Controller** and feed it FE. It starts a new network and has a built-in 8-block wireless range.
2. Sneak + right-click the controller with a **Network Linker** to copy its network.
3. Right-click any other network block with the linker to link it.
4. A device is online when the controller, or an active access point, covers it. Access points chain: an access point is active when the controller or another active access point covers it.

| Access point | Range | FE/t |
|---|---|---|
| Basic | 16 blocks | 1 |
| Advanced | 64 blocks | 4 |
| Elite | 256 blocks | 16 |
| Dimensional | Whole dimension | 32 |
| Quantum | All dimensions | 64 |

Breaking the controller keeps its network ID on the dropped item, so you can move it without relinking.

The controller and the chunks holding your devices must be loaded for the network to work.

## Getting items in

Place an **Import Interface** against a chest or machine (its arrow faces the block you clicked) and link it. It pulls up to 64 items every half second from that inventory, and accepts anything hoppers, pipes or auto-ejecting machines push into any side.

## Config

`config/tierednetworks-common.toml`: `requirePower` (set false to run without an FE mod), `controllerRange`, `controllerBaseCost`.

## Roadmap

- **0.1.0**: controller, access points, linker, drive bay, storage cells (1k–256k), terminal
- **0.1.1**: Import Interface
- **0.2.0**: export interface, wireless handheld terminal, level emitter
- **0.3.0**: autocrafting (pattern encoder, pattern provider, crafting CPU, crafting monitor)
- **0.4.0**: fluid and gas cells, Tiered Industries integration
- **1.0.0**: polish, balance, trusted-player access lists
