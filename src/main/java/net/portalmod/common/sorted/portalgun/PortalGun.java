package net.portalmod.common.sorted.portalgun;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.portalmod.common.entities.TestElementEntity;
import net.portalmod.common.sorted.fizzler.FizzlerEmitterBlock;
import net.portalmod.common.sorted.fizzler.FizzlerFieldBlock;
import net.portalmod.common.sorted.portal.*;
import net.portalmod.core.init.*;
import net.portalmod.core.math.AABBUtil;
import net.portalmod.core.math.Vec3;
import net.portalmod.core.packet.ClientNetworking;
import net.portalmod.core.util.ClientExecution;
import net.portalmod.core.util.Colour;
import net.portalmod.core.util.ModUtil;

import java.util.*;
import java.util.function.Consumer;

public class PortalGun extends Item {

    public String defaultLeftColor;
    public String defaultRightColor;
    public String defaultAccentColor;

    private static final HashMap<UUID, PortalGun> BY_UUID = new HashMap<>();

    public PortalGun(Properties properties) {
        this(properties, "blue", "orange", "none");
    }

    // Use for custom portal guns
    public PortalGun(
            Properties properties, String defaultLeftColor, String defaultRightColor, String defaultAccentColor) {
        super(properties);
        this.defaultLeftColor = defaultLeftColor;
        this.defaultRightColor = defaultRightColor;
        this.defaultAccentColor = defaultAccentColor;
    }

    // Store holding state on the gun so pickup and drop animations stay in sync.
    public static void updateHolding(ItemStack itemStack, Player player) {
        if (!itemStack.has(DataComponents.CUSTOM_DATA)) {
            return;
        }

        CompoundTag nbt = itemStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();

        boolean isInMainHand = player.getItemInHand(InteractionHand.MAIN_HAND) == itemStack;
        boolean isInOffHand = player.getItemInHand(InteractionHand.OFF_HAND) == itemStack;

        // Only hold one gun at a time
        boolean isInHand = isInMainHand || isInOffHand && !(player.getMainHandItem().getItem() instanceof PortalGun);

        boolean wasHolding = nbt.contains("Holding") && nbt.getBooleanOr("Holding", false);
        boolean isHolding =
                isInHand && player.getPassengers().stream().anyMatch(entity -> entity instanceof TestElementEntity);

        if (isHolding && !wasHolding) {
            pickCube(player, itemStack);
        }

        if (!isHolding && wasHolding) {
            dropCube(player, itemStack);
        }
    }

    public static void setHolding(ItemStack itemStack, boolean holding) {
        CustomData.update(DataComponents.CUSTOM_DATA, itemStack, nbt -> nbt.putBoolean("Holding", holding));
    }

    public static void pickCube(Player player, ItemStack gun) {
        setHolding(gun, true);

        player.level()
                .playSound(
                        player,
                        player,
                        SoundInit.PORTALGUN_LIFT.get(),
                        SoundSource.PLAYERS,
                        1,
                        ModUtil.randomSoundPitch());

        // Play lift animation
        Optional<UUID> uuid = getUUID(gun);
        if (player.level() instanceof ServerLevel && uuid.isPresent()) {
            PacketInit.sendToPlayer(
                    (ServerPlayer) player, new SPortalGunAnimationPacket(uuid.get(), PortalGunAnimation.LIFT));
            return;
        }

        ClientExecution.run(() -> () -> PortalGunGrabSoundClient.handlePacket(player, true));
    }

    public static void dropCube(Player player, ItemStack gun) {
        setHolding(gun, false);

        player.level()
                .playSound(
                        player,
                        player,
                        SoundInit.PORTALGUN_DROP.get(),
                        SoundSource.PLAYERS,
                        1,
                        ModUtil.randomSoundPitch());

        // Play drop animation
        Optional<UUID> uuid = getUUID(gun);
        if (player.level() instanceof ServerLevel && uuid.isPresent()) {
            PacketInit.sendToPlayer(
                    (ServerPlayer) player, new SPortalGunAnimationPacket(uuid.get(), PortalGunAnimation.DROP));
            return;
        }

        ClientExecution.run(() -> () -> PortalGunGrabSoundClient.handlePacket(player, false));
    }

