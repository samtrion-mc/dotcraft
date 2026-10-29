package net.samtrion.dotcraft.energy.kite.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import net.samtrion.dotcraft.energy.EnergyMod;
import net.samtrion.dotcraft.energy.kite.KiteEntity;

/**
 * Draws the kite: a flat diamond (one texture per tier, double-sided) that leans into the wind and sways a little, and the tether, a
 * straight line from the top of its generator block up to a point a bit below the kite, where it splits into the bridle: two short
 * lines to the two bottom corners of the kite. Every kite (both of a pair) draws its own tether and bridle, so two kites never share
 * a line. The bridle is only drawn, it has no effect on the game. The tether is drawn even when the kite itself is out of the frustum
 * (the kite can be hundreds of blocks above the player), so culling is off.
 */
public class KiteRenderer extends EntityRenderer<KiteEntity, KiteRenderState> {
    /** The render type of the kite, one per tier. */
    private static final RenderType[] TYPES = new RenderType[5];
    /** How far below the bottom edge of the kite the tether splits into the bridle, as a share of the kite size. */
    private static final float BRIDLE_DROP = 0.45F;
    /** Colour of the tether (ARGB). */
    private static final int LINE_COLOR = 0xFFE9E2D0;

    static {
        for (int tier = 1; tier <= TYPES.length; tier++) {
            TYPES[tier - 1] = RenderTypes.entityCutout(Identifier.fromNamespaceAndPath(EnergyMod.MODID, "textures/entity/kite_tier_" + tier + ".png"));
        }
    }

    /**
     * Creates the renderer.
     *
     * @param context the renderer context
     */
    public KiteRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public KiteRenderState createRenderState() {
        return new KiteRenderState();
    }

    @Override
    public void extractRenderState(KiteEntity entity, KiteRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        var anchor = entity.anchor();
        state.tier = Math.clamp(entity.tier(), 1, TYPES.length);
        state.anchorOffset = new Vec3(anchor.getX() + 0.5, anchor.getY() + 1.0, anchor.getZ() + 0.5).subtract(entity.getPosition(partialTicks));
        state.phase = (anchor.getX() * 31 + anchor.getZ() * 17 + entity.slot() * 101) & 0xFF;
    }

    @Override
    public void submit(KiteRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        double altitude = Math.max(0, -state.anchorOffset.y);
        float amplitude = (float) Math.min(2.0, altitude * 0.08);
        float sx = (float) Math.sin(state.ageInTicks * 0.05 + state.phase) * amplitude;
        float sz = (float) Math.cos(state.ageInTicks * 0.037 + state.phase) * amplitude;
        float size = 1.5F + 0.4F * state.tier;

        poseStack.pushPose();
        poseStack.translate(sx, 0, sz);
        poseStack.mulPose(Axis.YP.rotationDegrees(state.phase * 360F / 256F + (float) Math.sin(state.ageInTicks * 0.07) * 12F));
        poseStack.mulPose(Axis.XP.rotationDegrees(-25F));
        // the two bottom corners of the kite, in the space of the entity
        Matrix4f kiteMatrix = new Matrix4f(poseStack.last().pose());
        float h = size / 2;
        Vector3f left = kiteMatrix.transformPosition(-h, -h, 0, new Vector3f());
        Vector3f right = kiteMatrix.transformPosition(h, -h, 0, new Vector3f());
        collector.submitCustomGeometry(poseStack, TYPES[state.tier - 1], (pose, buffer) -> {
            quad(buffer, pose, state.lightCoords, size, 1);
            quad(buffer, pose, state.lightCoords, size, -1);
        });
        poseStack.popPose();

        float width = Minecraft.getInstance().gameRenderer.getGameRenderState().windowRenderState.appropriateLineWidth;
        float ex = (float) state.anchorOffset.x;
        float ey = (float) state.anchorOffset.y;
        float ez = (float) state.anchorOffset.z;
        // the junction: below the middle of the bottom edge, on the way to the anchor
        Vector3f anchor = new Vector3f(ex, ey, ez);
        Vector3f junction = left.add(right, new Vector3f()).mul(0.5F);
        Vector3f toAnchor = anchor.sub(junction, new Vector3f());
        if (toAnchor.lengthSquared() > 0) junction.add(toAnchor.normalize().mul(Math.min(size * BRIDLE_DROP, toAnchor.length())));
        collector.submitCustomGeometry(poseStack, RenderTypes.lines(), (pose, buffer) -> {
            line(buffer, pose, junction, anchor, width);
            line(buffer, pose, junction, left, width);
            line(buffer, pose, junction, right, width);
        });
        super.submit(state, poseStack, collector, camera);
    }

    /** One line of the tether or the bridle; the normal of a line is its direction. */
    private static void line(VertexConsumer buffer, PoseStack.Pose pose, Vector3f from, Vector3f to, float width) {
        Vector3f n = to.sub(from, new Vector3f());
        if (n.lengthSquared() == 0) n.set(0, 1, 0);
        n.normalize();
        buffer.addVertex(pose, from.x, from.y, from.z).setColor(LINE_COLOR).setNormal(pose, n.x, n.y, n.z).setLineWidth(width);
        buffer.addVertex(pose, to.x, to.y, to.z).setColor(LINE_COLOR).setNormal(pose, n.x, n.y, n.z).setLineWidth(width);
    }

    /** One side of the diamond: a square of the given size in the plane facing {@code side} (+1 front, -1 back). */
    private static void quad(VertexConsumer buffer, PoseStack.Pose pose, int light, float size, int side) {
        float h = size / 2;
        float z = 0.01F * side;
        vertex(buffer, pose, light, -h * side, -h, z, 0, 1, side);
        vertex(buffer, pose, light, h * side, -h, z, 1, 1, side);
        vertex(buffer, pose, light, h * side, h, z, 1, 0, side);
        vertex(buffer, pose, light, -h * side, h, z, 0, 0, side);
    }

    /** Adds one vertex of the double-sided kite quad. */
    private static void vertex(VertexConsumer buffer, PoseStack.Pose pose, int light, float x, float y, float z, float u, float v, int side) {
        buffer.addVertex(pose, x, y, z).setColor(-1).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 0, side);
    }

    @Override
    protected boolean affectedByCulling(KiteEntity entity) {
        return false;
    }
}
