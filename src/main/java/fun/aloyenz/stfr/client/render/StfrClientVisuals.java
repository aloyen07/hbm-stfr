package fun.aloyenz.stfr.client.render;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleRedstone;
import net.minecraft.entity.Entity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import fun.aloyenz.stfr.StfrClientEffectsConfig;
import fun.aloyenz.stfr.Tags;
import fun.aloyenz.stfr.api.client.StfrCoreLight;
import fun.aloyenz.stfr.reactor.StfrChamberLight;
import fun.aloyenz.stfr.reactor.StfrReactor;
import fun.aloyenz.stfr.reactor.physics.StfrMagneticCollapse;
import fun.aloyenz.stfr.reactor.physics.StfrRotorRunout;
import fun.aloyenz.stfr.reactor.physics.StfrRotorVibration;
import fun.aloyenz.stfr.reactor.physics.StfrSelfDestructLight;
import fun.aloyenz.stfr.reactor.physics.StfrSelfDestructPalette;
import fun.aloyenz.stfr.reactor.physics.StfrShakeMixer;
import fun.aloyenz.stfr.reactor.physics.StfrStartupFailureLight;
import fun.aloyenz.stfr.structure.StfrElectrodes;

/** Client render state keyed by reactor center, independent of any controller tile. */
@Mod.EventBusSubscriber(modid = Tags.MODID, value = Side.CLIENT)
public final class StfrClientVisuals {

	private static final Map<Long, Snapshot> STATES = new HashMap<>();
	private static World currentWorld;
	private static final Map<Long, StfrShakeMixer> SHAKES = new HashMap<>();
	private static World shakeWorld;
	private static final Map<Long, CoreRemnant> REMNANTS = new HashMap<>();

	private static final class CoreRemnant extends ParticleRedstone {

		CoreRemnant(World world, BlockPos center, int life) {
			super(
					world,
					center.getX() + .5,
					center.getY() + .5,
					center.getZ() + .5,
					.35F,
					1F,
					0F,
					0F);
			motionX = motionY = motionZ = 0;
			setMaxAge(Math.max(1, life));
		}
	}

	@SubscribeEvent
	public static void remnantTick(TickEvent.ClientTickEvent event) {
		if (event.phase != TickEvent.Phase.END) return;
		World world = Minecraft.getMinecraft().world;
		REMNANTS.entrySet()
				.removeIf(
						entry -> {
							Snapshot snapshot = STATES.get(entry.getKey());
							double recovery =
									snapshot == null || world == null
											? 0
											: snapshot.recoveryTicks
													+ Math.max(
															0,
															world.getTotalWorldTime()
																	- snapshot.time);
							boolean active =
									world != null
											&& currentWorld == world
											&& snapshot != null
											&& snapshot.runState
													== StfrReactor.RunState.SHUTDOWN_FAILURE
															.ordinal()
											&& recovery >= 990
											&& recovery < 1400
											&& world.getTotalWorldTime() - snapshot.time <= 80;
							if (!active) entry.getValue().setExpired();
							return !active;
						});
		if (world == null || currentWorld != world) return;
		for (Map.Entry<Long, Snapshot> entry : STATES.entrySet()) {
			Snapshot snapshot = entry.getValue();
			double recovery =
					snapshot.recoveryTicks + Math.max(0, world.getTotalWorldTime() - snapshot.time);
			if (snapshot.runState != StfrReactor.RunState.SHUTDOWN_FAILURE.ordinal()
					|| recovery < 990
					|| recovery >= 1400
					|| world.getTotalWorldTime() - snapshot.time > 80
					|| REMNANTS.containsKey(entry.getKey())) continue;
			CoreRemnant particle =
					new CoreRemnant(
							world,
							BlockPos.fromLong(entry.getKey()),
							(int) Math.ceil(1400 - recovery));
			REMNANTS.put(entry.getKey(), particle);
			Minecraft.getMinecraft().effectRenderer.addEffect(particle);
		}
	}

