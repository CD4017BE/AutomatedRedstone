package cd4017be.circuits;

import org.apache.logging.log4j.Logger;

import cd4017be.api.recipes.RecipeScriptContext;
import cd4017be.api.recipes.RecipeScriptContext.ConfigConstants;
import cd4017be.circuits.tileEntity.OC_ADC;
import cd4017be.circuits.tis3d.API;
import cd4017be.lib.script.ScriptFiles.Version;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 *
 * @author CD4017BE
 */
@Mod(modid = RedstoneCircuits.ID, useMetadata = true)
public class RedstoneCircuits {

	public static final String ID = "circuits";

	@Instance(ID)
	public static RedstoneCircuits instance;

	public static Logger LOG;

	@SidedProxy(clientSide="cd4017be.circuits.ClientProxy", serverSide="cd4017be.circuits.CommonProxy")
	public static CommonProxy proxy;

	public RedstoneCircuits() {
		RecipeScriptContext.scriptRegistry.add(new Version("automatedRedstone", "/assets/" + ID + "/config/recipes.rcp"));
	}

	@Mod.EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		LOG = event.getModLog();
		MinecraftForge.EVENT_BUS.register(proxy);
		RecipeScriptContext.instance.run("automatedRedstone.PRE_INIT");
	}

	@Mod.EventHandler
	public void load(FMLInitializationEvent event) {
		Objects.init();
		Objects.initConstants(new ConfigConstants(RecipeScriptContext.instance.modules.get("automatedRedstone")));
		proxy.registerBlocks();
		proxy.registerRenderers();
	}

	@Mod.EventHandler
	public void postInit(FMLPostInitializationEvent event) {
		if (Loader.isModLoaded("tis3d")) API.register();
		if (Loader.isModLoaded("opencomputers")) OC_ADC.registerAPI();
	}

}
