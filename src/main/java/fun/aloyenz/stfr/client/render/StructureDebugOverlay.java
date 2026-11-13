package fun.aloyenz.stfr.client.render;

import java.util.Map;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import fun.aloyenz.stfr.Tags;
import fun.aloyenz.stfr.block.STFRBlocks;
import fun.aloyenz.stfr.item.ItemReactorDebugWand;
import fun.aloyenz.stfr.item.STFRItems;
import fun.aloyenz.stfr.reactor.StfrStructureValidator;
import fun.aloyenz.stfr.structure.StfrBlockRole;
import fun.aloyenz.stfr.structure.StfrStructureTemplate;
import fun.aloyenz.stfr.tile.TileEntityElectrodeProxy;
import fun.aloyenz.stfr.tile.TileEntityStructureAnalyzer;

@Mod.EventBusSubscriber(modid = Tags.MODID, value = Side.CLIENT)
public final class StructureDebugOverlay {

	private StructureDebugOverlay() {}

	private static TileEntityStructureAnalyzer cachedAnalyzer;
	private static long cachedTick = Long.MIN_VALUE;
	private static TileEntityStructureAnalyzer.AuditSummary cachedSummary;

	@SubscribeEvent
	public static void renderSummary(RenderGameOverlayEvent.Text event) {
		Minecraft mc = Minecraft.getMinecraft();
		if (mc.player == null || mc.world == null) return;
		if (!holdsWand(mc) || holdsAnalysis(mc)) return;
		TileEntityStructureAnalyzer analyzer =
				ItemReactorDebugWand.findNearestAnalyzer(mc.world, mc.player.getPosition());
		if (analyzer == null) return;
		long tick = mc.world.getTotalWorldTime() / 20L;
		if (cachedSummary == null || cachedAnalyzer != analyzer || cachedTick != tick) {
			cachedSummary = analyzer.inspect();
			cachedAnalyzer = analyzer;
			cachedTick = tick;
		}
		if (!holdsAnalysis(mc))
			event.getLeft()
					.add(I18n.format("overlay.hbmstfr.wand.unassigned", cachedSummary.unassigned));
		event.getLeft()
				.add(
						I18n.format(
								cachedSummary.wrong == 0
										? "overlay.hbmstfr.wand.valid"
										: "overlay.hbmstfr.wand.errors",
								cachedSummary.wrong));
	}

	private static boolean holdsWand(Minecraft mc) {
		return holdsAnalysis(mc)
				|| mc.player.getHeldItemMainhand().getItem() == STFRItems.REACTOR_DEBUG_WAND
				|| mc.player.getHeldItemOffhand().getItem() == STFRItems.REACTOR_DEBUG_WAND;
	}

	private static boolean holdsAnalysis(Minecraft mc) {
		return mc.player.getHeldItemMainhand().getItem() == STFRItems.REACTOR_ANALYSIS_WAND
				|| mc.player.getHeldItemOffhand().getItem() == STFRItems.REACTOR_ANALYSIS_WAND;
	}

	private static boolean holdsRotorWand(Minecraft mc) {
		return mc.player.getHeldItemMainhand().getItem() == STFRItems.REACTOR_ROTOR_WAND
				|| mc.player.getHeldItemOffhand().getItem() == STFRItems.REACTOR_ROTOR_WAND;
	}

