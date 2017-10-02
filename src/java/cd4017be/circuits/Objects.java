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
import cd4017be.lib.property.PropertyOrientation;
import cd4017be.lib.templates.TabMaterials;

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
	public static BlockPipe rsp_32bit;
	public static BlockPipe rsp_basic;
	public static OrientedBlock multilever;
	public static OrientedBlock display;
	public static OrientedBlock sensor_reader;
	public static OrientedBlock oszillograph;
	public static OrientedBlock potentiometer;
	public static OrientedBlock bit_shifter;
	public static OrientedBlock fluid_valve;
	public static OrientedBlock energy_valve;
	public static OrientedBlock wireless_con;
	public static BlockPipe inv_connector;

	//Items
	public static ItemProgramm circuit_plan;
	public static ItemItemSensor item_sensor;
	public static ItemFluidSensor fluid_sensor;
	public static ItemEnergySensor energy_sensor;
	public static ItemTimeSensor time_sensor;

	/** creates and registers them all */
	public static void init() {
		tabCircuits = new TabMaterials("circuits");
		
		Capabilities.registerIntern(IntegerComp.class);
		
		new DefaultItemBlock((designer = OrientedBlock.create("designer", Material.ROCK, SoundType.STONE, 0, CircuitDesigner.class, PropertyOrientation.HOR_AXIS)).setCreativeTab(tabCircuits));
		new DefaultItemBlock((assembler = OrientedBlock.create("assembler", Material.ROCK, SoundType.STONE, 0, Assembler.class, PropertyOrientation.HOR_AXIS)).setCreativeTab(tabCircuits));
		new ItemCircuit((circuit = new BlockCircuit("circuit", Material.ROCK, SoundType.STONE, Circuit.class)).setCreativeTab(tabCircuits));
		new DefaultItemBlock((multilever = OrientedBlock.create("multilever", Material.ROCK, SoundType.STONE, 0, MultiLever.class, PropertyOrientation.HOR_AXIS)).setCreativeTab(tabCircuits));
		new DefaultItemBlock((potentiometer = OrientedBlock.create("potentiometer", Material.ROCK, SoundType.STONE, 0, Potentiometer.class, PropertyOrientation.HOR_AXIS)).setCreativeTab(tabCircuits));
		new DefaultItemBlock((display = OrientedBlock.create("display", Material.ROCK, SoundType.STONE, 0, Display8bit.class, PropertyOrientation.XY_12_ROT)).setCreativeTab(tabCircuits).setLightLevel(0.375F));
		new DefaultItemBlock((sensor_reader = OrientedBlock.create("sensor_reader", Material.ROCK, SoundType.STONE, 3, BlockSensor.class, PropertyOrientation.ALL_AXIS).setBlockBounds(new AxisAlignedBB(0.125, 0.125, 0, 0.875, 0.875, 0.25))).setLightOpacity(0).setCreativeTab(tabCircuits));
		new DefaultItemBlock((oszillograph = OrientedBlock.create("oszillograph", Material.ROCK, SoundType.STONE, 0, Oszillograph.class, PropertyOrientation.XY_12_ROT)).setCreativeTab(tabCircuits).setLightLevel(0.375F));
		new DefaultItemBlock((rsp_32bit = BlockPipe.create("rsp_32bit", Material.IRON, SoundType.METAL, IntegerPipe.class, 1).setSize(0.5)).setLightOpacity(0).setCreativeTab(tabCircuits));
		new ItemRSPipe((rsp_basic = BlockPipe.create("rsp_basic", Material.IRON, SoundType.METAL, BasicRSPipe.class, 3).setSize(0.25)).setLightOpacity(0).setCreativeTab(tabCircuits));
		new DefaultItemBlock((bit_shifter = OrientedBlock.create("bit_shifter", Material.IRON, SoundType.METAL, 3, BitShifter.class, PropertyOrientation.ALL_AXIS)).setBlockBounds(new AxisAlignedBB(0.25, 0.25, 0.0, 0.75, 0.75, 1.0)).setLightOpacity(0).setCreativeTab(tabCircuits));
		new DefaultItemBlock((fluid_valve = OrientedBlock.create("fluid_valve", Material.IRON, SoundType.METAL, 0, FluidValve.class, PropertyOrientation.ALL_AXIS)).setCreativeTab(tabCircuits));
		new DefaultItemBlock((energy_valve = OrientedBlock.create("energy_valve", Material.IRON, SoundType.METAL, 0, EnergyValve.class, PropertyOrientation.ALL_AXIS)).setCreativeTab(tabCircuits));
		new DefaultItemBlock((inv_connector = BlockPipe.create("inv_connector", Material.GLASS, SoundType.GLASS, InvConnector.class, 1).setSize(0.375)).setLightOpacity(0).setCreativeTab(tabCircuits).setHardness(0.5F));
		new ItemWirelessCon((wireless_con = OrientedBlock.create("wireless_con", Material.IRON, SoundType.METAL, 3, WirelessConnector.class, PropertyOrientation.ALL_AXIS)).setBlockBounds(new AxisAlignedBB(0.25, 0.25, 0.0, 0.75, 0.75, 0.875)).setLightOpacity(0).setCreativeTab(tabCircuits));
		
		(circuit_plan = new ItemProgramm("circuit_plan")).setCreativeTab(tabCircuits);
		(item_sensor = new ItemItemSensor("item_sensor")).setCreativeTab(tabCircuits);
		(fluid_sensor = new ItemFluidSensor("fluid_sensor")).setCreativeTab(tabCircuits);
		(energy_sensor = new ItemEnergySensor("energy_sensor")).setCreativeTab(tabCircuits);
		(time_sensor = new ItemTimeSensor("time_sensor")).setCreativeTab(tabCircuits);
		
		tabCircuits.item = new ItemStack(circuit);
	}

	public static void initConstants(ConfigConstants c) {
		c.getVect("circuit_ticks", Circuit.ClockSpeed);
		item_sensor.RangeSQ = c.getNumber("itemSensor_rangeSQ", 20);
		item_sensor.RangeSQ *= item_sensor.RangeSQ;
		fluid_sensor.RangeSQ = c.getNumber("fluidSensor_rangeSQ", 20);
		fluid_sensor.RangeSQ *= fluid_sensor.RangeSQ;
		energy_sensor.RangeSQ = c.getNumber("energySensor_rangeSQ", 20);
		energy_sensor.RangeSQ *= energy_sensor.RangeSQ;
		time_sensor.RangeSQ = c.getNumber("timeSensor_rangeSQ", 20);
		time_sensor.RangeSQ *= time_sensor.RangeSQ;
		Assembler.materials[0] = BlockItemRegistry.stack("m.io_relay", 1);
		Assembler.materials[1] = BlockItemRegistry.stack("m.ram_plate", 1);
		Assembler.materials[2] = BlockItemRegistry.stack("m.logic_prc", 1);
		Assembler.materials[3] = BlockItemRegistry.stack("m.calc_prc", 1);
	}
}