	@SubscribeEvent
	public static void disassemblyTick(TickEvent.ClientTickEvent event) {
		if (event.phase!=TickEvent.Phase.END || Minecraft.getMinecraft().isGamePaused()) return;
		World world=Minecraft.getMinecraft().world;
		StfrDisassemblyVisuals.tick(world);
		if (world==null || currentWorld!=world) return;
		for (Map.Entry<Long,Snapshot> entry:STATES.entrySet()) {
			Snapshot state=entry.getValue();
			if (world.getTotalWorldTime()-state.time>80) continue;
			double elapsed=world.getTotalWorldTime()-state.time;
			boolean shutdown=state.runState==StfrReactor.RunState.SHUTDOWN_FAILURE.ordinal()
					&& state.recoveryTicks==0 && state.ticks+elapsed>=4950 && state.ticks+elapsed<5300;
			if(state.runState==StfrReactor.RunState.SELF_DESTRUCT.ordinal() && state.ticks+elapsed>=3320)
				StfrDisassemblyVisuals.emitTracks(world,BlockPos.fromLong(entry.getKey()));
			if (state.runState==StfrReactor.RunState.MELTDOWN.ordinal() || shutdown)
				StfrDisassemblyVisuals.emit(world,BlockPos.fromLong(entry.getKey()),
						shutdown && state.ticks+elapsed>=5150);
		}
	}

	public static void shake(BlockPos pos, int ticks, int frequency, int magnitude) {
		shake(pos,ticks,frequency,magnitude,false);
	}

	public static void shake(BlockPos pos, int ticks, int frequency, int magnitude, boolean sustained) {
		World world=Minecraft.getMinecraft().world;
		if (world==null) return;
		if (shakeWorld!=world) { SHAKES.clear(); shakeWorld=world; }
		SHAKES.computeIfAbsent(pos.toLong(), key -> new StfrShakeMixer())
				.add(world.getTotalWorldTime(),ticks,frequency,magnitude,sustained);
	}

	public static void fadeShake(BlockPos pos, int ticks) {
		World world=Minecraft.getMinecraft().world;
		StfrShakeMixer mixer=SHAKES.get(pos.toLong());
		if (world!=null && world==shakeWorld && mixer!=null)
			mixer.fadeBackground(world.getTotalWorldTime(),ticks);
	}

	private static double shakeGain(World world,long center) {
		Snapshot state=currentWorld==world ? STATES.get(center) : null;
		return state!=null && state.runState==StfrReactor.RunState.SHUTDOWN_FAILURE.ordinal()
				&& state.recoveryTicks>0 ? .5 : 1;
	}

	@SubscribeEvent
	public static void camera(EntityViewRenderEvent.CameraSetup event) {
		World world = Minecraft.getMinecraft().world;
		if (world == null) {
			SHAKES.clear();
			return;
		}
		if (shakeWorld!=world) { SHAKES.clear(); shakeWorld=world; }
		SHAKES.values().removeIf(mixer -> mixer.prune(world.getTotalWorldTime()));
		double pitch=0,yaw=0,roll=0,impact=0;
		for (Map.Entry<Long,StfrShakeMixer> entry:SHAKES.entrySet()) {
			StfrShakeMixer.Sample sample=entry.getValue().sample(
					world.getTotalWorldTime()+event.getRenderPartialTicks(),shakeGain(world,entry.getKey()));
			pitch+=sample.pitch; yaw+=sample.yaw; roll+=sample.roll;
			impact=Math.max(impact,sample.impact);
		}
		Entity camera = Minecraft.getMinecraft().getRenderViewEntity();
		if (currentWorld == world && camera != null) {
			double weight = StfrRotorVibration.backgroundWeight(impact);
			for (Map.Entry<Long, Snapshot> entry : STATES.entrySet()) {
				Snapshot s = entry.getValue();
				if (world.getTotalWorldTime() - s.time > 80) continue;
                if(s.runState==StfrReactor.RunState.REACTION_LOSS.ordinal()
                        || s.runState==StfrReactor.RunState.UNDERLOAD.ordinal()) {
                    double t=s.ticks+world.getTotalWorldTime()-s.time+event.getRenderPartialTicks();
                    if(s.runState==StfrReactor.RunState.UNDERLOAD.ordinal() || t>=1780) {
                        double distance=Math.sqrt(camera.getDistanceSqToCenter(BlockPos.fromLong(entry.getKey())));
                        double gain=.065*Math.max(0,1-distance/70);
                        double phase=(world.getTotalWorldTime()+event.getRenderPartialTicks())*2.9+(entry.getKey()&255);
                        pitch+=(Math.sin(phase)+.3*Math.sin(phase*1.71))*gain;
                        yaw+=Math.sin(phase*1.31)*gain*.3;
                    }
                    continue;
                }
                double speed = s.velocity;
				if (s.runState == StfrReactor.RunState.SHUTDOWN_FAILURE.ordinal()
						&& s.recoveryTicks >= 990) {
					double elapsed = world.getTotalWorldTime() - s.time + event.getRenderPartialTicks();
					speed *= Math.max(0, 1400 - s.recoveryTicks - elapsed)
							/ Math.max(1D, 1400 - s.recoveryTicks);
				}
				BlockPos center = BlockPos.fromLong(entry.getKey());
				double amplitude = weight * shakeGain(world,entry.getKey()) * StfrRotorVibration.amplitude(speed,
						Math.sqrt(camera.getDistanceSqToCenter(center)));
				double phase = StfrRotorVibration.phase(speed,
						world.getTotalWorldTime() + event.getRenderPartialTicks())
						+ (entry.getKey() & 255);
				pitch += Math.sin(phase) * amplitude;
				yaw += Math.sin(phase * 1.37) * amplitude * .65;
				roll += Math.sin(phase * .73) * amplitude * .4;
			}
		}
		event.setPitch(event.getPitch() + (float) pitch);
		event.setYaw(event.getYaw() + (float) yaw);
		event.setRoll(event.getRoll() + (float) roll);
	}

