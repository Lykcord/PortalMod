package net.portalmod;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CakeBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.portalmod.common.entities.TestElementEntity;
import net.portalmod.common.sorted.radio.RadioBlock;
import net.portalmod.common.sorted.radio.RadioState;
import net.portalmod.core.init.BlockInit;
import net.portalmod.core.init.EntityInit;
import net.portalmod.core.init.ItemInit;

import java.util.List;
import java.util.Set;

public final class AdvancementInteractionsClientTest implements FabricClientGameTest {
    private static final BlockPos CAKE = new BlockPos(2, 100, 1);
    private static final BlockPos RADIO = new BlockPos(4, 100, 1);
    private static final List<String> ADVANCEMENTS =
            List.of(
                    "root",
                    "place_portals",
                    "portal_surface",
                    "grab_entity",
                    "fizzle_cubes",
                    "triple_teleportation",
                    "shoot_portal_far",
                    "shoot_moon",
                    "radio_transmission",
                    "turret_defense",
                    "survive_turret",
                    "obtain_fall_boots",
                    "faith_plate_elytra",
                    "bounce_on_gel",
                    "eat_forest_cake");

    @Override
    public void runTest(ClientGameTestContext context) {
        try (var world =
                context.worldBuilder()
                        .adjustSettings(
                                settings -> settings.setGameMode(WorldCreationUiState.SelectedGameMode.SURVIVAL))
                        .create()) {
            world.getServer()
                    .runOnServer(
                            server -> {
                                var player = world.getConnection().getServerPlayer();
                                for (var name : ADVANCEMENTS) advancement(player, name);
                                prepareFloor(player.level());
                                player.connection.teleport(2.5, 100, 4, 180, 22);
                                prepareCake(player, ItemStack.EMPTY, 20);
                            });
            world.getConnection().waitForClientboundPackets();
            context.waitFor(client -> client.player.getY() > 99 && !client.player.isCreative());
            var hit = new BlockHitResult(new Vec3(2.5, 100.5, 2), Direction.SOUTH, CAKE, false);
            context.runOnClient(client -> client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND, hit));
            world.getConnection().waitForServerboundPackets();
            world.getServer()
                    .runOnServer(
                            server -> {
                                var player = world.getConnection().getServerPlayer();
                                require(
                                        !done(player, "eat_forest_cake"),
                                        "Clicking with full hunger must not grant the cake advancement");
                                require(
                                        player.level().getBlockState(CAKE).getValue(CakeBlock.BITES) == 0,
                                        "Clicking with full hunger must leave the candle intact");
                            });

            for (var item : List.of(Items.AIR, Items.STONE, Items.CANDLE)) {
                world.getServer()
                        .runOnServer(
                                server ->
                                        prepareCake(
                                                world.getConnection().getServerPlayer(),
                                                item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item),
                                                18));
                world.getConnection().waitForClientboundPackets();
                context.waitFor(
                        client ->
                                client.player.getFoodData().getFoodLevel() == 18
                                        && client.player.getMainHandItem().getItem() == item);
                context.runOnClient(client -> client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND, hit));
                world.getConnection().waitForServerboundPackets();
                world.getServer()
                        .runOnServer(
                                server -> {
                                    var player = world.getConnection().getServerPlayer();
                                    var state = player.level().getBlockState(CAKE);
                                    require(
                                            state.is(BlockInit.FOREST_CAKE.get()),
                                            "Eating while holding "
                                                    + item
                                                    + " must preserve the original Forest Cake block");
                                    require(
                                            state.getValue(CakeBlock.BITES) == 1,
                                            "The first bite must eat the candle: " + item);
                                    require(
                                            done(player, "eat_forest_cake"),
                                            "Eating the candle must award Gullible: " + item);
                                    if (item != Items.AIR)
                                        require(
                                                player.getMainHandItem().getCount() == 1,
                                                "Eating cake must not consume the held item");
                                });
            }

            world.getServer()
                    .runOnServer(
                            server -> {
                                var player = world.getConnection().getServerPlayer();
                                prepareCake(player, ItemStack.EMPTY, 0);
                                int slices = player.getStats().getValue(Stats.CUSTOM.get(Stats.EAT_CAKE_SLICE));
                                for (int bite = 0; bite < 7; bite++) use(player, CAKE);
                                require(
                                        player.level().getBlockState(CAKE).isAir(),
                                        "Seven bites must finish the Forest Cake");
                                require(
                                        player.getStats().getValue(Stats.CUSTOM.get(Stats.EAT_CAKE_SLICE))
                                                == slices + 7,
                                        "Every cake bite must count toward the vanilla slice statistic");
                                checkInventoryAdvancements(player);
                                player.level().setBlockAndUpdate(RADIO, BlockInit.RADIO.get().defaultBlockState());
                            });
            context.waitTicks(3);
            world.getServer()
                    .runOnServer(
                            server -> {
                                var player = world.getConnection().getServerPlayer();
                                use(player, RADIO);
                                require(
                                        player.level().getBlockState(RADIO).getValue(RadioBlock.STATE) == RadioState.ON,
                                        "Overworld interaction must switch the radio on");
                                require(
                                        !done(player, "radio_transmission"),
                                        "An overworld radio must not grant the End transmission");
                            });

            for (var type :
                    List.of(
                            EntityInit.STORAGE_CUBE.get(),
                            EntityInit.COMPANION_CUBE.get(),
                            EntityInit.VINTAGE_CUBE.get())) {
                world.getServer()
                        .runOnServer(
                                server -> {
                                    var player = world.getConnection().getServerPlayer();
                                    var cube =
                                            (TestElementEntity) type.create(player.level(), EntitySpawnReason.COMMAND);
                                    cube.snapTo(2.5, 100, 3);
                                    player.level().addFreshEntity(cube);
                                    require(cube.pickUp(player), "Cube must be held before fizzling");
                                    cube.startFizzling();
                                });
                context.waitTicks(3);
                world.getServer()
                        .runOnServer(
                                server -> {
                                    var player = world.getConnection().getServerPlayer();
                                    require(
                                            done(player, "fizzle_cubes") == (type == EntityInit.VINTAGE_CUBE.get()),
                                            "Reduced to Pixels must require all three weighted cube types");
                                });
            }
            world.getServer()
                    .runOnServer(
                            server -> {
                                var player = world.getConnection().getServerPlayer();
                                require(
                                        done(player, "fizzle_cubes"),
                                        "Fizzling all three held cube types must grant Reduced to Pixels");
                                var end = server.getLevel(Level.END);
                                prepareFloor(end);
                                end.setBlockAndUpdate(
                                        RADIO,
                                        BlockInit.RADIO
                                                .get()
                                                .defaultBlockState()
                                                .setValue(RadioBlock.STATE, RadioState.INACTIVE));
                                player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                                player.teleportTo(end, 4.5, 100, 4, Set.of(), 180, 22, true);
                            });
            context.waitFor(client -> client.level.dimension() == Level.END && client.player.getY() > 99);
            context.waitTicks(5);
            world.getServer()
                    .runOnServer(
                            server -> {
                                var player = world.getConnection().getServerPlayer();
                                use(player, RADIO);
                                require(
                                        player.level().getBlockState(RADIO).getValue(RadioBlock.STATE)
                                                == RadioState.ACTIVE,
                                        "End interaction must start the radio transmission");
                                require(
                                        done(player, "radio_transmission"),
                                        "Starting an End radio must grant What's That Noise");
                            });
        }
    }

    private static void prepareFloor(ServerLevel level) {
        for (int x = 0; x <= 6; x++)
            for (int z = -1; z <= 6; z++) {
                level.setBlock(new BlockPos(x, 99, z), BlockInit.LUNECAST.get().defaultBlockState(), 2);
            }
    }

    private static void prepareCake(ServerPlayer player, ItemStack held, int food) {
        var holder = advancement(player, "eat_forest_cake");
        for (var criterion : holder.value().criteria().keySet()) player.getAdvancements().revoke(holder, criterion);
        player.setItemInHand(InteractionHand.MAIN_HAND, held);
        player.getFoodData().setFoodLevel(food);
        player.level().setBlockAndUpdate(CAKE, BlockInit.FOREST_CAKE.get().defaultBlockState());
    }

    private static void checkInventoryAdvancements(ServerPlayer player) {
        player.getInventory().add(new ItemStack(Items.CRAFTING_TABLE));
        player.getInventory().add(new ItemStack(ItemInit.LONGFALL_BOOTS.get()));
        player.inventoryMenu.broadcastChanges();
        require(done(player, "root"), "Crafting table inventory changes must unlock the PortalMod advancement tab");
        require(
                !done(player, "obtain_fall_boots"),
                "Carrying boots alone must not satisfy the original equipment condition");
        player.setItemSlot(EquipmentSlot.FEET, new ItemStack(ItemInit.LONGFALL_BOOTS.get()));
        player.inventoryMenu.broadcastChanges();
        require(done(player, "obtain_fall_boots"), "Equipping Long Fall Boots must grant New Heights");
    }

    private static void use(ServerPlayer player, BlockPos pos) {
        var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.SOUTH, pos, false);
        player.gameMode.useItemOn(player, player.level(), player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
    }

    private static AdvancementHolder advancement(ServerPlayer player, String name) {
        var advancement =
                player.level()
                        .getServer()
                        .getAdvancements()
                        .get(Identifier.fromNamespaceAndPath("portalmod", "portalmod/" + name));
        require(advancement != null, "Original advancement must load: " + name);
        return advancement;
    }

    private static boolean done(ServerPlayer player, String name) {
        return player.getAdvancements().getOrStartProgress(advancement(player, name)).isDone();
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
