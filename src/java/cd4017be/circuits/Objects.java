package cd4017be.circuits;

import multiblock.IntegerComp;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import cd4017be.api.Capabilities;
import cd4017be.api.recipes.RecipeScriptContext.ConfigConstants;
import cd4017be.circuits.block.*;
import cd4017be.circuits.item.*;
import cd4017be.circuits.tileEntity.Assembler;
import cd4017be.circuits.tileEntity.BasicRSPipe;
import cd4017be.circuits.tileEntity.BitShifter;
import cd4017be.circuits.tileEntity.BlockSensor;
import cd4017be.circuits.tileEntity.Circuit;
import cd4017be.circuits.tileEntity.CircuitDesigner;
import cd4017be.circuits.tileEntity.Display8bit;
import cd4017be.circuits.tileEntity.EnergyValve;
import cd4017be.circuits.tileEntity.FluidValve;
import cd4017be.circuits.tileEntity.IntegerPipe;
import cd4017be.circuits.tileEntity.InvConnector;
import cd4017be.circuits.tileEntity.MultiLever;
import cd4017be.circuits.tileEntity.Oszillograph;
import cd4017be.circuits.tileEntity.Potentiometer;
import cd4017be.circuits.tileEntity.WirelessConnector;
import cd4017be.lib.BlockItemRegistry;
import cd4017be.lib.DefaultItemBlock;
import cd4017be.lib.block.BlockPipe;
import cd4017be.lib.block.OrientedBlock;
import cd4017be.lib.templates.TabMaterials;
import cd4017be.lib.util.Orientation;

public class Objects {

	//Capabilities
	@CapabilityInject(IntegerComp.class)
	public static Capability<IntegerComp> RS_INTEGER_CAPABILITY = null;

	//Creative Tabs
	public static TabMaterials tabCircuits;

	//Blocks
	public static OrientedBlock designer;
	public static OrientedBlock assembler;
	public static BlockCircuit circuit;
	public static BlockPipe rsp8bit;
	public static BlockPipe rsp1bit;
	public static OrientedBlock lever8bit;
	public static OrientedBlock display8bit;
	public static OrientedBlock blockSensor;
	public static OrientedBlock oszillograph;
	public static OrientedBlock potentiometer;
	public static OrientedBlock bitShifter;
	public static OrientedBlock fluidValve;
	public static OrientedBlock energyValve;
	public static OrientedBlock wirelessCon;
	public static BlockPipe invConnector;

	//Items
	public static ItemProgramm circuitPlan;
	public static ItemItemSensor itemSensor;
	public static ItemFluidSensor fluidSensor;
	public static ItemEnergySensor energySensor;
	public static ItemTimeSensor timeSensor;

