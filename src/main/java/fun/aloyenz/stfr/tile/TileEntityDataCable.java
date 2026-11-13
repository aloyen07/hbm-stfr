package fun.aloyenz.stfr.tile;

import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.uninos.UniNodespace;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import fun.aloyenz.stfr.api.data.DataNetworkApi;
import fun.aloyenz.stfr.api.data.DataNode;
import fun.aloyenz.stfr.api.data.IDataMachine;

public final class TileEntityDataCable extends TileEntity implements ITickable {

	private DataNode node;

	@Override
	public void update() {
		if (world.isRemote) return;
		if (node == null || node.expired) {
			node = UniNodespace.getNode(world, pos, DataNetworkApi.PROVIDER);
			if (node == null || node.expired) {
				DirPos[] connections = new DirPos[EnumFacing.VALUES.length];
				for (EnumFacing side : EnumFacing.VALUES) {
					ForgeDirection direction = ForgeDirection.getOrientation(side);
					connections[side.getIndex()] = new DirPos(pos.offset(side), direction);
				}
				node = new DataNode(pos).setConnections(connections);
				UniNodespace.createNode(world, node);
			}
		}

		if (node != null && node.hasValidNet()) {
			for (EnumFacing side : EnumFacing.VALUES) {
				TileEntity neighbour = world.getTileEntity(pos.offset(side));
				if (neighbour instanceof IDataMachine machine && machine.acceptsDataCable() && !neighbour.isInvalid()) {
					node.net.addReceiver(machine);
				}
			}
		}
	}

	@Override
	public void invalidate() {
		super.invalidate();
		destroyNode();
	}

	@Override
	public void onChunkUnload() {
		super.onChunkUnload();
		destroyNode();
	}

	private void destroyNode() {
		if (world != null && !world.isRemote && node != null) {
			UniNodespace.destroyNode(world, node);
			node = null;
		}
	}
}
