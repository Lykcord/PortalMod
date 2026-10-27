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

## Support & Discussion

* **Bug Reports:** If you encounter any bugs or have a feature request, please report them in the [Issues tab](../../issues).
* **Community:** Stay up-to-date, share your test chambers, and chat with others in our [Discord Server](https://discord.gg/portalmod).

## Contributing

PortalMod is our passion project, and we manage development closely to maintain its specific vision. Please read our **[Contributing Guidelines](../../blob/master/CONTRIBUTING.md)** if you want to contribute.

---
Find more information at [portalmod.net](https://portalmod.net/).


## Fabric 26.3 build and checks

This fork targets Minecraft Java 26.3, Fabric Loader 0.20.1, and Java 25.

```sh
./gradlew build
./gradlew runGameTest
./gradlew runClientGameTest
./gradlew runClientGameTest -PperformanceMods
./gradlew runClientGameTest --args="--graphicsBackend VULKAN"
./gradlew runClientGameTest -PperformanceMods --args="--graphicsBackend VULKAN"
```

The optional performance test profile loads Sodium 0.9.2, Lithium 0.26.2, FerriteCore 9.0.0, ImmediatelyFast 1.17.1, and the Fabric 26.3 build of Entity Culling 1.11.3. These mods are not bundled or required by PortalMod.

The client suite passes with vanilla rendering and the performance profile on both OpenGL and Minecraft's native Vulkan backend. Vulkan was checked on Linux with an NVIDIA RTX 3080.

The client checks include rendered-pixel assertions for 50 crossing positions and angles, screen-edge portal openings, main-world terrain restoration, wall occlusion, destination sky, and cube meshes on both sides of an opening. They also check unwanted outlines, item translations, single-sample movement replay, TCP player/cube traversal, and chamber-content interactions. The server checks cover the original testing elements, persistence, and native ticking.

`./gradlew runClientGameTest -PperformanceMods -PwalkCapture` records every rendered frame and camera state under `build/reports/portal-walk/performance`. Optional `-PwalkCaptureDepth=9 -PwalkCaptureFov=110` reproduces the review settings. Captures include repeated side steps across the portal plane with a player and cube in view. `-PclientTest=PortalWalkingClientTest` runs just the capture; omit that selector for the full client suite.

`./gradlew runClientGameTest -PperformanceMods -PreviewWorld` also exports a separate creative world with cheats and review stations into `run-review/saves/PortalMod 26.3 Review`. It preserves an existing review world instead of overwriting it.