    public static double magneticTick(BlockPos center,float partial) {
        World world=Minecraft.getMinecraft().world;
        Snapshot snapshot=STATES.get(center.toLong());
        return world==null || world!=currentWorld || snapshot==null
                || snapshot.runState!=StfrReactor.RunState.UNDERLOAD.ordinal()
                || world.getTotalWorldTime()-snapshot.time>80 ? -1
                : snapshot.ticks+world.getTotalWorldTime()-snapshot.time+partial;
    }

    private StfrClientVisuals() {}

	public static void update(
			World world,
			BlockPos center,
			double angle,
			int velocity,
			int runState,
			int ticks,
			int recoveryTicks,
			boolean unstable,
			boolean coreExists,
			int electrodeMask,
			boolean digamma,
			int destabilizerStage,
			int destabilizerTicks,float selfCharge) {
		if (currentWorld != world) {
			STATES.clear();
			currentWorld = world;
		}
		STATES.put(
				center.toLong(),
				new Snapshot(
						angle,
						velocity,
						runState,
						ticks,
						recoveryTicks,
						unstable,
						coreExists,
						electrodeMask,
						digamma,
						destabilizerStage,
						destabilizerTicks,selfCharge,
						world.getTotalWorldTime()));
	}

	public static double angle(World world, BlockPos center, float partialTicks) {
		if (currentWorld != world) return 0;
		Snapshot snapshot = STATES.get(center.toLong());
		if (snapshot == null) return 0;
		double elapsed = Math.max(0, world.getTotalWorldTime() - snapshot.time) + partialTicks;
		if (snapshot.runState == StfrReactor.RunState.SHUTDOWN_FAILURE.ordinal()
				&& snapshot.recoveryTicks >= 990) {
			double remaining = Math.max(1, 1400 - snapshot.recoveryTicks);
			elapsed = Math.min(elapsed, remaining);
			return snapshot.angle
					+ snapshot.velocity * .001D * (elapsed - elapsed * elapsed / (2 * remaining));
		}
		return snapshot.angle + elapsed * snapshot.velocity * .001D;
	}

	public static StfrRotorRunout.Sample rotorRunout(World world, BlockPos center, float partialTicks, EnumFacing axis) {
		Snapshot s = currentWorld == world ? STATES.get(center.toLong()) : null;
		if (s == null
				|| s.runState != StfrReactor.RunState.SHUTDOWN_FAILURE.ordinal()
				|| s.coreExists
				|| s.recoveryTicks < 990
				|| s.recoveryTicks >= 1400) return StfrRotorRunout.sample(0,0,0,0);
		double t = s.recoveryTicks + Math.max(0, world.getTotalWorldTime() - s.time) + partialTicks;
		double speed =
				s.recoveryTicks >= 990
						? s.velocity * Math.max(0, 1400 - t) / Math.max(1, 1400 - s.recoveryTicks)
						: s.velocity;
		return StfrRotorRunout.sample(t,angle(world,center,partialTicks),speed,axis.ordinal());
	}

