package cd4017be.circuits;

import cd4017be.api.recipes.RecipeScriptContext;
import cd4017be.circuits.block.*;
import cd4017be.circuits.item.*;
import cd4017be.lib.BlockGuiHandler;
import cd4017be.lib.DefaultItemBlock;
import cd4017be.lib.TileBlock;
import cd4017be.lib.script.ScriptFiles.Version;
import cd4017be.lib.templates.BlockPipe;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import static cd4017be.circuits.Objects.*;

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

	public static CreativeTabs tabCircuits;

	public RedstoneCircuits() {
		RecipeScriptContext.scriptRegistry.add(new Version("automatedRedstone", 200, "/assets/circuits/config/recipes.rcp"));
	}

	@Mod.EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		tabCircuits = new CreativeTabCircuits("circuits");
		initBlocks();
		initItems();
		RecipeScriptContext.instance.run("automatedRedstone.PRE_INIT");
	}

	@Mod.EventHandler
	public void load(FMLInitializationEvent event) {
		BlockGuiHandler.registerMod(this);
		proxy.registerRenderers();
	}

	private void initBlocks() {
		new DefaultItemBlock((programmer = TileBlock.create("programmer", Material.WOOD, SoundType.WOOD, 0x1)).setCreativeTab(tabCircuits).setHardness(1.5F).setResistance(10F));
		new DefaultItemBlock((assembler = TileBlock.create("assembler", Material.ROCK, SoundType.STONE, 0x1)).setCreativeTab(tabCircuits).setHardness(1.5F).setResistance(10F));
		new ItemCircuit((circuit = TileBlock.create("circuit", Material.ROCK, SoundType.STONE, 0x50)).setCreativeTab(tabCircuits).setHardness(1.5F).setResistance(10F));
		new DefaultItemBlock((rsp8bit = new BlockPipe("rsp8bit", Material.IRON, SoundType.METAL, 0x10)).setCreativeTab(tabCircuits).setHardness(1.0F).setResistance(10F));
		rsp8bit.size = 0.5F;
		new ItemRSPipe((rsp1bit = new BlockRSPipe1("rsp1bit", Material.IRON, SoundType.METAL)).setCreativeTab(tabCircuits).setHardness(1.0F).setResistance(10F));
		new DefaultItemBlock((lever8bit = TileBlock.create("lever8bit", Material.ROCK, SoundType.STONE, 0x11)).setCreativeTab(tabCircuits).setHardness(1.5F).setResistance(10F));
		new DefaultItemBlock((display8bit = TileBlock.create("display8bit", Material.ROCK, SoundType.STONE, 0x1)).setCreativeTab(tabCircuits).setLightLevel(0.375F).setHardness(1.5F).setResistance(10F));
		new DefaultItemBlock((invConnector = new BlockInvConnector("invConnector", Material.GLASS, SoundType.GLASS)).setCreativeTab(tabCircuits).setHardness(0.5F).setResistance(10F));
		new DefaultItemBlock((blockSensor = TileBlock.create("blockSensor", Material.ROCK, SoundType.STONE, 0x10)).setCreativeTab(tabCircuits).setHardness(1.5F).setResistance(10F));
		new DefaultItemBlock((oszillograph = TileBlock.create("oszillograph", Material.ROCK, SoundType.STONE, 0x1)).setCreativeTab(tabCircuits).setLightLevel(0.375F).setHardness(1.5F).setResistance(10F));
		new DefaultItemBlock((designer = TileBlock.create("designer", Material.IRON, SoundType.METAL, 0x1)).setCreativeTab(tabCircuits).setHardness(1.5F).setResistance(10F));
		proxy.registerBlocks();
	}

	private void initItems() {
		(circuitPlan = new ItemProgramm("circuitPlan")).setCreativeTab(tabCircuits);
		(itemSensor = new ItemItemSensor("itemSensor")).setCreativeTab(tabCircuits);
		(fluidSensor = new ItemFluidSensor("fluidSensor")).setCreativeTab(tabCircuits);
		(energySensor = new ItemEnergySensor("energySensor")).setCreativeTab(tabCircuits);
		(timeSensor = new ItemTimeSensor("timeSensor")).setCreativeTab(tabCircuits);
	}

}
