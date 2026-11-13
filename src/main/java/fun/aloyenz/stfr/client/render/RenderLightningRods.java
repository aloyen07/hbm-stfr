package fun.aloyenz.stfr.client.render;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.lwjgl.opengl.GL11;
import fun.aloyenz.stfr.reactor.StfrReactor;
import fun.aloyenz.stfr.reactor.StfrStructureValidator;
import fun.aloyenz.stfr.structure.StfrBlockRole;
import fun.aloyenz.stfr.structure.StfrRadialSymmetry;
import fun.aloyenz.stfr.structure.StfrStructureTemplate;

/** Synchronous inward-to-outward white flashes on role-selected lightning-rod slices. */
final class RenderLightningRods {

	private static final List<StfrStructureTemplate.Cell> RODS = new ArrayList<>();
	private static int minDepth, maxDepth;
	private static final int[][] FACES = {
		{0, 2, 3, 1}, {4, 5, 7, 6}, {0, 1, 5, 4},
		{2, 6, 7, 3}, {0, 4, 6, 2}, {1, 3, 7, 5}
	};

	private RenderLightningRods() {}

	static void render(
			World world,
			BlockPos center,
			int stateId,
			double stateTicks,
			double time,
			double cameraX,
			double cameraY,
			double cameraZ) {
		StfrReactor.RunState[] states = StfrReactor.RunState.values();
		if (stateId < 0 || stateId >= states.length) return;
		StfrReactor.RunState state = states[stateId];
		if (!(state == StfrReactor.RunState.STARTUP_CONFIRMED && stateTicks >= 200
				|| state == StfrReactor.RunState.ONLINE
				|| state == StfrReactor.RunState.STARTUP_FAILURE
				|| state.ordinal() >= StfrReactor.RunState.OVERLOAD.ordinal()
						&& state != StfrReactor.RunState.UNDERLOAD
				|| state == StfrReactor.RunState.SHUTDOWN && stateTicks < 470)) return;
		initialize();
		if (RODS.isEmpty()) return;
		int slices = maxDepth - minDepth + 1;
		int slot = (int) Math.floor(time / 10D) % (slices + 1);
		if (slot >= slices) return; // half-second pause after the outermost slice
		int depth = minDepth + slot;
		float pulse = (float) (.65D + .25D * Math.sin(Math.PI * (time % 10D) / 10D));
		BlockPos origin = center.subtract(StfrStructureTemplate.CENTER);
		GlStateManager.pushMatrix();
		GlStateManager.enableDepth();
		GlStateManager.depthMask(false);
		GlStateManager.disableTexture2D();
		GlStateManager.disableLighting();
		GlStateManager.disableCull();
		GlStateManager.enableBlend();
		GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
		BufferBuilder buffer = Tessellator.getInstance().getBuffer();
		buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
		for (StfrStructureTemplate.Cell cell : RODS) {
			EnumFacing direction = StfrRadialSymmetry.sourceDirection(cell.localPos);
			if (direction == null
					|| StfrRadialSymmetry.radialDepth(cell.localPos, direction) != depth) continue;
			BlockPos pos = origin.add(cell.localPos);
			IBlockState actual = world.getBlockState(pos);
			if (!StfrStructureValidator.matchesCell(cell, cell.defaultRole, actual, world, pos))
				continue;
			AxisAlignedBB box = actual.getBoundingBox(world, pos).grow(.012D);
			double[] xs = {box.minX + pos.getX() - cameraX, box.maxX + pos.getX() - cameraX};
			double[] ys = {box.minY + pos.getY() - cameraY, box.maxY + pos.getY() - cameraY};
			double[] zs = {box.minZ + pos.getZ() - cameraZ, box.maxZ + pos.getZ() - cameraZ};
			for (int[] face : FACES)
				for (int index : face)
					buffer.pos(xs[index & 1], ys[(index >> 1) & 1], zs[(index >> 2) & 1])
							.color(1F, 1F, 1F, pulse)
							.endVertex();
		}
		Tessellator.getInstance().draw();
		GlStateManager.disableBlend();
		GlStateManager.enableCull();
		GlStateManager.enableLighting();
		GlStateManager.enableTexture2D();
		GlStateManager.depthMask(true);
		GlStateManager.color(1F, 1F, 1F, 1F);
		GlStateManager.popMatrix();
	}

	private static void initialize() {
		if (!RODS.isEmpty()) return;
		int minimum = Integer.MAX_VALUE, maximum = Integer.MIN_VALUE;
		for (StfrStructureTemplate.Cell cell : StfrStructureTemplate.cells()) {
			if (cell.defaultRole != StfrBlockRole.LIGHTNING_ROD) continue;
			EnumFacing direction = StfrRadialSymmetry.sourceDirection(cell.localPos);
			if (direction == null) continue;
			int depth = StfrRadialSymmetry.radialDepth(cell.localPos, direction);
			minimum = Math.min(minimum, depth);
			maximum = Math.max(maximum, depth);
			RODS.add(cell);
		}
		minDepth = minimum;
		maxDepth = maximum;
	}
}
