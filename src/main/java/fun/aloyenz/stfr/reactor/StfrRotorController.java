package fun.aloyenz.stfr.reactor;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.network.play.server.SPacketEntityVelocity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import fun.aloyenz.stfr.block.STFRBlocks;
import fun.aloyenz.stfr.logic.StfrLogicPacket;
import fun.aloyenz.stfr.logic.StfrRotorSyncPacket;
import fun.aloyenz.stfr.reactor.StfrReactor.RunState;
import fun.aloyenz.stfr.reactor.physics.StfrRotorPhysics;
import fun.aloyenz.stfr.structure.StfrElectrodes;
import fun.aloyenz.stfr.structure.StfrRadialSymmetry;
import fun.aloyenz.stfr.structure.StfrStructureTemplate;
import fun.aloyenz.stfr.tile.TileEntityElectrodeProxy;
import fun.aloyenz.stfr.tile.TileEntityRotorPlaceholder;
import fun.aloyenz.stfr.tile.TileEntityStructureAnalyzer;

final class StfrRotorController {

	private StfrRotorController() {}

	static int setRotorMap(
			StfrReactor r,
			World world,
			Set<Long> positions,
			Map<Long, EnumFacing> axes,
			Map<Long, Integer> particles,
			Map<Long, IBlockState> legacyHidden) {
		r.rotorPositions.clear();
		r.rotorPositions.addAll(positions);
		r.rotorAxes.clear();
		r.rotorAxes.putAll(axes);
		r.steamFaces.clear();
		r.steamFaces.putAll(particles);
		r.hiddenRotors.putAll(legacyHidden);
		r.rotorMapExplicit = true;
		return StfrRotorController.syncRotorBlocks(r, world, r.rotorVelocity > 0);
	}

	static Map.Entry<Long, EnumFacing> nearestAxis(StfrReactor r, BlockPos local) {
		EnumFacing sector = StfrRadialSymmetry.sourceDirection(local);
		Map.Entry<Long, EnumFacing> best = null;
		double distance = Double.MAX_VALUE;
		for (Map.Entry<Long, EnumFacing> entry : r.rotorAxes.entrySet()) {
			BlockPos candidate = BlockPos.fromLong(entry.getKey());
			if (StfrRadialSymmetry.sourceDirection(candidate) != sector) continue;
			double d = candidate.distanceSq(local);
			if (d < distance) {
				distance = d;
				best = entry;
			}
		}
		return best;
	}

	static int syncRotorBlocks(StfrReactor r, World world, boolean spinning) {
		BlockPos origin = r.center.subtract(StfrStructureTemplate.CENTER);
		if (!world.isAreaLoaded(origin, origin.add(28, 28, 28))) return 0;
		int changed = 0;
		for (Map.Entry<Long, IBlockState> entry : new HashMap<>(r.hiddenRotors).entrySet()) {
			if (spinning && r.rotorPositions.contains(entry.getKey())) continue;
			BlockPos target = origin.add(BlockPos.fromLong(entry.getKey()));
			if (world.getBlockState(target).getBlock() == STFRBlocks.ROTOR_PLACEHOLDER) {
				world.setBlockState(target, entry.getValue(), 3);
				changed++;
			}
			r.hiddenRotors.remove(entry.getKey());
		}
		if (!spinning) return changed;
		Map<Long, IBlockState> additions = new HashMap<>();
		for (Long key : r.rotorPositions) {
			BlockPos local = BlockPos.fromLong(key);
			BlockPos target = origin.add(local);
			IBlockState state = world.getBlockState(target);
			if (state.getBlock() == STFRBlocks.ROTOR_PLACEHOLDER) {
				TileEntityRotorPlaceholder tile =
						(TileEntityRotorPlaceholder) world.getTileEntity(target);
				if (tile != null && tile.original() != null)
					r.hiddenRotors.put(key, tile.original());
				if (tile != null
						&& r.hiddenRotors.containsKey(key)
						&& (tile.original() == null || !tile.center().equals(r.center)))
					StfrRotorController.configureRotor(r, tile, origin, local, r.hiddenRotors.get(key));
				continue;
			}
			if (state.getBlock() == Blocks.AIR || state.getBlock().hasTileEntity(state)) continue;
			StfrStructureTemplate.Cell cell = StfrStructureTemplate.cellAt(local);
			if (cell == null
					|| !StfrStructureValidator.matchesCell(
							cell, cell.defaultRole, state, world, target)) continue;
			additions.put(
					key,
					state.getBlock() instanceof BlockStairs
							? state.getActualState(world, target)
							: state);
		}
		for (Map.Entry<Long, IBlockState> entry : additions.entrySet()) {
			BlockPos local = BlockPos.fromLong(entry.getKey());
			BlockPos target = origin.add(local);
			if (world.setBlockState(target, STFRBlocks.ROTOR_PLACEHOLDER.getDefaultState(), 3)) {
				r.hiddenRotors.put(entry.getKey(), entry.getValue());
				TileEntity tile = world.getTileEntity(target);
				if (tile instanceof TileEntityRotorPlaceholder)
					StfrRotorController.configureRotor(r, (TileEntityRotorPlaceholder) tile, origin, local, entry.getValue());
				changed++;
			}
		}
		return changed;
	}