    public static BlockHitResult customClip(Level level, ClipContext context) {
        return BlockGetter.traverseBlocks(
                context.getFrom(),
                context.getTo(),
                context,
                (ctx, pos) -> {
                    BlockState blockState = level.getBlockState(pos);

                    if (blockState.is(BlockTagInit.PORTAL_TRANSPARENT)) {
                        return null;
                    }
                    FluidState fluidState = level.getFluidState(pos);

                    net.minecraft.world.phys.Vec3 rayStart = ctx.getFrom();
                    net.minecraft.world.phys.Vec3 rayEnd = ctx.getTo();
                    VoxelShape blockShape = blockState.getShape(level, pos, CollisionContext.empty());

                    Block block = blockState.getBlock();
                    if (block == BlockInit.FIZZLER_EMITTER.get()) {
                        if (blockState.getValue(FizzlerEmitterBlock.ACTIVE)) {
                            blockShape = ((FizzlerEmitterBlock) block).getFieldShape(blockState);
                        }
                    } else if (block == BlockInit.FIZZLER_FIELD.get()) {
                        blockShape = ((FizzlerFieldBlock) block).getFieldShape(blockState);
                    }

                    BlockHitResult blockHit =
                            level.clipWithInteractionOverride(rayStart, rayEnd, pos, blockShape, blockState);
                    VoxelShape fluidShape = ctx.getFluidShape(fluidState, level, pos);

                    BlockHitResult fluidHit = fluidShape.clip(rayStart, rayEnd, pos);
                    double blockDistanceSquared =
                            blockHit == null ? Double.MAX_VALUE : ctx.getFrom().distanceToSqr(blockHit.getLocation());
                    double fluidDistanceSquared =
                            fluidHit == null ? Double.MAX_VALUE : ctx.getFrom().distanceToSqr(fluidHit.getLocation());
                    return blockDistanceSquared <= fluidDistanceSquared ? blockHit : fluidHit;
                },
                ctx -> {
                    net.minecraft.world.phys.Vec3 rayStart = ctx.getFrom().subtract(ctx.getTo());
                    return BlockHitResult.miss(
                            ctx.getTo(), Direction.getApproximateNearest(rayStart), BlockPos.containing(ctx.getTo()));
                });
    }

    private static void triggerMoonAdvancement(Level level, ServerPlayer player) {
        double angle =
                Math.toRadians(
                        level.environmentAttributes().getValue(EnvironmentAttributes.SUN_ANGLE, player.position())
                                + 90);
        net.minecraft.world.phys.Vec3 moonVector =
                new net.minecraft.world.phys.Vec3(Math.cos(angle), Math.sin(angle), 0);
        boolean lookingUp = player.getLookAngle().dot(new net.minecraft.world.phys.Vec3(0, 1, 0)) > 0;
        boolean lookingAtTheMoon = player.getLookAngle().dot(moonVector) <= -0.997;

        if (lookingUp && lookingAtTheMoon) {
            CriteriaTriggerInit.SHOOT_MOON.get().trigger(player);
        }
    }

    private static void triggerPortalAdvancements(
            Level level, ServerPlayer player, PortalEntity portal, double distance) {
        boolean allQualityBlocks =
                portal.getBlocksBehind().stream()
                        .allMatch(pos -> level.getBlockState(pos).is(BlockTagInit.PORTALABLE_QUALITY));

        CriteriaTriggerInit.PLACE_PORTALS.get().trigger(player);

        if (allQualityBlocks) {
            CriteriaTriggerInit.PORTAL_SURFACE.get().trigger(player);
        }

        if (distance > 100) {
            CriteriaTriggerInit.SHOOT_PORTAL_FAR.get().trigger(player);
        }

        player.awardStat(StatsInit.PORTALS_SHOT);
    }

