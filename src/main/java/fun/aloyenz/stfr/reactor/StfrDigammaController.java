package fun.aloyenz.stfr.reactor;

import java.util.Random;
import net.minecraft.item.Item;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import fun.aloyenz.stfr.StfrConfig;
import fun.aloyenz.stfr.logic.StfrLogicPacket;
import fun.aloyenz.stfr.logic.StfrShakePacket;
import fun.aloyenz.stfr.reactor.StfrReactor.RunState;
import fun.aloyenz.stfr.sound.StfrSceneSounds;

final class StfrDigammaController {

	private StfrDigammaController() {}

	static void tickDigammaSettling(StfrReactor r) {
		if (!r.digammaActive || r.digammaSettlingTicks <= 0 || r.runState != RunState.ONLINE)
			return;
		r.digammaSettlingTicks--;
		if (r.autopilotActive) {
			r.digammaSettlingControl = new StfrDigammaSettlingControl();
			return;
		}
		if (r.digammaSettlingTicks % 20 != 0) return;
		StfrDigammaSettlingControl.Settings settings =
				r.digammaSettlingControl.tick(
						r.core.coreTemperature(),
						r.core.pressure(),
						r.suppression,
						r.fuelInputLevel,
						r.permeability,
						r.vent,
						r.savedDigammaFuel,
						r.savedDigammaPermeability,
						r.savedDigammaVent);
		r.suppression = settings.suppression;
		r.fuelInputLevel = settings.fuel;
		r.permeability = settings.permeability;
		r.vent = settings.vent;
	}

	static void destabilizerHeartbeat(StfrReactor r, World world, BlockPos pos) {
		r.destabilizerHeartbeat = world.getTotalWorldTime();
	}

	static boolean destabilizerPulse(StfrReactor r, World world) {
		if (r.destabilizerStage == 4) {
			r.destabilizerStage = 5;
			r.destabilizerTicks = 0;
			r.eventSound(world, StfrSceneSounds.get("alarms.beep2"), SoundCategory.MASTER, 8F, 1F);
			StfrAnnouncementController.announceDigamma(r, world, "digamma.final");
			world.playSound(
					null,
					r.center,
					StfrSceneSounds.get("charge_up.mega_chargeup"),
					SoundCategory.MASTER,
					24F,
					1F);
			StfrLogicPacket.CHANNEL.sendToAllAround(
					new StfrShakePacket(r.center, 460, 60, 1),
					new NetworkRegistry.TargetPoint(
							world.provider.getDimension(),
							r.center.getX(),
							r.center.getY(),
							r.center.getZ(),
							160));
			return true;
		}
		if (r.destabilizerStage != 0
				|| r.digammaActive
				|| r.runState != RunState.ONLINE
				|| r.hasWarnings()
				|| r.workingTurbines() != 6) return false;
		BlockPos poweredDestabilizer = StfrDestabilizer.poweredPosition(world, r);
		if (poweredDestabilizer == null) return false;
		StfrAutopilot.crashForDestabilizer(world, r);
		StfrDigammaController.ensureResonance(r, world);
		r.savedDigammaFuel = r.fuelInputLevel;
		r.savedDigammaSuppression = r.suppression;
		r.savedDigammaOffset = r.offset;
		r.savedDigammaLasers = r.lasers;
		r.savedDigammaVent = r.vent;
		r.savedDigammaPermeability = r.permeability;
		r.armedDestabilizerPos = poweredDestabilizer;
		r.destabilizerStage = 1;
		r.destabilizerTicks = 0;
		r.destabilizerHeartbeat = world.getTotalWorldTime();
		r.LOG.info(
				"STFR digamma destabilizer armed at {}: resonance temperature={}, pressure={},"
					+ " warnings={}",
				r.center,
				Math.round(r.resonanceTemperature),
				Math.round(r.resonancePressure),
				r.warningFlags);
		r.eventSound(world, StfrSceneSounds.get("alarms.beep2"), SoundCategory.MASTER, 8F, 1F);
		return true;
	}

