package net.portalmod.fabric.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionCheck;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.phys.Vec3;
import net.portalmod.fabric.PortalModFabric;
import net.portalmod.fabric.entity.PortalEntity;
import net.portalmod.fabric.portal.PortalColors;
import net.portalmod.fabric.portal.PortalManager;
import net.portalmod.fabric.portal.PortalPairRecord;
import net.portalmod.fabric.portal.PortalPlacementService;
import net.portalmod.fabric.portal.PortalRecord;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * /portal open|close command, matching the Forge mod's admin tooling.
 */
public final class PortalCommand {
    private PortalCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("portal")
                .requires(Commands.hasPermission(new PermissionCheck.Require(Permissions.COMMANDS_GAMEMASTER)))
                .then(Commands.literal("open")
                        .then(Commands.argument("uuid", UuidArgument.uuid()).suggests(PortalCommand::suggestUuids)
                        .then(Commands.argument("end", StringArgumentType.word()).suggests(PortalCommand::suggestEnds)
                        .then(Commands.argument("color", StringArgumentType.word()).suggests(PortalCommand::suggestColors)
                        .then(Commands.argument("pos", Vec3Argument.vec3())
                        .then(Commands.argument("face", StringArgumentType.word()).suggests(PortalCommand::suggestDirections)
                                .executes(context -> openPortal(context, null))
                                .then(Commands.argument("up", StringArgumentType.word()).suggests(PortalCommand::suggestDirections)
                                        .executes(context -> openPortal(context, StringArgumentType.getString(context, "up"))))))))))
                .then(Commands.literal("close")
                        .then(Commands.literal("all")
                                .executes(context -> closeAllPortals(context.getSource())))
                        .then(Commands.argument("uuid", UuidArgument.uuid()).suggests(PortalCommand::suggestUuids)
                                .executes(context -> closePortal(context.getSource(), UuidArgument.getUuid(context, "uuid"), null))
                                .then(Commands.argument("end", StringArgumentType.word()).suggests(PortalCommand::suggestEnds)
                                        .executes(context -> closePortal(
                                                context.getSource(),
                                                UuidArgument.getUuid(context, "uuid"),
                                                parseEnd(StringArgumentType.getString(context, "end"))))))));
    }

    private static int openPortal(CommandContext<CommandSourceStack> context, String upName) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        UUID gunId = UuidArgument.getUuid(context, "uuid");
        boolean primary = parseEnd(StringArgumentType.getString(context, "end"));
        String color = StringArgumentType.getString(context, "color").toLowerCase();
        Vec3 position = Vec3Argument.getVec3(context, "pos");
        Direction face = parseDirection(StringArgumentType.getString(context, "face"));

        if (!PortalColors.exists(color)) {
            throw new SimpleCommandExceptionType(text("open.failed")).create();
        }

        Direction up;
        if (upName != null) {
            up = parseDirection(upName);
            if (up.getAxis() == face.getAxis()) {
                throw new SimpleCommandExceptionType(text("open.failed")).create();
            }
        } else {
            up = face.getAxis() == Direction.Axis.Y ? Direction.NORTH : Direction.UP;
        }

        ServerLevel level = source.getLevel();
        PortalEntity portal = PortalPlacementService.forcePlace(level, gunId, primary, color, position, face, up);
        if (portal == null) {
            throw new SimpleCommandExceptionType(text("open.failed")).create();
        }

        source.sendSuccess(() -> text("open.success"), true);
        return 1;
    }

    private static int closeAllPortals(CommandSourceStack source) throws CommandSyntaxException {
        MinecraftServer server = source.getServer();
        PortalManager manager = PortalManager.get(server);
        Map<UUID, PortalPairRecord> pairs = new HashMap<>(manager.pairs());

        if (pairs.isEmpty()) {
            throw new SimpleCommandExceptionType(text("close.double.failed.null")).create();
        }

        for (UUID gunId : pairs.keySet()) {
            closeEnd(server, manager, gunId, true);
            closeEnd(server, manager, gunId, false);
        }

        source.sendSuccess(() -> text("close.double.success"), true);
        return 1;
    }

    private static int closePortal(CommandSourceStack source, UUID gunId, Boolean end) throws CommandSyntaxException {
        MinecraftServer server = source.getServer();
        PortalManager manager = PortalManager.get(server);
        PortalPairRecord pair = manager.pair(gunId);

        if (pair.isEmpty()) {
            throw new SimpleCommandExceptionType(text("close." + (end == null ? "double" : "single") + ".failed.null")).create();
        }

        if (end == null) {
            closeEnd(server, manager, gunId, true);
            closeEnd(server, manager, gunId, false);
            source.sendSuccess(() -> text("close.double.success"), true);
        } else {
            if (pair.end(end).isEmpty()) {
                throw new SimpleCommandExceptionType(text("close.single.failed.null")).create();
            }
            closeEnd(server, manager, gunId, end);
            source.sendSuccess(() -> text("close.single.success"), true);
        }

        return 1;
    }

    private static void closeEnd(MinecraftServer server, PortalManager manager, UUID gunId, boolean primary) {
        PortalRecord record = manager.end(gunId, primary).orElse(null);
        if (record == null) {
            return;
        }

        PortalEntity portal = manager.resolve(server, record);
        if (portal != null) {
            portal.discard();
        } else {
            manager.revoke(server, gunId, primary);
        }
    }

    private static boolean parseEnd(String name) throws CommandSyntaxException {
        return switch (name.toLowerCase()) {
            case "primary" -> true;
            case "secondary" -> false;
            default -> throw new SimpleCommandExceptionType(Component.literal("Unknown portal end: " + name)).create();
        };
    }

    private static Direction parseDirection(String name) throws CommandSyntaxException {
        Direction direction = Direction.byName(name.toLowerCase());
        if (direction == null) {
            throw new SimpleCommandExceptionType(Component.literal("Unknown direction: " + name)).create();
        }
        return direction;
    }

    private static CompletableFuture<Suggestions> suggestUuids(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        List<UUID> uuids = new ArrayList<>();
        uuids.add(new UUID(0L, 0L));
        uuids.addAll(PortalManager.get(context.getSource().getServer()).pairs().keySet());
        return SharedSuggestionProvider.suggest(uuids.stream().map(UUID::toString), builder);
    }

    private static CompletableFuture<Suggestions> suggestEnds(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(new String[]{"primary", "secondary"}, builder);
    }

    private static CompletableFuture<Suggestions> suggestColors(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(PortalColors.names(), builder);
    }

    private static CompletableFuture<Suggestions> suggestDirections(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(
                java.util.Arrays.stream(Direction.values()).map(Direction::getSerializedName),
                builder
        );
    }

    private static Component text(String key) {
        return Component.translatable("commands." + PortalModFabric.MOD_ID + ".portal." + key);
    }
}
