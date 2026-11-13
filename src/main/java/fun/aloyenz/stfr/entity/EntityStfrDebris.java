package fun.aloyenz.stfr.entity;

import io.netty.buffer.ByteBuf;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.item.EntityFallingBlock;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;
import fun.aloyenz.stfr.reactor.StfrManager;
import fun.aloyenz.stfr.reactor.StfrReactor;
import fun.aloyenz.stfr.reactor.physics.StfrLiftMotion;
import fun.aloyenz.stfr.reactor.physics.StfrMagneticCollapse;
import fun.aloyenz.stfr.reactor.physics.StfrVortex;

/** Rotating detached block: lifted until release height, then falls and settles like sand. */
public final class EntityStfrDebris extends EntityFallingBlock implements IEntityAdditionalSpawnData {
	private BlockPos center = BlockPos.ORIGIN;
	private BlockPos source = BlockPos.ORIGIN;
	private double releaseY;
	private int lifetime = 350, chargeAfter = 200;
	private boolean released;
	private boolean magnetic;
	private int fallingTicks;
	private static final DataParameter<Boolean> RELEASED =
			EntityDataManager.createKey(EntityStfrDebris.class,
				DataSerializers.BOOLEAN);
	@Override protected void entityInit() {
		super.entityInit();dataManager.register(RELEASED,false);
	}
	public void magnetic() { magnetic=true;noClip=false;setNoGravity(true); }
	public boolean magneticMode() { return magnetic; }
	public boolean released() { return released || dataManager.get(RELEASED); }
	private void release() {
		released=true;dataManager.set(RELEASED,true);
		motionY=Math.min(0,motionY);
	}

	/** Shell fragments leave the vortex permanently, retaining their upward launch and spin. */
	public void launch(Vec3d velocity) {
		released=true;dataManager.set(RELEASED,true);
		motionX=velocity.x;motionY=velocity.y;motionZ=velocity.z;
		velocityChanged=true;
	}

	public EntityStfrDebris(World world) {
		super(world);
		setSize(.98F, .98F);
		shouldDropItem = false;
		isImmuneToFire = true;
	}

	public EntityStfrDebris(World world, BlockPos pos, IBlockState state,
			BlockPos center, double releaseY, int lifetime, int chargeAfter) {
		super(world, pos.getX() + .5, pos.getY(), pos.getZ() + .5, state);
		this.center = center;
		this.source = pos;
		this.releaseY = releaseY;
		this.lifetime = lifetime;
		this.chargeAfter = chargeAfter;
		shouldDropItem = false;
		isImmuneToFire = true;
		fallTime = 1;
	}

	@Override
	public boolean canBeCollidedWith() { return !magnetic && released() && !isDead; }

	@Override public AxisAlignedBB getCollisionBoundingBox() {
		return magnetic?null:super.getCollisionBoundingBox();
	}
	@Override public AxisAlignedBB getCollisionBox(Entity entity) {
		return magnetic?null:super.getCollisionBox(entity);
	}

	@Override
	public void onUpdate() {
		prevPosX = posX;
		prevPosY = posY;
		prevPosZ = posZ;
		++fallTime;
		if(getEntityData().getBoolean("stfrGlassShard") && fallTime>40) {setDead();return;}
		if(magnetic) {
			if(!world.isRemote) {
				StfrReactor owner=StfrManager.get(world).get(center);
				if(owner==null || owner.runState()!=StfrReactor.RunState.UNDERLOAD) {setDead();return;}
			}
			noClip=false;
			Vec3d relative=new Vec3d(posX-center.getX()-.5,posY-center.getY(),posZ-center.getZ()-.5);
			Vec3d next=StfrMagneticCollapse.swarm(relative,
					new Vec3d(motionX,motionY,motionZ),fallTime,source.toLong());
			motionX=next.x;motionY=next.y;motionZ=next.z;
			move(MoverType.SELF,motionX,motionY,motionZ);
			if(collidedHorizontally) {motionX=-next.x*.65;motionZ=-next.z*.65;}
			if(collidedVertically)motionY=-next.y*.65;

			return;
		}
		released=released();
		double dx = posX - center.getX() - .5, dz = posZ - center.getZ() - .5;
		if (!released && StfrLiftMotion.debrisReleased(false,posY,releaseY)) release();
		if (!released) {
			Vec3d next = StfrVortex.velocity(new Vec3d(dx, 0, dz),
					new Vec3d(motionX, motionY, motionZ), fallTime >= chargeAfter);
			motionX = next.x; motionY = next.y; motionZ = next.z;
		} else {
			Vec3d next=StfrLiftMotion.falling(new Vec3d(motionX,motionY,motionZ));
			motionX=next.x;motionY=next.y;motionZ=next.z;
			fallingTicks++;
		}
		move(MoverType.SELF, motionX, motionY, motionZ);
		if(!world.isRemote && !getEntityData().getBoolean("stfrGlassShard") && released && (onGround || fallingTicks>600 || posY < -64))settle();
	}

