package fun.aloyenz.stfr.reactor;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityFallingBlock;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import fun.aloyenz.stfr.entity.EntityStfrDebris;
import fun.aloyenz.stfr.reactor.physics.StfrSelfDestructMechanics;
import fun.aloyenz.stfr.structure.StfrBlockRole;
import fun.aloyenz.stfr.structure.StfrRadialSymmetry;
import fun.aloyenz.stfr.structure.StfrStructureTemplate;
import fun.aloyenz.stfr.tile.TileEntityRotorPlaceholder;
import fun.aloyenz.stfr.tile.TileEntityStructureAnalyzer;

final class StfrReactorEffects {

	private StfrReactorEffects() {}

	static void disassembleSelfDestruct(StfrReactor r, World world, boolean seals) {
		if (!r.failure.allowsPhysical(world, StfrFailureEffects.STRUCTURE)) return;
		BlockPos origin = r.center.subtract(StfrStructureTemplate.CENTER);
		TileEntityStructureAnalyzer analyzer =
				StfrStructureValidator.matchingAnalyzer(world, r.center);
		for (StfrStructureTemplate.Cell cell : StfrStructureTemplate.cells()) {
			if (r.rotorPositions.contains(cell.localPos.toLong())) continue;
			StfrBlockRole role =
					analyzer == null ? cell.defaultRole : analyzer.getRole(cell.localPos);
			if (seals
					? role != StfrBlockRole.SEAL
					: !r.destructibleBlock(cell.localPos) || role == StfrBlockRole.SEAL) continue;
			BlockPos pos = origin.add(cell.localPos);
			if (!world.isBlockLoaded(pos)) continue;
			IBlockState state=world.getBlockState(pos);
			if(state.getBlock()==Blocks.AIR || state.getBlockHardness(world,pos)<0)continue;
			if(!seals) { world.destroyBlock(pos,false);continue; }
			EntityStfrDebris debris=new EntityStfrDebris(world,pos,state,r.center,pos.getY(),600,200);
			TileEntity tile=world.getTileEntity(pos);
			if(tile!=null)debris.tileEntityData=tile.writeToNBT(new NBTTagCompound());
			debris.launch(StfrSelfDestructMechanics.shellVelocity(pos.subtract(r.center),world.rand));
			debris.getEntityData().setUniqueId("stfrOwner",r.id);
			// Spawn cancellation must leave the original material and inventory in place.
			if(!world.spawnEntity(debris))continue;
			if(tile!=null)world.removeTileEntity(pos);
			if(!world.setBlockToAir(pos)) {
				debris.setDead();
				if(tile!=null)world.setTileEntity(pos,tile);
				continue;
			}
			world.playEvent(2001,pos,Block.getStateId(state));
		}
	}

	static void removeRecoveryShielding(StfrReactor r, World world, EnumFacing side) {
		if (!r.failure.allowsPhysical(world, StfrFailureEffects.STRUCTURE)) return;
		BlockPos origin = r.center.subtract(StfrStructureTemplate.CENTER);
		TileEntityStructureAnalyzer analyzer =
				StfrStructureValidator.matchingAnalyzer(world, r.center);
		for (BlockPos cursor :
				BlockPos.getAllInBox(
						BlockPos.ORIGIN,
						new BlockPos(
								StfrStructureTemplate.SIZE - 1,
								StfrStructureTemplate.SIZE - 1,
								StfrStructureTemplate.SIZE - 1))) {
			BlockPos local = cursor.toImmutable();
			if (side != null && StfrRadialSymmetry.sourceDirection(local) != side) continue;
			StfrStructureTemplate.Cell cell = StfrStructureTemplate.cellAt(local);
			StfrBlockRole role =
					analyzer == null
							? (cell == null ? StfrBlockRole.UNASSIGNED : cell.defaultRole)
							: analyzer.getRole(local);
			if (role != StfrBlockRole.RADIATION_SHIELD && role != StfrBlockRole.OPTIONAL_EFFICIENCY)
				continue;
			BlockPos pos = origin.add(local);
			if (world.isBlockLoaded(pos)) {
				world.setBlockToAir(pos);
				r.hiddenRotors.remove(local.toLong());
			}
		}
	}

	static void ejectSelfDestructRotor(StfrReactor r, World world, EnumFacing face) {
		if (!r.failure.allowsPhysical(world, StfrFailureEffects.STRUCTURE)) return;
		BlockPos origin = r.center.subtract(StfrStructureTemplate.CENTER);
		for (long key : r.rotorPositions) {
			BlockPos local = BlockPos.fromLong(key);
			if (StfrRadialSymmetry.sourceDirection(local) != face) continue;
			BlockPos pos = origin.add(local);
			if (!world.isBlockLoaded(pos)) continue;
			TileEntity tile = world.getTileEntity(pos);
			IBlockState state =
					tile instanceof TileEntityRotorPlaceholder
							? ((TileEntityRotorPlaceholder) tile).original()
							: world.getBlockState(pos);
			if (state == null
					|| state.getBlock() == Blocks.AIR
					|| state.getBlock().hasTileEntity(state)) continue;
			EntityFallingBlock debris =
					new EntityFallingBlock(
							world, pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5, state);
			debris.shouldDropItem = false;
			debris.fallTime = 1;
			debris.getEntityData().setUniqueId("stfrOwner", r.id);
			debris.motionX = face.getXOffset() * .8 + world.rand.nextGaussian() * .3;
			debris.motionY = face.getYOffset() * .8 + .3 + world.rand.nextDouble() * .4;
			debris.motionZ = face.getZOffset() * .8 + world.rand.nextGaussian() * .3;
			world.playEvent(2001, pos, Block.getStateId(state));
			world.setBlockToAir(pos);
			r.hiddenRotors.remove(key);
			world.spawnEntity(debris);
		}
	}
}
