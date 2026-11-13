package fun.aloyenz.stfr.reactor;

import com.hbm.entity.effect.EntityCloudFleija;
import com.hbm.entity.effect.EntityNukeTorex;
import com.hbm.entity.logic.EntityNukeExplosionMK3;
import java.util.ArrayList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityFallingBlock;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.play.server.SPacketEntityVelocity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import fun.aloyenz.stfr.StfrConfig;
import fun.aloyenz.stfr.block.BlockLogicDevice;
import fun.aloyenz.stfr.entity.EntityStfrDebris;
import fun.aloyenz.stfr.entity.EntityStfrDigammaDagger;
import fun.aloyenz.stfr.logic.StfrCompatParticlePacket;
import fun.aloyenz.stfr.logic.StfrCoreFlashPacket;
import fun.aloyenz.stfr.logic.StfrLogicNetwork;
import fun.aloyenz.stfr.logic.StfrLogicPacket;
import fun.aloyenz.stfr.logic.StfrMusicPacket;
import fun.aloyenz.stfr.reactor.physics.StfrBlastDamage;
import fun.aloyenz.stfr.reactor.physics.StfrShutdownDebris;
import fun.aloyenz.stfr.reactor.states.StfrEmergencyRules;
import fun.aloyenz.stfr.tile.TileEntityLogicDevice;

/** Catastrophe effects are owned by one saved reactor, never by global selectors. */
public final class StfrFailureEffects {

	public static final int STRUCTURE = 1,
			SHOCKWAVE = 2,
			NUCLEAR = 4,
			HIGH_ENERGY = 8,
			FIRE = 16,
			PLAYER_DAMAGE = 32,
			ALL = 63;
	final StfrReactor reactor;
	int protectedMask;
	int recoveryTicks;
	boolean recoveryLinksRequested;
	int previousRecoveryLinks=-1;
	boolean detonated;
	String cause = "";
	int broadcastLoad;
	int meltdownEjectionCursor;
	final StfrMagneticDisassembly magnetic;
	transient long physicalPolicyTick = Long.MIN_VALUE;
	transient int liveProtectedMask;

	public StfrFailureEffects(StfrReactor reactor) {
		this.reactor = reactor;
		this.magnetic = new StfrMagneticDisassembly(reactor);
	}

	public int recoveryTicks() {
		return recoveryTicks;
	}

	public String cause() {
		return cause;
	}

	public boolean detonated() {
		return detonated;
	}

	public boolean recovering() {
		return recoveryTicks > 0;
	}

	public void reset() {
		protectedMask = recoveryTicks = broadcastLoad = meltdownEjectionCursor = 0;
		recoveryLinksRequested=false;previousRecoveryLinks=-1;
		magnetic.read(new NBTTagCompound());
		detonated = false;
		cause = "";
	}

	/** Latch connected protection before self-inflicted damage can sever the network. */
	void resolveProtection(World world) {
		protectedMask |= connectedProtection(world);
	}

	int connectedProtection(World world) {
		int mask = 0;
		for (TileEntity tile : new ArrayList<>(world.loadedTileEntityList)) {
			if (!(tile instanceof TileEntityLogicDevice) || tile.isInvalid()) continue;
			TileEntityLogicDevice device = (TileEntityLogicDevice) tile;
			if (device.kind() != BlockLogicDevice.Kind.CRASH_SUPPRESSOR) continue;
			StfrLogicNetwork.Snapshot net = StfrLogicNetwork.inspect(world, tile.getPos());
			if (net.reactor == reactor && net.ioAvailable) mask |= device.suppressionMask();
		}
		return mask;
	}

	boolean allowed(int category) {
		return StfrFailurePolicy.allows(protectedMask, category);
	}

	public boolean allowsPhysical(World world, int category) {
		if (reactor.catastrophic()) {
			resolveProtection(world);
			return allowed(category);
		}
		if (physicalPolicyTick != world.getTotalWorldTime()) {
			physicalPolicyTick = world.getTotalWorldTime();
			liveProtectedMask = connectedProtection(world);
		}
		return StfrFailurePolicy.allows(liveProtectedMask, category);
	}

	public void crashMainframes(World world, int ticks) {
		for (TileEntity tile : new ArrayList<>(world.loadedTileEntityList))
			if (tile instanceof TileEntityLogicDevice && !tile.isInvalid()) {
				TileEntityLogicDevice device = (TileEntityLogicDevice) tile;
				if (device.kind() == BlockLogicDevice.Kind.MAINFRAME
						&& StfrLogicNetwork.inspect(world, tile.getPos()).reactor == reactor
						&& (ticks == 473 || device.crashTicks() == 0)) device.crash(ticks, reactor.center);
			}
	}

