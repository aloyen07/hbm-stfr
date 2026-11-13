package fun.aloyenz.stfr.tile;

import java.io.File;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import fun.aloyenz.stfr.reactor.StfrManager;
import fun.aloyenz.stfr.reactor.StfrReactor;
import fun.aloyenz.stfr.reactor.StfrStructureValidator;
import fun.aloyenz.stfr.structure.StfrBlockRole;
import fun.aloyenz.stfr.structure.StfrStructureTemplate;
import fun.aloyenz.stfr.tile.TileEntityStructureAnalyzer.AuditSummary;

/** Structure validation summaries and operator audit reports. */
final class StfrAnalyzerAudit {
	private StfrAnalyzerAudit() {}

	static void audit(TileEntityStructureAnalyzer c, EntityPlayer player, boolean save) {
		AuditSummary summary = c.inspect();
		player.sendMessage(
				new TextComponentTranslation(
						"message.hbmstfr.analyzer.audit", summary.wrong, summary.unassigned));
		StfrReactor reactor =
				StfrManager.get(c.getWorld()).get(c.getOrigin().add(StfrStructureTemplate.CENTER));
		if (reactor != null)
			player.sendMessage(
					new TextComponentTranslation(
							"message.hbmstfr.analyzer.reactor",
							reactor.id.toString(),
							reactor.wrongBlocks(),
							reactor.blockedAir(),
							reactor.missingPorts(),
							reactor.fixedHatchesWrong()));
		else
			player.sendMessage(new TextComponentTranslation("message.hbmstfr.analyzer.no_reactor"));
		if (save) {
			File output = c.saveRoles();
			player.sendMessage(
					new TextComponentTranslation(
							"message.hbmstfr.analyzer.saved", output.getPath()));
		}
	}

	static AuditSummary inspect(TileEntityStructureAnalyzer c) {
		int wrong = 0;
		int unassigned = 0;
		for (StfrStructureTemplate.Cell cell : StfrStructureTemplate.cells()) {
			BlockPos pos = c.getOrigin().add(cell.localPos);
			IBlockState actual =
					StfrStructureValidator.physicalState(
							c.getWorld(), pos, cell, c.getWorld().getBlockState(pos), c);
			StfrBlockRole role = c.getRole(cell.localPos);
			if (role.requiresAir()
					? !c.getWorld().isAirBlock(pos)
					: !StfrStructureValidator.matchesCell(cell, role, actual, c.getWorld(), pos, c))
				wrong++;
			if (role == StfrBlockRole.UNASSIGNED && cell.expected.getBlock() != Blocks.AIR)
				unassigned++;
		}
		for (BlockPos local : c.airRoles().keySet()) {
			if (!c.getWorld().isAirBlock(c.getOrigin().add(local))) wrong++;
		}
		return new AuditSummary(wrong, unassigned);
	}
}
