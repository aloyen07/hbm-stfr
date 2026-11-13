package fun.aloyenz.stfr.block;

import com.hbm.blocks.ModBlocks;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.render.model.BlockCableBakedModel;
import com.hbm.tileentity.machine.TileEntityMachineFluidTank;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.statemap.StateMap;
import net.minecraft.client.renderer.block.statemap.StateMapperBase;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fluids.BlockFluidBase;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import fun.aloyenz.stfr.Tags;
import fun.aloyenz.stfr.item.STFRItems;
import fun.aloyenz.stfr.client.render.RenderDigammaDagger;
import fun.aloyenz.stfr.client.render.RenderElectrodeProxy;
import fun.aloyenz.stfr.client.render.RenderFuelSlot;
import fun.aloyenz.stfr.client.render.RenderLogicMonitor;
import fun.aloyenz.stfr.client.render.RenderLogicMonitorItem;
import fun.aloyenz.stfr.client.render.RenderRotorPlaceholder;
import fun.aloyenz.stfr.client.render.RenderStfrDebris;
import fun.aloyenz.stfr.client.render.RenderStfrFluidTank;
import fun.aloyenz.stfr.client.render.RenderStructureAnalyzer;
import fun.aloyenz.stfr.entity.EntityStfrDebris;
import fun.aloyenz.stfr.entity.EntityStfrDigammaDagger;
import fun.aloyenz.stfr.reactor.StfrFluids;
import fun.aloyenz.stfr.tile.TileEntityElectrodeProxy;
import fun.aloyenz.stfr.tile.TileEntityFuelSlot;
import fun.aloyenz.stfr.tile.TileEntityLogicDevice;
import fun.aloyenz.stfr.tile.TileEntityRotorPlaceholder;
import fun.aloyenz.stfr.tile.TileEntityStructureAnalyzer;

@Mod.EventBusSubscriber(modid = Tags.MODID)
public final class STFRBlocks {

	public static final CreativeTabs TAB =
			new CreativeTabs(Tags.MODID) {
				@Override
				public ItemStack createIcon() {
					return new ItemStack(STFRItems.STFR_REACTOR);
				}
			};

	public static final Block STFR_PLASMA_LENS =
			new BlockStfrLens("stfr_plasma_lens", false, true);
	public static final Block STFR_AUXILIARY_QUARTZ_LENS =
			new BlockStfrLens("stfr_auxiliary_quartz_lens", false, false);
	public static final Block STFR_FROZEN_PLASMA_LENS =
			new BlockStfrLens("stfr_frozen_plasma_lens", true, false);