	public void startup(World world, String reason) {
		StfrStartupFailureSequence.startup(this, world, reason);
	}

	public boolean canRecover() {
		return StfrStartupFailureSequence.canRecover(this);
	}

	public boolean recover(World world) {
		return StfrStartupFailureSequence.recover(this, world);
	}

	void recoveryLinkProgress(World world,int links) {
		StfrStartupFailureSequence.recoveryLinkProgress(this, world, links);
	}

	/** Returns true once the recovery has completed; countdown remains paused meanwhile. */
	public boolean tickStartup(World world, int tick) {
		return StfrStartupFailureSequence.tickStartup(this, world, tick);
	}

	void caseBreach(World world) {
		StfrStartupFailureSequence.caseBreach(this, world);
	}

	public void meltdown(World world) {
		StfrMeltdownSequence.meltdown(this, world);
	}

	public void tickMeltdown(World world, int tick) {
		StfrMeltdownSequence.tickMeltdown(this, world, tick);
	}

	void siren(World world, float volume) {
		StfrMeltdownSequence.siren(this, world, volume);
	}

	/** Same vanilla sound/particles/impulse for each preliminary pulse, without terrain damage. */
	void visualShockwave(World world,float radius) {
		StfrMeltdownSequence.visualShockwave(this, world, radius);
	}

	void shockwave(World world, float radius) {
		StfrMeltdownSequence.shockwave(this, world, radius);
	}

	/** Case Breach deletes marked cells without spawning falling blocks or drops. */
	public void removeDestructible(World world) {
		StfrFailureStructure.removeDestructible(this, world);
	}

	public void liquifyBatch(World world, int count) {
		liquifyBatch(world, count, -1);
	}

	/** Uses the persisted breakup cursor and the same structure suppression as Meltdown. */
	public void shutdownDebris(World world, int tick) {
		int count = StfrShutdownDebris.releaseCount(tick, false);
		if (count > 0) liquifyBatch(world, count, tick);
		advanceDebris(world);
	}

	void liquifyBatch(World world, int count, int shutdownTick) {
		int lifetime = shutdownTick >= 0 ? StfrShutdownDebris.END - shutdownTick
				: reactor.runState() == StfrReactor.RunState.MELTDOWN ? 1398 - reactor.stateTicks() : 200;
		int charge = shutdownTick >= 0 ? Math.max(0, StfrShutdownDebris.CHARGE - shutdownTick) : 200;
		meltdownEjectionCursor = StfrDisassembly.release(reactor, world, meltdownEjectionCursor, count, lifetime, charge);
	}

	public void magneticTick(World world,int tick) { magnetic.tick(world,tick); }

	public void advanceDebris(World world) {
		StfrDisassembly.lift(reactor, world, reactor.runState() == StfrReactor.RunState.SHUTDOWN_FAILURE
				&& reactor.stateTicks() >= StfrShutdownDebris.CHARGE);
	}

	/**
	 * Intermediate high-energy failures use HBM, with the same latched policy as final detonation.
	 */
	public void highEnergyBlast(World world, BlockPos pos, int radius) {
		resolveProtection(world);
		if (!StfrFailurePolicy.allowsHbm(protectedMask, false)) return;
		EntityNukeExplosionMK3 blast =
				EntityNukeExplosionMK3.statFacFleija(
						world, pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5, radius);
		if (!blast.isDead) {
			blast.waste = false;
			world.spawnEntity(blast);
		}
	}

	/** A small pulse with no terrain explosion, fire or reactor-state changes. */
	public void overloadPulse(World world) {
		if(world.isRemote)return;
		world.playSound(null,reactor.center,SoundEvents.ENTITY_GENERIC_EXPLODE,
			SoundCategory.BLOCKS,2F,1.2F);
		particles(world,reactor.center,EnumParticleTypes.EXPLOSION_LARGE,3,.6,0);
		particles(world,reactor.center,EnumParticleTypes.FIREWORKS_SPARK,24,.5,.15);
		blastDamage(world,reactor.center,2F);
		for(EntityPlayer player:world.playerEntities)
			if(player instanceof EntityPlayerMP && player.getDistanceSq(reactor.center)<=192D*192D)
				StfrLogicPacket.CHANNEL.sendTo(new StfrCoreFlashPacket(
					world.provider.getDimension(),reactor.center),(EntityPlayerMP)player);
	}

