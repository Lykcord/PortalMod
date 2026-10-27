package net.portalmod.core.init;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.portalmod.PortalMod;
import net.portalmod.common.sorted.cube.Cube;
import net.portalmod.common.sorted.portal.PortalEntity;
import net.portalmod.common.sorted.sign.ChamberSignEntity;
import net.portalmod.common.sorted.turret.TurretEntity;
import net.portalmod.core.util.Registry;
import net.portalmod.core.util.Registry.Entry;

public class EntityInit {
    public static final Registry<EntityType<?>> ENTITIES =
            new Registry<>(
                    (id, value) ->
                            net.minecraft.core.Registry.register(
                                    BuiltInRegistries.ENTITY_TYPE,
                                    Identifier.fromNamespaceAndPath(PortalMod.MODID, id),
                                    value));

    private EntityInit() {}

    public static final Entry<EntityType<Cube>> GABE =
            ENTITIES.register(
                    "gabe",
                    () -> EntityType.Builder.of(Cube::new, MobCategory.AMBIENT).sized(.8f, .8f).build(key("gabe")));

    public static final Entry<EntityType<Cube>> COMPANION_CUBE =
            ENTITIES.register(
                    "companion_cube",
                    () ->
                            EntityType.Builder.of(Cube::new, MobCategory.AMBIENT)
                                    .sized(.8f, .8f)
                                    .build(key("companion_cube")));

    public static final Entry<EntityType<Cube>> STORAGE_CUBE =
            ENTITIES.register(
                    "storage_cube",
                    () ->
                            EntityType.Builder.of(Cube::new, MobCategory.AMBIENT)
                                    .sized(.8f, .8f)
                                    .build(key("storage_cube")));

    public static final Entry<EntityType<Cube>> VINTAGE_CUBE =
            ENTITIES.register(
                    "vintage_cube",
                    () ->
                            EntityType.Builder.of(Cube::new, MobCategory.AMBIENT)
                                    .sized(.8f, .8f)
                                    .build(key("vintage_cube")));

    public static final Entry<EntityType<TurretEntity>> TURRET =
            ENTITIES.register(
                    "turret",
                    () ->
                            EntityType.Builder.<TurretEntity>of(TurretEntity::new, MobCategory.CREATURE)
                                    .build(key("turret")));

    public static final Entry<EntityType<ChamberSignEntity>> CHAMBER_SIGN =
            ENTITIES.register(
                    "chamber_sign",
                    () ->
                            EntityType.Builder.<ChamberSignEntity>of(ChamberSignEntity::new, MobCategory.MISC)
                                    .sized(.5f, .5f)
                                    .updateInterval(Integer.MAX_VALUE)
                                    .build(key("chamber_sign")));

    //            () -> EntityType.Builder.<PelletEntity>of(PelletEntity::new, MobCategory.MISC)
    //                    .sized(.4f, .4f)

    //            () -> EntityType.Builder.of(CreeperEntity::new, MobCategory.MONSTER)
    //                    .sized(0.6F, 1.7F)
    //                    .clientTrackingRange(8)

    public static final Entry<EntityType<PortalEntity>> PORTAL =
            ENTITIES.register(
                    "portal",
                    () ->
                            EntityType.Builder.<PortalEntity>of(PortalEntity::new, MobCategory.MISC)
                                    .clientTrackingRange(10)
                                    .updateInterval(Integer.MAX_VALUE)
                                    .build(key("portal")));

    private static ResourceKey<EntityType<?>> key(String name) {
        return ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(PortalMod.MODID, name));
    }
}
