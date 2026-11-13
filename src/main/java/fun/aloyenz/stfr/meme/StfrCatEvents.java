package fun.aloyenz.stfr.meme;

import java.util.*;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.play.server.SPacketSoundEffect;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import fun.aloyenz.stfr.Tags;
import fun.aloyenz.stfr.logic.StfrMonitorDisplay;
import fun.aloyenz.stfr.reactor.StfrManager;
import fun.aloyenz.stfr.reactor.StfrReactor;
import fun.aloyenz.stfr.sound.STFRSounds;
import fun.aloyenz.stfr.tile.TileEntityLogicDevice;

/** Dimension-local, saved cosmetic events. No writes to the reactor or its controls. */
@Mod.EventBusSubscriber(modid=Tags.MODID)
public final class StfrCatEvents extends WorldSavedData {
	private static final String NAME="hbmstfr_cat_events";
	private static final class Group {
		final StfrCatSequence sequence;
		final Set<Long> monitors=new HashSet<>();
		Group(StfrCatSequence sequence) { this.sequence=sequence; }
	}
	private final Map<Long,Group> groups=new HashMap<>();
	private final Map<Long,Group> byMonitor=new HashMap<>();
	public StfrCatEvents() { super(NAME); }
	public StfrCatEvents(String name) { super(name); }
	private static StfrCatEvents get(World world) {
		StfrCatEvents data=(StfrCatEvents)world.getPerWorldStorage().getOrLoadData(StfrCatEvents.class,NAME);
		if (data==null) { data=new StfrCatEvents();world.getPerWorldStorage().setData(NAME,data); }
		return data;
	}

	public static void burned(World world,BlockPos burnPos) {
		if (world.isRemote) return;
		StfrCatEvents events=get(world);
		for (TileEntity tile:new ArrayList<>(world.loadedTileEntityList)) {
			if (!(tile instanceof TileEntityLogicDevice) || tile.isInvalid()) continue;
			TileEntityLogicDevice monitor=(TileEntityLogicDevice)tile;
			if (monitor.monitorDisplay()!=StfrMonitorDisplay.REACTOR || !monitor.monitorConnected()
					|| !monitor.monitorGraphics().hasKey("reactorCenter")) continue;
			long center=monitor.monitorGraphics().getLong("reactorCenter");
			StfrReactor reactor=StfrManager.get(world).get(BlockPos.fromLong(center));
			if (reactor==null || !StfrCatSequence.eligible(reactor.runState()==StfrReactor.RunState.ONLINE,
					reactor.workingStabilizers(),reactor.refuelingTurbines(),
					monitor.getPos().distanceSq(burnPos),true)) continue;
			// Existing groups never acquire additional monitors from another burned item.
			Group active=events.groups.get(center);
			if (active!=null) continue;
			Group group=new Group(new StfrCatSequence());
			for (TileEntity candidate:new ArrayList<>(world.loadedTileEntityList)) {
				if (!(candidate instanceof TileEntityLogicDevice) || candidate.isInvalid()) continue;
				TileEntityLogicDevice target=(TileEntityLogicDevice)candidate;
				if (target.monitorDisplay()==StfrMonitorDisplay.REACTOR && target.monitorConnected()
						&& target.monitorGraphics().hasKey("reactorCenter")
						&& target.monitorGraphics().getLong("reactorCenter")==center
						&& target.getPos().distanceSq(burnPos)<=100D*100D)
					group.monitors.add(target.getPos().toLong());
			}
			events.groups.put(center,group);
			for(long pos:group.monitors) events.byMonitor.put(pos,group);
			events.sync(world,group);
			events.markDirty();
		}
	}

	public static void updateMonitor(TileEntityLogicDevice tile) {
		if (tile.getWorld()==null || tile.getWorld().isRemote) return;
		Group group=get(tile.getWorld()).byMonitor.get(tile.getPos().toLong());
		tile.setCatOverlay(group==null ? 0 : group.sequence.stage());
	}

	public static boolean click(World world,TileEntityLogicDevice tile) {
		if (tile.monitorDisplay()!=StfrMonitorDisplay.REACTOR) return false;
		if (world.isRemote) return tile.catStage()>0;
		StfrCatEvents events=get(world);
		Group group=events.byMonitor.get(tile.getPos().toLong());
		if (group==null) { tile.setCatOverlay(0);return false; }
		boolean scare=group.sequence.click(world.getTotalWorldTime());
		events.sync(world,group);events.markDirty();
		if (scare) events.sound(world,group);
		return true;
	}

