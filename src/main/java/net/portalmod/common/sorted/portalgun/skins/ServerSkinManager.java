package net.portalmod.common.sorted.portalgun.skins;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.level.ServerPlayer;
import net.portalmod.PortalMod;
import net.portalmod.core.init.PacketInit;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ServerSkinManager extends SkinManager {
    private static ServerSkinManager instance;

    private ServerSkinManager() {
        super(false);
    }

    public static ServerSkinManager getInstance() {
        if (instance == null) {
            instance = new ServerSkinManager();
        }
        return instance;
    }

    @Override
    public File getModFolder() {
        return FabricLoader.getInstance().getGameDir().resolve("portalmod").toFile();
    }

    public void onServerLogin(ServerPlayer player) {
        if (this.clientSide) {
            return;
        }

        this.enqueueTask(
                () -> {
                    try {
                        this.loadSkinCatalog();
                    } catch (IOException error) {
                        PortalMod.LOGGER.error(error.getMessage());
                    }

                    Map<UUID, String> temp = new HashMap<>(this.getSelectedSkinMap());
                    temp.forEach(
                            (uuid, skin) -> {
                                if (!uuid.equals(player.getUUID())) {
                                    int tint = this.getTintForPlayerOnSkin(uuid, skin);
                                    PacketInit.sendToPlayer(player, new SSetPlayerSkinPacket(uuid, skin, tint));
                                }
                            });
                });
    }

    public void onServerReceivedPacket(ServerPlayer player, CSetPlayerSkinPacket packet) {
        if (this.clientSide) {
            return;
        }

        this.enqueueTask(
                () -> {
                    UUID uuid = player.getUUID();
                    String skin = packet.getSkin();
                    int tint = packet.getTint();

                    try {
                        this.loadPlayer(uuid);
                    } catch (IOException error) {
                        PortalMod.LOGGER.error(error.getMessage());
                    }

                    if (!this.playerHasSkin(uuid, skin)) {
                        try {
                            this.fetchPlayer(uuid);
                            this.cachePlayer(uuid);

                            if (!this.playerHasSkin(uuid, skin)) {
                                return;
                            }

                        } catch (IOException error) {
                            PortalMod.LOGGER.error(error.getMessage());
                            return;
                        }
                    }

                    this.setSkinAndTintForPlayer(uuid, skin, tint);

                    PacketInit.sendToAll(new SSetPlayerSkinPacket(uuid, skin, tint));
                });
    }
}
