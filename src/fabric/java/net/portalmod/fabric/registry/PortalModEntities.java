package net.portalmod.fabric.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.portalmod.fabric.PortalModFabric;
import net.portalmod.fabric.entity.PortalEntity;

public final class PortalModEntities {
    public static final EntityType<PortalEntity> PORTAL = register(
            "portal",
            EntityType.Builder.<PortalEntity>of(PortalEntity::new, MobCategory.MISC)
                    .sized(1.0F, 2.0F)
                    .clientTrackingRange(10)
                    .updateInterval(20)
                    .noLootTable()
    );

    private PortalModEntities() {
    }

    public static void register() {
        PortalModFabric.LOGGER.info("Registered PortalMod Fabric entity types.");
    }

    private static <T extends net.minecraft.world.entity.Entity> EntityType<T> register(String name, EntityType.Builder<T> builder) {
        Identifier id = Identifier.fromNamespaceAndPath(PortalModFabric.MOD_ID, name);
        ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, id);
        EntityType<T> type = builder.build(key);
        return Registry.register(BuiltInRegistries.ENTITY_TYPE, id, type);
    }
}
