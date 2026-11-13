package fun.aloyenz.stfr.reactor;

import com.hbm.util.ContaminationUtil;
import java.util.Comparator;
import java.util.Map;
import net.minecraft.block.Block;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.BlockTrapDoor;
import net.minecraft.block.material.Material;
import net.minecraft.entity.item.EntityFallingBlock;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import fun.aloyenz.stfr.logic.StfrLogicPacket;
import fun.aloyenz.stfr.logic.StfrShakePacket;
import fun.aloyenz.stfr.structure.StfrBlockRole;
import fun.aloyenz.stfr.structure.StfrStructureTemplate;

/** Physical consequences shared by ordinary ONLINE operation and active-core states. */
final class StfrOperatingEffects {

	private StfrOperatingEffects() {}

	static void tick(World world, StfrReactor reactor) {
		StfrCoreSimulation core = reactor.core();
		boolean sealed = reactor.caseIntact(world);
		boolean dirtyStopWaiting = reactor.dirtyShutdown
				&& reactor.runState() == StfrReactor.RunState.SHUTDOWN;
		if (!sealed) {
			if (!dirtyStopWaiting && core.casePressure() >= 150
					&& world.getTotalWorldTime() % 20 == 0) {
				particles(world, reactor.center, EnumParticleTypes.CLOUD, 30);
				world.playSound(
						null,
						reactor.center,
						SoundEvents.BLOCK_FIRE_EXTINGUISH,
						SoundCategory.BLOCKS,
						2F,
						.7F);
			}
		}
		if (reactor.failure().allowsPhysical(world, StfrFailurePolicy.PLAYER_DAMAGE))
			for (EntityPlayer player : world.playerEntities) {
				if (player.isSpectator()) continue;
				double distance = player.getDistanceSq(reactor.center);
				if (distance <= 16) player.attackEntityFrom(DamageSource.GENERIC, 7F);
				double radiation = distance <= 64 ? 2 : 0;
				if (!sealed && core.casePressure() >= 150 && distance <= 900) radiation += 3;
				if (!sealed
						&& core.casePressure() >= 150
						&& Math.abs(player.posX - reactor.center.getX()) <= 14
						&& Math.abs(player.posY - reactor.center.getY()) <= 14
						&& Math.abs(player.posZ - reactor.center.getZ()) <= 14) radiation += 19;
				if (!dirtyStopWaiting && radiation > 0)
					ContaminationUtil.contaminate(
							player,
							ContaminationUtil.HazardType.RADIATION,
							ContaminationUtil.ContaminationType.CREATIVE,
							radiation / 20D);
			}
		if (!dirtyStopWaiting && !sealed
				&& core.casePressure() >= 150
				&& reactor.failure().allowsPhysical(world, StfrFailurePolicy.PLAYER_DAMAGE)) {
			EntityPlayer nearest =
					world.playerEntities.stream()
							.filter(p -> !p.isSpectator() && p.getDistanceSq(reactor.center) <= 900)
							.min(Comparator.comparingDouble(p -> p.getDistanceSq(reactor.center)))
							.orElse(null);
			if (nearest != null) {
				Vec3d start = new Vec3d(reactor.center).add(.5, .5, .5);
				if (world.rayTraceBlocks(start, nearest.getPositionEyes(1F), false, true, false)
						== null) {
					double intensity =
							core.coreTemperature()
									/ 1000D
									/ 6D
									* Math.pow(
											.9D,
											start.distanceTo(nearest.getPositionEyes(1F)) / .3D);
					ContaminationUtil.contaminate(
							nearest,
							ContaminationUtil.HazardType.RADIATION,
							ContaminationUtil.ContaminationType.CREATIVE,
							intensity / 20D);
				}
			}
		}
		BlockPos origin = reactor.center.subtract(StfrStructureTemplate.CENTER);
		boolean destroy = reactor.failure().allowsPhysical(world, StfrFailurePolicy.STRUCTURE);
		BlockPos obstruction = null;
		if (destroy)
			for (Map.Entry<BlockPos, StfrBlockRole> entry :
					reactor.chamberAirRoles(world).entrySet()) {
				if (entry.getValue() != StfrBlockRole.MAYBE_AIR) continue;
				BlockPos pos = origin.add(entry.getKey());
				if (world.isAirBlock(pos)
						|| world.getBlockState(pos).getBlockHardness(world, pos) < 0) continue;
				if (world.destroyBlock(pos, true)) obstruction = pos;
			}
		if (obstruction != null) blast(world, reactor, obstruction, 2F, 3D);
		boolean blowout = sealed && core.casePressure() >= 81156 && world.rand.nextDouble() < .01;
		boolean explosion =
				sealed && core.casePressure() >= 95156 && world.rand.nextDouble() < .003;
		if (blowout || explosion) {
			boolean[] octants = new boolean[8];
			for (int i = 0; i < 8; i++) octants[i] = world.rand.nextDouble() < .1;
			boolean breached = false;
			if (destroy)
				for (StfrStructureTemplate.Cell cell : StfrStructureTemplate.cells()) {
					BlockPos offset = cell.localPos.subtract(StfrStructureTemplate.CENTER);
					int octant =
							(offset.getX() < 0 ? 1 : 0)
									| (offset.getY() < 0 ? 2 : 0)
									| (offset.getZ() < 0 ? 4 : 0);
					if (!reactor.operatingCaseCell(cell) || !explosion && !octants[octant])
						continue;
					int ax = Math.abs(offset.getX()),
							ay = Math.abs(offset.getY()),
							az = Math.abs(offset.getZ());
					if (!explosion
							&& (Math.min(ax, Math.min(ay, az)) != 2
									|| Math.max(ax, Math.max(ay, az)) != 3
									|| ax + ay + az != 7)) continue;
					if (explosion
							&& (Math.abs(offset.getX()) > 3
									|| Math.abs(offset.getY()) > 3
									|| Math.abs(offset.getZ()) > 3)) continue;
					BlockPos pos = origin.add(cell.localPos);
					if (world.isAirBlock(pos)
							|| world.getBlockState(pos).getBlockHardness(world, pos) < 0) continue;
					if (!world.getBlockState(pos)
							.getBlock()
							.hasTileEntity(world.getBlockState(pos))) {
						EntityFallingBlock debris =
								new EntityFallingBlock(
										world,
										pos.getX() + .5,
										pos.getY() + .5,
										pos.getZ() + .5,
										world.getBlockState(pos));
						debris.fallTime = 1;
						debris.shouldDropItem = false;
						double length = Math.max(1, Math.sqrt(ax * ax + ay * ay + az * az));
						debris.motionX = offset.getX() / length;
						debris.motionY = offset.getY() / length;
						debris.motionZ = offset.getZ() / length;
						world.setBlockToAir(pos);
						world.spawnEntity(debris);
						breached = true;
					} else breached |= world.destroyBlock(pos, true);
				}
			if (breached || !destroy && explosion) {
				blast(world, reactor, reactor.center, explosion ? 15F : 5F, 14D);
				reactor.serviceNotice(
						world,
						explosion ? "operation.case_explosion" : "operation.case_blowout",
						true);
			}
		}
		boolean fire = reactor.failure().allowsPhysical(world, StfrFailurePolicy.FIRE);
		if (core.caseTemperature() >= 1000) {
			if (core.caseTemperature() >= 1500) core.coolCase(.3);
			if (fire)
				for (int x = -2; x <= 2; x++)
					for (int z = -2; z <= 2; z++) {
						if (Math.abs(x) == 2 && Math.abs(z) == 2 || world.rand.nextDouble() >= .003)
							continue;
						BlockPos pos = reactor.center.add(x, 5, z);
						if (world.getBlockState(pos.down()).getMaterial() == Material.GLASS)
							continue;
						if (world.getBlockState(pos).getMaterial() == Material.WATER) {
							if (destroy) {
								world.setBlockToAir(pos);
								core.coolCase(.26);
								particles(world, pos, EnumParticleTypes.CLOUD, 20);
								world.playSound(
										null,
										pos,
										SoundEvents.BLOCK_FIRE_EXTINGUISH,
										SoundCategory.BLOCKS,
										1F,
										.7F);
							}
						} else if (world.isAirBlock(pos) && Blocks.FIRE.canPlaceBlockAt(world, pos))
							world.setBlockState(pos, Blocks.FIRE.getDefaultState(), 3);
					}
			if (destroy && core.caseTemperature() >= 1084 && world.rand.nextDouble() < .03)
				for (StfrStructureTemplate.Cell cell : StfrStructureTemplate.cells()) {
					if (!reactor.operatingCaseCell(cell) || world.rand.nextDouble() >= .003)
						continue;
					BlockPos pos = origin.add(cell.localPos);
					Block block = world.getBlockState(pos).getBlock();
					if ((block instanceof BlockStairs
									|| block instanceof BlockTrapDoor
									|| block instanceof BlockSlab)
							&& world.rand.nextDouble() < .95) continue;
					if (world.getBlockState(pos).getBlockHardness(world, pos) >= 0)
						world.destroyBlock(pos, true);
				}
		}
		if (reactor.resources().steam().getFill() >= 83_580_000 && world.rand.nextDouble() < .01) {
			reactor.serviceNotice(world, "operation.steam_overpressure", true);
			for (EnumFacing side : EnumFacing.VALUES) {
				StfrComponentStatus status = reactor.componentStatus(side);
				if (status == null || !status.turbineStructureValid) continue;
				BlockPos pos = reactor.center.offset(side, 11);
				particles(world, pos, EnumParticleTypes.CLOUD, 20);
				if (world.rand.nextDouble() < .003 && !reactor.sectionHasPort(world, side)) {
					if (destroy
							&& reactor.failure()
									.allowsPhysical(world, StfrFailurePolicy.PLAYER_DAMAGE)
							&& reactor.failure()
									.allowsPhysical(world, StfrFailurePolicy.SHOCKWAVE)) {
						world.newExplosion(
								null,
								pos.getX() + .5,
								pos.getY() + .5,
								pos.getZ() + .5,
								4F,
								false,
								true);
					} else {
						if (destroy && world.getBlockState(pos).getBlockHardness(world, pos) >= 0)
							world.destroyBlock(pos, true);
						blast(world, reactor, pos, 8F, 4D);
					}
				}
			}
		}
	}

