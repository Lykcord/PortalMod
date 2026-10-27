package net.portalmod.client.render;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.portalmod.common.sorted.portal.EulerConverter;
import net.portalmod.common.sorted.portal.PortalEntity;
import net.portalmod.core.config.PortalModConfigManager;
import net.portalmod.core.math.Vec3;
import net.portalmod.mixins.accessors.ActiveRenderInfoAccessor;

import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector4f;
import org.joml.Vector4fc;

public class PortalCamera extends Camera {
    private float roll;
    private Vector4f clipPlane;

    public PortalCamera(
            BlockGetter level, Entity entity, Vec3 position, float pitch, float yaw, float roll, float partialTicks) {
        setLevel((ClientLevel) level);
        setEntity(entity);
        setRotation(yaw, pitch);
        setPosition(position.to3d());
        // Portal cameras need a seeded environment probe to read sky and fog colors.
        attributeProbe().tick((ClientLevel) level, position.to3d());
        var access = (ActiveRenderInfoAccessor) this;
        var main = Minecraft.getInstance().gameRenderer.mainCamera();
        var mainAccess = (ActiveRenderInfoAccessor) main;
        access.pmSetInitialized(true);
        access.pmSetFov(main.getFov());
        access.pmSetHudFov(mainAccess.pmGetHudFov());
        access.pmSetDepthFar(mainAccess.pmGetDepthFar());
        var window = Minecraft.getInstance().getWindow();
        access.pmSetupPerspective(
                PROJECTION_Z_NEAR, mainAccess.pmGetDepthFar(), main.getFov(), window.getWidth(), window.getHeight());
        setRoll(roll);
        refreshFrustum();
    }

    public PortalCamera(Camera camera, float partialTicks) {
        this(
                camera.entity().level(),
                camera.entity(),
                new Vec3(camera.position()),
                camera.xRot(),
                camera.yRot(),
                camera instanceof PortalCamera portal ? portal.getRoll() : 0,
                partialTicks);
    }

    @Override
    public boolean isDetached() {
        return PortalModConfigManager.RENDER_SELF.get();
    }

    public void setPitch(float pitch) {
        setRotation(yRot(), pitch);
        setRoll(roll);
    }

    public void setYaw(float yaw) {
        setRotation(yaw, xRot());
        setRoll(roll);
    }

    public void setRoll(float roll) {
        this.roll = roll;
        applyRoll(this, roll);
    }

    public static void applyRoll(Camera camera, float roll) {
        var access = (ActiveRenderInfoAccessor) camera;
        access.pmSetRotation(camera.yRot(), camera.xRot());
        camera.rotation().set(EulerConverter.toCameraRotation(camera.xRot(), camera.yRot(), roll));
        access.pmGetUp().set(0, 1, 0).rotate(camera.rotation());
        access.pmGetLeft().set(-1, 0, 0).rotate(camera.rotation());
    }

    public float getRoll() {
        return roll;
    }

    public Vector4fc clipPlane() {
        return clipPlane == null ? new Vector4f() : new Vector4f(clipPlane);
    }

    public void clipAtExit(PortalEntity exit) {
        var view = getViewRotationMatrix(new Matrix4f());
        var normal = view.transformDirection(exit.getNormal());
        var surface =
                new Vec3(.5, 1, 0)
                        .transform(
                                PortalEntity.setupMatrix(
                                        exit.getDirection(), exit.getUpVector(), exit.getPivotPoint()));
        var point = surface.sub(new Vec3(position())).to3f();
        view.transformPosition(point);
        clipPlane = new Vector4f(normal, -normal.dot(point) - .0001f);
        refreshFrustum();
    }

    @Override
    public void extractRenderState(CameraRenderState state, DeltaTracker delta) {
        super.extractRenderState(state, delta);
        if (clipPlane != null) {
            state.projectionMatrix.set(projectionForRendering(state.projectionMatrix));
        }
    }

    public Matrix4f projectionForRendering(Matrix4fc projection) {
        return clipPlane == null
                ? new Matrix4f(projection)
                : PortalProjection.clipAtPlane(
                        projection, clipPlane, RenderSystem.getDevice().getDeviceInfo().isZZeroToOne());
    }

    /** Culls only the aperture that will be drawn, without using the GPU's oblique depth plane. */
    public void refreshFrustum(Matrix4fc baseProjection, PortalViewport viewport) {
        var projection =
                PortalProjection.forCulling(
                        viewport.projection(baseProjection), RenderSystem.getDevice().getDeviceInfo().isZZeroToOne());
        ((ActiveRenderInfoAccessor) this)
                .pmPrepareCullFrustum(getViewRotationMatrix(new Matrix4f()), projection, position());
    }

    public void refreshFrustum() {
        var access = (ActiveRenderInfoAccessor) this;
        var projection = access.pmProjectionForCulling();
        // Native CPU culling uses forward depth; the oblique reversed-depth plane belongs to GPU rendering.
        access.pmPrepareCullFrustum(getViewRotationMatrix(new Matrix4f()), projection, position());
    }
}