	static void ensureResonance(StfrReactor r, World world) {
		if (r.resonanceTemperature > 0 && r.resonancePressure > 0) return;
		Random random =
				new Random(
						r.id.getMostSignificantBits()
								^ r.id.getLeastSignificantBits()
								^ world.getSeed());
		r.resonanceTemperature = 101_000_000D + random.nextInt(18_000_001);
		r.resonancePressure = 3_100_000D + random.nextInt(1_200_001);
	}

	static void tickDestabilizer(StfrReactor r, World world) {
		if (r.destabilizerStage == 0 || r.destabilizerStage == 6) return;
		r.destabilizerTicks++;
		boolean powered = StfrDestabilizer.powered(world, r, r.armedDestabilizerPos);
		if (r.runState != RunState.ONLINE || (!powered && r.destabilizerStage < 5)) {
			r.LOG.warn(
					"STFR digamma destabilizer aborted at {}: stage={}, state={}, powered={},"
						+ " warnings={}, shield={}, stress={}",
					r.center,
					r.destabilizerStage,
					r.runState,
					powered,
					r.warningFlags,
					r.core.shield(),
					r.core.shieldStress());
			StfrDigammaController.abortDestabilizer(r, world);
			return;
		}
		if (r.destabilizerStage == 1) {
			if (r.destabilizerTicks == 20) {
				r.destabilizerStage = 2;
				r.destabilizerTicks = 0;
				StfrAnnouncementController.announceDigamma(r, world, "digamma.search");
			}
			return;
		}
		if (r.destabilizerStage >= 2 && r.destabilizerStage <= 4) {
			long available = r.resources.energy();
			if (r.resources.takeEnergy(5_000_000L) < 5_000_000L) {
				r.LOG.warn(
						"STFR digamma destabilizer lost power at {}: stage={}, available={} HE,"
								+ " required=5000000 HE/t",
						r.center,
						r.destabilizerStage,
						available);
				StfrDigammaController.abortDestabilizer(r, world);
				return;
			}
			double compression = Math.min(1D, r.destabilizerTicks / 400D);
			r.core.setCompressionPressure(r.resonancePressure, compression);
			double delta = r.resonanceTemperature - r.core.coreTemperature();
			// Part of the fixed 5 MHE/t load is the continuous work needed to
			// keep compressing a non-reactive, ultra-hot plasma. It remains a
			// physical heat input after the target pressure itself has settled.
			r.core.applyCompressionHeating(r.resonanceTemperature, 300_000D * compression);
			StfrDigammaController.controlDigammaResonance(r, delta, r.resonancePressure - r.core.pressure());
			if (r.destabilizerTicks % 100 == 0)
				r.LOG.info(
						"STFR digamma seek at {}: temperature={}/{}, pressure={}/{},"
							+ " casePressure={}, fuel={}, suppress={}, lasers={}, permeability={},"
							+ " vent={}, shield={}, stress={}",
						r.center,
						Math.round(r.core.coreTemperature()),
						Math.round(r.resonanceTemperature),
						Math.round(r.core.pressure()),
						Math.round(r.resonancePressure),
						Math.round(r.core.casePressure()),
						r.fuelInputLevel,
						r.suppression,
						r.lasers,
						r.permeability,
						r.vent,
						r.core.shield(),
						r.core.shieldStress());
			if (r.resources.lubricant().getFill() > 0) r.resources.take(r.resources.lubricant(), 1);
			else if (r.destabilizerTicks >= 440 && (r.destabilizerTicks - 440) % 20 == 0) {
				int broken = (r.destabilizerTicks - 440) / 20;
				if (broken < 6) r.ejectSelfDestructRotor(world, EnumFacing.VALUES[broken]);
			}
			if (r.destabilizerTicks == 20)
				world.playSound(
						null,
						r.center,
						StfrSceneSounds.get("charge_up.heavy_startup"),
						SoundCategory.MASTER,
						20F,
						1F);
			boolean resonant =
					Math.abs(r.core.coreTemperature() - r.resonanceTemperature) <= 1_000_000D
							&& Math.abs(r.core.pressure() - r.resonancePressure) <= 100_000D;
			if (r.destabilizerStage == 2 && resonant) {
				r.destabilizerStage = 3;
				r.LOG.info(
						"STFR digamma resonance acquired at {}: temperature={}, pressure={},"
							+ " shield={}, stress={}",
						r.center,
						Math.round(r.core.coreTemperature()),
						Math.round(r.core.pressure()),
						r.core.shield(),
						r.core.shieldStress());
				StfrAnnouncementController.announceDigamma(r, world, "digamma.resonance");
			} else if (r.destabilizerStage == 3) {
				long charged =
						r.resources.takeEnergy(
								Math.min(
										3_000_000L,
										(long) Math.ceil(100_000_000D - r.digammaCharge)));
				r.digammaCharge += charged;
				if (r.digammaCharge >= 100_000_000D) {
					r.digammaCharge = 100_000_000D;
					r.destabilizerStage = 4;
					r.LOG.info("STFR digamma charge ready at {}", r.center);
					StfrAnnouncementController.announceDigamma(r, world, "digamma.ready");
				}
			}
			return;
		}
		if (r.destabilizerStage == 5) {
			// The field pressure during resonance includes externally driven
			// compression. Refill the plasma itself while clearing old gas
			// from the case, or it loses rigidity when the field is released.
			r.fuelInputLevel = r.destabilizerRefillLevel(r.core.intrinsicPressure());
			r.suppression = 100;
			r.lasers = 0;
			r.permeability = 0;
			r.vent = 100;
			r.resources.beginOutputTick(0);
			r.generatedPower = 0;
			if (r.destabilizerTicks >= 460) StfrDigammaController.completeDigammaTransition(r, world);
		}
	}