	/** creates and registers them all */
	public static void init() {
		tabCircuits = new TabMaterials("circuits");
		
		Capabilities.registerIntern(IntegerComp.class);
		
		new DefaultItemBlock((designer = OrientedBlock.create("designer", Material.ROCK, SoundType.STONE, 0, CircuitDesigner.class, Orientation.HOR_AXIS)).setCreativeTab(tabCircuits));
		new DefaultItemBlock((assembler = OrientedBlock.create("assembler", Material.ROCK, SoundType.STONE, 0, Assembler.class, Orientation.HOR_AXIS)).setCreativeTab(tabCircuits));
		new ItemCircuit((circuit = new BlockCircuit("circuit", Material.ROCK, SoundType.STONE, Circuit.class)).setCreativeTab(tabCircuits));
		new DefaultItemBlock((lever8bit = OrientedBlock.create("lever8bit", Material.ROCK, SoundType.STONE, 0, MultiLever.class, Orientation.XY_12_ROT)).setCreativeTab(tabCircuits));
		new DefaultItemBlock((potentiometer = OrientedBlock.create("potentiometer", Material.ROCK, SoundType.STONE, 0, Potentiometer.class, Orientation.XY_12_ROT)).setCreativeTab(tabCircuits));
		new DefaultItemBlock((display8bit = OrientedBlock.create("display8bit", Material.ROCK, SoundType.STONE, 0, Display8bit.class, Orientation.XY_12_ROT)).setCreativeTab(tabCircuits).setLightLevel(0.375F));
		new DefaultItemBlock((blockSensor = OrientedBlock.create("blockSensor", Material.ROCK, SoundType.STONE, 0, BlockSensor.class, Orientation.ALL_AXIS).setBlockBounds(new AxisAlignedBB(0.125, 0.125, 0, 0.875, 0.875, 0.25))).setCreativeTab(tabCircuits));
		new DefaultItemBlock((oszillograph = OrientedBlock.create("oszillograph", Material.ROCK, SoundType.STONE, 0, Oszillograph.class, Orientation.XY_12_ROT)).setCreativeTab(tabCircuits).setLightLevel(0.375F));
		new DefaultItemBlock((rsp8bit = BlockPipe.create("rsp8bit", Material.IRON, SoundType.METAL, IntegerPipe.class, 1).setSize(0.5)).setCreativeTab(tabCircuits));
		new ItemRSPipe((rsp1bit = BlockPipe.create("rsp1bit", Material.IRON, SoundType.METAL, BasicRSPipe.class, 3).setSize(0.25)).setCreativeTab(tabCircuits));
		new DefaultItemBlock((bitShifter = OrientedBlock.create("bitShifter", Material.IRON, SoundType.METAL, 0, BitShifter.class, Orientation.ALL_AXIS)).setBlockBounds(new AxisAlignedBB(0.25, 0.25, 0.0, 0.75, 0.75, 1.0)).setCreativeTab(tabCircuits));
		new DefaultItemBlock((fluidValve = OrientedBlock.create("fluidValve", Material.IRON, SoundType.METAL, 0, FluidValve.class, Orientation.ALL_AXIS)).setCreativeTab(tabCircuits));
		new DefaultItemBlock((energyValve = OrientedBlock.create("energyValve", Material.IRON, SoundType.METAL, 0, EnergyValve.class, Orientation.ALL_AXIS)).setCreativeTab(tabCircuits));
		new DefaultItemBlock((invConnector = BlockPipe.create("invConnector", Material.GLASS, SoundType.GLASS, InvConnector.class, 1).setSize(0.375)).setCreativeTab(tabCircuits).setHardness(0.5F));
		new ItemWirelessCon((wirelessCon = OrientedBlock.create("wirelessCon", Material.IRON, SoundType.METAL, 0, WirelessConnector.class, Orientation.ALL_AXIS)).setBlockBounds(new AxisAlignedBB(0.25, 0.25, 0.0, 0.75, 0.75, 0.875)).setCreativeTab(tabCircuits));
		
		(circuitPlan = new ItemProgramm("circuitPlan")).setCreativeTab(tabCircuits);
		(itemSensor = new ItemItemSensor("itemSensor")).setCreativeTab(tabCircuits);
		(fluidSensor = new ItemFluidSensor("fluidSensor")).setCreativeTab(tabCircuits);
		(energySensor = new ItemEnergySensor("energySensor")).setCreativeTab(tabCircuits);
		(timeSensor = new ItemTimeSensor("timeSensor")).setCreativeTab(tabCircuits);
		
		tabCircuits.item = new ItemStack(circuit);
	}

	public static void initConstants(ConfigConstants c) {
		c.getVect("circuit_ticks", Circuit.ClockSpeed);
		itemSensor.RangeSQ = c.getNumber("itemSensor_rangeSQ", 20);
		itemSensor.RangeSQ *= itemSensor.RangeSQ;
		fluidSensor.RangeSQ = c.getNumber("fluidSensor_rangeSQ", 20);
		fluidSensor.RangeSQ *= fluidSensor.RangeSQ;
		energySensor.RangeSQ = c.getNumber("energySensor_rangeSQ", 20);
		energySensor.RangeSQ *= energySensor.RangeSQ;
		timeSensor.RangeSQ = c.getNumber("timeSensor_rangeSQ", 20);
		timeSensor.RangeSQ *= timeSensor.RangeSQ;
		Assembler.materials[0] = BlockItemRegistry.stack("m.IORelay", 1);
		Assembler.materials[1] = BlockItemRegistry.stack("m.RAMPlate", 1);
		Assembler.materials[2] = BlockItemRegistry.stack("m.LogicPrc", 1);
		Assembler.materials[3] = BlockItemRegistry.stack("m.CalcPrc", 1);
	}
}
