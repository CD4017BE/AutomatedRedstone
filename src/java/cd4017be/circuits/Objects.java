package cd4017be.circuits;

import multiblock.IntegerComp;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import cd4017be.api.Capabilities;
import cd4017be.circuits.block.*;
import cd4017be.circuits.item.*;
import cd4017be.lib.DefaultItemBlock;
import cd4017be.lib.TileBlock;
import cd4017be.lib.templates.BlockPipe;

public class Objects {

	//Capabilities
	@CapabilityInject(IntegerComp.class)
	public static Capability<IntegerComp> RS_INTEGER_CAPABILITY = null;

	//Creative Tabs
	public static CreativeTabs tabCircuits;

	//Blocks
	public static TileBlock designer;
	public static TileBlock programmer;
	public static TileBlock assembler;
	public static TileBlock circuit;
	public static BlockPipe rsp8bit;
	public static BlockRSPipe1 rsp1bit;
	public static TileBlock lever8bit;
	public static TileBlock display8bit;
	public static TileBlock blockSensor;
	public static TileBlock oszillograph;
	public static TileBlock potentiometer;
	public static TileBlock bitShifter;
	public static BlockInvConnector invConnector;

	//Items
	public static ItemProgramm circuitPlan;
	public static ItemItemSensor itemSensor;
	public static ItemFluidSensor fluidSensor;
	public static ItemEnergySensor energySensor;
	public static ItemTimeSensor timeSensor;

	/** creates and registers them all */
	public static void init() {
		tabCircuits = new CreativeTabCircuits("circuits");
		
		Capabilities.registerIntern(IntegerComp.class);
		
		new DefaultItemBlock((designer = TileBlock.create("designer", Material.ROCK, SoundType.STONE, 0x1)).setCreativeTab(tabCircuits).setHardness(1.5F).setResistance(10F));
		new DefaultItemBlock((assembler = TileBlock.create("assembler", Material.ROCK, SoundType.STONE, 0x1)).setCreativeTab(tabCircuits).setHardness(1.5F).setResistance(10F));
		new ItemCircuit((circuit = new BlockCircuit("circuit", Material.ROCK, SoundType.STONE)).setCreativeTab(tabCircuits).setHardness(1.5F).setResistance(10F));
		new DefaultItemBlock((lever8bit = TileBlock.create("lever8bit", Material.ROCK, SoundType.STONE, 0x11)).setCreativeTab(tabCircuits).setHardness(1.5F).setResistance(10F));
		new DefaultItemBlock((potentiometer = TileBlock.create("potentiometer", Material.ROCK, SoundType.STONE, 0x11)).setCreativeTab(tabCircuits).setHardness(1.5F).setResistance(10F));
		new DefaultItemBlock((display8bit = TileBlock.create("display8bit", Material.ROCK, SoundType.STONE, 0x1)).setCreativeTab(tabCircuits).setLightLevel(0.375F).setHardness(1.5F).setResistance(10F));
		new DefaultItemBlock((blockSensor = TileBlock.create("blockSensor", Material.ROCK, SoundType.STONE, 0x10)).setCreativeTab(tabCircuits).setHardness(1.5F).setResistance(10F));
		new DefaultItemBlock((oszillograph = TileBlock.create("oszillograph", Material.ROCK, SoundType.STONE, 0x1)).setCreativeTab(tabCircuits).setLightLevel(0.375F).setHardness(1.5F).setResistance(10F));
		new DefaultItemBlock((rsp8bit = new BlockPipe("rsp8bit", Material.IRON, SoundType.METAL, 0x10)).setCreativeTab(tabCircuits).setHardness(1.0F).setResistance(10F));
		rsp8bit.size = 0.5F;
		new ItemRSPipe((rsp1bit = new BlockRSPipe1("rsp1bit", Material.IRON, SoundType.METAL)).setCreativeTab(tabCircuits).setHardness(1.0F).setResistance(10F));
		new DefaultItemBlock((bitShifter = TileBlock.create("bitShifter", Material.IRON, SoundType.METAL, 0x32)).setBlockBounds(new AxisAlignedBB(0.25, 0.25, 0.0, 0.75, 0.75, 1.0)).setCreativeTab(tabCircuits).setHardness(1.0F).setResistance(10F));
		new DefaultItemBlock((invConnector = new BlockInvConnector("invConnector", Material.GLASS, SoundType.GLASS)).setCreativeTab(tabCircuits).setHardness(0.5F).setResistance(10F));
		new DefaultItemBlock((programmer = TileBlock.create("programmer", Material.WOOD, SoundType.WOOD, 0x1)).setCreativeTab(tabCircuits).setHardness(1.5F).setResistance(10F));
		
		(circuitPlan = new ItemProgramm("circuitPlan")).setCreativeTab(tabCircuits);
		(itemSensor = new ItemItemSensor("itemSensor")).setCreativeTab(tabCircuits);
		(fluidSensor = new ItemFluidSensor("fluidSensor")).setCreativeTab(tabCircuits);
		(energySensor = new ItemEnergySensor("energySensor")).setCreativeTab(tabCircuits);
		(timeSensor = new ItemTimeSensor("timeSensor")).setCreativeTab(tabCircuits);
	}
}