	static void controlDigammaResonance(
			StfrReactor r, double temperatureError, double pressureError) {
		int targetFuel, targetSuppression, targetLasers, targetPermeability, targetVent;
		if (temperatureError > 5_000_000D) {
			targetFuel = 100;
			targetSuppression = 0;
			targetLasers = 100;
			targetPermeability = 0;
			targetVent = 0;
		} else if (temperatureError > 1_000_000D) {
			targetFuel = 80;
			targetSuppression = 20;
			targetLasers = 70;
			targetPermeability = 0;
			targetVent = 0;
		} else if (temperatureError >= -1_000_000D) {
			double position = (temperatureError + 1_000_000D) / 2_000_000D;
			targetFuel = (int) Math.round(20D + position * 40D);
			targetSuppression = (int) Math.round(80D - position * 40D);
			targetLasers = (int) Math.round(position * 35D);
			targetPermeability = 0;
			targetVent = 0;
		} else {
			targetFuel = 0;
			targetSuppression = 100;
			targetLasers = 0;
			targetPermeability = 35;
			targetVent = 35;
		}
		// Above the resonance pressure, additional fuel only accumulates: at
		// these temperatures its reactivity is already effectively zero.
		if (pressureError < -100_000D) {
			targetFuel = 0;
			targetPermeability = pressureError < -300_000D ? 70 : 35;
			targetVent = targetPermeability;
		} else if (pressureError < 100_000D) {
			targetFuel = Math.min(targetFuel, 20);
		}
		// Case gas does not help core resonance. Vent it before the random
		// overpressure blowout can break the control network during stage 4.
		targetVent = r.resonanceVentTarget(r.core.casePressure(), targetVent);
		r.fuelInputLevel = r.approach(r.fuelInputLevel, targetFuel, 2);
		r.suppression = r.approach(r.suppression, targetSuppression, 2);
		r.lasers = r.approach(r.lasers, targetLasers, 2);
		r.permeability = r.approach(r.permeability, targetPermeability, 2);
		r.vent = r.approach(r.vent, targetVent, 2);
	}

	static void abortDestabilizer(StfrReactor r, World world) {
		r.core.setShieldOverdrive(false);
		r.core.setCompressionPressure(0, 0);
		r.core.loseShieldFraction(.35D);
		StfrDigammaController.restoreDigammaControls(r);
		r.destabilizerStage = 0;
		r.destabilizerTicks = 0;
		r.digammaCharge = 0;
		r.armedDestabilizerPos = null;
		StfrAnnouncementController.announceDigamma(r, world, "digamma.abort");
	}