	public static final Block DATA_CABLE = new BlockDataCable();
	public static final Block COVERED_LOGIC_CABLE =
			new BlockCoveredDataCable("covered_logic_cable", false);
	public static final Block RADIATION_SHIELDED_LOGIC_CABLE =
			new BlockCoveredDataCable("radiation_shielded_logic_cable", true);
	public static final Block MULTI_FLUID_PORT = new BlockMultiFluidPort();
	public static final Block FUEL_SLOT = new BlockFuelSlot();
	public static final Block CAPSULE_FILLER = new BlockCapsuleFiller();
	public static final Block STRUCTURE_ANALYZER = new BlockStructureAnalyzer();
	public static final Block ROTOR_PLACEHOLDER = new BlockRotorPlaceholder();
	public static final Block ELECTRODE_PROXY = new BlockElectrodeProxy();
	public static final Block REACTOR_LIGHT = new BlockReactorLight();
	public static final Block LOGIC_MAINFRAME = new BlockLogicMainframe();
	public static final Block MAINFRAME = new BlockLogicMainframe("mainframe", false);
	public static final Block LOGIC_TRANSMITTER =
			new BlockLogicDevice("logic_transmitter", BlockLogicDevice.Kind.TRANSMITTER);
	public static final Block LOGIC_RECEIVER =
			new BlockLogicDevice("logic_receiver", BlockLogicDevice.Kind.RECEIVER);
	public static final Block LOGIC_MONITOR = new BlockLogicMonitor();
	public static final Block STFR_ALARM =
			new BlockLogicDevice("stfr_alarm", BlockLogicDevice.Kind.ALARM);
	public static final Block LOGIC_BREAKER =
			new BlockLogicDevice("logic_breaker", BlockLogicDevice.Kind.BREAKER);
	public static final Block CRASH_SUPPRESSOR =
			new BlockLogicDevice("crash_suppressor", BlockLogicDevice.Kind.CRASH_SUPPRESSOR);
	public static final Block PRIVILEGES =
			new BlockLogicDevice("stfr_privileges", BlockLogicDevice.Kind.PRIVILEGES);
	public static final Block DETONATOR_CONNECTOR =
			new BlockLogicDevice("detonator_connector", BlockLogicDevice.Kind.DETONATOR_CONNECTOR);
	public static final Block AUTOPILOT_1 =
			new BlockLogicDevice("autopilot_1", BlockLogicDevice.Kind.AUTOPILOT_1);
	public static final Block AUTOPILOT_2 =
			new BlockLogicDevice("autopilot_2", BlockLogicDevice.Kind.AUTOPILOT_2);
	public static final Block AUTOPILOT_3 =
			new BlockLogicDevice("autopilot_3", BlockLogicDevice.Kind.AUTOPILOT_3);
	public static final Block DESTABILIZER =
			new BlockLogicDevice("destabilizer", BlockLogicDevice.Kind.DESTABILIZER);
	public static final Block SOLID_INJECTOR = new BlockSolidInjector();
	// Constructed during registration, once HBM's fluid registry is initialized.
	public static Block YARONITE;
	public static final Block TECHNETIUM_STAIRS = new BlockTechnetiumStairs();
	public static final Block VERTICAL_TECHNETIUM_STAIRS = new BlockVerticalStairs();
	public static final Block VERTICAL_BSCCO_STAIRS =
			new BlockVerticalStairs("vertical_bscco_stairs");
	public static final Block BSCCO_STAIRS = new BlockBsccoStairs();
	public static final Block BSCCO_COIL_STAIRS =
			new BlockBsccoCoilStairs("bscco_coil_stairs", false);
	public static final Block BSCCO_COIL_STAIRS_WELDED =
			new BlockBsccoCoilStairs("bscco_coil_stairs_welded", true);
	public static final Block VERTICAL_BSCCO_COIL_STAIRS =
			new BlockBsccoCoilVerticalStairs("vertical_bscco_coil_stairs", false);
	public static final Block VERTICAL_BSCCO_COIL_STAIRS_WELDED =
			new BlockBsccoCoilVerticalStairs("vertical_bscco_coil_stairs_welded", true);
	public static final Block COMBINE_STEEL_RIVETED =
			new BlockAllianceSteel("combine_steel_revited", false);
	public static final Block COMBINE_STEEL_STAIRS = new BlockAllianceSteelPlainStairs();
	public static final Block COMBINE_STEEL_VERTICAL_STAIRS =
			new BlockAllianceSteelPlainVerticalStairs();
	public static final Block COMBINE_STEEL_RIVETED_WELDED =
			new BlockAllianceSteel("combine_steel_revited_welded", true);
	public static final Block COMBINE_STEEL_RIVETED_STAIRS =
			new BlockAllianceSteelStairs("combine_steel_revited_stairs", false);
	public static final Block COMBINE_STEEL_RIVETED_WELDED_STAIRS =
			new BlockAllianceSteelStairs("combine_steel_revited_welded_stairs", true);
	public static final Block COMBINE_STEEL_RIVETED_VERTICAL_STAIRS =
			new BlockAllianceSteelVerticalStairs("vertical_combine_steel_revited_stairs", false);
	public static final Block COMBINE_STEEL_RIVETED_WELDED_VERTICAL_STAIRS =
			new BlockAllianceSteelVerticalStairs(
					"vertical_combine_steel_revited_welded_stairs", true);
	public static final Block TECHNETIUM_ELECTRODE = new BlockTechnetiumElectrode();
	public static final Block SCHRABIDATE_ELECTRODE =
			new BlockTechnetiumElectrode("schrabidate_electrode");
	public static final Block EUPHEMIUM_COATED_ELECTRODE =
			new BlockTechnetiumElectrode("euphemium_coated_electrode");
	public static final Block BSCCO_ELECTRODE_HANDLER =
			new BlockElectrodeControl("bscco_electrode_handler");
	public static final Block ELECTRODE_MECHANISM =
			new BlockElectrodeControl("electrode_mechanism");
	private static final Block[] ALL = {
		STFR_PLASMA_LENS, STFR_AUXILIARY_QUARTZ_LENS, STFR_FROZEN_PLASMA_LENS,
		DATA_CABLE,
		COVERED_LOGIC_CABLE,
		RADIATION_SHIELDED_LOGIC_CABLE,
		MULTI_FLUID_PORT,
		FUEL_SLOT,
		CAPSULE_FILLER,
		STRUCTURE_ANALYZER,
		ROTOR_PLACEHOLDER,
		ELECTRODE_PROXY,
		REACTOR_LIGHT,
		LOGIC_MAINFRAME,
		MAINFRAME,
		LOGIC_TRANSMITTER,
		LOGIC_RECEIVER,
		LOGIC_MONITOR,
		LOGIC_BREAKER,
		STFR_ALARM,
		CRASH_SUPPRESSOR,
		PRIVILEGES,
		DETONATOR_CONNECTOR,
		AUTOPILOT_1,
		AUTOPILOT_2,
		AUTOPILOT_3,
		DESTABILIZER,
		SOLID_INJECTOR,
		TECHNETIUM_STAIRS,
		VERTICAL_TECHNETIUM_STAIRS,
		VERTICAL_BSCCO_STAIRS,
		BSCCO_STAIRS,
		BSCCO_COIL_STAIRS,
		BSCCO_COIL_STAIRS_WELDED,
		VERTICAL_BSCCO_COIL_STAIRS,
		VERTICAL_BSCCO_COIL_STAIRS_WELDED,
		COMBINE_STEEL_RIVETED,
		COMBINE_STEEL_STAIRS,
		COMBINE_STEEL_VERTICAL_STAIRS,
		COMBINE_STEEL_RIVETED_WELDED,
		COMBINE_STEEL_RIVETED_STAIRS,
		COMBINE_STEEL_RIVETED_WELDED_STAIRS,
		COMBINE_STEEL_RIVETED_VERTICAL_STAIRS,
		COMBINE_STEEL_RIVETED_WELDED_VERTICAL_STAIRS,
		TECHNETIUM_ELECTRODE,
		SCHRABIDATE_ELECTRODE,
		EUPHEMIUM_COATED_ELECTRODE,
		BSCCO_ELECTRODE_HANDLER,
		ELECTRODE_MECHANISM
	};

