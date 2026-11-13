package fun.aloyenz.stfr;

import com.hbm.inventory.RecipesCommon;
import com.hbm.inventory.recipes.AssemblyMachineRecipes;
import com.hbm.inventory.recipes.BlastFurnaceRecipes;
import com.hbm.inventory.recipes.SolidificationRecipes;
import com.hbm.inventory.recipes.loader.SerializableRecipe;
import com.hbm.items.ModItems;
import java.util.HashMap;
import java.util.HashSet;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.minecraftforge.fml.common.registry.GameRegistry;
import org.apache.logging.log4j.LogManager;
import fun.aloyenz.stfr.crafting.StfrCrafting;
import fun.aloyenz.stfr.crafting.StfrHbmRecipes;
import fun.aloyenz.stfr.debug.StfrFakeCorePacket;
import fun.aloyenz.stfr.entity.EntityStfrDebris;
import fun.aloyenz.stfr.entity.EntityStfrDigammaDagger;
import fun.aloyenz.stfr.item.STFRItems;
import fun.aloyenz.stfr.logic.StfrLogicPacket;
import fun.aloyenz.stfr.reactor.StfrChunkLoader;
import fun.aloyenz.stfr.reactor.StfrFluids;
import fun.aloyenz.stfr.security.StfrSecurityGui;
import fun.aloyenz.stfr.structure.StfrRotorTemplate;
import fun.aloyenz.stfr.structure.StfrStructureTemplate;
import fun.aloyenz.stfr.tile.TileEntityCapsuleFiller;
import fun.aloyenz.stfr.tile.TileEntityDataCable;
import fun.aloyenz.stfr.tile.TileEntityElectrodeProxy;
import fun.aloyenz.stfr.tile.TileEntityFrozenLens;
import fun.aloyenz.stfr.tile.TileEntityFuelSlot;
import fun.aloyenz.stfr.tile.TileEntityLogicDevice;
import fun.aloyenz.stfr.tile.TileEntityMultiFluidPort;
import fun.aloyenz.stfr.tile.TileEntityRotorPlaceholder;
import fun.aloyenz.stfr.tile.TileEntitySolidInjector;
import fun.aloyenz.stfr.tile.TileEntityStructureAnalyzer;

@Mod(
		modid = Tags.MODID,
		name = "HBM:STFR",
		guiFactory = "fun.aloyenz.stfr.client.StfrEffectsGuiFactory",
		version = Tags.VERSION,
		acceptedMinecraftVersions = "[1.12.2]",
		dependencies = "required-after:mixinbooter@[10.7,);required-after:hbm@[2.6.0.0,);after:hbmspace@[0.9.2,)")
public final class STFR {

	@Mod.Instance(Tags.MODID)
	public static STFR instance;

	@Mod.EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		StfrConfig.load(event.getSuggestedConfigurationFile());
		StfrClientEffectsConfig.load(event.getModConfigurationDirectory());
		// HBM has initialized its base fluids. Register before client models
		// and HBM's container registry enumerate all available fluid types.
		StfrFluids.init();
		StfrLogicPacket.register();
		StfrFakeCorePacket.register();
		NetworkRegistry.INSTANCE.registerGuiHandler(this, new StfrSecurityGui());
		ForgeChunkManager.setForcedChunkLoadingCallback(this, StfrChunkLoader::restoreTickets);
		GameRegistry.registerTileEntity(
				TileEntityMultiFluidPort.class,
				new ResourceLocation(Tags.MODID, "multi_fluid_port"));
		GameRegistry.registerTileEntity(
				TileEntityDataCable.class, new ResourceLocation(Tags.MODID, "data_cable"));
		GameRegistry.registerTileEntity(
				TileEntityFuelSlot.class, new ResourceLocation(Tags.MODID, "fuel_slot"));
		GameRegistry.registerTileEntity(
				TileEntityCapsuleFiller.class, new ResourceLocation(Tags.MODID, "capsule_filler"));
		GameRegistry.registerTileEntity(
				TileEntityStructureAnalyzer.class,
				new ResourceLocation(Tags.MODID, "structure_analyzer"));
		GameRegistry.registerTileEntity(
				TileEntityRotorPlaceholder.class,
				new ResourceLocation(Tags.MODID, "rotor_placeholder"));
		GameRegistry.registerTileEntity(
				TileEntityElectrodeProxy.class,
				new ResourceLocation(Tags.MODID, "electrode_proxy"));
		GameRegistry.registerTileEntity(
				TileEntityLogicDevice.class, new ResourceLocation(Tags.MODID, "logic_device"));
		GameRegistry.registerTileEntity(
				TileEntitySolidInjector.class, new ResourceLocation(Tags.MODID, "solid_injector"));
		GameRegistry.registerTileEntity(TileEntityFrozenLens.class,
				new ResourceLocation(Tags.MODID, "frozen_lens"));
		EntityRegistry.registerModEntity(
				new ResourceLocation(Tags.MODID, "digamma_dagger"),
				EntityStfrDigammaDagger.class,
				"digamma_dagger",
				1,
				this,
				1024,
				1,
				true);
		EntityRegistry.registerModEntity(
				new ResourceLocation(Tags.MODID, "reactor_debris"),
				EntityStfrDebris.class, "reactor_debris", 2, this, 192, 2, true);
		event.getModLog().info("STFR %s initialized".formatted(Tags.VERSION));
	}

	@Mod.EventHandler
	public void init(FMLInitializationEvent event) {
		StfrFluids.init();
		// Add native defaults before HBM writes its templates; enabled HBM JSON
		// files are authoritative and are never overwritten by a late registration.
		SerializableRecipe.additionalListeners.add(
				handler -> {
					if ("AssemblyMachineRecipes".equals(handler))
						StfrHbmRecipes.addCoilWires(
								AssemblyMachineRecipes.INSTANCE.recipeNameMap.get(
										"ass.fusionblanket"));
					if ("SolidificationRecipes".equals(handler))
						SolidificationRecipes.registerRecipe(
								StfrFluids.YARONITE, 96, ModItems.billet_yharonite);
					if ("BlastFurnaceRecipes".equals(handler))
						BlastFurnaceRecipes.addRecipe(
								new RecipesCommon.ComparableStack(
										new ItemStack(STFRItems.YARONITE_DUST_TINY, 6)),
								ModItems.powder_flux,
								new ItemStack(ModItems.billet_yharonite));
				});
		// Validate the generated structure JSON during startup.
		StfrStructureTemplate.cells();
		StfrRotorTemplate.populate(new HashSet<>(), new HashMap<>(), new HashMap<>());
	}

	@Mod.EventHandler
	public void postInit(FMLPostInitializationEvent event) {
		StfrCrafting.register();
		LogManager.getLogger("STFR")
				.info(
						"Registered {} STFR machine recipes after HBM",
						StfrCrafting.recipes().size());
	}
}
