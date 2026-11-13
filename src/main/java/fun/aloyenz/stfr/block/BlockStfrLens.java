package fun.aloyenz.stfr.block;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import fun.aloyenz.stfr.Tags;
import fun.aloyenz.stfr.tile.TileEntityFrozenLens;

/** Solid industrial optics. Frost never melts into water or disappears in sunlight. */
public final class BlockStfrLens extends Block {
	private final boolean frozen;

	public BlockStfrLens(String name, boolean frozen, boolean luminous) {
		super(Material.IRON);
		this.frozen = frozen;
		setRegistryName(Tags.MODID, name);
		setTranslationKey(Tags.MODID + "." + name);
		setCreativeTab(STFRBlocks.TAB);
		setHardness(3F);
		setResistance(15F);
		setHarvestLevel("pickaxe", 1);
		setSoundType(SoundType.METAL);
		if (luminous) setLightLevel(12F / 15F);
	}

	@Override public boolean hasTileEntity(IBlockState state) { return frozen; }
	@Override public TileEntity createTileEntity(World world, IBlockState state) {
		return frozen ? new TileEntityFrozenLens() : null;
	}

	@Override public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state,
			EntityLivingBase placer, ItemStack stack) {
		TileEntity tile = world.getTileEntity(pos);
		if (!world.isRemote && tile instanceof TileEntityFrozenLens)
			((TileEntityFrozenLens) tile).setLensData(stack.getTagCompound());
	}

	private ItemStack droppedLens(TileEntity tile) {
		ItemStack stack = new ItemStack(this);
		if (tile instanceof TileEntityFrozenLens)
			stack.setTagCompound(((TileEntityFrozenLens) tile).lensData());
		return stack;
	}

	@Override public void getDrops(NonNullList<ItemStack> drops, IBlockAccess world,
			BlockPos pos, IBlockState state, int fortune) {
		drops.add(droppedLens(world.getTileEntity(pos)));
	}

	@Override public void harvestBlock(World world, EntityPlayer player, BlockPos pos,
			IBlockState state, TileEntity tile, ItemStack tool) {
		// The TileEntity passed by Minecraft survives removal; querying the world here
		// would lose the saved counter, including when mining with Silk Touch.
		if (frozen) {
			player.addExhaustion(0.005F);
			spawnAsEntity(world, pos, droppedLens(tile));
		} else super.harvestBlock(world, player, pos, state, tile, tool);
	}

	@Override public ItemStack getPickBlock(IBlockState state,
			RayTraceResult target, World world, BlockPos pos,
			EntityPlayer player) {
		return droppedLens(world.getTileEntity(pos));
	}
}
