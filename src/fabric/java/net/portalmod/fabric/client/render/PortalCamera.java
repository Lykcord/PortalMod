package net.portalmod.fabric.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * A freely positionable camera used for offscreen portal view passes.
 *
 * <p>Unlike the vanilla camera it is oriented from an explicit forward/up basis, which makes it
 * roll-aware: walking up to a floor portal that exits through a wall produces a rolled view, and
 * the portal isometry can map the camera basis to any orthonormal frame.</p>
 */
public final class PortalCamera extends Camera {
    private static final float Z_NEAR = 0.05F;

    /**
     * Configures this camera as the main camera teleported through a portal.
     *
     * @param level     the client level rendered by this camera
     * @param entity    the camera entity (used for fog and render-state extraction)
     * @param position  world-space camera position behind the destination portal
     * @param forward   normalized view direction
     * @param upVector  normalized up direction (must be orthogonal to {@code forward})
     * @param fovDegrees vertical field of view in degrees, matching the main camera
     * @param cullDistanceChunks chunk-section cull distance for this pass; the cull frustum's far
     *                           plane is clamped to it while the render projection keeps the full
     *                           depth so sky, clouds and fog are unaffected
     */
    public void prepare(ClientLevel level, Entity entity, Vec3 position, Vec3 forward, Vec3 upVector, float fovDegrees, int cullDistanceChunks) {
        Minecraft minecraft = Minecraft.getInstance();
        setLevel(level);
        setEntity(entity);
        setPosition(position);

        Vector3f f = new Vector3f((float) forward.x, (float) forward.y, (float) forward.z).normalize();
        Vector3f u = new Vector3f((float) upVector.x, (float) upVector.y, (float) upVector.z);
        // Re-orthonormalize up against forward to guard against drift in the portal transform.
        u.sub(new Vector3f(f).mul(f.dot(u))).normalize();
        Vector3f l = new Vector3f(u).cross(f).normalize();

        // The camera rotation maps FORWARDS(0,0,-1)->f, UP(0,1,0)->u, LEFT(-1,0,0)->l.
        Matrix3f basis = new Matrix3f(-l.x, -l.y, -l.z, u.x, u.y, u.z, -f.x, -f.y, -f.z);
        rotation().setFromNormalized(basis);
        forwards.set(f);
        up.set(u);
        left.set(l);
        xRot = (float) Math.toDegrees(-Math.asin(Mth.clamp(f.y, -1.0F, 1.0F)));
        yRot = (float) Math.toDegrees(Math.atan2(-f.x, f.z));
        matrixPropertiesDirty |= 3;

        float renderDistance = minecraft.options.getEffectiveRenderDistance() * 16;
        depthFar = Math.max(renderDistance * 4.0F, minecraft.options.cloudRange().get() * 16);
        fov = fovDegrees;
        float windowWidth = minecraft.getWindow().getWidth();
        float windowHeight = minecraft.getWindow().getHeight();
        setupPerspective(Z_NEAR, depthFar, fov, windowWidth, windowHeight);
        float cullFar = Math.min(depthFar, cullDistanceChunks * 16.0F + 16.0F);
        prepareCullFrustum(getViewRotationMatrix(new Matrix4f()), createCullingProjection(windowWidth / windowHeight, cullFar), position());
        initialized = true;
    }

    private Matrix4f createCullingProjection(float aspectRatio, float farPlane) {
        return new Matrix4f().perspective(
                fov * (float) (Math.PI / 180.0),
                aspectRatio,
                Z_NEAR,
                farPlane,
                RenderSystem.getDevice().isZZeroToOne()
        );
    }
}
