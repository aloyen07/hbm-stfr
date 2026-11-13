package fun.aloyenz.stfr.entity;

import com.hbm.explosion.ExplosionNT;
import com.hbm.explosion.ExplosionNT.ExAttrib;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.lib.ModDamageSource;
import com.hbm.main.MainRegistry;
import com.hbm.particle.helper.HbmEffectNT;
import com.hbm.util.ContaminationUtil;
import java.util.List;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

/** STFR's bounded form of HBM's digamma spear: the final pulse has a 512 block radius. */
public final class EntityStfrDigammaDagger extends Entity {

	private int groundedTicks;

	public EntityStfrDigammaDagger(World world) {
		super(world);
		setSize(3F, 14F);
		isImmuneToFire = true;
		ignoreFrustumCheck = true;
	}

	public EntityStfrDigammaDagger(World world, BlockPos target) {
		this(world);
		setPosition(target.getX() + .5, target.getY() + 100.5, target.getZ() + .5);
	}

	@Override
	protected void entityInit() {}

	@Override
	public void onUpdate() {
		super.onUpdate();
		motionX = motionZ = 0;
		motionY = -.35D;
		BlockPos below = new BlockPos(posX, posY - 2, posZ);
		if (world.getBlockState(below).getMaterial() == Material.AIR) {
			setPosition(posX, posY + motionY, posZ);
			groundedTicks = 0;
			if (!world.isRemote && ticksExisted % 5 == 0) {
				burst(8, 2D);
				double ix = posX + rand.nextGaussian() * 25D, iz = posZ + rand.nextGaussian() * 25D;
				double iy = world.getHeight((int) Math.floor(ix), (int) Math.floor(iz)) + 2D;
				new ExplosionNT(world, this, ix, iy, iz, 7.5F)
						.addAttrib(ExAttrib.NOHURT)
						.addAttrib(ExAttrib.NOPARTICLE)
						.addAttrib(ExAttrib.NODROP)
						.addAttrib(ExAttrib.NOSOUND)
						.addAttrib(ExAttrib.DIGAMMA)
						.explode();
			}
			if (world.isRemote && ticksExisted % 3 == 0) {
				NBTTagCompound effect = new NBTTagCompound();
				effect.setInteger("count", 5);
				MainRegistry.proxy.effectNT(
						HbmEffectNT.Smoke_RadialDigamma,
						posX,
						world.getHeight((int) Math.floor(posX), (int) Math.floor(posZ)) + 2D,
						posZ,
						effect);
			}
			return;
		}
		groundedTicks++;
		if (!world.isRemote && groundedTicks % 5 == 0) burst(30, 8D);
		if (!world.isRemote && groundedTicks >= 100) impact();
	}

	private void burst(int count, double spread) {
		if (world instanceof WorldServer)
			((WorldServer) world)
					.spawnParticle(
							EnumParticleTypes.REDSTONE,
							true,
							posX,
							posY + 4,
							posZ,
							count,
							spread,
							spread * .4,
							spread,
							.2);
	}

	private void impact() {
		AxisAlignedBB area =
				new AxisAlignedBB(
						posX - 512, posY - 512, posZ - 512, posX + 512, posY + 512, posZ + 512);
		List<EntityLivingBase> victims =
				world.getEntitiesWithinAABB(
						EntityLivingBase.class,
						area,
						entity -> entity != null && entity.getDistanceSq(this) <= 512D * 512D);
		for (EntityLivingBase entity : victims) {
			ContaminationUtil.contaminate(
					entity,
					ContaminationUtil.HazardType.DIGAMMA,
					ContaminationUtil.ContaminationType.DIGAMMA2,
					10F);
			entity.attackEntityFrom(ModDamageSource.digamma, Float.MAX_VALUE);
			if (!entity.isDead) entity.setHealth(0F);
		}
		world.playSound(
				null, posX, posY, posZ, HBMSoundHandler.dflash, SoundCategory.HOSTILE, 25000F, 1F);
		NBTTagCompound effect = new NBTTagCompound();
		effect.setInteger("count", 100);
		MainRegistry.proxy.effectNT(HbmEffectNT.Smoke_RadialDigamma, posX, posY + 7, posZ, effect);
		burst(600, 24D);
		setDead();
	}

	@Override
	protected void readEntityFromNBT(NBTTagCompound tag) {
		groundedTicks = tag.getInteger("groundedTicks");
	}

	@Override
	protected void writeEntityToNBT(NBTTagCompound tag) {
		tag.setInteger("groundedTicks", groundedTicks);
	}

	@Override
	public boolean isInRangeToRenderDist(double distance) {
		return distance < 512D * 512D;
	}

	@Override
	public int getBrightnessForRender() {
		return 15728880;
	}

	@Override
	public float getBrightness() {
		return 1F;
	}
}
