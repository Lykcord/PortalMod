package net.portalmod.fabric.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.portalmod.fabric.PortalModFabric;

public final class PortalModTags {
    public static final TagKey<Block> PORTALABLE = blockTag("portal/portalable");
    public static final TagKey<Block> UNPORTALABLE = blockTag("portal/unportalable");
    public static final TagKey<Block> PORTAL_TRANSPARENT = blockTag("portal/portal_transparent");
    public static final TagKey<Block> PORTAL_NONBLOCKING = blockTag("portal/portal_nonblocking");
    public static final TagKey<Block> PORTAL_INHERITING = blockTag("portal/portal_inheriting");
    public static final TagKey<Block> PORTALABLE_QUALITY = blockTag("portal/portalable_quality");

    private PortalModTags() {
    }

    private static TagKey<Block> blockTag(String path) {
        return TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(PortalModFabric.MOD_ID, path));
    }
}
