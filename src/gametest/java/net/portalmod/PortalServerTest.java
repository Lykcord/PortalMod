package net.portalmod;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec2;
import net.portalmod.common.entities.TestElementEntity;
import net.portalmod.common.sorted.antline.AntlineBlock;
import net.portalmod.common.sorted.antline.AntlineTileEntity;
import net.portalmod.common.sorted.antline.indicator.AntlineIndicatorBlock;
import net.portalmod.common.sorted.antline.indicator.AntlineOutput;
import net.portalmod.common.sorted.autoportal.AutoPortalBlock;
import net.portalmod.common.sorted.autoportal.AutoPortalTileEntity;
import net.portalmod.common.sorted.button.ButtonMode;
import net.portalmod.common.sorted.button.QuadBlockCorner;
import net.portalmod.common.sorted.button.SuperButtonBlock;
import net.portalmod.common.sorted.cubedropper.CubeDropperBlock;
import net.portalmod.common.sorted.cubedropper.CubeDropperTileEntity;
import net.portalmod.common.sorted.door.ChamberDoorBlock;
import net.portalmod.common.sorted.faithplate.FaithPlateBlock;
import net.portalmod.common.sorted.faithplate.FaithPlateTileEntity;
import net.portalmod.common.sorted.faithplate.Flingable;
import net.portalmod.common.sorted.fizzler.FizzlerEmitterBlock;
import net.portalmod.common.sorted.gel.AbstractGelBlock;
import net.portalmod.common.sorted.gel.IGelAffected;
import net.portalmod.common.sorted.goo.GooBlock;
import net.portalmod.common.sorted.portal.PortalEnd;
import net.portalmod.common.sorted.portal.PortalEntity;
import net.portalmod.common.sorted.portal.PortalManager;
import net.portalmod.common.sorted.portal.PortalPlacer;
import net.portalmod.common.sorted.radio.RadioBlock;
import net.portalmod.common.sorted.radio.RadioBlockTileEntity;
import net.portalmod.common.sorted.sign.ChamberSignEntity;
import net.portalmod.common.sorted.trigger.TriggerBlock;
import net.portalmod.common.sorted.trigger.TriggerTileEntity;
import net.portalmod.common.sorted.trigger.TriggerType;
import net.portalmod.common.sorted.turret.TurretEntity;
import net.portalmod.core.init.BlockInit;
import net.portalmod.core.init.BlockTagInit;
import net.portalmod.core.init.EntityInit;
import net.portalmod.core.init.ItemInit;
import net.portalmod.core.init.TileEntityTypeInit;
import net.portalmod.core.math.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

