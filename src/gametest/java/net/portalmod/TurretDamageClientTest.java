package net.portalmod;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.portalmod.common.sorted.turret.TurretEntity;
import net.portalmod.core.init.BlockInit;
import net.portalmod.core.init.EntityInit;

/** Exercises real player damage, including the review-world settings that deliberately prevent it. */
public final class TurretDamageClientTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        try (var world =
                context.worldBuilder()
                        .adjustSettings(
                                settings -> {
                                    settings.setGameMode(WorldCreationUiState.SelectedGameMode.SURVIVAL);
                                    settings.setDifficulty(Difficulty.NORMAL);
                                })
                        .create()) {
            int id =
                    world.getServer()
                            .computeOnServer(
                                    server -> {
                                        var player = world.getConnection().getServerPlayer();
                                        var level = player.level();
                                        for (int x = -2; x <= 6; x++) {
                                            for (int z = -2; z <= 10; z++) {
                                                level.setBlock(
                                                        new BlockPos(x, 99, z),
                                                        BlockInit.LUNECAST.get().defaultBlockState(),
                                                        2);
                                            }
                                        }
                                        for (var slot : EquipmentSlot.values()) {
                                            if (slot.isArmor()) {
                                                player.setItemSlot(slot, ItemStack.EMPTY);
                                            }
                                        }
                                        player.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
                                        player.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
                                        var turret = EntityInit.TURRET.get().create(level, EntitySpawnReason.COMMAND);
                                        turret.snapTo(2.5, 100, 2.5, 0, 0);
                                        turret.setInfiniteAmmo(true);
                                        level.addFreshEntity(turret);
                                        player.connection.teleport(2.5, 100, 6.5, 180, 0);
                                        return turret.getId();
                                    });
            world.getConnection().waitForClientboundPackets();
            context.waitFor(client -> client.player.getY() > 99 && !client.player.isCreative());
            context.waitFor(client -> client.player.getHealth() < 20, 150);
            world.getServer()
                    .runOnServer(
                            server -> {
                                var player = world.getConnection().getServerPlayer();
                                if (player.getHealth() >= 20) {
                                    throw new AssertionError(
                                            "Loaded turret must hurt an unarmored Normal-difficulty Survival player");
                                }
                                var turret = (TurretEntity) player.level().getEntity(id);
                                turret.setInfiniteAmmo(false);
                                turret.setAmmo(0);
                                player.setHealth(20);
                            });
            context.waitTicks(25);
            world.getServer()
                    .runOnServer(
                            server -> {
                                var player = world.getConnection().getServerPlayer();
                                if (player.getHealth() != 20) {
                                    throw new AssertionError("Empty turret must not damage its target");
                                }
                                ((TurretEntity) player.level().getEntity(id)).setAmmo(TurretEntity.AMMO_PER_BULLET);
                            });
            context.waitFor(client -> client.player.getHealth() < 20, 100);
            world.getServer()
                    .runOnServer(
                            server -> {
                                var player = world.getConnection().getServerPlayer();
                                server.setDifficulty(Difficulty.PEACEFUL, true);
                                player.setHealth(20);
                                ((TurretEntity) player.level().getEntity(id)).setInfiniteAmmo(true);
                            });
            context.waitTicks(25);
            world.getServer()
                    .runOnServer(
                            server -> {
                                if (world.getConnection().getServerPlayer().getHealth() != 20) {
                                    throw new AssertionError(
                                            "Peaceful player-damage scaling must match vanilla and the original mod");
                                }
                            });
        }
    }
}
