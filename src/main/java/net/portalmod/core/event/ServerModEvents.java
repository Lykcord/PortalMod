package net.portalmod.core.event;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.portalmod.common.sorted.cube.Cube;
import net.portalmod.common.sorted.turret.TurretEntity;
import net.portalmod.core.init.EntityInit;

public class ServerModEvents {
    public static void registerAttributes() {
        FabricDefaultAttributeRegistry.register(EntityInit.COMPANION_CUBE.get(), Cube.createAttributes().build());
        FabricDefaultAttributeRegistry.register(EntityInit.STORAGE_CUBE.get(), Cube.createAttributes().build());
        FabricDefaultAttributeRegistry.register(EntityInit.VINTAGE_CUBE.get(), Cube.createAttributes().build());
        FabricDefaultAttributeRegistry.register(EntityInit.GABE.get(), Cube.createAttributes().build());
        FabricDefaultAttributeRegistry.register(EntityInit.TURRET.get(), TurretEntity.createAttributes().build());
    }
}