    public static void placePortal(Player player, Level level, PortalEnd end, ItemStack gun, BlockHitResult ray) {
        if (player.isSpectator() || level.isClientSide()) {
            return;
        }

        Optional<UUID> uuid = getUUID(gun);
        if (!uuid.isPresent()) {
            return;
        }

        CompoundTag nbt = gun.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();

        boolean isPrimary = end == PortalEnd.PRIMARY;

        if (nbt.contains("Locked") && nbt.getStringOr("Locked", "").equals(isPrimary ? "Left" : "Right")) {
            return;
        }

        // Play shooting animation and sound
        PacketInit.sendToPlayer(
                (ServerPlayer) player, new SPortalGunAnimationPacket(uuid.get(), PortalGunAnimation.SHOOT));

        level.playSound(
                null,
                player.position().x,
                player.position().y,
                player.position().z,
                (Objects.equals(end.getSerializedName(), "primary")
                        ? SoundInit.PORTALGUN_FIRE_PRIMARY.get()
                        : SoundInit.PORTALGUN_FIRE_SECONDARY.get()),
                SoundSource.PLAYERS,
                1f,
                ModUtil.randomSoundPitch());

        if (ray.getType() == HitResult.Type.MISS) {
            triggerMoonAdvancement(level, (ServerPlayer) player);
            return;
        }

        Vec3 position = new Vec3(ray.getLocation());
        Direction face = ray.getDirection();
        Direction up = face.getAxis().isHorizontal() ? Direction.UP : player.getDirection();

        String hue;
        if (nbt.contains(isPrimary ? "LeftColor" : "RightColor")) {
            hue = nbt.getStringOr(isPrimary ? "LeftColor" : "RightColor", isPrimary ? "blue" : "orange");
        } else {
            hue = "blue";
        }

        boolean inFizzler =
                AABBUtil.getBlocksWithin(player.getBoundingBox()).stream()
                        .anyMatch(
                                pos -> {
                                    BlockState state = level.getBlockState(pos);
                                    VoxelShape blockShape;

                                    if (state.getBlock() == BlockInit.FIZZLER_EMITTER.get()) {
                                        blockShape =
                                                ((FizzlerEmitterBlock) BlockInit.FIZZLER_EMITTER.get())
                                                        .getFieldShape(state);
                                    } else if (state.getBlock() == BlockInit.FIZZLER_FIELD.get()) {
                                        blockShape =
                                                ((FizzlerFieldBlock) BlockInit.FIZZLER_FIELD.get())
                                                        .getFieldShape(state);
                                    } else {
                                        return false;
                                    }

                                    VoxelShape movedBlockShape = blockShape.move(pos.getX(), pos.getY(), pos.getZ());
                                    VoxelShape entityShape = Shapes.create(player.getBoundingBox());
                                    return Shapes.joinIsNotEmpty(movedBlockShape, entityShape, BooleanOp.AND);
                                });

        PortalEntity portal;
        double distance = ray.getLocation().subtract(player.getEyePosition(0)).length();
        int ticks = (int) Math.ceil(distance / 2);

        PortalEnd finalEnd = end;
        Consumer<PortalEntity> onPlace =
                placedPortal -> {
                    if (placedPortal == null) {
                        level.playSound(
                                null,
                                position.x,
                                position.y,
                                position.z,
                                SoundInit.PORTALGUN_MISS.get(),
                                SoundSource.PLAYERS,
                                1f,
                                ModUtil.randomSlightSoundPitch());
                        PacketInit.sendToDimension(
                                level.dimension(),
                                new SPortalGunFailShotPacket(position, new Vec3(face), new Vec3(up), hue));
                        return;
                    }

                    triggerPortalAdvancements(level, (ServerPlayer) player, placedPortal, distance);

                    // todo convert to string
                    CustomData.update(
                            DataComponents.CUSTOM_DATA,
                            gun,
                            data -> data.putInt("LastPortal", finalEnd == PortalEnd.PRIMARY ? -1 : 1));
                    CustomData.update(
                            DataComponents.CUSTOM_DATA,
                            player.getMainHandItem(),
                            data -> data.putByte("color", (byte) finalEnd.ordinal()));
                };

        if (!inFizzler) {
            // Player shots replace foreign portals only; portals from the same gun still bump.
            boolean overwriteForeign = GameRuleInit.getBoolean(level, GameRuleInit.ALLOW_PORTAL_OVERWRITE);
            if (GameRuleInit.getBoolean(level, GameRuleInit.PORTAL_SLOWSHOT)) {
                PortalManager.getInstance()
                        .schedulePlacement(
                                level,
                                end,
                                hue,
                                uuid.get(),
                                position.clone(),
                                face,
                                up,
                                false,
                                overwriteForeign,
                                Direction.orderedByNearest(player),
                                (ServerPlayer) player,
                                ticks,
                                onPlace);
            } else {
                portal =
                        PortalPlacer.placePortal(
                                level,
                                end,
                                hue,
                                uuid.get(),
                                position.clone(),
                                face,
                                up,
                                false,
                                overwriteForeign,
                                Direction.orderedByNearest(player),
                                (ServerPlayer) player);
                onPlace.accept(portal);
            }
        } else {
            level.playSound(
                    null,
                    position.x,
                    position.y,
                    position.z,
                    SoundInit.PORTALGUN_MISS.get(),
                    SoundSource.PLAYERS,
                    1f,
                    ModUtil.randomSlightSoundPitch());
            PacketInit.sendToDimension(
                    level.dimension(), new SPortalGunFailShotPacket(position, new Vec3(face), new Vec3(up), hue));
        }
    }