	private void sync(World world,Group group) {
		for(long key:group.monitors) {
			BlockPos pos=BlockPos.fromLong(key);
			if (!world.isBlockLoaded(pos)) continue;
			TileEntity tile=world.getTileEntity(pos);
			if (tile instanceof TileEntityLogicDevice) ((TileEntityLogicDevice)tile).setCatOverlay(group.sequence.stage());
		}
	}
	private void sound(World world,Group group) {
		for(EntityPlayer player:world.playerEntities) {
			if (!(player instanceof EntityPlayerMP)) continue;
			BlockPos nearest=null;double best=32D*32D;
			for(long key:group.monitors) {
				BlockPos pos=BlockPos.fromLong(key);
				if (!world.isBlockLoaded(pos)) continue;
				TileEntity tile=world.getTileEntity(pos);
				if (!(tile instanceof TileEntityLogicDevice)
						|| ((TileEntityLogicDevice)tile).monitorDisplay()!=StfrMonitorDisplay.REACTOR) continue;
				double distance=player.getDistanceSqToCenter(pos);
				if(distance<best) { best=distance;nearest=pos; }
			}
			if(nearest!=null) ((EntityPlayerMP)player).connection.sendPacket(new SPacketSoundEffect(
					STFRSounds.CAT_BUFFER_STALL,SoundCategory.BLOCKS,nearest.getX()+.5,nearest.getY()+.5,
					nearest.getZ()+.5,.8F,1F));
		}
	}

	/** Runs before held-item use, including sneaking with a preset wand. */
	@SubscribeEvent(priority=EventPriority.HIGHEST)
	public static void interact(PlayerInteractEvent.RightClickBlock event) {
		World world=event.getWorld();
		TileEntity raw=world.getTileEntity(event.getPos());
		if (!(raw instanceof TileEntityLogicDevice)) return;
		TileEntityLogicDevice tile=(TileEntityLogicDevice)raw;
		if (tile.monitorDisplay()!=StfrMonitorDisplay.REACTOR) return;
		boolean active=world.isRemote ? tile.catStage()>0 : get(world).byMonitor.containsKey(event.getPos().toLong());
		if (!active) return;
		if (event.getHand()==EnumHand.MAIN_HAND) click(world,tile);
		event.setCancellationResult(EnumActionResult.SUCCESS);
		event.setCanceled(true);
	}

	@SubscribeEvent public static void tick(TickEvent.WorldTickEvent event) {
		if(event.phase!=TickEvent.Phase.END || event.world.isRemote) return;
		StfrCatEvents events=get(event.world);
		Iterator<Group> iterator=events.groups.values().iterator();
		while(iterator.hasNext()) {
			Group group=iterator.next();
			if(group.sequence.expire(event.world.getTotalWorldTime())) {
				events.sync(event.world,group);
				for(long key:group.monitors) events.byMonitor.remove(key);
				iterator.remove();events.markDirty();
			}
		}
	}

	@Override public void readFromNBT(NBTTagCompound nbt) {
		groups.clear();byMonitor.clear();
		NBTTagList list=nbt.getTagList("groups",10);
		for(int i=0;i<list.tagCount();i++) {
			NBTTagCompound tag=list.getCompoundTagAt(i);
			Group group=new Group(StfrCatSequence.restore(tag.getInteger("stage"),tag.getLong("until"),tag.getLong("lastClick")));
			if(group.sequence.stage()==0) continue;
			NBTTagList targets=tag.getTagList("monitors",10);
			for(int j=0;j<targets.tagCount();j++) {
				long key=targets.getCompoundTagAt(j).getLong("pos");
				group.monitors.add(key);byMonitor.put(key,group);
			}
			groups.put(tag.getLong("center"),group);
		}
	}
	@Override public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
		NBTTagList list=new NBTTagList();
		for(Map.Entry<Long,Group> entry:groups.entrySet()) {
			Group group=entry.getValue();NBTTagCompound tag=new NBTTagCompound();
			tag.setLong("center",entry.getKey());tag.setInteger("stage",group.sequence.stage());
			tag.setLong("until",group.sequence.until());tag.setLong("lastClick",group.sequence.lastClick());
			NBTTagList targets=new NBTTagList();
			for(long key:group.monitors) { NBTTagCompound target=new NBTTagCompound();target.setLong("pos",key);targets.appendTag(target); }
			tag.setTag("monitors",targets);list.appendTag(tag);
		}
		nbt.setTag("groups",list);return nbt;
	}
}
