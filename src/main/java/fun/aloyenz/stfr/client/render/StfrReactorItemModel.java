package fun.aloyenz.stfr.client.render;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.model.SimpleBakedModel;
import net.minecraft.init.Biomes;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.client.event.ModelBakeEvent;
import org.apache.logging.log4j.LogManager;
import fun.aloyenz.stfr.block.STFRBlocks;
import fun.aloyenz.stfr.item.STFRItems;
import fun.aloyenz.stfr.structure.StfrStructureTemplate;

/** One baked item model assembled from the block models in the current template. */
public final class StfrReactorItemModel {

	private StfrReactorItemModel() {}

	public static void bake(ModelBakeEvent event) {
		IBakedModel reference = event.getModelRegistry().getObject(
				new ModelResourceLocation(STFRBlocks.STRUCTURE_ANALYZER.getRegistryName(), "inventory"));
		TemplateAccess access = new TemplateAccess();
		List<BakedQuad> quads = new ArrayList<>();
		Set<String> missing = new HashSet<>();
		for (StfrStructureTemplate.Cell cell : StfrStructureTemplate.cells()) {
			IBlockState state = cell.expected;
			if (state.getRenderType() != EnumBlockRenderType.MODEL) continue;
			if (state.getBlock() instanceof BlockStairs)
				state = state.getActualState(access, cell.localPos);
			ModelResourceLocation location = event.getModelManager()
					.getBlockModelShapes().getBlockStateMapper()
					.getVariants(state.getBlock()).get(state);
			IBakedModel blockModel = location == null ? null : event.getModelRegistry().getObject(location);
			if (blockModel == null || blockModel == event.getModelManager().getMissingModel()) {
				missing.add(String.valueOf(location));
				continue;
			}
			add(quads, blockModel.getQuads(state, null, 0), cell.localPos);
			for (EnumFacing face : EnumFacing.VALUES)
				add(quads, blockModel.getQuads(state, face, 0), cell.localPos);
		}
		if (!missing.isEmpty())
			LogManager.getLogger("STFR").warn("Reactor item model skipped missing block models: {}", missing);
		EnumMap<EnumFacing, List<BakedQuad>> sides = new EnumMap<>(EnumFacing.class);
		for (EnumFacing face : EnumFacing.VALUES) sides.put(face, Collections.emptyList());
		IBakedModel assembled = new SimpleBakedModel(
				Collections.unmodifiableList(quads),
				sides,
				true,
				true,
				reference.getParticleTexture(),
				reference.getItemCameraTransforms(),
				ItemOverrideList.NONE);
		event.getModelRegistry().putObject(
				new ModelResourceLocation(STFRItems.STFR_REACTOR.getRegistryName(), "inventory"),
				assembled);
	}

	private static final class TemplateAccess implements IBlockAccess {
		private final Map<BlockPos, IBlockState> states = new HashMap<>();

		TemplateAccess() {
			for (StfrStructureTemplate.Cell cell : StfrStructureTemplate.cells())
				states.put(cell.localPos, cell.expected);
		}

		@Override
		public TileEntity getTileEntity(BlockPos pos) {
			return null;
		}

		@Override
		public int getCombinedLight(BlockPos pos, int lightValue) {
			return 0xF000F0;
		}

		@Override
		public IBlockState getBlockState(BlockPos pos) {
			return states.getOrDefault(pos, Blocks.AIR.getDefaultState());
		}

		@Override
		public boolean isAirBlock(BlockPos pos) {
			return getBlockState(pos).getBlock() == Blocks.AIR;
		}

		@Override
		public Biome getBiome(BlockPos pos) {
			return Biomes.PLAINS;
		}

		@Override
		public int getStrongPower(BlockPos pos, EnumFacing direction) {
			return 0;
		}

		@Override
		public WorldType getWorldType() {
			return WorldType.DEFAULT;
		}

		@Override
		public boolean isSideSolid(BlockPos pos, EnumFacing side, boolean defaultValue) {
			return getBlockState(pos).isSideSolid(this, pos, side);
		}
	}

	private static void add(List<BakedQuad> result, List<BakedQuad> source, BlockPos pos) {
		for (BakedQuad quad : source) {
			int[] data = quad.getVertexData().clone();
			int stride = data.length / 4;
			for (int vertex = 0; vertex < 4; vertex++) {
				int base = vertex * stride;
				data[base] = Float.floatToRawIntBits((pos.getX() + Float.intBitsToFloat(data[base]))
						/ StfrStructureTemplate.SIZE);
				data[base + 1] = Float.floatToRawIntBits((pos.getY() + Float.intBitsToFloat(data[base + 1]))
						/ StfrStructureTemplate.SIZE);
				data[base + 2] = Float.floatToRawIntBits((pos.getZ() + Float.intBitsToFloat(data[base + 2]))
						/ StfrStructureTemplate.SIZE);
			}
			result.add(new BakedQuad(data, quad.getTintIndex(), quad.getFace(), quad.getSprite(),
					quad.shouldApplyDiffuseLighting(), quad.getFormat()));
		}
	}
}