	@SubscribeEvent
	public static void render(RenderWorldLastEvent event) {
		Minecraft mc = Minecraft.getMinecraft();
		if (mc.player == null || mc.world == null) return;
		boolean structureWand = holdsWand(mc);
		boolean rotorWand = holdsRotorWand(mc);
		if (!structureWand && !rotorWand) return;
		TileEntityStructureAnalyzer analyzer =
				ItemReactorDebugWand.findNearestAnalyzer(mc.world, mc.player.getPosition());
		if (analyzer == null) return;

		Entity camera = mc.getRenderViewEntity();
		float partial = event.getPartialTicks();
		double cx = camera.lastTickPosX + (camera.posX - camera.lastTickPosX) * partial;
		double cy = camera.lastTickPosY + (camera.posY - camera.lastTickPosY) * partial;
		double cz = camera.lastTickPosZ + (camera.posZ - camera.lastTickPosZ) * partial;

		GlStateManager.pushMatrix();
		GlStateManager.disableTexture2D();
		GlStateManager.enableBlend();
		GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
		GlStateManager.disableDepth();
		GlStateManager.depthMask(false);
		GlStateManager.glLineWidth(2.5F);
		if (structureWand)
			for (StfrStructureTemplate.Cell cell : StfrStructureTemplate.cells()) {
				BlockPos target = analyzer.getOrigin().add(cell.localPos);
				IBlockState actual = mc.world.getBlockState(target);
				if (actual.getBlock() == STFRBlocks.ROTOR_PLACEHOLDER
						&& analyzer.originalRotorState(cell.localPos) != null)
					actual = analyzer.originalRotorState(cell.localPos);
				StfrBlockRole role = analyzer.getRole(cell.localPos);
				if (actual.getBlock() == STFRBlocks.ELECTRODE_PROXY) {
					TileEntity tile = mc.world.getTileEntity(target);
					if (tile instanceof TileEntityElectrodeProxy
							&& ((TileEntityElectrodeProxy) tile).original() != null)
						actual = ((TileEntityElectrodeProxy) tile).original();
				}
				boolean breach = analyzer.isCaseBreach(cell.localPos);
				boolean correct =
						role.requiresAir()
								? mc.world.isAirBlock(target)
								: StfrStructureValidator.matchesCell(
										cell, role, actual, mc.world, target, analyzer);
				if (holdsAnalysis(mc) && correct) continue;
				if (!breach
						&& correct
						&& (role != StfrBlockRole.UNASSIGNED
								|| cell.expected.getBlock() == Blocks.AIR)) continue;
				float r = breach && correct ? .7F : 1F;
				float g = breach && correct ? .15F : correct ? 0.8F : 0.1F;
				float b = breach && correct ? 1F : correct ? 0.05F : 0.1F;
				float a = 0.95F;
				AxisAlignedBB box = new AxisAlignedBB(target).grow(0.003D).offset(-cx, -cy, -cz);
				RenderGlobal.drawSelectionBoundingBox(box, r, g, b, a);
			}
		if (structureWand)
			for (Map.Entry<BlockPos, StfrBlockRole> entry : analyzer.airRoles().entrySet()) {
				BlockPos target = analyzer.getOrigin().add(entry.getKey());
				if (mc.world.isAirBlock(target)) continue;
				AxisAlignedBB box = new AxisAlignedBB(target).grow(0.003D).offset(-cx, -cy, -cz);
				RenderGlobal.drawSelectionBoundingBox(box, 1F, 0.1F, 0.1F, 0.95F);
			}
		if (rotorWand) {
			for (Long key : analyzer.rotors()) {
				BlockPos target = analyzer.getOrigin().add(BlockPos.fromLong(key));
				RenderGlobal.drawSelectionBoundingBox(
						new AxisAlignedBB(target).grow(0.012D).offset(-cx, -cy, -cz),
						0.1F,
						0.85F,
						1F,
						0.9F);
			}
			for (Map.Entry<Long, EnumFacing> entry : analyzer.axes().entrySet()) {
				BlockPos target = analyzer.getOrigin().add(BlockPos.fromLong(entry.getKey()));
				RenderGlobal.drawSelectionBoundingBox(
						faceBox(target, entry.getValue()).offset(-cx, -cy, -cz),
						1F,
						0.85F,
						0.1F,
						0.95F);
			}
			for (Map.Entry<Long, Integer> entry : analyzer.particleFaces().entrySet()) {
				BlockPos target = analyzer.getOrigin().add(BlockPos.fromLong(entry.getKey()));
				for (EnumFacing face : EnumFacing.VALUES)
					if ((entry.getValue() & 1 << face.getIndex()) != 0)
						RenderGlobal.drawSelectionBoundingBox(
								faceBox(target, face).grow(0.008D).offset(-cx, -cy, -cz),
								0.9F,
								0.95F,
								1F,
								0.95F);
			}
		}
		GlStateManager.depthMask(true);
		GlStateManager.enableDepth();
		GlStateManager.glLineWidth(1F);
		GlStateManager.disableBlend();
		GlStateManager.enableTexture2D();
		GlStateManager.popMatrix();
	}

	private static AxisAlignedBB faceBox(BlockPos pos, EnumFacing face) {
		double x = pos.getX(), y = pos.getY(), z = pos.getZ();
		switch (face) {
			case DOWN:
				return new AxisAlignedBB(x + .15, y - .02, z + .15, x + .85, y + .02, z + .85);
			case UP:
				return new AxisAlignedBB(x + .15, y + .98, z + .15, x + .85, y + 1.02, z + .85);
			case NORTH:
				return new AxisAlignedBB(x + .15, y + .15, z - .02, x + .85, y + .85, z + .02);
			case SOUTH:
				return new AxisAlignedBB(x + .15, y + .15, z + .98, x + .85, y + .85, z + 1.02);
			case WEST:
				return new AxisAlignedBB(x - .02, y + .15, z + .15, x + .02, y + .85, z + .85);
			default:
				return new AxisAlignedBB(x + .98, y + .15, z + .15, x + 1.02, y + .85, z + .85);
		}
	}
}