	static void configureRotor(
			StfrReactor r,
			TileEntityRotorPlaceholder tile,
			BlockPos origin,
			BlockPos local,
			IBlockState original) {
		Map.Entry<Long, EnumFacing> axis = StfrRotorController.nearestAxis(r, local);
		tile.configure(
				original,
				r.center,
				axis == null ? origin.add(local) : origin.add(BlockPos.fromLong(axis.getKey())),
				axis == null ? EnumFacing.UP : axis.getValue());
	}

	static void tickRotorContacts(StfrReactor r, World world) {
		if (world.isRemote || r.rotorVelocity <= 0) return;
		long now = world.getTotalWorldTime();
		r.rotorContactTicks
				.entrySet()
				.removeIf(
						entry -> now - entry.getValue() >= StfrRotorPhysics.CONTACT_COOLDOWN_TICKS);
		BlockPos origin = r.center.subtract(StfrStructureTemplate.CENTER);
		for (EntityPlayer player : world.playerEntities) {
			if (player.isSpectator()
					|| player.capabilities.isFlying
					|| r.rotorContactTicks.containsKey(player.getUniqueID())
					|| player.getDistanceSq(r.center) > 32D * 32D
					|| player.motionY > .1) continue;
			AxisAlignedBB bounds = player.getEntityBoundingBox();
			AxisAlignedBB feet =
					new AxisAlignedBB(
							bounds.minX,
							bounds.minY - .08,
							bounds.minZ,
							bounds.maxX,
							bounds.minY + .08,
							bounds.maxZ);
			Vec3d impulse = Vec3d.ZERO;
			for (long key : r.rotorPositions) {
				BlockPos pos = origin.add(BlockPos.fromLong(key));
				if (!world.isBlockLoaded(pos) || !feet.intersects(new AxisAlignedBB(pos))) continue;
				TileEntity tile = world.getTileEntity(pos);
				if (!(tile instanceof TileEntityRotorPlaceholder)) continue;
				TileEntityRotorPlaceholder rotor = (TileEntityRotorPlaceholder) tile;
				if (!rotor.center().equals(r.center) || rotor.original() == null) continue;
				Vec3d radial =
						new Vec3d(
								player.posX - rotor.pivot().getX() - .5,
								bounds.minY - rotor.pivot().getY() - .5,
								player.posZ - rotor.pivot().getZ() - .5);
				Vec3d candidate = StfrRotorPhysics.impulse(radial, rotor.axis(), r.rotorVelocity);
				if (candidate.lengthSquared() > impulse.lengthSquared()) impulse = candidate;
			}
			if (impulse.lengthSquared() < 1E-8) continue;
			player.motionX += impulse.x;
			player.motionZ += impulse.z;
			player.motionY += impulse.y + Math.min(.45, .21 * r.rotorVelocity / 100D);
			player.onGround = false;
			player.velocityChanged = true;
			r.rotorContactTicks.put(player.getUniqueID(), now);
			if (player instanceof EntityPlayerMP)
				((EntityPlayerMP) player).connection.sendPacket(new SPacketEntityVelocity(player));
		}
	}

