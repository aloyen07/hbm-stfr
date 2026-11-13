package fun.aloyenz.stfr.api.data;

import com.hbm.lib.DirPos;
import com.hbm.uninos.GenNode;
import net.minecraft.util.math.BlockPos;

public final class DataNode extends GenNode<DataNetwork> {

	public DataNode(BlockPos... positions) {
		super(DataNetworkApi.PROVIDER, positions);
	}

	@Override
	public DataNode setConnections(DirPos... connections) {
		return (DataNode) super.setConnections(connections);
	}
}
