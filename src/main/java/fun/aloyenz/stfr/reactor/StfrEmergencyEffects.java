package fun.aloyenz.stfr.reactor;

import static fun.aloyenz.stfr.reactor.StfrReactor.RunState.*;

import java.util.Random;
import net.minecraft.block.BlockSnow;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import fun.aloyenz.stfr.logic.StfrCoreFlashPacket;
import fun.aloyenz.stfr.reactor.physics.StfrSelfDestructLight;
import fun.aloyenz.stfr.structure.StfrStructureTemplate;

/** 1.12 particles and physical ray consequences; the existing mesh renders the core. */
public final class StfrEmergencyEffects {

	private StfrEmergencyEffects() {}

	public static Vec3d rayDirection(long center, long worldTick, int index) {
		Random random =
				new Random(center ^ worldTick * 0x9E3779B97F4A7C15L ^ index * 0x632BE59BD9B4E019L);
		double y = random.nextDouble() * 2 - 1,
				angle = random.nextDouble() * Math.PI * 2,
				radius = Math.sqrt(1 - y * y);
		return new Vec3d(Math.cos(angle) * radius, y, Math.sin(angle) * radius);
	}

	public static int rays(StfrReactor.RunState state, int tick) {
		if (state == SELF_DESTRUCT && tick >= 3320) return 4;
		if (state == SHUTDOWN_FAILURE && tick >= 4050) return 4;
		if (state == OVERLOAD && tick >= 1200) return 2;
		return 0;
	}

	public static void tick(World w, StfrReactor r, int tick, int recovery) {
		StfrReactor.RunState state = r.runState();
		if (state == OVERLOAD || state == MANUAL_RESTABILIZATION) {
			w.playSound(
					null,
					r.center,
					SoundEvents.ENTITY_ZOMBIE_VILLAGER_CONVERTED,
					SoundCategory.MASTER,
					5,
					2);
			particle(
					w,
					new Vec3d(r.center).add(.5, .5, .5),
					EnumParticleTypes.EXPLOSION_NORMAL,
					6,
					1,
					0);
		}
		if (state == REACTION_LOSS) {
			if (w.rand.nextDouble() < .1
					&& r.failure().allowsPhysical(w, StfrFailureEffects.STRUCTURE))
				freezeCase(w, r);
		}
		if (state == STABILIZER_LOSS) {
			if (tick <= 5 && recovery == 0)
				r.emergency()
						.broadcast(
								w,
								"",
								"Monitoring Error! CODE: x"
										+ (1_000_000 + w.rand.nextInt(9_000_000)),
								5);
			if (tick == 280 && recovery == 0) {
				r.emergency()
						.broadcast(
								w,
								"stfr.stab_loss.restab_options",
								"Primary Shield Stabilization Lost! Stabilizers Are Now Operating"
									+ " In Contingency Mode.",
								1);
				w.playSound(
						null,
						r.center,
						SoundEvents.BLOCK_FIRE_EXTINGUISH,
						SoundCategory.MASTER,
						5,
						.5F);
			}
		}
		if (state == SELF_DESTRUCT) {
			if (StfrSelfDestructLight.ignition(r.emergency().selfFormingOffline(),tick)) {
				StfrCoreFlashPacket.selfDestruct(w,r.center,.25F,
					StfrSelfDestructLight.IGNITION_MS,0);
				w.playSound(
						null, r.center, SoundEvents.BLOCK_NOTE_SNARE, SoundCategory.MASTER, 5, 1);
				particle(
						w,
						new Vec3d(r.center).add(.5, .5, .5),
						EnumParticleTypes.EXPLOSION_NORMAL,
						10,
						1,
						0);
			}
			if (tick >= 2800)
				for (EnumFacing side : EnumFacing.VALUES) {
					Vec3d origin = new Vec3d(r.center.offset(side, 10)).add(.5, .5, .5);
					particle(w, origin, EnumParticleTypes.END_ROD, 8, .8, .1);
				}
			if (tick >= 2980)
				for (EnumFacing side : EnumFacing.VALUES)
					particle(
							w,
							new Vec3d(r.center.offset(side, 9)).add(.5, .5, .5),
							tick >= 3124 ? EnumParticleTypes.LAVA : EnumParticleTypes.FLAME,
							tick >= 3124 ? 2 : 10,
							.7,
							.03);
			if (tick >= 3700 && w.rand.nextDouble() < .1)
				r.emergency()
						.broadcast(
								w,
								"",
								"Monitoring Error! CODE: x"
										+ (1_000_000 + w.rand.nextInt(9_000_000)),
								5);
		}
		if (state == SHUTDOWN_FAILURE
				&& ((tick >= 300 && recovery <= 695) || (recovery >= 695 && recovery <= 990)))
			StfrFailureEffects.compatParticles(
					w, r.center, 2, recovery >= 695 ? 10 : 3, recovery >= 695 ? 0 : 1, 0);
		if (state == SELF_DESTRUCT && tick >= 3320 && tick % 30 == 15)
			StfrFailureEffects.compatParticles(w, r.center, 2, 1, 1, .1);
		if (state == SHUTDOWN_FAILURE && recovery == 0) {
			if (tick >= 4050)
				particle(
						w,
						new Vec3d(r.center).add(.5, .5, .5),
						EnumParticleTypes.END_ROD,
						100,
						.3,
						0);
			if (tick >= 4950) {
				StfrFailureEffects.compatParticles(w, r.center, 0, 10, 3, .18);
				particle(
						w,
						new Vec3d(r.center).add(.5, .5, .5),
						EnumParticleTypes.EXPLOSION_HUGE,
						1,
						0,
						1);
				particle(
						w,
						new Vec3d(r.center).add(.5, .5, .5),
						EnumParticleTypes.FIREWORKS_SPARK,
						100,
						0,
						1);
			}
			if (tick == 5150)
				for (int i = 0; i < 20; i++) r.emergency().sound(w, "charge_up.heavy_startup", 10, 2);
			if (tick == 5150 || tick == 5200 || tick == 5250)
				for (int i = 0; i < 50; i++)
					visualRay(
							w,
							r,
							rayDirection(r.center.toLong(), w.getTotalWorldTime(), i),
							12,
							false);
		}
		if (state == SHUTDOWN_FAILURE && recovery >= 695 && recovery <= 990) {
			w.playSound(
					null,
					r.center,
					SoundEvents.ENTITY_PLAYER_BREATH,
					SoundCategory.MASTER,
					10,
					.5F);
			w.playSound(
					null,
					r.center,
					SoundEvents.ENTITY_PLAYER_BREATH,
					SoundCategory.MASTER,
					10,
					.6F);
		}
		if (state == SHUTDOWN_FAILURE && recovery >= 990 && recovery < 1400) {
			double fade = (1400 - recovery) / 410D;
			Vec3d core = new Vec3d(r.center).add(.5, .5, .5);
			for (int i = 0; i < 18; i++) {
				double y = 9 - ((w.getTotalWorldTime() * .6 + i) % 18);
				double angle = w.getTotalWorldTime() * .25 + i * 2.39996;
				Vec3d point =
						core.add(Math.cos(angle) * .45 * fade, y, Math.sin(angle) * .45 * fade);
				particle(
						w,
						point,
						EnumParticleTypes.FIREWORKS_SPARK,
						Math.max(1, (int) (4 * fade)),
						.12 * fade,
						.08 * fade);
				particle(w, point, EnumParticleTypes.END_ROD, 1, .08 * fade, .03 * fade);
			}
		}
		if (recovery == 0)
			for (int i = 0; i < rays(state, tick); i++)
				visualRay(
						w,
						r,
						rayDirection(r.center.toLong(), w.getTotalWorldTime(), i),
						20,
						state == SELF_DESTRUCT);
	}

