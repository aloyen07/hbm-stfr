package fun.aloyenz.stfr.reactor;

import java.util.*;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.MobEffects;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.play.server.SPacketEntityVelocity;
import net.minecraft.potion.PotionEffect;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import fun.aloyenz.stfr.block.STFRBlocks;
import fun.aloyenz.stfr.entity.EntityStfrDebris;
import fun.aloyenz.stfr.reactor.physics.StfrMagneticCollapse;
import fun.aloyenz.stfr.sound.StfrSceneSounds;
import fun.aloyenz.stfr.structure.*;
import fun.aloyenz.stfr.tile.TileEntityElectrodeProxy;
import fun.aloyenz.stfr.tile.TileEntityRotorPlaceholder;

/** Per-reactor persistent partial breakup. Ports, inventories and frame members survive. */
final class StfrMagneticDisassembly {
	private final StfrReactor reactor;
	private int radius=2,detached;
	private long lastSnap=Long.MIN_VALUE;
	private final List<BlockPos> interior=new ArrayList<>();
	private int insideCursor;
	StfrMagneticDisassembly(StfrReactor reactor) { this.reactor=reactor; }
	private boolean available(World world,BlockPos pos) {
		if(!world.isBlockLoaded(pos) || world.isAirBlock(pos))return false;
		IBlockState state=world.getBlockState(pos);
		return state.getBlock()!=STFRBlocks.REACTOR_LIGHT
				&& !state.getMaterial().isLiquid() && state.getBlockHardness(world,pos)>=0;
	}
	void tick(World world,int tick) {
		if(world.isRemote || tick<StfrMagneticCollapse.ELECTRODES)return;
		if(reactor.failure().allowsPhysical(world,StfrFailureEffects.SHOCKWAVE))pull(world,tick);
		if(!reactor.failure().allowsPhysical(world,StfrFailureEffects.STRUCTURE))return;
		// Scan a bounded part of the cleared volume every tick, catching newly placed blocks.
		if(interior.isEmpty())for(int x=-16;x<=16;x++)for(int y=-16;y<=16;y++)for(int z=-16;z<=16;z++)
			if(x*x+y*y+z*z<=256)interior.add(new BlockPos(x,y,z));
		for(int i=0;i<interior.size();i++) {
			BlockPos offset=interior.get(insideCursor++%interior.size());
			if(StfrMagneticCollapse.shellBand(radius,offset.distanceSq(BlockPos.ORIGIN))<0) {
				BlockPos pos=reactor.center.add(offset);
				if(available(world,pos))detach(world,pos,true);
			}
		}
		if(world.rand.nextDouble()>=StfrMagneticCollapse.detachmentChance(tick))return;
		List<BlockPos> shell=new ArrayList<>();boolean unloaded=false;
		for(int x=-radius;x<=radius;x++)for(int y=-radius;y<=radius;y++)for(int z=-radius;z<=radius;z++) {
			int d=x*x+y*y+z*z;
			if(StfrMagneticCollapse.shellBand(radius,d)!=0)continue;
			BlockPos pos=reactor.center.add(x,y,z);
			if(!world.isBlockLoaded(pos))unloaded=true;
			else if(available(world,pos))shell.add(pos);
		}
		if(shell.isEmpty()) {
			if(!unloaded && radius<16) {radius++;StfrManager.get(world).markDirty();}
			return;
		}
		detach(world,shell.get(world.rand.nextInt(shell.size())),false);
	}
	private void detach(World world,BlockPos pos,boolean immediate) {
		TileEntity tile=world.getTileEntity(pos);
		IBlockState physical=StfrStructureValidator.originalAnimationState(tile);
		if(physical==null)physical=world.getBlockState(pos);
		if(tile!=null && !(tile instanceof TileEntityElectrodeProxy) && !(tile instanceof TileEntityRotorPlaceholder)) {
			// Extract contents before normal block breaking; emptied inventories cannot drop twice.
			if(tile instanceof IInventory) {
				IInventory inventory=(IInventory)tile;
				for(int slot=0;slot<inventory.getSizeInventory();slot++) {
					ItemStack stack=inventory.removeStackFromSlot(slot);
					if(!stack.isEmpty())InventoryHelper.spawnItemStack(world,pos.getX(),pos.getY(),pos.getZ(),stack);
				}
			} else if(tile.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY,null)) {
				IItemHandler inventory=tile.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY,null);
				if(inventory!=null)for(int slot=0;slot<inventory.getSlots();slot++) {
					ItemStack stack=inventory.extractItem(slot,Integer.MAX_VALUE,false);
					if(!stack.isEmpty())InventoryHelper.spawnItemStack(world,pos.getX(),pos.getY(),pos.getZ(),stack);
				}
			}
			if(!world.destroyBlock(pos,true))return;
		} else {
			int active=world.getEntitiesWithinAABB(EntityStfrDebris.class,new AxisAlignedBB(reactor.center).grow(20),e -> e.magneticMode()).size();
			if(active>=160)return;
			EntityStfrDebris debris=new EntityStfrDebris(world,pos,physical,reactor.center,pos.getY(),600,0);
			debris.magnetic();debris.getEntityData().setUniqueId("stfrOwner",reactor.id);
			if(immediate) {
				Vec3d inward=new Vec3d(reactor.center.subtract(pos)).normalize().scale(.65);
				debris.motionX=inward.x;debris.motionY=inward.y;debris.motionZ=inward.z;
			}
			if(!world.spawnEntity(debris))return;
			if(!world.setBlockToAir(pos)) {debris.setDead();return;}
		}
		BlockPos local=pos.subtract(reactor.center).add(StfrStructureTemplate.CENTER);
		reactor.aimedElectrodes.remove(local.toLong());reactor.hiddenRotors.remove(local.toLong());
		detached++;
		if(lastSnap==Long.MIN_VALUE || world.getTotalWorldTime()-lastSnap>=2) {
			lastSnap=world.getTotalWorldTime();
			world.playSound(null,pos,StfrSceneSounds.get("magnetic.tear"),SoundCategory.BLOCKS,4F,.85F+world.rand.nextFloat()*.3F);
		}
		if(world instanceof WorldServer)((WorldServer)world).spawnParticle(
				EnumParticleTypes.BLOCK_DUST,true,pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,8,.25,.25,.25,.06,
				Block.getStateId(physical));
		StfrManager.get(world).markDirty();
	}
	private void pull(World world,int tick) {
		Vec3d center=new Vec3d(reactor.center).add(.5,.5,.5);
		for(Entity entity:world.getEntitiesWithinAABB(Entity.class,new AxisAlignedBB(reactor.center).grow(StfrMagneticCollapse.RADIUS))) {
			if(entity.isDead || entity instanceof EntityStfrDebris && ((EntityStfrDebris)entity).magneticMode())continue;
			if(entity instanceof EntityPlayer && (((EntityPlayer)entity).isSpectator()
					|| ((EntityPlayer)entity).capabilities.isCreativeMode))continue;
			Vec3d delta=new Vec3d(entity.posX,entity.posY+entity.height*.5,entity.posZ).subtract(center);
			if(delta.lengthSquared()>=StfrMagneticCollapse.RADIUS*StfrMagneticCollapse.RADIUS)continue;
			if(delta.lengthSquared()<1.4*1.4 && !(entity instanceof EntityPlayer)
					&& reactor.failure().allowsPhysical(world,StfrFailureEffects.PLAYER_DAMAGE)) {
				entity.setDead();continue;
			}
			Vec3d next=StfrMagneticCollapse.velocity(delta,new Vec3d(entity.motionX,entity.motionY,entity.motionZ),
					StfrMagneticCollapse.strength(tick),false);
			entity.motionX=next.x;entity.motionY=next.y;entity.motionZ=next.z;
			entity.fallDistance=0;entity.velocityChanged=true;
			if(entity instanceof EntityPlayerMP) {
				EntityPlayerMP player=(EntityPlayerMP)entity;
				PotionEffect old=player.getActivePotionEffect(MobEffects.LEVITATION);
				if(next.y>.05 && (old==null || old.getDuration()<=6))
					player.addPotionEffect(new PotionEffect(MobEffects.LEVITATION,6,0,true,false));
				if(tick%2==0 && player.connection!=null)player.connection.sendPacket(new SPacketEntityVelocity(player));
			}
		}
	}
	NBTTagCompound write() {
		NBTTagCompound tag=new NBTTagCompound();
		tag.setInteger("radius",radius);tag.setInteger("detached",detached);return tag;
	}
	void read(NBTTagCompound tag) {
		radius=Math.max(2,Math.min(16,tag.hasKey("radius")?tag.getInteger("radius"):2));
		insideCursor=0;
		detached=Math.max(0,tag.getInteger("detached"));
	}
}
