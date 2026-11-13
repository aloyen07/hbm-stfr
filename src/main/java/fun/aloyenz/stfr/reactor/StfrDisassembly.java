package fun.aloyenz.stfr.reactor;

import java.util.List;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityHanging;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.MobEffects;
import net.minecraft.network.play.server.SPacketEntityVelocity;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import fun.aloyenz.stfr.entity.EntityStfrDebris;
import fun.aloyenz.stfr.reactor.physics.StfrDisassemblyOrder;
import fun.aloyenz.stfr.reactor.physics.StfrLiftMotion;
import fun.aloyenz.stfr.reactor.physics.StfrVortex;
import fun.aloyenz.stfr.structure.StfrBlockRole;
import fun.aloyenz.stfr.structure.StfrStructureTemplate;

/** Shared, bounded physical breakup for Meltdown, Shutdown Failure and Underload. */
final class StfrDisassembly {
	private static List<StfrStructureTemplate.Cell> order;
	private StfrDisassembly() {}
	private static List<StfrStructureTemplate.Cell> cells() {
		if (order == null) order = StfrDisassemblyOrder.centerOut(StfrStructureTemplate.cells(),
				cell -> cell.localPos, StfrStructureTemplate.CENTER);
		return order;
	}

	static int release(StfrReactor reactor, World world, int cursor, int count, int lifetime, int chargeAfter) {
		if (world.isRemote || !reactor.failure().allowsPhysical(world, StfrFailureEffects.STRUCTURE)) return cursor;
		BlockPos origin = reactor.center.subtract(StfrStructureTemplate.CENTER);
		int released = 0;
		while (cursor < cells().size() && released < count) {
			StfrStructureTemplate.Cell cell = cells().get(cursor++);
			if (cell.defaultRole.requiresAir() || cell.defaultRole == StfrBlockRole.PORT) continue;
			BlockPos pos = origin.add(cell.localPos);
			if (!world.isBlockLoaded(pos)) { cursor--; break; }
			IBlockState state = world.getBlockState(pos);
			if (state.getBlock().hasTileEntity(state) || state.getBlock().getBlockHardness(state, world, pos) < 0
					|| !StfrStructureValidator.matchesCell(cell, cell.defaultRole, state, world, pos)) continue;
			EntityStfrDebris debris = new EntityStfrDebris(world, pos, state, reactor.center,
					reactor.liftsDebris(cell.localPos) ? reactor.debrisReleaseY() : pos.getY(),
					Math.max(2, lifetime), chargeAfter);
			BlockPos delta = pos.subtract(reactor.center);
			debris.motionX = delta.getX() * .025;
			debris.motionZ = delta.getZ() * .025;
			debris.getEntityData().setUniqueId("stfrOwner", reactor.id);
			world.setBlockToAir(pos);
			world.spawnEntity(debris);
			released++;
		}
		return cursor;
	}

	static void lift(StfrReactor reactor, World world, boolean charging) {
		if (world.isRemote || !reactor.failure().allowsPhysical(world, StfrFailureEffects.SHOCKWAVE)) return;
		BlockPos origin = reactor.center.subtract(StfrStructureTemplate.CENTER);
		BlockPos first = reactor.liftBoxFirst == null ? BlockPos.ORIGIN : reactor.liftBoxFirst;
		BlockPos second = reactor.liftBoxSecond == null ? new BlockPos(28,28,28) : reactor.liftBoxSecond;
		AxisAlignedBB box = new AxisAlignedBB(
				origin.getX()+Math.min(first.getX(),second.getX()), origin.getY()+Math.min(first.getY(),second.getY()),
				origin.getZ()+Math.min(first.getZ(),second.getZ()), origin.getX()+Math.max(first.getX(),second.getX())+1,
				origin.getY()+Math.max(first.getY(),second.getY())+1, origin.getZ()+Math.max(first.getZ(),second.getZ())+1);
		// Capture only inside the configured cube, but keep lifting a captured entity
		// through the column above it until the configured release height.
		AxisAlignedBB column=new AxisAlignedBB(box.minX,Math.min(box.minY,reactor.debrisReleaseY()),box.minZ,
				box.maxX,Math.max(box.maxY,reactor.debrisReleaseY()+2),box.maxZ);
		for (Entity entity : world.getEntitiesWithinAABB(Entity.class, column)) {
			if (entity.isDead || entity instanceof EntityStfrDebris || entity instanceof EntityHanging
					|| entity.isRiding()) continue;
			if (entity instanceof EntityPlayer && (((EntityPlayer) entity).capabilities.isCreativeMode
					|| ((EntityPlayer) entity).isSpectator())) continue;
			if(entity.getEntityData().getBoolean("stfrReleasedDebris"))continue;
			boolean held=entity.getEntityData().hasUniqueId("stfrLiftOwner")
					&& reactor.id.equals(entity.getEntityData().getUniqueId("stfrLiftOwner"));
			if (entity.posY >= reactor.debrisReleaseY()) {
				if(held) {
					entity.getEntityData().removeTag("stfrLiftOwnerMost");
					entity.getEntityData().removeTag("stfrLiftOwnerLeast");
					entity.motionY=Math.min(0,entity.motionY);
					if(entity instanceof EntityPlayerMP && entity.getEntityData().getBoolean("stfrLiftLevitation")) {
						EntityPlayerMP player=(EntityPlayerMP)entity;
						PotionEffect effect=player.getActivePotionEffect(MobEffects.LEVITATION);
						if(effect!=null && effect.getDuration()<=6)player.removePotionEffect(MobEffects.LEVITATION);
						entity.getEntityData().removeTag("stfrLiftLevitation");
					}
					entity.velocityChanged=true;
					if(entity instanceof EntityPlayerMP)
						((EntityPlayerMP)entity).connection.sendPacket(new SPacketEntityVelocity(entity));
				}
				continue;
			}
			if(!StfrLiftMotion.liftEntity(entity.posY,reactor.debrisReleaseY(),
					box.intersects(entity.getEntityBoundingBox()),held))continue;
			entity.getEntityData().setUniqueId("stfrLiftOwner",reactor.id);
			Vec3d next = StfrVortex.velocity(new Vec3d(entity.posX-reactor.center.getX()-.5,
					entity.posY-reactor.center.getY()-.5, entity.posZ-reactor.center.getZ()-.5),
					new Vec3d(entity.motionX, entity.motionY, entity.motionZ), charging);
			entity.motionX = next.x; entity.motionY = next.y; entity.motionZ = next.z;
			entity.fallDistance = 0;
			entity.velocityChanged = true;
			// Vanilla's flight check explicitly accepts levitation. A short, invisible refresh
			// prevents the server from treating its own sustained lift as illegal player flight.
			if (entity instanceof EntityPlayerMP) {
				EntityPlayerMP player=(EntityPlayerMP)entity;
				PotionEffect active=player.getActivePotionEffect(MobEffects.LEVITATION);
				if (active==null || active.getDuration()<3) {
					player.addPotionEffect(new PotionEffect(MobEffects.LEVITATION,6,0,true,false));
					entity.getEntityData().setBoolean("stfrLiftLevitation",true);
				}
			}
			if (entity instanceof EntityPlayerMP && world.getTotalWorldTime()%2 == 0)
				((EntityPlayerMP) entity).connection.sendPacket(new SPacketEntityVelocity(entity));
		}
	}
}
