package cd4017be.circuits;

import cd4017be.api.recipes.RecipeScriptContext;
import cd4017be.lib.BlockGuiHandler;
import cd4017be.lib.script.ScriptFiles.Version;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 *
 * @author CD4017BE
 */
@Mod(modid="Circuits", useMetadata = true)
public class RedstoneCircuits {

	@Instance("Circuits")
	public static RedstoneCircuits instance;

	@SidedProxy(clientSide="cd4017be.circuits.ClientProxy", serverSide="cd4017be.circuits.CommonProxy")
	public static CommonProxy proxy;

	public RedstoneCircuits() {
		RecipeScriptContext.scriptRegistry.add(new Version("automatedRedstone", 304, "/assets/circuits/config/recipes.rcp"));
	}

	@Mod.EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		Objects.init();
		proxy.registerBlocks();
		RecipeScriptContext.instance.run("automatedRedstone.PRE_INIT");
	}

	@Mod.EventHandler
	public void load(FMLInitializationEvent event) {
		BlockGuiHandler.registerMod(this);
		proxy.registerRenderers();
	}

}