	static void tickRotorEffects(StfrReactor r, World world) {
		boolean steam =
				r.runState == RunState.STARTUP_CONFIRMED
								&& r.stateTicks >= 200
								&& r.stateTicks <= 210
						|| r.destabilizerStage >= 2 && r.destabilizerStage <= 5;
		boolean spinning = r.rotorVelocity > 0;
		boolean transition = r.rotorBlocksSpinning == null || r.rotorBlocksSpinning != spinning;
		boolean visualTransition = r.lastSentVisualState != r.runState;
		boolean electrodeTransition =
				r.runState == RunState.STARTUP_CONFIRMED && r.stateTicks == 375
						|| r.runState == RunState.SHUTDOWN && r.stateTicks == 470;
		if (!steam
				&& !transition
				&& !visualTransition
				&& !electrodeTransition
				&& world.getTotalWorldTime() % 10 != 0) return;
		if (!r.rotorMapExplicit)
			for (TileEntity tile : world.loadedTileEntityList) {
				if (tile instanceof TileEntityStructureAnalyzer) {
					TileEntityStructureAnalyzer analyzer = (TileEntityStructureAnalyzer) tile;
					if (analyzer.getOrigin().add(StfrStructureTemplate.CENTER).equals(r.center)) {
						r.setRotorMap(
								world,
								analyzer.rotors(),
								analyzer.axes(),
								analyzer.particleFaces(),
								analyzer.hiddenRotorStates());
						break;
					}
				}
			}
		if (transition || world.getTotalWorldTime() % 20 == 0) {
			StfrRotorController.syncRotorBlocks(r, world, spinning);
			r.rotorBlocksSpinning = spinning;
		}
		if (visualTransition || electrodeTransition || world.getTotalWorldTime() % 20 == 0)
			StfrRotorController.syncElectrodes(r, world,
					r.runState == RunState.ONLINE
							|| r.runState == RunState.STARTUP_FAILURE
							|| r.emergency.beamsActive()
							|| r.runState == RunState.UNDERLOAD
							|| r.runState == RunState.STARTUP_CONFIRMED && r.stateTicks >= 375
							|| r.runState == RunState.SHUTDOWN && r.stateTicks < 470);
		if (steam && world instanceof WorldServer) {
			WorldServer server = (WorldServer) world;
			for (Map.Entry<Long, Integer> entry : r.steamFaces.entrySet()) {
				BlockPos marker =
						r.center
								.subtract(StfrStructureTemplate.CENTER)
								.add(BlockPos.fromLong(entry.getKey()));
				for (EnumFacing face : EnumFacing.VALUES) {
					if ((entry.getValue() & 1 << face.getIndex()) == 0) continue;
					for (int i = 0; i < (r.destabilizerStage == 5 ? 16 : 8); i++) {
						// count=0 means the packet offsets are particle velocity in
						// 1.12; count>0 would scatter them in random directions.
						double spreadX =
								face.getXOffset() == 0 ? server.rand.nextGaussian() * 0.22D : 0D;
						double spreadY =
								face.getYOffset() == 0 ? server.rand.nextGaussian() * 0.22D : 0D;
						double spreadZ =
								face.getZOffset() == 0 ? server.rand.nextGaussian() * 0.22D : 0D;
						double speed = 0.32D + server.rand.nextDouble() * 0.18D;
						server.spawnParticle(
								EnumParticleTypes.CLOUD,
								true,
								marker.getX() + 0.5D + face.getXOffset() * 0.57D + spreadX,
								marker.getY() + 0.5D + face.getYOffset() * 0.57D + spreadY,
								marker.getZ() + 0.5D + face.getZOffset() * 0.57D + spreadZ,
								0,
								face.getXOffset() * speed + spreadX * 0.2D,
								face.getYOffset() * speed + spreadY * 0.2D,
								face.getZOffset() * speed + spreadZ * 0.2D,
								1.0D);
					}
				}
			}
		}
		int visualInterval = r.destabilizerStage >= 2 && r.destabilizerStage <= 6 ? 2 : 10;
		if ((visualTransition || world.getTotalWorldTime() % visualInterval == 0)
				&& world instanceof WorldServer) {
			StfrLogicPacket.CHANNEL.sendToAllAround(
					new StfrRotorSyncPacket(
							r.center,
							r.rotorAngleUnits * 0.001D,
							r.rotorVelocity,
							r.runState.ordinal(),
							r.stateTicks,
							r.emergencyState() ? r.emergency.recoveryTicks() : r.failure.recoveryTicks(),
							r.unstable(),
							r.core.exists(),
							r.electrodeMask(),
							r.digammaActive,
							r.destabilizerStage,
							r.destabilizerTicks,r.emergency.selfChargeFraction()),
					new NetworkRegistry.TargetPoint(
							world.provider.getDimension(),
							r.center.getX(),
							r.center.getY(),
							r.center.getZ(),
							160));
			r.lastSentVisualState = r.runState;
		}
	}

