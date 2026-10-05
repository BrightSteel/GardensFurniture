# Hoppers and custom furnace fuel (deferred)

## The gap

A GardensFurniture item with `tags:` (e.g. maple planks as `minecraft:planks`), or one with a `type: fuel`
recipe, burns in furnaces. Players can click or shift-click it into a furnace's fuel slot. **A hopper
can't feed it in.**

Vanilla only lets real fuel items into the fuel slot. Our items are paper underneath, so the hopper
refuses them unless a plugin steps in.

The code that steps in is already written. `CustomFuelHandler.onHopperMove` moves custom fuel from a
hopper beside a furnace into its fuel slot. `FurnaceInputRefresher.onHopperMove` swaps an old copy of
an item, dropped in by a hopper above a furnace, for a current one. Both listen for
`InventoryMoveItemEvent`, which the server never fires with our Paper config:

```yaml
# config/paper-world-defaults.yml
hopper:
  disable-move-event: true
```

So both handlers are dormant on our servers. Everything else about custom fuel works.

## Options

### 1. Leave it (current state)

Hoppers can't feed custom fuel; players click it in. This is how it was under CustomCrafting.

### 2. Set `disable-move-event: false`

The existing code starts working with no changes. **But the setting silences hopper events for every
plugin, not just ours.** Turning it off on the dev server would also switch on (as of 2026-10-04):

- **CoreProtect:** `hopper-transactions: true`, so it logs every item every hopper moves. That's
  main-thread work per transfer, and a database that grows fast with sorters or farms. This is most of
  the cost the setting saves.
- **WorldGuard:** `ignore-hopper-item-move-events: false` and `break-hoppers-on-denied-move: true`, so
  it checks region permissions on every move and **breaks hoppers** that move items across regions
  they aren't allowed into. That changes how existing builds behave.
- **Other plugins:** QuickShop and mcMMO, plus the hopper code in GardensCrops, GardensEnchants,
  GardensPlayers, EnderFriends and HMCColor, which has never run on our servers.

A busy hopper moves an item every 8 ticks, so 1,000 active hoppers means about 125 events per tick,
each passed through all of the above.

Before picking this, profile on the dev server with the setting off (`/spark profiler` with hoppers
running), and check the WorldGuard and CoreProtect settings above.

### 3. Watch only hoppers next to furnaces (recommended, not built)

Keep the setting on (events off) and feed custom fuel without events:

- Keep a set of furnaces with a hopper pointing into their side. Add to it when a hopper or furnace
  is placed, and when a chunk loads (scan its block entities). Remove from it on break and chunk unload.
- Every 8 ticks (a hopper's speed), for each tracked furnace: if the hopper holds custom fuel and the
  fuel slot has room, move one item across.
- The same watcher can refresh old copies of items that a hopper above a furnace drops into its input
  slot (see `FurnaceInputRefresher`).

This costs nothing for other hoppers and doesn't wake CoreProtect or WorldGuard. The cost is more code
to keep the tracked set correct. Once built, the `onHopperMove` handlers can be removed.