	private static void freezeCase(World w, StfrReactor r) {
		for (int x = -6; x <= 6; x++)
			for (int z = -6; z <= 6; z++) {
				boolean top = Math.abs(x) <= 3 && Math.abs(z) <= 3;
				if (!top
						&& !(Math.abs(x) >= 4 && Math.abs(z) <= 3
								|| Math.abs(z) >= 4 && Math.abs(x) <= 3)) continue;
				if (w.rand.nextDouble() >= (top ? .01 : .003)) continue;
				BlockPos p = r.center.add(x, top ? 5 : 1, z);
				IBlockState actual = w.getBlockState(p);
				if (actual.getBlock() == Blocks.SNOW_LAYER) {
					int layers = actual.getValue(BlockSnow.LAYERS);
					if (layers < 8)
						w.setBlockState(p, actual.withProperty(BlockSnow.LAYERS, layers + 1), 3);
				} else if (w.isAirBlock(p) && Blocks.SNOW_LAYER.canPlaceBlockAt(w, p))
					w.setBlockState(p, Blocks.SNOW_LAYER.getDefaultState(), 3);
			}
	}

	private static void visualRay(
			World w, StfrReactor r, Vec3d direction, double range, boolean ignite) {
		Vec3d start = new Vec3d(r.center).add(.5, .5, .5), point = start;
		for (double distance = .2; distance < range; distance += .2) {
			point = start.add(direction.scale(distance));
			BlockPos p = new BlockPos(point);
			if (!w.isBlockLoaded(p)) break;
			if(r.runState()!=SELF_DESTRUCT) {
			particle(
					w,
					point,
					ignite ? EnumParticleTypes.DRAGON_BREATH : EnumParticleTypes.END_ROD,
					1,
					0,
					0);
			}
			IBlockState actual = w.getBlockState(p);
			if (w.isAirBlock(p) || actual.getBlock() == Blocks.FIRE) continue;
			BlockPos local = p.subtract(r.center).add(StfrStructureTemplate.CENTER);
			if (!(r.runState() == SELF_DESTRUCT && (r.stateTicks() < 3360 || r.rotorBlock(local)))
					&& r.destructibleBlock(local)
					&& actual.getBlockHardness(w, p) >= 0
					&& r.failure().allowsPhysical(w, StfrFailureEffects.STRUCTURE)) {
				if (ignite) {
					if (r.failure().allowsPhysical(w, StfrFailureEffects.FIRE))
						w.setBlockState(p, Blocks.FIRE.getDefaultState(), 3);
				} else w.setBlockToAir(p);
			}
			break;
		}
		particle(w, point, EnumParticleTypes.FIREWORKS_SPARK, 2, .05, .02);
	}

	private static void particle(
			World w, Vec3d p, EnumParticleTypes type, int count, double spread, double speed) {
		if (w instanceof WorldServer)
			((WorldServer) w)
					.spawnParticle(type, true, p.x, p.y, p.z, count, spread, spread, spread, speed);
	}
}
