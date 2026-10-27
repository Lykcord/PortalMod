package net.portalmod;

import net.fabricmc.api.ModInitializer;
import net.portalmod.common.items.ModSpawnEggItem;
import net.portalmod.core.PortalModTab;
import net.portalmod.core.event.CommonEvents;
import net.portalmod.core.event.ServerModEvents;
import net.portalmod.core.init.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PortalMod implements ModInitializer {
    public static final Logger LOGGER = LogManager.getLogger();
    public static final String MODID = "portalmod";
    public static final boolean DEBUG = false;
    public static final boolean WATERMARK = false;

    @Override
    public void onInitialize() {
        SoundInit.SOUNDS.registerAll();
        AttributeInit.ATTRIBUTES.registerAll();
        EntityInit.ENTITIES.registerAll();
        FluidInit.FLUIDS.registerAll();
        BlockInit.BLOCKS.registerAll();
        ItemInit.ITEMS.registerAll();
        TileEntityTypeInit.TILE_ENTITY_TYPES.registerAll();
        ParticleInit.PARTICLE_TYPES.registerAll();
        RecipeInit.RECIPES.registerAll();
        CriteriaTriggerInit.REGISTRY.registerAll();
        GameRuleInit.registerAll();
        ArgumentTypeInit.registerAll();
        PacketInit.init();
        StatsInit.init();
        ItemTagInit.init();
        BlockTagInit.init();
        EntityTagInit.init();
        FluidTagInit.init();
        ItemInit.registerFluidDispenserBehavior();
        ModSpawnEggItem.register();
        PortalModTab.register();
        ServerModEvents.registerAttributes();
        CommonEvents.init();
    }
}