	static Map<Long, Boolean> electrodeTargets(StfrReactor r) {
		Map<Long, Boolean> targets = new HashMap<>();
		for (StfrElectrodes.Chain chain : StfrElectrodes.chains())
			targets.put(chain.electrode.localPos.toLong(), chain.index < 0);
		return targets;
	}

	static void syncElectrodes(StfrReactor r, World world, boolean active) {
		BlockPos origin = r.center.subtract(StfrStructureTemplate.CENTER);
		if (!world.isAreaLoaded(origin, origin.add(28, 28, 28))) return;
		Map<Long, Boolean> targets = active ? StfrRotorController.electrodeTargets(r) : Collections.emptyMap();
		for (Map.Entry<Long, IBlockState> entry : new HashMap<>(r.aimedElectrodes).entrySet()) {
			if (targets.containsKey(entry.getKey())) continue;
			BlockPos pos = origin.add(BlockPos.fromLong(entry.getKey()));
			if (world.getBlockState(pos).getBlock() == STFRBlocks.ELECTRODE_PROXY)
				world.setBlockState(pos, entry.getValue(), 3);
			r.aimedElectrodes.remove(entry.getKey());
		}
		if (!active) return;
		for (Map.Entry<Long, Boolean> entry : targets.entrySet()) {
			BlockPos pos = origin.add(BlockPos.fromLong(entry.getKey()));
			IBlockState state = world.getBlockState(pos);
			if (state.getBlock() == STFRBlocks.ELECTRODE_PROXY) {
				TileEntity tile = world.getTileEntity(pos);
				if (tile instanceof TileEntityElectrodeProxy) {
					TileEntityElectrodeProxy proxy = (TileEntityElectrodeProxy) tile;
					if (proxy.original() != null)
						r.aimedElectrodes.put(entry.getKey(), proxy.original());
					else if (r.aimedElectrodes.containsKey(entry.getKey()))
						proxy.configure(
								r.aimedElectrodes.get(entry.getKey()), r.center, entry.getValue());
				}
				continue;
			}
			if (state.getBlock() == Blocks.AIR || state.getBlock().hasTileEntity(state)) {
				r.aimedElectrodes.remove(entry.getKey());
				continue;
			}
			StfrStructureTemplate.Cell cell =
					StfrStructureTemplate.cellAt(BlockPos.fromLong(entry.getKey()));
			if (cell == null
					|| !StfrStructureValidator.matchesCell(
							cell, cell.defaultRole, state, world, pos)) continue;
			if (world.setBlockState(pos, STFRBlocks.ELECTRODE_PROXY.getDefaultState(), 3)) {
				r.aimedElectrodes.put(entry.getKey(), state);
				TileEntity tile = world.getTileEntity(pos);
				if (tile instanceof TileEntityElectrodeProxy)
					((TileEntityElectrodeProxy) tile).configure(state, r.center, entry.getValue());
			}
		}
	}

	static void ignitionBursts(StfrReactor r, World world, int tick) {
		if (!(world instanceof WorldServer)) return;
		WorldServer server = (WorldServer) world;
		for (int i = 0; i < 4; i++) {
			double angle = (tick * .61D + i * 1.5707963268D);
			double elevation = ((tick + i * 7) % 13 - 6) * .085D;
			double radius = .38D + .17D * Math.sin(tick * .31D + i);
			server.spawnParticle(
					EnumParticleTypes.EXPLOSION_LARGE,
					true,
					r.center.getX() + .5D + Math.cos(angle) * radius,
					r.center.getY() + .5D + elevation,
					r.center.getZ() + .5D + Math.sin(angle) * radius,
					0,
					0D,
					0D,
					0D,
					.03D);
		}
	}
}