public class PortalServerTest {
    @GameTest
    public void killCommandRemovesProtectedTestElements(GameTestHelper test) {
        var level = test.getLevel();
        var server = level.getServer();
        for (var type :
                List.of(
                        EntityInit.TURRET.get(),
                        EntityInit.STORAGE_CUBE.get(),
                        EntityInit.COMPANION_CUBE.get(),
                        EntityInit.VINTAGE_CUBE.get(),
                        EntityInit.GABE.get())) {
            var entity = test.spawn(type, 3, 3, 3);
            entity.setPermanentlyInvulnerable(true);
            test.assertTrue(
                    !entity.hurtServer(level, entity.damageSources().generic(), Float.MAX_VALUE),
                    "Ordinary damage must retain the original test-element protection: " + type);
            server.getCommands()
                    .performPrefixedCommand(server.createCommandSourceStack(), "kill " + entity.getStringUUID());
            test.assertTrue(entity.isRemoved(), "The actual kill command must remove " + type);
        }
        var holder = test.makeMockPlayer(GameType.SURVIVAL);
        var held = test.spawn(EntityInit.TURRET.get(), 3, 3, 3);
        test.assertTrue(held.pickUp(holder), "The command must also work on a carried turret");
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "kill " + held.getStringUUID());
        test.assertTrue(
                held.isRemoved() && holder.getPassengers().isEmpty(),
                "Killing a held turret must also detach it from the holder");
        test.succeed();
    }

    @GameTest(maxTicks = 110)
    public void turretDamageCadenceMatchesOriginalWithoutExtraHurtCooldown(GameTestHelper test) {
        for (int x = 1; x <= 5; x++) {
            for (int z = 1; z <= 7; z++) {
                test.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
            }
        }
        var turret =
                new TurretEntity(EntityInit.TURRET.get(), test.getLevel()) {
                    @Override
                    protected boolean rollBulletHit() {
                        return true;
                    }
                };
        turret.snapTo(test.absoluteVec(new net.minecraft.world.phys.Vec3(3, 2, 2)), 0, 0);
        turret.setSilent(true);
        var target = test.spawn(EntityTypes.COW, 3, 2, 6);
        target.setNoAi(true);
        target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);
        target.setHealth(1000);
        turret.setYRot(0);
        turret.targetEntity = target;
        turret.setAmmo(40);
        var targetPosition = target.position();
        for (int i = 0; i < 40; i++) {
            int shot = i;
            test.runAfterDelay(
                    i * 2 + 1,
                    () -> {
                        target.snapTo(targetPosition);
                        turret.tickCount = shot * 2 + (turret.getId() & 1);
                        turret.shoot();
                        target.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
                    });
        }
        test.runAfterDelay(
                82,
                () -> {
                    test.assertTrue(turret.getAmmo() == 0, "Original cadence consumes 40 rounds in 80 ticks");
                    test.assertTrue(
                            Math.abs(target.getHealth() - 980) < .001F,
                            "Successful turret shots must bypass the ordinary hurt cooldown: health="
                                    + target.getHealth());
                    test.succeed();
                });
    }

    @GameTest
    public void fizzleDropsMatchTheOriginalProject(GameTestHelper test) {
        for (boolean fromDropper : new boolean[] {false, true}) {
            var cube = test.spawn(EntityInit.STORAGE_CUBE.get(), 3, 3, 3);
            cube.setFromDropper(fromDropper);
            var bounds = cube.getBoundingBox().inflate(2);
            cube.fizzleKill();
            var drops = test.getLevel().getEntitiesOfClass(ItemEntity.class, bounds);
            test.assertTrue(
                    drops.stream().anyMatch(item -> item.getItem().is(ItemInit.STORAGE_CUBE.get())) == !fromDropper,
                    "Original fizzle behavior drops ordinary cubes and suppresses dropper cubes");
            drops.forEach(Entity::discard);
        }
        var turret = test.spawn(EntityInit.TURRET.get(), 3, 3, 3);
        var bounds = turret.getBoundingBox().inflate(2);
        turret.fizzleKill();
        test.assertTrue(
                test.getLevel().getEntitiesOfClass(ItemEntity.class, bounds).isEmpty(),
                "Original no-fizzle-drops turret tag remains effective");
        test.succeed();
    }

    @GameTest(maxTicks = 30)
    public void radioRespondsToManualAndRedstoneControl(GameTestHelper test) {
        var pos = test.absolutePos(new BlockPos(3, 2, 3));
        var level = test.getLevel();
        level.setBlock(pos.below(), Blocks.STONE.defaultBlockState(), 2);
        level.setBlock(pos, BlockInit.RADIO.get().defaultBlockState(), 2);
        var radio = (RadioBlockTileEntity) level.getBlockEntity(pos);
        test.runAfterDelay(
                2,
                () -> {
                    test.assertTrue(!radio.isPlaying(), "Unpowered radio starts silent");
                    radio.switchManual();
                    test.assertTrue(radio.isPlaying(), "Manual interaction starts the radio");
                    radio.switchManual();
                    test.assertTrue(!radio.isPlaying(), "Manual interaction stops the radio");
                    level.setBlock(pos.east(), Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
                    test.assertTrue(
                            radio.isPlaying() && level.getBlockState(pos).getValue(RadioBlock.POWERED),
                            "Redstone starts the radio");
                    radio.switchManual();
                    test.assertTrue(radio.isPlaying(), "Manual interaction cannot turn off a powered radio");
                    level.setBlock(pos.east(), Blocks.AIR.defaultBlockState(), 3);
                    test.assertTrue(!radio.isPlaying(), "Removing redstone stops the radio in the overworld");
                    level.setBlock(pos.below(), Blocks.AIR.defaultBlockState(), 3);
                    test.assertTrue(level.getBlockState(pos).isAir(), "Radio breaks when its support is removed");
                    test.succeed();
                });
    }

    @GameTest
    public void chamberSignWrenchEditsSurviveNativeSaveAndLoad(GameTestHelper test) {
        var level = test.getLevel();
        for (int x = 1; x <= 5; x++) {
            for (int y = 1; y <= 5; y++) {
                test.setBlock(new BlockPos(x, y, 2), Blocks.STONE);
            }
        }
        var sign = new ChamberSignEntity(level, test.absolutePos(new BlockPos(3, 3, 3)), Direction.SOUTH, true);
        test.assertTrue(sign.survives(), "Chamber sign can attach to its supporting wall");
        level.addFreshEntity(sign);
        var player = test.makeMockPlayer(GameType.SURVIVAL);
        player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ItemInit.WRENCH.get()));
        sign.interact(player, InteractionHand.MAIN_HAND, new net.minecraft.world.phys.Vec3(-.25, 0, 0));
        sign.interact(player, InteractionHand.MAIN_HAND, new net.minecraft.world.phys.Vec3(.25, 0, 0));
        sign.interact(player, InteractionHand.MAIN_HAND, new net.minecraft.world.phys.Vec3(-.125, -.75, 0));
        var data = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
        sign.saveWithoutId(data);
        var box = sign.getBoundingBox();
        sign.discard();
        var restored = EntityInit.CHAMBER_SIGN.get().create(level, EntitySpawnReason.COMMAND);
        restored.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), data.buildResult()));
        test.assertTrue(
                restored.getLeftDigit() == 1 && restored.getRightDigit() == 1 && restored.getIcon2(),
                "Wrench-edited sign digits and icon survive entity reload");
        test.assertTrue(
                restored.getDirection() == Direction.SOUTH
                        && restored.getVerticallyAligned()
                        && restored.getBoundingBox().equals(box)
                        && restored.survives(),
                "Sign mounting and bounds survive entity reload");
        test.succeed();
    }

    @GameTest
    public void namedHoldablesKeepTheirNamesWhenDropped(GameTestHelper test) {
        var name = Component.literal("Keep this name");
        var types =
                List.of(
                        EntityInit.STORAGE_CUBE.get(),
                        EntityInit.COMPANION_CUBE.get(),
                        EntityInit.VINTAGE_CUBE.get(),
                        EntityInit.GABE.get(),
                        EntityInit.TURRET.get());
        var expected =
                List.of(
                        ItemInit.STORAGE_CUBE.get(),
                        ItemInit.COMPANION_CUBE.get(),
                        ItemInit.VINTAGE_CUBE.get(),
                        ItemInit.GABE.get(),
                        ItemInit.TURRET.get());
        for (int index = 0; index < types.size(); index++) {
            var entity = (TestElementEntity) test.spawn(types.get(index), 3, 3, 3);
            entity.setCustomName(name);
            entity.applyDamage(entity.damageSources().generic(), 5, false, false);
            var drops = test.getLevel().getEntitiesOfClass(ItemEntity.class, entity.getBoundingBox().inflate(2));
            var item = expected.get(index);
            var drop =
                    drops.stream()
                            .filter(value -> value.getItem().is(item))
                            .findFirst()
                            .orElseThrow(
                                    () ->
                                            new AssertionError(
                                                    "Broken holdable did not drop its item: " + entity.getType()));
            test.assertTrue(
                    name.equals(drop.getItem().get(DataComponents.CUSTOM_NAME)),
                    "Broken holdable retains its custom name: " + entity.getType());
            drops.forEach(Entity::discard);
        }
        test.succeed();
    }

    @GameTest
    public void longFallBootsPreventFallDamageOnlyWhileEquipped(GameTestHelper test) {
        var wearer = test.spawn(EntityTypes.COW, 2, 3, 2);
        wearer.setItemSlot(EquipmentSlot.FEET, new ItemStack(ItemInit.LONGFALL_BOOTS.get()));
        float health = wearer.getHealth();
        wearer.causeFallDamage(12, 1, wearer.damageSources().fall());
        test.assertTrue(wearer.getHealth() == health, "Long fall boots cancel fall damage");
        wearer.hurtServer(test.getLevel(), wearer.damageSources().generic(), 1);
        test.assertTrue(wearer.getHealth() < health, "Boots do not cancel unrelated damage");
        var unprotected = test.spawn(EntityTypes.COW, 5, 3, 2);
        unprotected.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ItemInit.LONGFALL_BOOTS.get()));
        float unprotectedHealth = unprotected.getHealth();
        unprotected.causeFallDamage(12, 1, unprotected.damageSources().fall());
        test.assertTrue(unprotected.getHealth() < unprotectedHealth, "Holding boots does not grant fall protection");
        test.succeed();
    }

    @GameTest
    public void triggerFieldEditsAreSavedWithoutAStateChange(GameTestHelper test) {
        var pos = test.absolutePos(new BlockPos(2, 2, 2));
        test.getLevel()
                .setBlock(
                        pos,
                        BlockInit.TRIGGER.get().defaultBlockState().setValue(TriggerBlock.TYPE, TriggerType.MOB),
                        2);
        var trigger = (TriggerTileEntity) test.getLevel().getBlockEntity(pos);
        var chunk = test.getLevel().getChunkAt(pos);
        chunk.tryMarkSaved();
        trigger.setField(new BlockPos(-2, 0, -1), new BlockPos(3, 2, 4));
        test.assertTrue(chunk.isUnsaved(), "Editing a trigger field schedules its new bounds for saving");
        var restored =
                (TriggerTileEntity)
                        BlockEntity.loadStatic(
                                pos,
                                trigger.getBlockState(),
                                trigger.saveWithFullMetadata(test.getLevel().registryAccess()),
                                test.getLevel().registryAccess());
        test.assertTrue(
                restored.getField().equals(new AABB(-2, 0, -1, 4, 3, 5)),
                "Configured trigger bounds survive native serialization");
        test.assertTrue(
                restored.getTriggerType() == TriggerType.MOB,
                "Reloaded mob trigger keeps the correct field rendering type");
        test.succeed();
    }

    @GameTest
    public void autoPortalLinkAndEndEditsAreSaved(GameTestHelper test) {
        var pos = test.absolutePos(new BlockPos(2, 2, 2));
        var state =
                BlockInit.AUTOPORTAL
                        .get()
                        .defaultBlockState()
                        .setValue(AutoPortalBlock.CORNER, QuadBlockCorner.DOWN_LEFT);
        test.getLevel().setBlock(pos, state, 2);
        var emitter = (AutoPortalTileEntity) test.getLevel().getBlockEntity(pos);
        var chunk = test.getLevel().getChunkAt(pos);
        var gun = UUID.randomUUID();
        chunk.tryMarkSaved();
        emitter.link(gun, PortalEnd.PRIMARY, 1, 10);
        test.assertTrue(chunk.isUnsaved(), "Linking an automatic emitter saves its gun and colors");
        chunk.tryMarkSaved();
        emitter.swapEnd();
        test.assertTrue(chunk.isUnsaved(), "Changing an automatic emitter's portal end is saved");
        var restored =
                (AutoPortalTileEntity)
                        BlockEntity.loadStatic(
                                pos,
                                state,
                                emitter.saveWithFullMetadata(test.getLevel().registryAccess()),
                                test.getLevel().registryAccess());
        test.assertTrue(
                gun.equals(restored.gunUUID)
                        && restored.end == PortalEnd.SECONDARY
                        && restored.primaryColor == 1
                        && restored.secondaryColor == 10,
                "Automatic emitter link survives native serialization");
        test.succeed();
    }

    @GameTest(maxTicks = 30)
    public void indicatorOpensLinkedAutomaticPortal(GameTestHelper test) {
        var level = test.getLevel();
        var block = (AutoPortalBlock) BlockInit.AUTOPORTAL.get();
        var state =
                block.defaultBlockState()
                        .setValue(AutoPortalBlock.FACING, Direction.SOUTH)
                        .setValue(AutoPortalBlock.CORNER, QuadBlockCorner.DOWN_LEFT);
        var pos = test.absolutePos(new BlockPos(2, 2, 3));
        var parts = new HashMap<>(block.getOtherParts(state, pos));
        parts.put(pos, state);
        parts.forEach(
                (part, value) -> {
                    level.setBlock(part.north(), BlockInit.LUNECAST.get().defaultBlockState(), 2);
                    level.setBlock(part, value, 2);
                });
        var emitter = (AutoPortalTileEntity) level.getBlockEntity(pos);
        var gun = UUID.randomUUID();
        emitter.link(gun, PortalEnd.PRIMARY, 1, 10);
        var indicatorPos = pos.below();
        level.setBlock(indicatorPos.below(), Blocks.STONE.defaultBlockState(), 2);
        level.setBlock(
                indicatorPos,
                BlockInit.ANTLINE_INDICATOR.get().defaultBlockState().setValue(AntlineOutput.REVERSED, true),
                3);
        test.runAfterDelay(
                5,
                () -> {
                    var portal = PortalManager.getInstance().get(gun, PortalEnd.PRIMARY);
                    test.assertTrue(
                            portal != null && portal.isAlive() && portal.getDirection() == Direction.SOUTH,
                            "Powered automatic emitter opens its linked portal");
                    test.assertTrue(
                            portal.getUUID().equals(emitter.lastOpenedUUID) && portal.getColor().equals("blue"),
                            "Automatic emitter tracks its portal and configured color");
                    for (var part : parts.keySet()) {
                        test.assertTrue(
                                level.getBlockState(part).getValue(AutoPortalBlock.POWERED),
                                "Power state covers every emitter part");
                    }
                    emitter.closePortal();
                    test.runAfterDelay(
                            2,
                            () -> {
                                test.assertTrue(
                                        portal.isRemoved() && emitter.lastOpenedUUID == null,
                                        "Emitter closes its own portal");
                                test.succeed();
                            });
                });
    }

    @GameTest
    public void removingOneAntlineFaceMarksItsChunkForSaving(GameTestHelper test) {
        var pos = test.absolutePos(new BlockPos(3, 3, 3));
        var level = test.getLevel();
        var block = (AntlineBlock) BlockInit.ANTLINE.get();
        level.setBlock(pos.below(), Blocks.STONE.defaultBlockState(), 2);
        level.setBlock(pos.north(), Blocks.STONE.defaultBlockState(), 2);
        level.setBlock(pos, block.defaultBlockState(), 2);
        var entity = (AntlineTileEntity) level.getBlockEntity(pos);
        entity.getSideMap().put(Direction.DOWN, AntlineTileEntity.Side.dot(Direction.DOWN));
        entity.getSideMap().put(Direction.NORTH, AntlineTileEntity.Side.dot(Direction.NORTH));
        var chunk = level.getChunkAt(pos);
        chunk.tryMarkSaved();
        test.assertTrue(!chunk.isUnsaved(), "Fixture starts with a saved chunk");
        block.breakDot(block.defaultBlockState(), level, pos, null, Direction.NORTH);
        test.assertTrue(
                level.getBlockState(pos).is(block) && entity.getSideMap().hasSide(Direction.DOWN),
                "Removing a wall wire preserves the floor wire in the same block");
        test.assertTrue(chunk.isUnsaved(), "Editing wire faces schedules their changed data for saving");
        test.succeed();
    }

    @GameTest
    public void antlineSelectionShapesWorkOnEveryBlockFace(GameTestHelper test) {
        var pos = test.absolutePos(new BlockPos(3, 3, 3));
        var block = (AntlineBlock) BlockInit.ANTLINE.get();
        test.getLevel().setBlock(pos, block.defaultBlockState(), 2);
        var entity = (AntlineTileEntity) test.getLevel().getBlockEntity(pos);
        for (var face : Direction.values()) {
            entity.getSideMap().put(face, AntlineTileEntity.Side.dot(face));
            for (boolean large : new boolean[] {false, true}) {
                var shape = block.getSideShape(test.getLevel(), pos, face, large);
                test.assertTrue(!shape.isEmpty(), "Antline has a selectable shape on " + face);
                var box = shape.bounds();
                test.assertTrue(
                        box.minX >= 0
                                && box.minY >= 0
                                && box.minZ >= 0
                                && box.maxX <= 1
                                && box.maxY <= 1
                                && box.maxZ <= 1,
                        "Antline shape stays within its block on " + face);
                test.assertTrue(
                        Math.abs(box.max(face.getAxis()) - box.min(face.getAxis()) - .125) < .000001,
                        "Antline hugs its supporting face on " + face);
            }
        }
        test.succeed();
    }

    @GameTest(maxTicks = 40)
    public void cubeButtonPowersAntlineAndOpensChamberDoor(GameTestHelper test) {
        var level = test.getLevel();
        for (int x = 1; x <= 7; x++) {
            for (int z = 1; z <= 6; z++) {
                test.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
            }
        }
        var button = (SuperButtonBlock) BlockInit.SUPER_BUTTON.get();
        var buttonPos = test.absolutePos(new BlockPos(2, 2, 2));
        var buttonState = button.defaultBlockState();
        level.setBlock(buttonPos, buttonState, 2);
        button.getOtherParts(buttonState, buttonPos).forEach((pos, state) -> level.setBlock(pos, state, 2));
        var door = (ChamberDoorBlock) BlockInit.CHAMBER_DOOR.get();
        var doorPos = test.absolutePos(new BlockPos(6, 2, 4));
        var doorState = door.defaultBlockState();
        level.setBlock(doorPos, doorState, 2);
        door.getOtherParts(doorState, doorPos).forEach((pos, state) -> level.setBlock(pos, state, 2));
        var indicatorPos = test.absolutePos(new BlockPos(6, 2, 3));
        level.setBlock(indicatorPos, BlockInit.ANTLINE_INDICATOR.get().defaultBlockState(), 2);
        var wire = (AntlineBlock) BlockInit.ANTLINE.get();
        var wires = List.of(test.absolutePos(new BlockPos(4, 2, 3)), test.absolutePos(new BlockPos(5, 2, 3)));
        for (var pos : wires) {
            level.setBlock(pos, wire.defaultBlockState(), 2);
            ((AntlineTileEntity) level.getBlockEntity(pos))
                    .getSideMap()
                    .put(Direction.DOWN, AntlineTileEntity.Side.dot(Direction.DOWN));
            wire.sideUpdate(
                    level,
                    ((AntlineTileEntity) level.getBlockEntity(pos)).getSideMap().get(Direction.DOWN),
                    pos,
                    true,
                    false,
                    null);
        }
        var cube = test.spawn(EntityInit.STORAGE_CUBE.get(), 2, 3, 2);
        cube.setNoGravity(true);
        var trigger = button.getTrigger(buttonState, buttonPos);
        cube.snapTo(trigger.getCenter().x, trigger.minY + .01, trigger.getCenter().z);
        test.runAfterDelay(
                5,
                () -> {
                    for (var pos : wires) {
                        var wireEntity = (AntlineTileEntity) level.getBlockEntity(pos);
                        var side = wireEntity.getSideMap().get(Direction.DOWN);
                        test.assertTrue(side.isActive(), "Cube button powers every wire segment");
                        var restored =
                                (AntlineTileEntity)
                                        BlockEntity.loadStatic(
                                                pos,
                                                wire.defaultBlockState(),
                                                wireEntity.saveWithFullMetadata(level.registryAccess()),
                                                level.registryAccess());
                        test.assertTrue(
                                restored.getSideMap().get(Direction.DOWN).getActualValue() == side.getActualValue(),
                                "Wire connections and power survive native serialization");
                    }
                    test.assertTrue(
                            level.getBlockState(doorPos).getValue(ChamberDoorBlock.OPEN),
                            "Button signal crosses wires and indicator to open the door");
                    // Branch beside the segment adjacent to the indicator: it must not disconnect
                    // the existing powered network or momentarily close its output.
                    var branchSupport = test.absolutePos(new BlockPos(5, 1, 2));
                    var branchPos = branchSupport.above();
                    var player = test.makeMockPlayer(GameType.SURVIVAL);
                    player.setShiftKeyDown(true);
                    var stack = new ItemStack(ItemInit.ANTLINE.get());
                    player.setItemInHand(InteractionHand.MAIN_HAND, stack);
                    var hit =
                            new BlockHitResult(
                                    net.minecraft.world.phys.Vec3.atCenterOf(branchSupport).add(0, .5, 0),
                                    Direction.UP,
                                    branchSupport,
                                    false);
                    var item = (BlockItem) stack.getItem();
                    test.assertTrue(
                            item.place(new BlockPlaceContext(player, InteractionHand.MAIN_HAND, stack, hit))
                                    .consumesAction(),
                            "Shift placement must add a branch");
                    test.assertTrue(
                            ((AntlineTileEntity) level.getBlockEntity(branchPos))
                                    .getSideMap()
                                    .get(Direction.DOWN)
                                    .isActive(),
                            "New branch must inherit the powered network");
                    test.assertTrue(
                            level.getBlockState(doorPos).getValue(ChamberDoorBlock.OPEN),
                            "Adding a branch next to the indicator must keep its output active");
                    level.destroyBlock(branchPos, false);
                    test.assertTrue(
                            level.getBlockState(doorPos).getValue(ChamberDoorBlock.OPEN),
                            "Removing a branch must preserve the original network");
                    cube.snapTo(
                            net.minecraft.world.phys.Vec3.atBottomCenterOf(test.absolutePos(new BlockPos(1, 3, 6))));
                    test.runAfterDelay(
                            12,
                            () -> {
                                for (var pos : wires) {
                                    test.assertTrue(
                                            !((AntlineTileEntity) level.getBlockEntity(pos))
                                                    .getSideMap()
                                                    .get(Direction.DOWN)
                                                    .isActive(),
                                            "Wire power clears when button releases");
                                }
                                test.assertTrue(
                                        !level.getBlockState(doorPos).getValue(ChamberDoorBlock.OPEN),
                                        "Door closes when button releases");
                                test.succeed();
                            });
                });
    }

    @GameTest(maxTicks = 30)
    public void chamberDoorFollowsItsIndicatorAcrossAllFourParts(GameTestHelper test) {
        var block = (ChamberDoorBlock) BlockInit.CHAMBER_DOOR.get();
        var state = block.defaultBlockState();
        var pos = test.absolutePos(new BlockPos(4, 2, 4));
        test.getLevel().setBlock(pos, state, 2);
        block.getOtherParts(state, pos)
                .forEach((partPos, partState) -> test.getLevel().setBlock(partPos, partState, 2));
        var indicatorPos = pos.north();
        test.getLevel().setBlock(indicatorPos.below(), Blocks.STONE.defaultBlockState(), 2);
        var indicator = (AntlineIndicatorBlock) BlockInit.ANTLINE_INDICATOR.get();
        test.getLevel().setBlock(indicatorPos, indicator.defaultBlockState(), 2);
        test.runAfterDelay(
                3,
                () -> {
                    test.assertTrue(
                            !test.getLevel().getBlockState(pos).getValue(ChamberDoorBlock.OPEN),
                            "Inactive indicator keeps the door closed");
                    test.getLevel()
                            .setBlockAndUpdate(
                                    indicatorPos,
                                    test.getLevel().getBlockState(indicatorPos).setValue(AntlineOutput.REVERSED, true));
                    test.runAfterDelay(
                            3,
                            () -> {
                                for (var part : block.getAllPositions(state, pos)) {
                                    test.assertTrue(
                                            test.getLevel().getBlockState(part).getValue(ChamberDoorBlock.OPEN),
                                            "Indicator opens every door part");
                                }
                                test.getLevel()
                                        .setBlockAndUpdate(
                                                indicatorPos,
                                                test.getLevel()
                                                        .getBlockState(indicatorPos)
                                                        .setValue(
                                                                net.portalmod.common.sorted.antline.indicator
                                                                        .AntlineOutput.REVERSED,
                                                                false));
                                test.runAfterDelay(
                                        3,
                                        () -> {
                                            for (var part : block.getAllPositions(state, pos)) {
                                                test.assertTrue(
                                                        !test.getLevel()
                                                                .getBlockState(part)
                                                                .getValue(
                                                                        net.portalmod.common.sorted.door
                                                                                .ChamberDoorBlock.OPEN),
                                                        "Indicator closes every door part");
                                            }
                                            test.succeed();
                                        });
                            });
                });
    }

    @GameTest(maxTicks = 100)
    public void turretAcquiresAndDamagesVisibleTarget(GameTestHelper test) {
        for (int x = 1; x <= 5; x++) {
            for (int z = 1; z <= 7; z++) {
                test.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
            }
        }
        var turret = test.spawn(EntityInit.TURRET.get(), 3, 2, 2);
        turret.setYRot(0);
        turret.setInfiniteAmmo(true);
        var target = test.spawn(EntityTypes.COW, 3, 2, 6);
        target.setNoAi(true);
        float health = target.getHealth();
        test.succeedWhen(
                () -> {
                    test.assertTrue(turret.hasTarget(), "Turret acquires a target in its forward cone");
                    test.assertTrue(target.getHealth() < health, "Turret damages the visible target");
                });
    }

    @GameTest(maxTicks = 60)
    public void cubeDropperReplenishesAndRemovesItsCubes(GameTestHelper test) {
        var block = (CubeDropperBlock) BlockInit.CUBE_DROPPER.get();
        var state = block.defaultBlockState();
        var pos = test.absolutePos(new BlockPos(2, 5, 2));
        test.getLevel().setBlock(pos, state, 2);
        block.getOtherParts(state, pos)
                .forEach((partPos, partState) -> test.getLevel().setBlock(partPos, partState, 2));
        var dropper = (CubeDropperTileEntity) test.getLevel().getBlockEntity(pos);
        dropper.setEntityNBT(EntityInit.STORAGE_CUBE.get());
        test.runAfterDelay(
                3,
                () -> {
                    test.assertTrue(
                            test.getLevel().getBlockState(pos).getValue(CubeDropperBlock.OPEN),
                            "Configured dropper opens");
                    test.assertTrue(dropper.entityUUIDs.size() == 1, "Dropper creates its first cube");
                    test.runAfterDelay(
                            22,
                            () -> {
                                test.assertTrue(
                                        dropper.entityUUIDs.size() == 2, "Dropper keeps a replacement cube ready");
                                var first =
                                        (TestElementEntity) test.getLevel().getEntity(dropper.entityUUIDs.getFirst());
                                test.assertTrue(first.isFromDropper(), "Spawned cube retains dropper origin");
                                var replacement =
                                        (TestElementEntity) test.getLevel().getEntity(dropper.entityUUIDs.getLast());
                                test.getLevel().setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                                test.assertTrue(
                                        first.isFizzling() && replacement.isFizzling(),
                                        "Removing the dropper fizzles its cubes");
                                test.succeed();
                            });
                });
    }

    @GameTest(maxTicks = 30)
    public void configuredTriggerCountsMobsAndClearsItsSignal(GameTestHelper test) {
        var pos = test.absolutePos(new BlockPos(1, 2, 1));
        var state = BlockInit.TRIGGER.get().defaultBlockState().setValue(TriggerBlock.TYPE, TriggerType.MOB);
        test.getLevel().setBlock(pos, state, 2);
        var trigger = (TriggerTileEntity) test.getLevel().getBlockEntity(pos);
        trigger.setField(new BlockPos(1, 0, 1), new BlockPos(3, 2, 3));
        var cube = test.spawn(EntityInit.STORAGE_CUBE.get(), 3, 2, 3);
        cube.setNoGravity(true);
        test.runAfterDelay(
                3,
                () -> {
                    test.assertTrue(
                            trigger.getEntityCount() == 1, "Configured trigger counts the cube inside its field");
                    test.assertTrue(
                            test.getLevel().getBlockState(pos).getSignal(test.getLevel(), pos, Direction.UP) == 1,
                            "Trigger emits one redstone strength per entity");
                    cube.discard();
                    test.runAfterDelay(
                            3,
                            () -> {
                                test.assertTrue(
                                        trigger.getEntityCount() == 0,
                                        "Trigger clears its count after the cube leaves");
                                test.assertTrue(
                                        test.getLevel().getBlockState(pos).getSignal(test.getLevel(), pos, Direction.UP)
                                                == 0,
                                        "Trigger clears its redstone signal");
                                test.succeed();
                            });
                });
    }

    @GameTest
    public void forgePortalEntityDataRestoresGeometryAndIdentity(GameTestHelper test) {
        var gun = UUID.randomUUID();
        var pos = net.minecraft.world.phys.Vec3.atCenterOf(test.absolutePos(new BlockPos(2, 3, 4)));
        var old = new CompoundTag();
        old.store("Pos", net.minecraft.world.phys.Vec3.CODEC, pos);
        old.store("Motion", net.minecraft.world.phys.Vec3.CODEC, net.minecraft.world.phys.Vec3.ZERO);
        old.store("Rotation", Vec2.CODEC, Vec2.ZERO);
        old.putString("end", "primary");
        old.putIntArray("gunUUID", UUIDUtil.uuidToIntArray(gun));
        old.putString("up", "UP");
        old.putString("facing", "EAST");
        old.putString("hue", "cyan");
        var portal = new PortalEntity(test.getLevel());
        portal.load(TagValueInput.create(ProblemReporter.DISCARDING, test.getLevel().registryAccess(), old));
        test.assertTrue(portal.position().equals(pos), "Forge portal position restored");
        test.assertTrue(
                portal.getGunUUID().equals(gun) && portal.getEnd() == PortalEnd.PRIMARY,
                "Forge portal identity restored");
        test.assertTrue(
                portal.getDirection() == Direction.EAST && portal.getUpVector() == Direction.UP,
                "Forge portal orientation restored");
        test.assertTrue(
                portal.getColor().equals("cyan") && portal.getBoundingBox().getYsize() == 2,
                "Forge portal hue and bounds restored");
        test.succeed();
    }

    @GameTest(maxTicks = 60)
    public void repulsionGelBouncesFallingEntity(GameTestHelper test) {
        for (int x = 2; x <= 4; x++) {
            for (int z = 2; z <= 4; z++) {
                test.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
                test.setBlock(
                        new BlockPos(x, 2, z),
                        BlockInit.REPULSION_GEL.get().defaultBlockState().setValue(AbstractGelBlock.DOWN, true));
            }
        }
        var cow = test.spawn(EntityTypes.COW, 3, 6, 3);
        test.succeedWhen(
                () ->
                        test.assertTrue(
                                ((IGelAffected) cow).getBounced() && cow.getDeltaMovement().y > .5,
                                "Repulsion gel bounces falling entity through native tick hooks"));
    }

    @GameTest(maxTicks = 40)
    public void propulsionGelBoostsMovingEntityAndResetsOffGel(GameTestHelper test) {
        for (int x = 2; x <= 4; x++) {
            for (int z = 2; z <= 4; z++) {
                test.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
                test.setBlock(
                        new BlockPos(x, 2, z),
                        BlockInit.PROPULSION_GEL.get().defaultBlockState().setValue(AbstractGelBlock.DOWN, true));
            }
        }
        test.setBlock(new BlockPos(6, 1, 6), Blocks.STONE);
        var cow = test.spawn(EntityTypes.COW, 3, 2, 3);
        cow.setOnGround(true);
        cow.setDeltaMovement(.15, 0, 0);
        var speed = cow.getAttribute(Attributes.MOVEMENT_SPEED);
        test.runAfterDelay(
                3,
                () -> {
                    test.assertTrue(
                            ((IGelAffected) cow).getPropulsionTicks() > 0,
                            "Moving on propulsion gel accumulates boost");
                    test.assertTrue(
                            speed.getValue() > speed.getBaseValue(), "Propulsion gel raises native movement speed");
                    cow.snapTo(net.minecraft.world.phys.Vec3.atBottomCenterOf(test.absolutePos(new BlockPos(6, 2, 6))));
                    cow.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
                    cow.setOnGround(true);
                    test.runAfterDelay(
                            5,
                            () -> {
                                test.assertTrue(
                                        Math.abs(speed.getValue() - speed.getBaseValue()) < .000001,
                                        "Movement speed resets off gel");
                                test.succeed();
                            });
                });
    }

    @GameTest(maxTicks = 40)
    public void gooFluidIsDetectedAndDamagesLivingEntities(GameTestHelper test) {
        test.setBlock(new BlockPos(3, 1, 3), Blocks.STONE);
        test.setBlock(new BlockPos(3, 2, 3), BlockInit.GOO.get());
        test.setBlock(new BlockPos(3, 3, 3), BlockInit.GOO.get());
        var cow = test.spawn(EntityTypes.COW, 3, 2, 3);
        cow.setNoGravity(true);
        test.runAfterDelay(
                5,
                () -> {
                    test.assertTrue(GooBlock.isInGoo(cow), "Native fluid interaction detects goo");
                    test.assertTrue(cow.getHealth() < cow.getMaxHealth(), "Goo damages an immersed entity");
                    test.succeed();
                });
    }

    @GameTest(maxTicks = 40)
    public void opposingFizzlerEmittersCreateFieldAndFizzleCube(GameTestHelper test) {
        var first = new BlockPos(1, 2, 2);
        var second = new BlockPos(5, 2, 2);
        for (var pos : new BlockPos[] {first, second}) {
            var direction = pos == first ? Direction.EAST : Direction.WEST;
            var state =
                    BlockInit.FIZZLER_EMITTER.get().defaultBlockState().setValue(FizzlerEmitterBlock.FACING, direction);
            test.setBlock(pos, state);
            test.setBlock(pos.above(), state.setValue(FizzlerEmitterBlock.HALF, DoubleBlockHalf.UPPER));
        }
        var cube = test.spawn(EntityInit.STORAGE_CUBE.get(), 3, 2, 2);
        cube.setNoGravity(true);
        test.runAfterDelay(
                5,
                () -> {
                    test.assertTrue(
                            test.getBlockState(new BlockPos(3, 2, 2)).is(BlockInit.FIZZLER_FIELD.get()),
                            "Emitters generate lower field");
                    test.assertTrue(!test.getBlockState(new BlockPos(3, 2, 2)).isAir(), "Field is terrain, not air");
                    test.assertTrue(
                            test.getBlockState(new BlockPos(3, 2, 2)).getRenderShape() == RenderShape.MODEL,
                            "Field span renders its model");
                    test.assertTrue(
                            test.getBlockState(new BlockPos(3, 3, 2)).is(BlockInit.FIZZLER_FIELD.get()),
                            "Emitters generate upper field");
                    test.assertTrue(cube.isFizzling(), "Cube touching the field fizzles");
                    test.getLevel().setBlock(test.absolutePos(second), Blocks.AIR.defaultBlockState(), 3);
                    test.runAfterDelay(
                            2,
                            () -> {
                                test.assertTrue(
                                        test.getBlockState(new BlockPos(3, 2, 2)).isAir(),
                                        "Field disappears after emitter removal");
                                test.succeed();
                            });
                });
    }

    @GameTest(maxTicks = 40)
    public void configuredFaithPlateLaunchesCube(GameTestHelper test) {
        var pos = test.absolutePos(new BlockPos(2, 2, 2));
        var state = BlockInit.FAITHPLATE.get().defaultBlockState();
        test.getLevel().setBlock(pos.below(), Blocks.STONE.defaultBlockState(), 2);
        test.getLevel().setBlock(pos.north().below(), Blocks.STONE.defaultBlockState(), 2);
        test.getLevel().setBlock(pos.north(), state.setValue(FaithPlateBlock.HALF, DoubleBlockHalf.LOWER), 2);
        test.getLevel().setBlock(pos, state, 2);
        var plate = (FaithPlateTileEntity) test.getLevel().getBlockEntity(pos);
        var data = new CompoundTag();
        data.putBoolean("enabled", true);
        var target = new CompoundTag();
        target.putDouble("x", 3);
        target.putDouble("y", 0);
        target.putDouble("z", 0);
        target.putByte("side", (byte) Direction.UP.get3DDataValue());
        target.putFloat("height", 4);
        data.put("target", target);
        plate.loadCustomOnly(TagValueInput.create(ProblemReporter.DISCARDING, test.getLevel().registryAccess(), data));
        var trigger = plate.getTrigger();
        var cube = test.spawn(EntityInit.STORAGE_CUBE.get(), 2, 3, 2);
        cube.snapTo(trigger.getCenter().x, trigger.minY + .005, trigger.getCenter().z);
        cube.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        test.runAfterDelay(
                5,
                () -> {
                    test.assertTrue(((Flingable) cube).isFlinging(), "Faith plate marks launched cube as flinging");
                    test.assertTrue(
                            cube.getDeltaMovement().y > .1 && cube.getDeltaMovement().x > 0,
                            "Faith plate launches cube toward configured target");
                    test.succeed();
                });
    }

    @GameTest(maxTicks = 60)
    public void normalButtonReleasesWithCube(GameTestHelper test) {
        checkButtonMode(test, ButtonMode.NORMAL);
    }

    @GameTest(maxTicks = 60)
    public void persistentButtonStaysActiveAfterCubeLeaves(GameTestHelper test) {
        checkButtonMode(test, ButtonMode.PERSISTENT);
    }

    @GameTest(maxTicks = 60)
    public void toggleButtonChangesOnEachCubePress(GameTestHelper test) {
        checkButtonMode(test, ButtonMode.TOGGLE);
    }

    private static void checkButtonMode(GameTestHelper test, ButtonMode mode) {
        var button = (SuperButtonBlock) BlockInit.SUPER_BUTTON.get();
        var pos = test.absolutePos(new BlockPos(2, 2, 2));
        var state = button.defaultBlockState().setValue(SuperButtonBlock.MODE, mode);
        var parts = new HashMap<>(button.getOtherParts(state, pos));
        parts.put(pos, state);
        parts.forEach(
                (part, value) -> {
                    test.getLevel().setBlock(part.below(), Blocks.STONE.defaultBlockState(), 2);
                    test.getLevel().setBlock(part, value, 2);
                });
        var cube = test.spawn(EntityInit.STORAGE_CUBE.get(), 2, 3, 2);
        var trigger = button.getTrigger(state, pos);
        var center = trigger.getCenter();
        cube.setNoGravity(true);
        cube.snapTo(center.x, trigger.minY + .01, center.z);
        test.runAfterDelay(
                3,
                () -> {
                    for (var part : parts.keySet()) {
                        var pressed = test.getLevel().getBlockState(part);
                        test.assertTrue(
                                pressed.getValue(SuperButtonBlock.PRESSED) && pressed.getValue(SuperButtonBlock.ACTIVE),
                                "Cube presses every button corner: " + mode);
                    }
                    cube.snapTo(
                            net.minecraft.world.phys.Vec3.atBottomCenterOf(test.absolutePos(new BlockPos(6, 3, 6))));
                    test.runAfterDelay(
                            12,
                            () -> {
                                var released = test.getLevel().getBlockState(pos);
                                test.assertTrue(
                                        !released.getValue(SuperButtonBlock.PRESSED),
                                        "Button releases after cube leaves");
                                test.assertTrue(
                                        released.getValue(SuperButtonBlock.ACTIVE) == (mode != ButtonMode.NORMAL),
                                        "Button preserves mode behavior: " + mode);
                                if (mode != ButtonMode.TOGGLE) {
                                    test.succeed();
                                    return;
                                }
                                cube.snapTo(center.x, trigger.minY + .01, center.z);
                                test.runAfterDelay(
                                        3,
                                        () -> {
                                            test.assertTrue(
                                                    !test.getLevel()
                                                            .getBlockState(pos)
                                                            .getValue(SuperButtonBlock.ACTIVE),
                                                    "Second cube press toggles button off");
                                            test.succeed();
                                        });
                            });
                });
    }

    @GameTest(maxTicks = 40)
    public void registeredContentTicksAndRoundTripsOnServer(GameTestHelper test) {
        ItemInit.ITEMS
                .getRegistry()
                .forEach(
                        (name, entry) ->
                                test.assertTrue(
                                        !new ItemStack(entry.get()).isEmpty(), "Item components initialized: " + name));
        int index = 0;
        var types = new HashSet<BlockEntityType<?>>();
        for (var entry : BlockInit.BLOCKS.getRegistry().values()) {
            var block = entry.get();
            if (!(block instanceof EntityBlock factory)) {
                continue;
            }
            var state =
                    block.getStateDefinition().getPossibleStates().stream()
                            .filter(candidate -> factory.newBlockEntity(BlockPos.ZERO, candidate) != null)
                            .findFirst()
                            .orElseThrow();
            var pos = test.absolutePos(new BlockPos(1 + index % 4, 2, 1 + index / 4));
            test.getLevel().setBlock(pos, state, 2);
            var entity = test.getLevel().getBlockEntity(pos);
            test.assertTrue(entity != null, "Block entity created for " + block);
            var saved = entity.saveWithFullMetadata(test.getLevel().registryAccess());
            var restored = BlockEntity.loadStatic(pos, state, saved, test.getLevel().registryAccess());
            test.assertTrue(
                    restored != null && restored.getType() == entity.getType(), "Block entity round trip: " + block);
            types.add(entity.getType());
            index++;
        }
        test.assertTrue(
                types.size() == TileEntityTypeInit.TILE_ENTITY_TYPES.getRegistry().size(),
                "Every registered block entity type covered");
        var entities = new ArrayList<Entity>();
        EntityInit.ENTITIES
                .getRegistry()
                .forEach(
                        (name, entry) -> {
                            if (entry.get() == EntityInit.PORTAL.get()) {
                                return;
                            }
                            entities.add(test.spawn(entry.get(), 3, 4, 3));
                        });
        test.runAfterDelay(
                5,
                () -> {
                    entities.forEach(entity -> test.assertTrue(entity.isAlive(), "Entity ticks: " + entity.getType()));
                    test.succeed();
                });
    }

    @GameTest(maxTicks = 40)
    public void portalPairSurvivesServerTicksAndSerializes(GameTestHelper test) {
        test.assertTrue(
                BlockInit.LUNECAST.get().defaultBlockState().is(BlockTagInit.PORTALABLE), "Lunecast portal tag loaded");
        test.assertTrue(
                BlockInit.LUNECAST.get().defaultBlockState().is(BlockTags.MINEABLE_WITH_PICKAXE),
                "Panel blocks use vanilla pickaxe mining tag");
        test.assertTrue(
                Blocks.SHORT_GRASS.defaultBlockState().is(BlockTagInit.PORTAL_NONBLOCKING), "Nonblocking tag loaded");
        test.assertTrue(
                Blocks.IRON_CHAIN.defaultBlockState().is(BlockTagInit.PORTAL_TRANSPARENT), "Transparent tag loaded");
        test.assertTrue(!new ItemStack(ItemInit.PORTALGUN.get()).isEmpty(), "Portal gun components initialized");
        for (int x = 1; x <= 5; x++) {
            for (int y = 1; y <= 4; y++) {
                test.setBlock(new BlockPos(x, y, 1), BlockInit.LUNECAST.get());
                test.setBlock(new BlockPos(x, y, 6), BlockInit.LUNECAST.get());
            }
        }
        var origin = test.absolutePos(BlockPos.ZERO);
        var uuid = UUID.randomUUID();
        var primary =
                PortalPlacer.placePortal(
                        test.getLevel(),
                        PortalEnd.PRIMARY,
                        "blue",
                        uuid,
                        new Vec3(origin.getX() + 3, origin.getY() + 2.5, origin.getZ() + 2),
                        Direction.SOUTH,
                        Direction.UP,
                        false,
                        null,
                        null);
        var secondary =
                PortalPlacer.placePortal(
                        test.getLevel(),
                        PortalEnd.SECONDARY,
                        "orange",
                        uuid,
                        new Vec3(origin.getX() + 3, origin.getY() + 2.5, origin.getZ() + 6),
                        Direction.NORTH,
                        Direction.UP,
                        false,
                        null,
                        null);
        test.assertTrue(primary != null && secondary != null, "Both portals placed on native tagged blocks");
        var cube = test.spawn(EntityInit.STORAGE_CUBE.get(), 3, 2, 4);
        test.runAfterDelay(
                5,
                () -> {
                    test.assertTrue(
                            primary.isAlive() && secondary.isAlive(), "Portal entities survive native server ticks");
                    test.assertTrue(primary.isOpen() && secondary.isOpen(), "Portal pair linked");
                    test.assertTrue(cube.isAlive(), "Cube ticks on dedicated server");
                    cube.setNoGravity(true);
                    cube.snapTo(primary.getX(), primary.getY() - cube.getBbHeight() / 2, primary.getZ() + .25);
                    cube.setDeltaMovement(0, 0, -.5);
                    cube.move(MoverType.SELF, cube.getDeltaMovement());
                    test.assertTrue(
                            cube.getZ() > origin.getZ() + 5, "Cube crosses the portal through native entity movement");
                    var saved =
                            (CompoundTag)
                                    PortalManager.TYPE
                                            .codec()
                                            .encodeStart(NbtOps.INSTANCE, PortalManager.getInstance())
                                            .getOrThrow();
                    var pair = saved.getCompoundOrEmpty(uuid.toString());
                    test.assertTrue(
                            pair.contains("primary") && pair.contains("secondary"), "Both portal ends persisted");
                    test.succeed();
                });
    }
}