	/** Damage and impulse only; HBM/vanilla terrain explosions retain their own damage. */
	public void blastDamage(World world, BlockPos pos, float strength) {
		if (world.isRemote || !allowsPhysical(world, PLAYER_DAMAGE)) return;
		Vec3d center = new Vec3d(pos).add(.5, .5, .5);
		for (EntityLivingBase victim : world.getEntitiesWithinAABB(EntityLivingBase.class,
				new AxisAlignedBB(pos).grow(strength * 2))) {
			if (victim.isDead) continue;
			if (victim instanceof EntityPlayer && (((EntityPlayer) victim).capabilities.isCreativeMode
					|| ((EntityPlayer) victim).isSpectator())) continue;
			Vec3d direction = victim.getPositionVector().add(0, victim.getEyeHeight(), 0).subtract(center);
			double distance = victim.getPositionVector().distanceTo(center);
			double exposure = world.getBlockDensity(center, victim.getEntityBoundingBox());
			float damage = StfrBlastDamage.damage(strength, distance, exposure);
			if (damage <= 0) continue;
			victim.attackEntityFrom(new DamageSource("explosion").setExplosion(), damage);
			if (allowsPhysical(world, SHOCKWAVE)) {
				Vec3d impulse = direction.normalize().scale(StfrBlastDamage.impact(strength, distance, exposure));
				victim.addVelocity(impulse.x, impulse.y, impulse.z);
				if (victim instanceof EntityPlayerMP)
					((EntityPlayerMP) victim).connection.sendPacket(new SPacketEntityVelocity(victim));
			}
		}
	}

	public void detonate(World world) {
		if (detonated) return;
		silenceEmergencyMusic(world);
		resolveProtection(world);
		// Persist the dispatch flag BEFORE spawning an independently saved HBM entity.
		detonated = true;
		if (reactor.digammaActive())
			world.spawnEntity(new EntityStfrDigammaDagger(world, reactor.center));
		StfrAdvancements.grant(world, reactor.center, "detonation");
		StfrManager.get(world).markDirty();
		sound(world, "explosion.huge_explosion", SoundCategory.AMBIENT, 1000F, .8F);
		particles(world, reactor.center, EnumParticleTypes.EXPLOSION_HUGE, 20, 10, 0);
		shake(world, 512, 200, 100, 50);
		if (reactor.emergency().selfBlast())
			StfrAdvancements.grant(world, reactor.center, "self_destruct");
		int range =
				StfrEmergencyRules.blastRange(
						StfrConfig.detonationRadius, reactor.emergency().selfMultiplier());
		boolean nuclear = StfrConfig.nuclearDetonation;
		for (EntityFallingBlock debris :
				world.getEntitiesWithinAABB(
						EntityFallingBlock.class, new AxisAlignedBB(reactor.center).grow(200)))
			if (debris.getEntityData().hasUniqueId("stfrOwner")
					&& reactor.id.equals(debris.getEntityData().getUniqueId("stfrOwner"))
					&& (!(debris instanceof EntityStfrDebris)
						|| !((EntityStfrDebris)debris).released()))
				debris.setDead();
		if (nuclear)
			EntityNukeTorex.statFac(
					world,
					reactor.center.getX() + .5,
					reactor.center.getY() + .5,
					reactor.center.getZ() + .5,
					range);
		else {
			EntityCloudFleija cloud = new EntityCloudFleija(world, range);
			cloud.setPosition(
					reactor.center.getX() + .5,
					reactor.center.getY() + .5,
					reactor.center.getZ() + .5);
			world.spawnEntity(cloud);
		}
		// HBM MK3 intrinsically combines terrain, entity damage and (nuclear) fallout.
		// Never dispatch it when any of these physical effects has been suppressed.
		if (!StfrFailurePolicy.allowsHbm(protectedMask, nuclear)) return;
		EntityNukeExplosionMK3 blast =
				EntityNukeExplosionMK3.statFacFleija(
						world,
						reactor.center.getX() + .5,
						reactor.center.getY() + .5,
						reactor.center.getZ() + .5,
						range);
		if (blast.isDead) return;
		blast.waste = nuclear;
		world.spawnEntity(blast);
	}

	void silenceEmergencyMusic(World world) {
		reactor.emergency().stopMusic(world);
		for(EntityPlayer player:world.playerEntities)
			if(player instanceof EntityPlayerMP)
				StfrMusicPacket.send((EntityPlayerMP)player,reactor.center,"*",1,false);
	}