    @Override
    public boolean allowComponentsUpdateAnimation(
            Player player, InteractionHand hand, ItemStack oldStack, ItemStack newStack) {
        Optional<UUID> newUUID = getUUID(newStack);
        Optional<UUID> oldUUID = getUUID(oldStack);
        return oldUUID.isPresent() && newUUID.isPresent() && !oldUUID.equals(newUUID);
    }

    public static int getColorOverride(ItemStack itemStack, Level level, LivingEntity entity) {
        CompoundTag nbt = itemStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!nbt.contains("color")) {
            return 0;
        }

        byte color = nbt.getByteOr("color", (byte) 0);
        return color == 1 || color == 2 ? 1 : 0;
    }

    @Override
    public void inventoryTick(ItemStack itemStack, ServerLevel level, Entity entity, EquipmentSlot slot) {
        super.inventoryTick(itemStack, level, entity, slot);

        if (entity instanceof Player) {
            updateHolding(itemStack, (Player) entity);
        }

        addUUID(itemStack);

        Optional<UUID> uuid = getUUID(itemStack);
        boolean hasBlue = uuid.isPresent() && PortalManager.getInstance().has(uuid.get(), PortalEnd.PRIMARY);
        boolean hasOrange = uuid.isPresent() && PortalManager.getInstance().has(uuid.get(), PortalEnd.SECONDARY);
        CustomData.update(
                DataComponents.CUSTOM_DATA,
                itemStack,
                nbt -> {
                    nbt.putBoolean("primary", hasBlue);
                    nbt.putBoolean("secondary", hasOrange);
                    this.addDefaultNbt(nbt);
                });
    }

    public void addDefaultNbt(CompoundTag nbt) {
        addDefaultNbt(nbt, this.defaultLeftColor, this.defaultRightColor, this.defaultAccentColor);
    }

    public static void addDefaultNbt(CompoundTag nbt, String leftColor, String rightColor, String accentColor) {
        if (!nbt.contains("LeftColor")) {
            nbt.putString("LeftColor", leftColor);
        }
        if (!nbt.contains("RightColor")) {
            nbt.putString("RightColor", rightColor);
        }
        if (!nbt.contains("LastPortal")) {
            nbt.putInt("LastPortal", 0);
        }
        if (!nbt.contains("AccentColor")) {
            nbt.putString("AccentColor", accentColor);
        }
        if (!nbt.contains("Locked")) {
            nbt.putString("Locked", "None");
        }
    }

    public static ItemStack modifyColors(ItemStack itemStack, String leftColor, String rightColor, String accentColor) {
        CustomData.update(
                DataComponents.CUSTOM_DATA, itemStack, nbt -> addDefaultNbt(nbt, leftColor, rightColor, accentColor));
        return itemStack;
    }

    public static void addUUID(ItemStack itemStack) {
        CompoundTag nbt = itemStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (nbt.read("gunUUID", UUIDUtil.CODEC).isEmpty()) {
            CustomData.update(
                    DataComponents.CUSTOM_DATA,
                    itemStack,
                    data -> data.store("gunUUID", UUIDUtil.CODEC, UUID.randomUUID()));
        }
    }

    public static void removeUUID(ItemStack itemStack) {
        CustomData.update(DataComponents.CUSTOM_DATA, itemStack, nbt -> nbt.remove("gunUUID"));
    }

    public static Optional<UUID> getUUID(ItemStack itemStack) {
        if (!(itemStack.getItem() instanceof PortalGun gun)) {
            return Optional.empty();
        }
        CompoundTag nbt = itemStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        Optional<UUID> uuid = nbt.read("gunUUID", UUIDUtil.CODEC);
        uuid.ifPresent(value -> BY_UUID.put(value, gun));
        return uuid;
    }

    public static void onDuplicate(ItemStack itemStack) {
        removeUUID(itemStack);
        addUUID(itemStack);
    }

    public static Colour getLeftColour(CompoundTag nbt) {
        return PortalColors.getColour(getLeftDyeColour(nbt).getName());
    }

    public static DyeColor getLeftDyeColour(CompoundTag nbt) {
        DyeColor color = DyeColor.BLUE; // Fallback

        if (nbt.getStringOr("Locked", "").equals("Left")) {
            return getRightDyeColour(nbt);
        }

        if (nbt.contains("LeftColor")) {
            try {
                color = DyeColor.byName(nbt.getStringOr("LeftColor", ""), color);
            } catch (NullPointerException ignored) {
            }
        }

        return color;
    }

    public static Colour getRightColour(CompoundTag nbt) {
        return PortalColors.getColour(getRightDyeColour(nbt).getName());
    }

    public static DyeColor getRightDyeColour(CompoundTag nbt) {
        DyeColor color = DyeColor.ORANGE; // Fallback

        if (nbt.getStringOr("Locked", "").equals("Right")) {
            return getLeftDyeColour(nbt);
        }

        if (nbt.contains("RightColor")) {
            try {
                color = DyeColor.byName(nbt.getStringOr("RightColor", ""), color);
            } catch (NullPointerException ignored) {
            }
        }

        return color;
    }

    public static PortalGunModel getModel(UUID gunUUID) {
        return PortalGunModelManager.getInstance().getModel(gunUUID);
    }

    public static Colour getAccentColour(CompoundTag nbt) {
        Colour colour = new Colour(1f, 1, 1, 0);
        if (nbt.contains("AccentColor")) {
            String accentColor = nbt.getStringOr("AccentColor", "");
            if (!accentColor.equals("none")) {
                colour = new Colour(PortalColors.getColor(accentColor).getRGB());
            }
        }
        return colour;
    }

    public static void fizzleGunsInInventory(Player player) {
        if (player.level().isClientSide()) {
            // The local client catches fast fizzler crossings using its swept movement path.
            // The server handler only fizzles the sending player's guns.
            ClientNetworking.sendToServer(new CPortalGunInteractionPacket.Builder(PortalGunInteraction.FIZZLE).build());
            return;
        }

        boolean didFizzleAny = false;

        ArrayList<ItemStack> test = new ArrayList<>(player.getInventory().getNonEquipmentItems());
        test.add(player.getOffhandItem());

        for (ItemStack itemStack : test) {
            if (itemStack.getItem() instanceof PortalGun) {
                didFizzleAny = fizzleGunItem(itemStack) || didFizzleAny;
            }
        }

        if (didFizzleAny) {
            PacketInit.sendToPlayer(
                    (ServerPlayer) player, new SPortalGunAnimationPacket(UUID.randomUUID(), PortalGunAnimation.FIZZLE));
            player.level()
                    .playSound(
                            null,
                            player.position().x,
                            player.position().y,
                            player.position().z,
                            SoundInit.PORTALGUN_FIZZLE.get(),
                            SoundSource.PLAYERS,
                            1f,
                            ModUtil.randomSlightSoundPitch());
        }
    }

    /**
     * Fizzles one portalgun item.
     *
     * @return whether any portals got fizzled.
     */
    public static boolean fizzleGunItem(ItemStack itemStack) {
        Optional<UUID> gunUUID = PortalGun.getUUID(itemStack);
        if (!gunUUID.isPresent()) {
            return false;
        }

        PortalManager.getInstance().clearScheduledPlacements(gunUUID.get());

        PortalPair pair = PortalManager.getInstance().getPair(gunUUID.get());
        if (pair == null) {
            return false;
        }
        boolean fizzledAPortal = false;

        String lock =
                itemStack
                        .getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                        .copyTag()
                        .getStringOr("Locked", "");

        if (pair.has(PortalEnd.PRIMARY) && !lock.equals("Left")) {
            PortalEntity primary = pair.get(PortalEnd.PRIMARY);
            PortalManager.getInstance().scheduleRemoval(primary);
            fizzledAPortal = true;
        }
        if (pair.has(PortalEnd.SECONDARY) && !lock.equals("Right")) {
            PortalEntity secondary = pair.get(PortalEnd.SECONDARY);
            PortalManager.getInstance().scheduleRemoval(secondary);
            fizzledAPortal = true;
        }

        CustomData.update(DataComponents.CUSTOM_DATA, itemStack, nbt -> nbt.putInt("LastPortal", 0));
        return fizzledAPortal;
    }

    @Override
    public void appendHoverText(
            ItemStack itemStack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> tooltip,
            TooltipFlag flag) {
        CompoundTag nbt = itemStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        String lock = nbt.contains("Locked") ? nbt.getStringOr("Locked", "") : "None";

        String leftColorName = lock.equals("Left") ? "locked" : getLeftDyeColour(nbt).toString();
        String rightColorName = lock.equals("Right") ? "locked" : getRightDyeColour(nbt).toString();

        int leftColor =
                lock.equals("Left")
                        ? DyeColor.LIGHT_GRAY.getTextureDiffuseColor()
                        : PortalColors.getColor(leftColorName).getRGB();
        int rightColor =
                lock.equals("Right")
                        ? DyeColor.LIGHT_GRAY.getTextureDiffuseColor()
                        : PortalColors.getColor(rightColorName).getRGB();

        tooltip.accept(ModUtil.tooltipComponent("tooltip.portalmod.portalgun.colors"));
        tooltip.accept(
                Component.translatable("tooltip.portalmod.colors." + leftColorName)
                        .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(leftColor)))
                        .append("§7 & ")
                        .append(
                                Component.translatable("tooltip.portalmod.colors." + rightColorName)
                                        .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(rightColor)))));
        if (Minecraft.getInstance().hasControlDown()) {
            tooltip.accept(Component.empty());
        }

        ModUtil.addTooltip("portalgun", tooltip);
    }

    @Override
    public Component getName(ItemStack item) {
        CompoundTag nbt = item.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        Style colorStyle = Style.EMPTY.withColor(TextColor.fromRgb(getAccentColour(nbt).getRGBValue()));

        return super.getName(item).copy().setStyle(colorStyle);
    }
}