	/** Current client data, independent of whether built-in rendering is enabled. */
	public static List<StfrCoreLight> coreLights(float partial) {
		List<StfrCoreLight> result=new ArrayList<>(StfrFakeCoreVisuals.lights(partial));
		result.addAll(StfrCoreFlashes.lights(partial));
		World world=Minecraft.getMinecraft().world;
		if(world==null || currentWorld!=world)return result;
		for(Map.Entry<Long,Snapshot> entry:STATES.entrySet()) {
			Snapshot s=entry.getValue();
            if(world.getTotalWorldTime()-s.time>80)continue;
            if(s.runState==StfrReactor.RunState.REACTION_LOSS.ordinal() || s.runState==StfrReactor.RunState.UNDERLOAD.ordinal()) {
                double t=s.ticks+world.getTotalWorldTime()-s.time+partial;
                StfrReactor.RunState mode=StfrReactor.RunState.values()[s.runState];
                result.add(new StfrCoreLight(world.provider.getDimension(),BlockPos.fromLong(entry.getKey()),
                        mode,t,(float)StfrMagneticCollapse.light(mode,t)*StfrClientEffectsConfig.intensity,
                        StfrClientEffectsConfig.radius,false,StfrMagneticCollapse.exposurePulse(mode,t),mode==StfrReactor.RunState.UNDERLOAD?1F:.25F,mode==StfrReactor.RunState.UNDERLOAD?1F:.85F,1F));
                continue;
            }
            if(s.runState==StfrReactor.RunState.STARTUP_FAILURE.ordinal()) {
                double t=s.ticks+world.getTotalWorldTime()-s.time+partial;
                float power=StfrStartupFailureLight.surface(t,s.recoveryTicks);
                if(power>0)result.add(new StfrCoreLight(world.provider.getDimension(),BlockPos.fromLong(entry.getKey()),
                    StfrReactor.RunState.STARTUP_FAILURE,t,power*StfrClientEffectsConfig.intensity,
                    StfrClientEffectsConfig.radius,false,false,.57F,.09F,1F));
                continue;
            }
            if(!s.coreExists)continue;
			if(s.runState==StfrReactor.RunState.SELF_DESTRUCT.ordinal()) {
				double t=s.ticks+world.getTotalWorldTime()-s.time+partial;
				float power=StfrSelfDestructLight.intensity(t,s.selfCharge,s.coreExists);
				StfrSelfDestructPalette.Color color=StfrSelfDestructPalette.color(t,s.selfCharge);
				if(power>0)result.add(new StfrCoreLight(world.provider.getDimension(),BlockPos.fromLong(entry.getKey()),
					StfrReactor.RunState.SELF_DESTRUCT,t,power*StfrClientEffectsConfig.intensity,StfrClientEffectsConfig.radius,false,false,color.red,color.green,color.blue));
				continue;
			}
			if(s.runState!=StfrReactor.RunState.MELTDOWN.ordinal() && s.runState!=StfrReactor.RunState.DETONATION.ordinal())continue;
			result.add(new StfrCoreLight(world.provider.getDimension(),BlockPos.fromLong(entry.getKey()),
				StfrReactor.RunState.values()[s.runState],s.ticks+world.getTotalWorldTime()-s.time+partial,
				StfrClientEffectsConfig.intensity,StfrClientEffectsConfig.radius));
		}
		return result;
	}

