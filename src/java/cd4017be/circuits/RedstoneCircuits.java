package cd4017be.circuits;

import cd4017be.api.recipes.RecipeScriptContext;
import cd4017be.api.recipes.RecipeScriptContext.ConfigConstants;
import cd4017be.lib.script.ScriptFiles.Version;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
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

	@SidedProxy(clientSide="cd4017be.circuits.ClientProxy", serverSide="cd4017be.circuits.CommonProxy")
	public static CommonProxy proxy;

	public RedstoneCircuits() {
		RecipeScriptContext.scriptRegistry.add(new Version("automatedRedstone", 405, "/assets/" + ID + "/config/recipes.rcp"));
	}

	@Mod.EventHandler
	public void preInit(FMLPreInitializationEvent event) {
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

}
