# Version 1.3.1+26.3-fabric.10

- Suppressed item drops when `/kill` removes turrets, cubes, or Gabe, including carried turrets.

# Version 1.3.1+26.3-fabric.9

- Fixed `/kill` being rejected by the shared damage filter for turrets, cubes, and Gabe.
- Added command regressions for protected test elements and carried turrets while retaining ordinary damage protection.

# Version 1.3.1+26.3-fabric.8

- Restored the Forest Cake and End radio advancements for normal block interactions on 26.3.
- Kept held candles from replacing Forest Cake with a vanilla candle cake.
- Added real-player checks for cake eating, the radio transmission, boots, inventory unlocks, and fizzling all three cube types.

# Version 1.3.1+26.3-fabric.7

- Hid Gabe from the creative inventory and creative search, matching the original mod.

# Version 1.3.1+26.3-fabric.6

- Restored the original encoder's immediate response to ordinary redstone changes while deferring repeated feedback across ticks.
- Reproduced the standing-button feedback circuit in the original Forge 1.16.5 mod and added matching checks for input timing and normal-mode latching.
- Added rendered-frame checks that the client displays both states of a running inverted clock.

# Version 1.3.1+26.3-fabric.5

- Replaced recursive antline signal propagation with a connected-network scan that reads all inputs before changing wires or outputs.
- Fixed encoder/decoder feedback producing repeated intermediate signal changes and exhausting the heap when another input, such as a standing button, shares the loop.
- Kept inverted indicator feedback running as a clock on server ticks, while stable powered circuits stop sending redundant wire updates.
- Kept shared indicators powered when another connected network still has an active input.
- Added packet-count regressions and opt-in client/server memory profiles for the button circuit and running clock, including Sodium with native Vulkan.

# Version 1.3.1+26.3-fabric.4

- Kept the first-person body hidden during crouching and swimming pose transitions.
- Fixed breaking one antline face discarding the other faces on the client, including wall/floor combinations.
- Moved encoder input changes to scheduled ticks to separate feedback across server ticks.
- Fixed Vulkan's same-submission cloud fence crash by keeping cloud buffers separate for each portal view.
- Restored the original portal texture orientation, with the bright border at the bottom.
- Stopped back-face portal highlights drawing over visible portal openings while keeping highlights through solid walls.
- Removed unintended outlines from worn Long Fall Boots.
- Fixed Done and Escape navigation in PortalMod options and centered the PortalMod title artwork.
- Added client, server, and rendered-pixel regressions for the reported cases.

# Version 1.3.1+26.3-fabric.3

- Fixed the walking-bob and auto-jump measurements counting portal displacement as physical walking, which caused the screen to shake during traversal.
- Kept the distant local body visible when the camera crossed before the body; refreshed asynchronous culling during side changes so nearby entities do not blink out.
- Rejected recursive views entirely behind an exit clip plane without discarding visible portal slivers.
- Restored the original turret damage cadence using the current damage-cooldown tag; retained the original 60-percent roll, damage, ammo, and difficulty behavior.
- Put the PortalMod options button into Minecraft's options grid and kept the configuration controls within small windows.
- Verified original fizzle-item drops, including dropper-cube and turret exceptions, and retained that behavior.
- Added real Survival-player turret checks, sustained-fire cooldown coverage, and menu layout checks at several GUI sizes.
- Verified the Sodium performance profile with Minecraft's native Vulkan backend.

# Version 1.3.1+26.3-fabric.2

- Fixed the portal aperture disappearing during angled crossings by preserving per-fragment depth while clipping its projected silhouette.
- Matched camera handoff to the portal's exact transform and hid the local avatar when a portal camera is inside its interpolated head.
- Removed the redundant portal box-frustum rejection that could hide an opening at a screen edge.
- Reused texture-size buckets during portal approach instead of reallocating render targets for tiny aperture changes.
- Build Sodium's visible section lists for the current portal or restored main camera instead of reusing an asynchronous result from another view.
- Fixed unintended white glowing outlines on cube emissive layers and chamber signs.
- Snapshot entity teleport history before asynchronous packet encoding, replay it once per entity tick, and reset rotation interpolation at crossings.
- Kept local held-object prediction separate from delayed server echoes and remote interpolation; sized the carry offset to avoid catching on portal headroom.
- Restored block-item translation prefixes, including Radio, panel variants, test elements, and gel containers.
- Added rendered-pixel checks for crossing angles, screen-edge openings, main-world terrain, ordinary wall occlusion, cube rendering and split geometry, plus native and TCP cube traversal.
- Added opt-in per-rendered-frame recordings for straight, angled, loop, separate-room, and carried-cube traversal and turning.

# Version 1.3.1+26.3-fabric.1

- Integrated the complete Fabric 26.3 source port.
- Fixed black portal skies by initializing destination environment probes and rendering the sky with an unclipped projection and the active scene target.
- Preserved full terrain distance through recursive portals and added conservative screen-edge bounds and aperture frusta.
- Added Sodium camera, terrain-list, and projection integration; retained Entity Culling visibility decisions separately for portal views.
- Fixed the Lithium collision-mixin startup conflict and dedicated-server loading of client-only portal methods.
- Added visual regressions for distant terrain, angled portal edges, destination sky, and main-camera-occluded entities, plus ordinary swimming and antline branch coverage.

# Version 1.3.1
### Changes
- Added stonecutter recipes for Lunecast and Blackplate

### Fixes
- Fixed recursive Portals rendering outside their frame
- Fixed Portal placement on lowered Platforms
- Fixed `/portal` command being able to create broken Portals
- Fixed levitation effect and firework rockets' flight
- Removed additional upwards velocity applied from ceiling to floor Portals

# Version 1.3
### Changes
- Updated translations
- Platform Beams can now be placed inside Frames
- Portal Guns can now be made identical by placing two of them in a crafting grid
- Portals can no longer be placed in liquids
- Wrenches no longer toggle Beams on Platforms
- Gel can no longer be placed where another type of Gel is already present
- Booster skin is now animated

### Fixes
- Fixed Super Button crash when not all of its blocks are present
- Fixed Platform Beams always being sticky with Create
- Fixed Portal texel snapping causing problems

# Version 1.2
### Changes
- Cubes now drop when held out of sight through a Portal
- Antline Encoders and Decoders now show all their variants
- Added fire to tag portal_nonblocking
- Chamber signs now turn off only when strongly powered by redstone

### Fixes
- Fixed Portals not rendering behind translucent geometry
- Fixed Antline Encoders signal propagation
- Internal tags now do not override external overrides
- Fixed memory leak
- Fixed Faith Plate locking Wrench when destroyed
- Fixed some language inconsistencies
- Fixed fireballs teleportation

# Version 1.1.1
### Fixes
- Players can bounce again
- Chamber signs turn off when powered from all directions

# Version 1.1
### Changes
- MacOS is now supported
- Updated credits
- Updated Finnish
- New `/portal close all` option to close all Portals
- New Portal occlusion techniques for performance
- Co-op Portals can now be replaced by other Portal Guns

### Fixes
- Using Essential mod no longer crashes
- Fizzlers no longer fizzle all Portal Guns on servers
- Antlines no longer crash in rare cases
- Repulsion Gel now properly bounces Cubes and Turrets
- Push Doors no longer close automatically when powered
- Push Doors now play the proper sounds when powered
- Prevented some stray Portal pixels from staying on the screen
- Other small fixes

# Version 1.0
### Mod was finally released! 🎉
