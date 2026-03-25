# PART 1 - Bedrock
## Geyser particle trail.
## What this implements
- Customisable Geyser trails.
- Custom implemented texture as proof of understanding
After hours of trying many different designs, with custom textures and basic ones. I decided a complex particle texture for this use case would not look right.
I found the best results using a basic square that is colourable via particle settings in snowstorm. I implemented more than 1 individual particles to create a combined particle trail effect arrangement.

The particle trail I created was in respect to the projcet of GeyserMC, a pivotal plugin in Minecraft Java -> Bedrock relationships. respecting this by creating a rocky water geyser that follows the player and erupts after a second before slowly fading away.

I already created a full demonstration world with the command blocks setup and preview tracks for easy viewing!
(Included is also a fire / lava version of the same geyser, more of a volcano geyser)


## Manual implementation without demo world
(Water version)
- Repeating command block: [/execute as @a at @s run particle novua:hydro_geyser_trail ~ ~ ~] (optional rubble trail)
- Repeating command block: [/execute as @a at @s run particle novua:hydro_geyser_buildup ~ ~ ~] (Delay In Ticks: 15)
  - Chain command block: [/execute as @a at @s run particle novua:hydro_geyser_spout ~ ~-0.1 ~] (Always active)
  - Chain command block: [/execute as @a at @s run particle novua:hydro_geyser_spray ~ ~-0.1 ~] (Always active)

(Fire version)
- Repeating command block: [/execute as @a at @s run particle novua:magma_geyser_trail ~ ~ ~] (optional rubble trail)
- Repeating command block: [/execute as @a at @s run particle novua:magma_geyser_buildup ~ ~ ~] (Delay In Ticks: 15)
  - Chain command block: [/execute as @a at @s run particle novua:magma_geyser_spout ~ ~-0.1 ~] (Always active)
  - Chain command block: [/execute as @a at @s run particle novua:magma_geyser_spray ~ ~-0.1 ~] (Always active)

<img width="935" height="383" alt="image" src="https://github.com/user-attachments/assets/bc591c4a-dbc4-46f8-a7f2-9ddac4eb96be" />




# PART 2 - Java
## cc_trails — Java Edition Bukkit/Paper Particle Trail Submission

one clean particle-trail plugin, Gradle build, and three math-driven examples.

## What this implements

- **Platform:** Paper/Bukkit API (Java Edition)
- **Build system:** **Gradle** (no Maven)
- **Trail types:** configurable **helix**, **orbit**, and **wave** examples
- **Math-driven placement:** Uses a **parametric equation** (not a static offset line)
- **Tunable parameters:** radius, frequency, step spacing, phase speed, vertical stretch, point count
- **Flat orientation:** trail basis ignores pitch so head up/down movement does not tilt/rotate effects
- **Variance:** point playback is grouped and sequenced so not all points spawn on the same tick
- **Move-event activation:** particles are driven by remembered `PlayerMoveEvent` position changes

## Mathematical algorithm (with tunable parameters)

The trail uses horizontal (yaw-only) orientation and supports these examples:

- **Helix:** `x(t) = r cos(ft)`, `z(t) = r sin(ft)`, `y(i) = v*i`
- **Orbit:** `right = r cos(ft)`, `forward = r sin(ft)`, `vertical = 0` (flat horizontal ring)
- **Wave:** `back(i) = i*step`, `side(t) = r sin(ft)`, `vertical = 0` (trails behind player)

For each sampled point, the plugin maps to a yaw-only basis:
- `right` from `worldUp × forward`
- `forward` flattened to horizontal (pitch removed)
- `worldUp` fixed to `(0,1,0)`

This keeps effects visually flat while still following player yaw.

### Grouped point sequencing (variance)

To avoid all points firing at once, points are split into virtual groups by index:

- Group bucket: `i % groupSize`
- Active frame: `(groupTick / groupFrameDelayTicks) % groupSize`

Only points matching the active frame render that tick, so all groups progress `1 -> 2 -> 3 -> restart` in lockstep.
this can save performance while giving a fluttering appearence to the particle. especially noticable in Wave and Helix

### Movement true/false state

- On every `PlayerMoveEvent`, if XYZ position changed from remembered location, movement is set to **true**.
- A timer (`movementTimeoutMs`) is refreshed on each real position change.
- If later move events occur without position change and the timer has expired, movement flips to **false**.
- Particle rendering checks this true/false state when `movingOnly` is enabled.

From personal experience i understood PlayerMoveEvent doesnt happen reliably enough to hold onto it solely. hense why i created the timeout refreshment instead of just triggering it directly.

### Relevant Java code

- `src/main/java/net/novua/cc_trails/HelixTrailRenderer.java`
  - Contains all three equations and particle placement logic.
  - Includes inline comments describing the math and coordinate mapping.
- `src/main/java/net/novua/cc_trails/TrailManager.java`
  - Loads parameters from config and advances per-player phase over time.
- `src/main/java/net/novua/cc_trails/Cc_trails.java`
  - Minimal command handling: `/trail on`, `/trail off`, `/trail reload`.

## Configuration

All key parameters are in `src/main/resources/config.yml` under `trail:`.

Most relevant keys to tweak:
- `radius`
- `frequency`
- `points`
- `step`
- `phaseStep`
- `verticalStretch`
- `movingOnly`
- `groupSize`
- `groupFrameDelayTicks`
- `movementTimeoutMs` (how long movement stays active after a position-changing move event)

## Setup / Run

```bash
./gradlew build
```

Jar output:

```text
build/libs/cc_trails-1.jar
```

(Alternatively see "production" folder to get a ready made jar and configuration files tailored to this application)

## In-game usage

- `/trail on` – enable your trail with default config style
- `/trail off` – disable your trail
- `/trail reload` – reload config (requires `cc_trails.reload`)
- `/trail helix` – switch to helix style
- `/trail orbit` – switch to orbit style (example 2)
- `/trail wave` – switch to wave style (example 3)