	static {
		COVERED_LOGIC_CABLE.setCreativeTab(TAB);
		RADIATION_SHIELDED_LOGIC_CABLE.setCreativeTab(TAB);
	}

	private STFRBlocks() {}

	@SubscribeEvent
	public static void registerBlocks(RegistryEvent.Register<Block> event) {
		event.getRegistry().registerAll(ALL);
		StfrFluids.init();
		YARONITE = new BlockYaronite(StfrFluids.YARONITE.getFF());
		event.getRegistry().register(YARONITE);
	}

	@SubscribeEvent
	public static void registerItems(RegistryEvent.Register<Item> event) {
		for (Block block : ALL)
			if (block != ROTOR_PLACEHOLDER && block != ELECTRODE_PROXY && block != REACTOR_LIGHT)
				event.getRegistry()
						.register(new ItemBlock(block).setMaxStackSize(block == STFR_FROZEN_PLASMA_LENS ? 1 : 64)
								.setRegistryName(block.getRegistryName()));
	}

	@Mod.EventBusSubscriber(modid = Tags.MODID, value = Side.CLIENT)
	public static final class ClientRegistration {

		private ClientRegistration() {}

		@SubscribeEvent
		public static void registerModels(ModelRegistryEvent event) {
			ModelLoader.setCustomStateMapper(
					YARONITE, new StateMap.Builder().ignore(BlockFluidBase.LEVEL).build());
			RenderingRegistry.registerEntityRenderingHandler(
					EntityStfrDigammaDagger.class, RenderDigammaDagger::new);
			RenderingRegistry.registerEntityRenderingHandler(
					EntityStfrDebris.class, RenderStfrDebris::new);
			for (Block block : ALL) {
				if (block == ROTOR_PLACEHOLDER
						|| block == ELECTRODE_PROXY
						|| block == REACTOR_LIGHT) continue;
				ModelLoader.setCustomModelResourceLocation(
						Item.getItemFromBlock(block),
						0,
						new ModelResourceLocation(block.getRegistryName(), "inventory"));
			}
			Item.getItemFromBlock(LOGIC_MONITOR)
					.setTileEntityItemStackRenderer(new RenderLogicMonitorItem());
			ModelLoader.setCustomStateMapper(
					DATA_CABLE,
					new StateMapperBase() {
						@Override
						protected ModelResourceLocation getModelResourceLocation(
								IBlockState state) {
							return new ModelResourceLocation(
									DATA_CABLE.getRegistryName(), "normal");
						}
					});
		}