	@SubscribeEvent
	public static void render(RenderWorldLastEvent event) {
        try (StfrEmissiveState emission = new StfrEmissiveState()) {
		Minecraft mc = Minecraft.getMinecraft();
		World world = mc.world;
		Entity camera = mc.getRenderViewEntity();
		if (world == null || camera == null) return;
		if (currentWorld != world) {
			STATES.clear();
			currentWorld = world;
			return;
		}
		float partial = event.getPartialTicks();
		double cx = camera.lastTickPosX + (camera.posX - camera.lastTickPosX) * partial;
		double cy = camera.lastTickPosY + (camera.posY - camera.lastTickPosY) * partial;
		double cz = camera.lastTickPosZ + (camera.posZ - camera.lastTickPosZ) * partial;
		STATES.entrySet().removeIf(entry -> world.getTotalWorldTime() - entry.getValue().time > 80);
		for (Map.Entry<Long, Snapshot> entry : STATES.entrySet()) {
			BlockPos center = BlockPos.fromLong(entry.getKey());
			if (center.distanceSq(cx, cy, cz) > 192D * 192D) continue;
			Snapshot state = entry.getValue();
			boolean failure = state.runState == StfrReactor.RunState.STARTUP_FAILURE.ordinal();
			boolean meltdown = state.runState == StfrReactor.RunState.MELTDOWN.ordinal();
			boolean emergency = failure || meltdown;
			if(state.runState==StfrReactor.RunState.SELF_DESTRUCT.ordinal() && state.coreExists && state.ticks>=3320) {
				StfrEmergencyLight.renderSelfDestruct(world,center,cx,cy,cz,world.getTotalWorldTime()+partial);
				StfrSelfDestructRays.render(world,center,cx,cy,cz,state.ticks+Math.max(0,world.getTotalWorldTime()-state.time)+partial);
			}
			if(failure)StfrEmergencyLight.startupFailure(world,center,cx,cy,cz,world.getTotalWorldTime()+partial,
					state.ticks+Math.max(0,world.getTotalWorldTime()-state.time)+partial,state.recoveryTicks);
			else if (emergency)
				StfrEmergencyLight.render(
						world, center, cx, cy, cz, world.getTotalWorldTime() + partial, meltdown);
			double elapsed = Math.max(0, world.getTotalWorldTime() - state.time) + partial;
            if(state.runState==StfrReactor.RunState.REACTION_LOSS.ordinal() || state.runState==StfrReactor.RunState.UNDERLOAD.ordinal())
                StfrMagneticVisuals.render(world,center,cx,cy,cz,StfrReactor.RunState.values()[state.runState],state.ticks+elapsed);
            // Keep vanilla terrain occlusion; never turn chamber glass into an opaque depth wall.
			if (!emergency
					&& state.runState >= 0
					&& state.runState < StfrReactor.RunState.OVERLOAD.ordinal())
				StfrVolumetricLight.stable(
						world,
						center,
						cx,
						cy,
						cz,
						world.getTotalWorldTime() + partial,
						StfrChamberLight.formation(
								StfrReactor.RunState.values()[state.runState],
								state.ticks + elapsed),
						state.destabilizerStage,
						state.destabilizerTicks + elapsed);
			if (state.runState >= StfrReactor.RunState.OVERLOAD.ordinal()
					&& state.runState < StfrReactor.RunState.values().length) {
				StfrReactor.RunState mode = StfrReactor.RunState.values()[state.runState];
				if (mode != StfrReactor.RunState.SELF_DESTRUCT || state.coreExists)
					StfrVolumetricLight.contingency(
							world,
							center,
							cx,
							cy,
							cz,
							world.getTotalWorldTime() + partial,
							mode,
							state.ticks,
							state.recoveryTicks);
			}
			if (state.runState >= 0 && state.runState < StfrReactor.RunState.values().length)
				RenderStfrEmergencyEffects.render(
						world,
						center,
						cx,
						cy,
						cz,
						StfrReactor.RunState.values()[state.runState],
						state.ticks,
						state.recoveryTicks > 0 ? state.recoveryTicks + elapsed : 0,
						state.destabilizerStage,
						state.destabilizerTicks + elapsed,
						angle(world, center, partial));
			List<StfrElectrodes.Chain> electrodes = new ArrayList<>();
			for (StfrElectrodes.Chain chain : StfrElectrodes.chains())
				if ((state.electrodeMask & 1 << (chain.direction.ordinal() * 5 + chain.index + 1))
								!= 0
						&& chain.works(world, center)) electrodes.add(chain);
			RenderReactorCore.render(
					center.getX() - cx + .5D,
					center.getY() - cy + .5D,
					center.getZ() - cz + .5D,
					state.runState,
					state.recoveryTicks > 0
									&& state.runState
											== StfrReactor.RunState.STABILIZER_LOSS.ordinal()
							? Math.max(0, state.ticks - 10 * elapsed)
							: state.ticks + (state.recoveryTicks > 0 ? 0 : elapsed),
					world.getTotalWorldTime() + partial,
					state.recoveryTicks > 0 ? state.recoveryTicks + elapsed : 0,
					electrodes,
					state.coreExists,
					state.digamma,
					state.destabilizerStage,state.selfCharge);
			RenderLightningRods.render(
					world,
					center,
					state.runState,
					state.ticks + Math.max(0, world.getTotalWorldTime() - state.time) + partial,
					world.getTotalWorldTime() + partial,
					cx,
					cy,
					cz);
		}

        }
}

	private static final class Snapshot {

		final double angle;
		final int velocity, runState, ticks, recoveryTicks;
		final boolean unstable;
		final boolean coreExists, digamma;
		final int destabilizerStage, destabilizerTicks;
		final int electrodeMask;
		final long time;
		final float selfCharge;

		Snapshot(
				double angle,
				int velocity,
				int runState,
				int ticks,
				int recoveryTicks,
				boolean unstable,
				boolean coreExists,
				int electrodeMask,
				boolean digamma,
				int destabilizerStage,
				int destabilizerTicks,float selfCharge,
				long time) {
			this.angle = angle;
			this.velocity = velocity;
			this.runState = runState;
			this.ticks = ticks;
			this.time = time;
			this.recoveryTicks = recoveryTicks;
			this.unstable = unstable;
			this.coreExists = coreExists;
			this.electrodeMask = electrodeMask;
			this.digamma = digamma;
			this.destabilizerStage = destabilizerStage;
			this.destabilizerTicks = destabilizerTicks;
			this.selfCharge=selfCharge;
		}
	}
}
