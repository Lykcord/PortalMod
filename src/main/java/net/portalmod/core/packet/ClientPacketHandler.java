package net.portalmod.core.packet;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.portalmod.common.sorted.portal.PortalEntity;
import net.portalmod.common.sorted.portal.PortalPhotonParticle;
import net.portalmod.common.sorted.portal.SPortalShotPacket;
import net.portalmod.common.sorted.portalgun.SPortalGunFailShotPacket;
import net.portalmod.common.sorted.trigger.STriggerStartConfigPacket;
import net.portalmod.common.sorted.trigger.TriggerSelectionClient;
import net.portalmod.common.sorted.trigger.TriggerTileEntity;

public class ClientPacketHandler {
    public static void handleSPortalShotPacket(SPortalShotPacket packet) {
        Level level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }

        PortalEntity portal = (PortalEntity) level.getEntity(packet.id);
        if (portal != null) {
            PortalPhotonParticle.createOpeningParticles(portal);
        }
    }

    public static void handleSPortalGunFailShotPacket(SPortalGunFailShotPacket packet) {
        PortalPhotonParticle.createFailParticles(
                Minecraft.getInstance().level, packet.position, packet.normal, packet.upVector, packet.dyeColor);
    }

    public static void handleSTriggerStartConfigPacket(STriggerStartConfigPacket packet) {
        Level level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }

        BlockEntity be = level.getBlockEntity(packet.pos);
        if (be instanceof TriggerTileEntity) {
            TriggerSelectionClient.startSelecting(((TriggerTileEntity) be));
        }
    }
}