		@SubscribeEvent
		public static void bindTileRenderers(ModelRegistryEvent event) {
			ClientRegistry.bindTileEntitySpecialRenderer(
					TileEntityFuelSlot.class, new RenderFuelSlot());
			ClientRegistry.bindTileEntitySpecialRenderer(
					TileEntityStructureAnalyzer.class, new RenderStructureAnalyzer());
			ClientRegistry.bindTileEntitySpecialRenderer(
					TileEntityRotorPlaceholder.class, new RenderRotorPlaceholder());
			ClientRegistry.bindTileEntitySpecialRenderer(
					TileEntityElectrodeProxy.class, new RenderElectrodeProxy());
			ClientRegistry.bindTileEntitySpecialRenderer(
					TileEntityLogicDevice.class, new RenderLogicMonitor());
			RenderStfrFluidTank tankRenderer = new RenderStfrFluidTank();
			ClientRegistry.bindTileEntitySpecialRenderer(
					TileEntityMachineFluidTank.class, tankRenderer);
			Item tankItem = Item.getItemFromBlock(ModBlocks.machine_fluidtank);
			tankItem.setTileEntityItemStackRenderer(tankRenderer.getRenderer(tankItem));
		}

		@SubscribeEvent
		public static void stitchTextures(TextureStitchEvent.Pre event) {
			event.getMap().registerSprite(new ResourceLocation(Tags.MODID, "blocks/data_cable"));
		}

		@SubscribeEvent
		public static void bakeCable(ModelBakeEvent event) {
			ResourceLocation texture = new ResourceLocation(Tags.MODID, "blocks/data_cable");
			TextureAtlasSprite sprite =
					Minecraft.getMinecraft()
							.getTextureMapBlocks()
							.getAtlasSprite(texture.toString());
			HFRWavefrontObject obj =
					new HFRWavefrontObject(
							new ResourceLocation("hbm", "models/blocks/cable_neo.obj"));
			IBakedModel block = BlockCableBakedModel.forBlock(obj, sprite);
			IBakedModel item = BlockCableBakedModel.forItem(obj, sprite);
			event.getModelRegistry()
					.putObject(
							new ModelResourceLocation(DATA_CABLE.getRegistryName(), "normal"),
							block);
			event.getModelRegistry()
					.putObject(
							new ModelResourceLocation(DATA_CABLE.getRegistryName(), "inventory"),
							item);
		}
	}
}
