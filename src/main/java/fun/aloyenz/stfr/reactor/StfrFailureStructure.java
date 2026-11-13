package fun.aloyenz.stfr.reactor;

import static fun.aloyenz.stfr.reactor.StfrFailureEffects.*;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityFallingBlock;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import fun.aloyenz.stfr.entity.EntityStfrDebris;
import fun.aloyenz.stfr.structure.StfrStructureTemplate;

/** Local casing damage and glass ejection under the reactor failure policy. */
final class StfrFailureStructure {
	private StfrFailureStructure() {}

	static void removeDestructible(StfrFailureEffects c, World world) {
		if (!c.allowsPhysical(world, STRUCTURE)) return;
		BlockPos origin = c.reactor.center.subtract(StfrStructureTemplate.CENTER);
		for (StfrStructureTemplate.Cell cell : StfrStructureTemplate.cells()) {
			if (!c.reactor.destructibleBlock(cell.localPos)
					&& !c.reactor.caseBreachBlock(cell.localPos)) continue;
			BlockPos pos = origin.add(cell.localPos);
			IBlockState state = world.getBlockState(pos);
			if (state.getBlock().getBlockHardness(state, world, pos) < 0) continue;
			if (StfrStructureValidator.matchesCell(cell, cell.defaultRole, state, world, pos))
				world.setBlockToAir(pos);
		}
	}

	static void caseExplosion(StfrFailureEffects c, World world) {
		if (c.allowsPhysical(world, STRUCTURE)) {
			BlockPos origin = c.reactor.center.subtract(StfrStructureTemplate.CENTER);
			for (StfrStructureTemplate.Cell cell : StfrStructureTemplate.cells()) {
				BlockPos delta = cell.localPos.subtract(StfrStructureTemplate.CENTER);
				if (Math.abs(delta.getX()) > 3
						|| Math.abs(delta.getY()) > 3
						|| Math.abs(delta.getZ()) > 3
						|| !c.reactor.destructibleBlock(cell.localPos)
								&& !c.reactor.caseBreachBlock(cell.localPos)) continue;
				BlockPos pos = origin.add(cell.localPos);
				if (!world.isBlockLoaded(pos)) continue;
				IBlockState state = world.getBlockState(pos);
				if (state.getBlock().hasTileEntity(state)
						|| state.getBlock().getBlockHardness(state, world, pos) < 0
						|| !StfrStructureValidator.matchesCell(
								cell, cell.defaultRole, state, world, pos)) continue;
				EntityFallingBlock debris =
						new EntityFallingBlock(
								world, pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5, state);
				debris.shouldDropItem = false;
				debris.fallTime = 1;
				debris.getEntityData().setUniqueId("stfrOwner", c.reactor.id);
				debris.motionX = delta.getX() * .15;
				debris.motionY = .3 + delta.getY() * .15;
				debris.motionZ = delta.getZ() * .15;
				world.setBlockToAir(pos);
				world.spawnEntity(debris);
			}
		}
		c.compatParticles(world, c.reactor.center, 0, 3000, .5, 1);
		c.particles(world, c.reactor.center, EnumParticleTypes.EXPLOSION_NORMAL, 10, 0, 10);
		for (float pitch : new float[] {0, .6F, .7F, .8F, .9F})
			c.reactor.emergency().sound(world, "explosion.pressuregigaboom", 10, pitch);
		c.blindness(world, 35);
		c.shake(world, 80, 100, 100, 100);
		c.blastDamage(world, c.reactor.center, 6F);
	}

	static void glassBlowout(StfrFailureEffects c, World world) {
		if(world.isRemote || !c.allowsPhysical(world,STRUCTURE))return;
		BlockPos origin=c.reactor.center.subtract(StfrStructureTemplate.CENTER);
		boolean broken=false;
		for(StfrStructureTemplate.Cell cell:StfrStructureTemplate.cells()) {
			if(!c.reactor.caseBreachBlock(cell.localPos)
					&& !cell.expected.getBlock().getRegistryName().getPath().contains("glass"))continue;
			BlockPos pos=origin.add(cell.localPos);
			if(!world.isBlockLoaded(pos) || world.getTileEntity(pos)!=null)continue;
			IBlockState state=world.getBlockState(pos);
			if((state.getMaterial()!=Material.GLASS
					&& !state.getBlock().getRegistryName().getPath().contains("glass"))
					|| state.getBlockHardness(world,pos)<0)continue;
			Vec3d direction=new Vec3d(pos.subtract(c.reactor.center)).normalize();
			EntityStfrDebris shard=new EntityStfrDebris(
					world,pos,state,c.reactor.center,pos.getY(),40,0);
			shard.getEntityData().setBoolean("stfrGlassShard",true);
			shard.getEntityData().setUniqueId("stfrOwner",c.reactor.id);
			shard.launch(direction.scale(.4).add(0,.12,0));
			if(!world.spawnEntity(shard))continue;
			if(!world.setBlockToAir(pos)) {shard.setDead();continue;}
			broken=true;
			if(world instanceof WorldServer) {
				WorldServer server=(WorldServer)world;
				server.spawnParticle(EnumParticleTypes.BLOCK_DUST,true,pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,
						16,.35,.35,.35,.08,Block.getStateId(state));
				for(int j=0;j<4;j++)server.spawnParticle(EnumParticleTypes.CLOUD,true,
						pos.getX()+.5+world.rand.nextGaussian()*.3,
						pos.getY()+.5+world.rand.nextGaussian()*.3,
						pos.getZ()+.5+world.rand.nextGaussian()*.3,
						0,direction.x*.16,direction.y*.16+.025,direction.z*.16,.8);
			}
		}
		if(broken)world.playSound(null,c.reactor.center,SoundEvents.BLOCK_GLASS_BREAK,
				SoundCategory.BLOCKS,2,.8F);
	}

	static void caseBlowout(StfrFailureEffects c, World world) {
		c.caseBlowout(world, true);
	}

	static void caseBlowout(StfrFailureEffects c, World world, boolean entityDamage) {
		boolean[] octants = new boolean[8];
		for (int i = 0; i < 8; i++) octants[i] = world.rand.nextDouble() < .1;
		boolean breached = false;
		if (c.allowsPhysical(world, STRUCTURE)) {
			BlockPos origin = c.reactor.center.subtract(StfrStructureTemplate.CENTER);
			for (StfrStructureTemplate.Cell cell : StfrStructureTemplate.cells())
				if (c.reactor.caseBreachBlock(cell.localPos)) {
					BlockPos offset = cell.localPos.subtract(StfrStructureTemplate.CENTER);
					int octant =
							(offset.getX() < 0 ? 1 : 0)
									| (offset.getY() < 0 ? 2 : 0)
									| (offset.getZ() < 0 ? 4 : 0);
					if (!octants[octant]) continue;
					BlockPos pos = origin.add(cell.localPos);
					if (world.isBlockLoaded(pos)
							&& !world.isAirBlock(pos)
							&& world.getBlockState(pos).getBlockHardness(world, pos) >= 0)
						breached |= world.setBlockToAir(pos);
				}
		}
		if (!breached) return;
		for (int pitch = 0; pitch < 3; pitch++)
			c.reactor.emergency().sound(world, "explosion.pressuregigaboom", 10, pitch);
		c.particles(world, c.reactor.center, EnumParticleTypes.CLOUD, 3000, .5, 5);
		c.blindness(world, 35);
		c.shake(world, 80, 100, 100, 100);
		if (entityDamage) c.blastDamage(world, c.reactor.center, 3F);
	}
}
