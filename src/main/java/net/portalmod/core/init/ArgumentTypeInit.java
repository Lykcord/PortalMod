package net.portalmod.core.init;

import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.portalmod.PortalMod;
import net.portalmod.common.commands.LowercaseEnumArgument;
import net.portalmod.common.commands.PortalCommand;
import net.portalmod.common.sorted.portal.PortalColors;
import net.portalmod.common.sorted.portal.PortalEnd;

public class ArgumentTypeInit {
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void registerAll() {
        ArgumentTypeRegistry.registerArgumentType(
                Identifier.fromNamespaceAndPath(PortalMod.MODID, "lowercase_enum"),
                (Class) LowercaseEnumArgument.class,
                new LowercaseEnumArgument.Serializer(
                        PortalEnd.Safe.class,
                        PortalColors.class,
                        AttachFace.class,
                        PortalCommand.HorizontalDirection.class));
    }
}