	private void settle() {
		IBlockState state=getBlock();
		BlockPos target=new BlockPos(posX,posY+.01,posZ);
		BlockPos support=new BlockPos(posX,posY-.01,posZ);
		boolean full=world.getBlockState(support).isFullCube();
		if(onGround && full && world.mayPlace(state.getBlock(),target,true,EnumFacing.UP,this)
				&& world.setBlockState(target,state,3)) {
			if(tileEntityData!=null) {
				TileEntity tile=world.getTileEntity(target);
				if(tile!=null) {
					NBTTagCompound data=tile.writeToNBT(new NBTTagCompound());
					for(String key:tileEntityData.getKeySet())
						if(!key.equals("x") && !key.equals("y") && !key.equals("z"))
							data.setTag(key,tileEntityData.getTag(key).copy());
					tile.readFromNBT(data);tile.markDirty();
				}
			}
			setDead();return;
		}
		if(world.getGameRules().getBoolean("doEntityDrops")) {
			ItemStack stack=new ItemStack(state.getBlock(),1,state.getBlock().damageDropped(state));
			if(tileEntityData!=null) {
				NBTTagCompound payload=tileEntityData.copy();
				payload.removeTag("x");payload.removeTag("y");payload.removeTag("z");
				stack.setTagInfo("BlockEntityTag",payload);
			}
			EntityItem item=entityDropItem(stack,0);
			if(item!=null)item.getEntityData().setBoolean("stfrReleasedDebris",true);
		}
		setDead();
	}

	public float spin(float partialTicks) {
		float age = fallTime + partialTicks;
		return age * 2F + Math.max(0F, age - chargeAfter) * 3F
				+ (source.toLong() & 63) * 5F;
	}

	@Override
	protected void writeEntityToNBT(NBTTagCompound tag) {
		super.writeEntityToNBT(tag);
		tag.setBoolean("stfrMagnetic",magnetic);
		tag.setBoolean("stfrGlassShard",getEntityData().getBoolean("stfrGlassShard"));
		tag.setLong("stfrCenter", center.toLong());
		tag.setLong("stfrOrigin", source.toLong());
		tag.setDouble("stfrReleaseY", releaseY);
		tag.setInteger("stfrLifetime", lifetime);
		tag.setInteger("stfrChargeAfter", chargeAfter);
		tag.setBoolean("stfrReleased", released());
		tag.setInteger("stfrFallingTicks",fallingTicks);
	}

	@Override
	protected void readEntityFromNBT(NBTTagCompound tag) {
		super.readEntityFromNBT(tag);
		magnetic=tag.getBoolean("stfrMagnetic");
		getEntityData().setBoolean("stfrGlassShard",tag.getBoolean("stfrGlassShard"));
		if(magnetic)magnetic();
		center = BlockPos.fromLong(tag.getLong("stfrCenter"));
		source = BlockPos.fromLong(tag.getLong("stfrOrigin"));
		setOrigin(source);
		releaseY = tag.getDouble("stfrReleaseY");
		lifetime = tag.getInteger("stfrLifetime");
		chargeAfter = tag.getInteger("stfrChargeAfter");
		released = tag.getBoolean("stfrReleased");
		dataManager.set(RELEASED,released);
		fallingTicks=tag.getInteger("stfrFallingTicks");
		shouldDropItem = false;
	}

	@Override
	public void writeSpawnData(ByteBuf buffer) {
		NBTTagCompound tag = new NBTTagCompound();
		writeEntityToNBT(tag);
		ByteBufUtils.writeTag(buffer, tag);
	}

	@Override
	public void readSpawnData(ByteBuf buffer) {
		NBTTagCompound tag = ByteBufUtils.readTag(buffer);
		if (tag != null) readEntityFromNBT(tag);
	}
}
