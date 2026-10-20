# Attribution and Third-Party Notices

This project supports Minecraft 1.20.1 (Forge and Fabric) and Minecraft 1.21.1 (NeoForge and Fabric).

This project is licensed under the GNU Lesser General Public License version 3 only (SPDX: `LGPL-3.0-only`). See `LICENSE` for the full license text, including the incorporated GNU General Public License version 3 terms.

The implementation includes server-authoritative networking, transient data attachments, tick-based timing, optional Player Animator 2.x integration, optional CameraOverhaul parry camera feedback.

Shared source code and runtime assets are in `shared/`, grouped into common code, Minecraft version adapters, loader helpers, gameplay handlers, and optional integrations. Each installable target has its own standard source tree under `platforms/<loader>/<minecraft-version>/`. Build conventions and dependency versions are maintained in `gradle/`.

## Third-Party Dependencies

### NightConfig

The Fabric artifacts bundle NightConfig core and TOML 3.8.3 as nested JARs for configuration parsing. NightConfig is authored by TheElectronWill and licensed under LGPL-3.0. Project and source: https://github.com/TheElectronWill/night-config. Forge and NeoForge use their loader's configuration system.

### Player Animator

Player Animator is an optional external dependency, authored by KosmX and licensed under MIT. It is not bundled in this mod's JAR.

### CameraOverhaul

CameraOverhaul is an optional external dependency, authored by Mirsario & Contributors and licensed under GPL-3.0. It is not bundled in this mod's JAR.

The impact animation and integration code in this project were independently implemented; no CameraOverhaul source code or assets are included.

### Particle Interactions / Block Place Particles

Metal spark visuals were inspired by Particle Interactions' FlyingSpark and SparkFlash effects by TallestRed and Enchanted_Games, referenced through:

https://github.com/seymourimadeit/block-place-particles-1.20.1

The spark implementation here is independent and uses vanilla Minecraft sprites; no source code or textures from that project are bundled.

### NeoOrigins

NeoOrigins is an optional external dependency by CyberDay1, licensed under MIT.

Its Java power API is used for pack-defined parry permissions. No NeoOrigins source code or assets are bundled in the published mod JAR.

## Development Tools

The Gradle wrapper is licensed under Apache-2.0, as stated in its scripts.

CFR is a development tool licensed under MIT and is not bundled in this mod's JAR.