	public void sound(World world, String id, SoundCategory category, float volume, float pitch) {
		StfrFailurePresentation.sound(this, world, id, category, volume, pitch);
	}

	public void stop(World world, String soundId, int radius) {
		StfrFailurePresentation.stop(this, world, soundId, radius);
	}

	void broadcast(World world, String event, String voice, int level, Object... args) {
		StfrFailurePresentation.broadcast(this, world, event, voice, level, args);
	}

	public void decayBroadcastLoad(World world) {
		if (world.getTotalWorldTime() % 20 == 0) broadcastLoad = Math.max(0, broadcastLoad - 3);
	}

	public void recordBroadcast(World world) {
		StfrFailurePresentation.recordBroadcast(this, world);
	}

	/** Original case_explosion affects the central case, never all six remote stabilizers. */
	public void caseExplosion(World world) {
		StfrFailureStructure.caseExplosion(this, world);
	}

	/** Local outward glass and dust impulse; no blast or damage to structural members. */
	public void glassBlowout(World world) {
		StfrFailureStructure.glassBlowout(this, world);
	}

	public void caseBlowout(World world) {
		StfrFailureStructure.caseBlowout(this, world);
	}

	public void caseBlowout(World world, boolean entityDamage) {
		StfrFailureStructure.caseBlowout(this, world, entityDamage);
	}

	public void blindness(World world, int radius) {
		StfrFailurePresentation.blindness(this, world, radius);
	}

	public void fadeShake(World world, int radius, int ticks) {
		StfrFailurePresentation.fadeShake(this, world, radius, ticks);
	}

	public void shake(World world, int radius, int ticks, int frequency, int magnitude) {
		StfrFailurePresentation.shake(this, world, radius, ticks, frequency, magnitude);
	}

	public void shake(World world, int radius, int ticks, int frequency, int magnitude, boolean sustained) {
		StfrFailurePresentation.shake(this, world, radius, ticks, frequency, magnitude, sustained);
	}

	static void particles(
			World world,
			BlockPos pos,
			EnumParticleTypes type,
			int count,
			double spread,
			double speed) {
		if (world instanceof WorldServer)
			((WorldServer) world)
					.spawnParticle(
							type,
							true,
							pos.getX() + .5,
							pos.getY() + .5,
							pos.getZ() + .5,
							count,
							spread,
							spread,
							spread,
							speed);
	}

	public static void compatParticles(
			World world, BlockPos pos, int kind, int count, double spread, double speed) {
		StfrLogicPacket.CHANNEL.sendToAllAround(
				new StfrCompatParticlePacket(
						pos.getX() + .5,
						pos.getY() + .5,
						pos.getZ() + .5,
						kind,
						count,
						spread,
						speed),
				new NetworkRegistry.TargetPoint(
						world.provider.getDimension(), pos.getX(), pos.getY(), pos.getZ(), 512));
	}

	public NBTTagCompound write() {
		NBTTagCompound tag = new NBTTagCompound();
		tag.setInteger("protectedMask", protectedMask);
		tag.setInteger("recoveryTicks", recoveryTicks);
		tag.setBoolean("recoveryLinksRequested",recoveryLinksRequested);
		tag.setBoolean("detonated", detonated);
		tag.setString("cause", cause);
		tag.setInteger("broadcastLoad", broadcastLoad);
		tag.setInteger("meltdownEjectionCursor", meltdownEjectionCursor);
		tag.setInteger("disassemblyOrderVersion", 1);
		tag.setTag("magneticCollapse", magnetic.write());
		return tag;
	}

	public void read(NBTTagCompound tag) {
		protectedMask = tag.getInteger("protectedMask") & ALL;
		magnetic.read(tag.getCompoundTag("magneticCollapse"));
		recoveryTicks = Math.max(0, Math.min(600, tag.getInteger("recoveryTicks")));
		recoveryLinksRequested=tag.getBoolean("recoveryLinksRequested");previousRecoveryLinks=-1;
		detonated = tag.getBoolean("detonated");
		cause = tag.getString("cause");
		broadcastLoad = Math.max(0, tag.getInteger("broadcastLoad"));
		// Old cursors index JSON order. Restart the radial scan; already detached cells are skipped.
		meltdownEjectionCursor = tag.getInteger("disassemblyOrderVersion") == 1
				? Math.max(0, tag.getInteger("meltdownEjectionCursor")) : 0;
	}
}
