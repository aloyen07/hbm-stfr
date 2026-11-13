package fun.aloyenz.stfr.api.data;

import com.hbm.uninos.INetworkProvider;
import com.hbm.uninos.UniNodespace;
import java.util.Collections;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class DataNetworkApi {

	public static final INetworkProvider<DataNetwork> PROVIDER = DataNetwork::new;

	private DataNetworkApi() {}

	@Nullable
	public static DataNetwork getNetwork(World world, BlockPos pos) {
		DataNode node = UniNodespace.getNode(world, pos, PROVIDER);
		if (node != null && node.hasValidNet()) return node.net;

		TileEntity tile = world.getTileEntity(pos);
		if (tile instanceof IDataMachine machine && machine.acceptsDataCable()) {
			for (EnumFacing side : EnumFacing.VALUES) {
				node = UniNodespace.getNode(world, pos.offset(side), PROVIDER);
				if (node != null && node.hasValidNet()) return node.net;
			}
		}
		return null;
	}

	public static Set<IDataMachine> getConnectedMachines(World world, BlockPos pos) {
		DataNetwork network = getNetwork(world, pos);
		return network == null ? Collections.emptySet() : network.getMachines();
	}
}
