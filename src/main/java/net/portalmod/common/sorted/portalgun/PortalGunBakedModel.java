package net.portalmod.common.sorted.portalgun;

import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3fc;

import java.util.ArrayList;

public final class PortalGunBakedModel implements ItemModel {
    private final PortalGunISTER renderer = new PortalGunISTER();
    private final Matrix4f transformation;
    private final Material.Baked particle;
    private final Vector3fc[] extents;

    private PortalGunBakedModel(Matrix4fc transformation, Material.Baked particle) {
        this.transformation = new Matrix4f(transformation);
        this.particle = particle;
        var points = new ArrayList<Vector3fc>();
        renderer.getExtents(points::add);
        extents = points.toArray(Vector3fc[]::new);
    }

    public static void init() {
        ModelLoadingPlugin.register(
                plugin ->
                        plugin.modifyItemModelAfterBake()
                                .register(
                                        (model, context) -> {
                                            if (!context.itemId()
                                                    .equals(
                                                            Identifier.fromNamespaceAndPath(
                                                                    "portalmod", "portalgun"))) {
                                                return model;
                                            }
                                            var particle =
                                                    context.bakingContext()
                                                            .blockModelBaker()
                                                            .materials()
                                                            .get(
                                                                    new Material(
                                                                            Identifier.fromNamespaceAndPath(
                                                                                    "portalmod", "portalgun/default")),
                                                                    () -> "portalmod:item/portalgun");
                                            return new PortalGunBakedModel(context.transformation(), particle);
                                        }));
    }

    @Override
    public void update(
            ItemStackRenderState state,
            ItemStack stack,
            ItemModelResolver resolver,
            ItemDisplayContext context,
            ClientLevel level,
            ItemOwner owner,
            int seed) {
        var argument = renderer.extractArgument(stack, context, owner);
        if (argument == null) {
            return;
        }
        state.setAnimated();
        state.appendModelIdentityElement(this);
        state.appendModelIdentityElement(argument);
        var layer = state.newLayer();
        layer.setUsesBlockLight(true);
        layer.setParticleMaterial(particle);
        layer.setExtents(() -> extents);
        layer.setLocalTransform(new Matrix4f(transformation).mul(PortalGunISTER.displayTransform(stack, context)));
        layer.setupSpecialModel(renderer, argument);
    }
}