	static void completeDigammaTransition(StfrReactor r, World world) {
		r.core.setShieldOverdrive(false);
		r.digammaActive = true;
		r.destabilizerStage = 6;
		r.destabilizerTicks = 0;
		r.digammaCharge = 0;
		r.armedDestabilizerPos = null;
		r.core.setCompressionPressure(0, 0);
		r.core.eventTemperature(StfrCoreSimulation.CORE_TEMP_NOMINAL);
		StfrDigammaController.restoreDigammaControls(r);
		r.suppression = r.digammaSuppression(r.savedDigammaSuppression, r.poloniumMb());
		r.digammaSettlingTicks = 1_000;
		r.digammaSettlingControl = new StfrDigammaSettlingControl();
		StfrDigammaController.destroyDigammaGlass(r, world);
		world.playSound(
				null,
				r.center,
				StfrSceneSounds.get("explosion.shieldboom"),
				SoundCategory.MASTER,
				24F,
				.8F);
		StfrLogicPacket.CHANNEL.sendToAllAround(
				new StfrShakePacket(r.center, 140, 100, 30),
				new NetworkRegistry.TargetPoint(
						world.provider.getDimension(),
						r.center.getX(),
						r.center.getY(),
						r.center.getZ(),
						160));
		r.particles(world, EnumParticleTypes.EXPLOSION_HUGE, r.center, 24, 5, 5, 5, .2);
		for (int component = 1; component <= 6; component++)
			r.maintenance.simulate(world, component);
	}

	static void restoreDigammaControls(StfrReactor r) {
		r.fuelInputLevel = r.savedDigammaFuel;
		r.suppression = r.savedDigammaSuppression;
		r.offset = r.savedDigammaOffset;
		r.lasers = r.savedDigammaLasers;
		r.vent = r.savedDigammaVent;
		r.permeability = r.savedDigammaPermeability;
	}

	static void tickDigammaRadiation(StfrReactor r, World world) {
		StfrDigammaContainment.tickDigammaRadiation(r, world);
	}

	static void tickDigammaSoundscape(StfrReactor r, World world) {
		StfrDigammaContainment.tickDigammaSoundscape(r, world);
	}

	static boolean hasBrokenPolaroidNearby(StfrReactor r, World world) {
		return StfrDigammaContainment.hasBrokenPolaroidNearby(r, world);
	}

	static void playDigammaScream(StfrReactor r, World world, boolean brokenPolaroid) {
		StfrDigammaContainment.playDigammaScream(r, world, brokenPolaroid);
	}

	static void tickDigammaChemistry(StfrReactor r, World world) {
		if (!r.digammaActive) return;
		StfrDigammaController.exportDigammaProduct(r);
		r.digammaMatter.feedLegacyReserve();
		r.digammaMatter.convert(r.core.reactionPower());
		StfrDigammaController.exportDigammaProduct(r);
	}

	static void exportDigammaProduct(StfrReactor r) {
		r.digammaMatter.feedLegacyYaronite();
		int space = r.resources.yaronite().getMaxFill() - r.resources.yaronite().getFill();
		int inCore = r.digammaMatter.takeYaronite(space);
		if (inCore > 0) r.resources.add(r.resources.yaronite(), inCore);
	}

	static boolean insertPoloniumBillet(StfrReactor r) {
		return r.insertPolonium(96) == 96;
	}

	static int insertPolonium(StfrReactor r, int requested) {
		if (requested <= 0 || StfrConfig.maxCorePoloniumMb - r.poloniumMb() < requested) return 0;
		return (int) Math.round(r.insertPolonium((double) requested));
	}

	static double insertPolonium(StfrReactor r, double requested) {
		return r.digammaMatter.acceptPolonium(requested);
	}

	static int missingDigammaGlass(StfrReactor r, World world) {
		return StfrDigammaContainment.missingDigammaGlass(r, world);
	}

	static void destroyDigammaGlass(StfrReactor r, World world) {
		StfrDigammaContainment.destroyDigammaGlass(r, world);
	}

	static void finishDigammaShutdown(StfrReactor r, World world) {
		StfrDigammaContainment.finishDigammaShutdown(r, world);
	}

	static void scatterDigammaProduct(StfrReactor r, World world, Item item, int count) {
		StfrDigammaContainment.scatterDigammaProduct(r, world, item, count);
	}
}
