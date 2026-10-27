package net.portalmod.common.sorted.portalgun.skins;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import net.portalmod.PortalMod;
import net.portalmod.client.animation.PortalGunAnimatedTexture;
import net.portalmod.core.config.PortalModConfigManager;
import net.portalmod.core.packet.ClientNetworking;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class ClientSkinManager extends SkinManager {
    private static ClientSkinManager instance;

    private ClientSkinManager() {
        super(true);
    }

    public static ClientSkinManager getInstance() {
        if (instance == null) {
            instance = new ClientSkinManager();
        }
        return instance;
    }

    @Override
    public File getModFolder() {
        return new File(Minecraft.getInstance().gameDirectory, "portalmod");
    }

    public Identifier getSkinLocation(String id) {
        return Identifier.fromNamespaceAndPath(PortalMod.MODID, "portalgun/" + id);
    }

    public PortalGunAnimatedTexture getSkinTexture(String id) {
        var textures = Minecraft.getInstance().getTextureManager();
        var texture = textures.getTexture(this.getSkinLocation(id == null ? "default" : id));
        if (texture instanceof PortalGunAnimatedTexture skin) {
            return skin;
        }
        var fallback = textures.getTexture(this.getSkinLocation("default"));
        return fallback instanceof PortalGunAnimatedTexture skin ? skin : null;
    }

    @Override
    protected void uploadAllSkins(boolean overwrite) {
        this.getSkinCatalog().forEach((k, v) -> this.uploadSkin(v, overwrite));
    }

    protected void uploadSkin(PortalGunSkin skin, boolean overwrite) {
        AbstractTexture currentTexture =
                Minecraft.getInstance().getTextureManager().getTexture(this.getSkinLocation(skin.skin_id));
        if (!(currentTexture instanceof PortalGunAnimatedTexture) || overwrite) {
            PortalGunAnimatedTexture skinTexture =
                    new PortalGunAnimatedTexture(skin.skin_id, skin.getFilename(), skin.framerate);
            Minecraft.getInstance()
                    .getTextureManager()
                    .registerAndLoad(this.getSkinLocation(skin.skin_id), skinTexture);
        }
    }

    public boolean hasUUID() {
        return getOwnUUID().isPresent();
    }

    @Override
    public Optional<UUID> getOwnUUID() {
        return Optional.ofNullable(Minecraft.getInstance().getUser().getProfileId());
    }

    public void onClientStartup() {
        if (!this.clientSide) {
            return;
        }

        try {
            this.loadSkinCatalog();
            this.uploadAllSkins(true);
        } catch (IOException error) {
            PortalMod.LOGGER.error(error.getMessage());
        }

        String selectedSkin = PortalModConfigManager.PORTALGUN_SKIN.get();
        int tint = PortalModConfigManager.SKIN_TINT.get();

        if (selectedSkin.equals("default") && !PortalModConfigManager.HAS_SKINS.get()) {
            return;
        }

        Optional<UUID> optionalUUID = this.getOwnUUID();
        if (optionalUUID.isPresent()) {
            UUID uuid = optionalUUID.get();

            try {
                this.fetchNewSkins();
                this.fetchPlayer(uuid);
                this.updateConfigHasSkinsFlag();
                this.uploadAllSkins(true);

                selectedSkin = this.adjustSelectedSkin(selectedSkin);
                this.setConfigSelectedSkin(selectedSkin);
                this.setSelectedSkinForPlayer(null, selectedSkin);

                if (this.getSkinDefinition(selectedSkin).tintable) {
                    tint = this.adjustTint(tint);
                    this.setTintForPlayer(uuid, tint);
                }

            } catch (IOException error) {
                PortalMod.LOGGER.error(error.getMessage());
            }
        }
    }

    public void onClientLogin() {
        if (!this.clientSide) {
            return;
        }

        this.clearSelectedSkinMapExceptSelf();
        this.clearTintMapExceptSelf();
        String selectedSkin = this.getSelectedSkinForPlayer(null);
        int tint = this.getTintForPlayerOnSkin(null, selectedSkin);
        ClientNetworking.sendToServer(new CSetPlayerSkinPacket(selectedSkin, tint));
    }

    public void onClientReceivedPacket(SSetPlayerSkinPacket packet) {
        if (!this.clientSide) {
            return;
        }

        this.enqueueTask(
                () ->
                        CompletableFuture.runAsync(
                                () -> {
                                    UUID uuid = packet.getPlayer();
                                    String skin = packet.getSkin();
                                    int tint = this.isSkinTintable(skin) ? packet.getTint() : 0;

                                    this.setSkinAndTintForPlayer(uuid, skin, tint);
                                }));
    }

    public void onSkinCatalogRefresh() {
        if (!this.clientSide) {
            return;
        }

        this.enqueueTask(
                () -> {
                    Optional<UUID> ownUUID = this.getOwnUUID();

                    if (ownUUID.isPresent()) {
                        try {
                            this.fetchPlayer(ownUUID.get());
                            if (!PortalModConfigManager.HAS_SKINS.get()) {
                                this.applyConfigDefaultTint();
                            }
                            this.updateConfigHasSkinsFlag();

                        } catch (IOException error) {
                            PortalMod.LOGGER.error(error.getMessage());
                        }
                    }

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException error) {
                        return;
                    }
                });
    }

    public void onSkinSelected(String skin, int tint) {
        if (!this.clientSide) {
            return;
        }

        tint = this.isSkinTintable(skin) ? tint : 0;
        this.setSkinAndTintForPlayer(null, skin, tint);

        this.setConfigSelectedSkin(skin);
        if (this.isSkinTintable(skin)) {
            this.setConfigTint(tint);
        }

        if (Minecraft.getInstance().getConnection() != null) {
            ClientNetworking.sendToServer(new CSetPlayerSkinPacket(skin, tint));
        }
    }
}
