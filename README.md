![PortalMod Logo](https://cdn.modrinth.com/data/cached_images/d2a8ac1e46d43d63102a3dcb0b2541e60ea033b9.png)

<br>

<div align="center">
  <a target="_blank" href="https://youtube.com/@portalmod"><img src="https://cdn.modrinth.com/data/cached_images/28f46f6f66eed237e1c37b9a251105d688d69f56.png"></a>
  <a target="_blank" href="https://discord.gg/hUXXtfJc44"><img src="https://cdn.modrinth.com/data/cached_images/b019f707c8c05a8129490e1c1839822537521968.png"></a>
  <a target="_blank" href="https://curseforge.com/minecraft/mc-mods/portalmod"><img src="https://cdn.modrinth.com/data/cached_images/fa4b39c31e79e8ea0017eef4260b7e32f628d65e.png"></a>
  <a target="_blank" href="https://modrinth.com/mod/portalmod" rel="nofollow"><img src="https://cdn.modrinth.com/data/cached_images/6bb36ca8862c46f90ae788d560c5c6dd9c9d82f2.png"></a>
  <a target="_blank" href="https://portalmod.net/"><img src="https://cdn.modrinth.com/data/cached_images/296f0d8a2baa2a35da73b3e8ce0a1251c2c673a4.png"></a>
</div>

<br>
<div align="center">
  <img src="https://cdn.modrinth.com/data/cached_images/c0fbbf79fd11a2b47b0fca7481caf73cfe7f4eb6.png">
</div>

## Fabric 26.3 port

This checkout ports the main Forge 1.16.5 mod to Fabric 26.3. The complete source builds, the packaged JAR starts on both client and dedicated server, and the automated gameplay checks below pass.

The build targets Java 25, Gradle 9.6.1, Loom 1.17.21, Fabric Loader 0.20.1, and Fabric API 0.163.0+26.3. Run Gradle with Java 25.

`./gradlew build` compiles the complete mod and runs unit tests and dedicated-server game tests. The Fabric JAR is written to `build/libs`. Install it with Fabric API on Minecraft 26.3. The TOML configuration keeps the existing `portalmod-client.toml` filename and settings.

Use `./gradlew runClient` for manual testing, `./gradlew runClientGameTest` for automated portal rendering, surface visibility at screen edges, particle isolation, player traversal, holdable movement/release and look rotation, configuration screens, connected fizzler rendering, antline face synchronization, emitter/trigger configuration save/reload, trigger selection/cancellation/disconnect cleanup, faith-plate target/height editing and disconnect cleanup, chamber-sign appearance, radio sound playback/stop and stale updates, and local TCP multiplayer checks, and `./gradlew runDatagen` to regenerate native models, recipes and loot tables. Client game tests require a working graphics display and audio device.

Server game tests cover registered content, portal persistence, cube traversal, all three super-button modes, faith plates, gels, goo, fizzler fields, cube dropper replenishment/removal, trigger signals and saved bounds, automatic portal activation and saved links, indicator-controlled chamber doors, complete button/antline/indicator/door circuits, antline selection and saved face edits, turret targeting/damage, long-fall boot protection, named holdable drops, radio manual/redstone control and chamber-sign wrench edits/save-load. The packaged JAR has also started a production dedicated server and loaded an isolated singleplayer world through the production client loader. Legacy game rules, item data and portal entity data also have targeted migration tests.

A client test runs Minecraft's native folder/metadata converter on a generated 1.16.5-format world, then opens and reopens it. It checks the world name, mod game rules, chunks from the old Overworld/Nether/End folders, the original global portal file, mod blocks, a chest's portal gun identity, trigger bounds/type, automatic portal links/colors, antline faces, a named cube's identity and chamber-sign mounting/display settings. The fixture includes old Forge data-pack references; Minecraft warns about those missing pack names and loads the Fabric resources. Mod entity IDs are registered with Minecraft's upgrade schemas so conversion preserves them; chamber signs also accept their old wall-position fields through the native position validator. These tests use generated historical-format saves; existing player worlds have not been manually verified.

## Support & Discussion

* **Bug Reports:** If you encounter any bugs or have a feature request, please report them in the [Issues tab](../../issues).
* **Community:** Stay up-to-date, share your test chambers, and chat with others in our [Discord Server](https://discord.gg/portalmod).

## Contributing

PortalMod is our passion project, and we manage development closely to maintain its specific vision. Please read our **[Contributing Guidelines](../../blob/master/CONTRIBUTING.md)** if you want to contribute.

---
Find more information at [portalmod.net](https://portalmod.net/).