	private static void particles(World world, BlockPos pos, EnumParticleTypes type, int count) {
		if (world instanceof WorldServer)
			((WorldServer) world)
					.spawnParticle(
							type,
							pos.getX() + .5,
							pos.getY() + .5,
							pos.getZ() + .5,
							count,
							.6,
							.6,
							.6,
							.1);
	}

	private static void blast(
			World world, StfrReactor reactor, BlockPos pos, float damage, double radius) {
		particles(world, pos, EnumParticleTypes.EXPLOSION_LARGE, 8);
		world.playSound(
				null, pos, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.BLOCKS, 4F, .7F);
		if (reactor.failure().allowsPhysical(world, StfrFailurePolicy.PLAYER_DAMAGE))
			for (EntityPlayer player : world.playerEntities)
				if (!player.isSpectator()) {
					boolean hit =
							radius == 14D
									? Math.abs(player.posX - pos.getX()) <= 14
											&& Math.abs(player.posY - pos.getY()) <= 14
											&& Math.abs(player.posZ - pos.getZ()) <= 14
									: player.getDistanceSq(pos) <= radius * radius;
					if (hit) player.attackEntityFrom(DamageSource.GENERIC, damage);
					if (radius == 14D && player.getDistanceSq(pos) <= 35D * 35D)
						player.addPotionEffect(
								new PotionEffect(MobEffects.BLINDNESS, 20, 0, false, false));
				}
		if (radius == 14D && reactor.failure().allowsPhysical(world, StfrFailurePolicy.SHOCKWAVE))
			StfrLogicPacket.CHANNEL.sendToAllAround(
					new StfrShakePacket(pos, 100, 100, 5),
					new NetworkRegistry.TargetPoint(
							world.provider.getDimension(), pos.getX(), pos.getY(), pos.getZ(), 80));
	}
}
