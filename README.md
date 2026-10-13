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

## Getting items in and out

- **Import Interface**: the full block pulls from inventories on all six sides; the **Flat Import Interface** sticks to one chest or machine. Hoppers, pipes and auto-ejecting machines can push into either. 64 items every half second.
- **Export Interface** (full and flat): pushes the items in its filter out of the network into the inventory next to it.
- Right-click any interface for its 9-slot filter and online status. Import filters can be an allow list or a deny list; an empty import filter takes everything, an empty export filter exports nothing.

## Wireless Terminal and Level Emitter

- **Wireless Terminal**: sneak + right-click a controller or linked device to bind it, then right-click to open the network anywhere it reaches.
- **Level Emitter**: redstone signal when the network holds at least (or fewer than) a set amount of an item. Empty slot counts everything.

## Autocrafting

1. **Pattern Encoder**: put Blank Patterns in the top slot, lay out a crafting recipe (Crafting mode) or a machine's inputs and outputs (Processing mode), press Encode.
2. **Pattern Provider** (linked): holds 9 patterns. A **Molecular Assembler** touching it runs crafting patterns; a machine touching it runs processing patterns. Machine output must come back into the network: eject into the provider, or use an Import Interface.
3. **Crafting CPU**: touching Crafting Units, at least one Crafting Storage (1k-64k), optional Co-Processors and a Crafting Monitor. Link one block and the whole CPU links. One job per CPU.
4. In a terminal, middle-click (or Ctrl + click) a craftable item, enter an amount, press Craft. Missing ingredients are listed in chat.
5. With JEI: press **+** on a recipe to fill the Pattern Encoder (crafting grid or processing pattern), or - with a terminal open - to request that item.

## Fluids and gases

- **Fluid Cells** (liquids) and **Gas Cells** (any lighter-than-air fluid, including every Tiered Industries gas) go in a Drive Bay. 1k holds 256 buckets, up to 256k at 65,536 buckets; 15 types per cell.
- Terminal **Fluids** tab: click with an empty bucket/Gas Cylinder to fill it, a full one to empty it.
- Import/Export Interfaces move fluids and gases too (8,000 mB every half second). A filled bucket or cylinder in the filter names a fluid.

## Config

`config/tierednetworks-common.toml`: `requirePower` (set false to run without an FE mod), `controllerRange`, `controllerBaseCost`.

## Roadmap

- **0.1.0**: controller, access points, linker, drive bay, storage cells (1k–256k), terminal
- **0.1.1**: Import Interface
- **0.1.2**: filters, flat interfaces, Export Interface
- **0.2.0**: Wireless Terminal, Level Emitter
- **0.3.0**: autocrafting - Pattern Encoder, Pattern Provider, Molecular Assembler, Crafting CPU
- **0.4.0**: Fluid and Gas Cells, terminal Fluids tab, interfaces move fluids/gases (works with Tiered Industries gases)
- **0.4.1**: JEI + button for the Pattern Encoder and terminals
- **1.0.0**: polish, balance, trusted-player access lists
