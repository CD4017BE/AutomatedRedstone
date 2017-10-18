package cd4017be.circuits;

import multiblock.IntegerComp;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry.ObjectHolder;
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
import cd4017be.circuits.tileEntity.MultiLever;
import cd4017be.circuits.tileEntity.Oszillograph;
import cd4017be.circuits.tileEntity.Potentiometer;
import cd4017be.circuits.tileEntity.WirelessConnector;
import cd4017be.lib.BlockItemRegistry;
import cd4017be.lib.block.BlockPipe;
import cd4017be.lib.block.OrientedBlock;
import cd4017be.lib.item.BaseItemBlock;
import cd4017be.lib.property.PropertyOrientation;
import cd4017be.lib.templates.TabMaterials;

/**
 * 
 * @author CD4017BE
 */
@EventBusSubscriber(modid = RedstoneCircuits.ID)
@ObjectHolder(value = RedstoneCircuits.ID)
public class Objects {

	//Capabilities
	@CapabilityInject(IntegerComp.class)
	public static Capability<IntegerComp> RS_INTEGER_CAPABILITY = null;

	//Creative Tabs
	public static TabMaterials tabCircuits = new TabMaterials(RedstoneCircuits.ID);

	//Blocks
	public static final OrientedBlock DESIGNER = null;
	public static final OrientedBlock ASSEMBLER = null;
	public static final BlockCircuit CIRCUIT = null;
	public static final BlockPipe RSP_32BIT = null;
	public static final BlockPipe RSP_BASIC = null;
	public static final OrientedBlock MULTILEVER = null;
	public static final OrientedBlock DISPLAY = null;
	public static final OrientedBlock SENSOR_READER = null;
	public static final OrientedBlock OSZILLOGRAPH = null;
	public static final OrientedBlock POTENTIOMETER = null;
	public static final OrientedBlock BIT_SHIFTER = null;
	public static final OrientedBlock FLUID_VALVE = null;
	public static final OrientedBlock ENERGY_VALVE = null;
	public static final OrientedBlock WIRELESS_CON = null;

	//ItemBlocks
	public static final BaseItemBlock designer = null;
	public static final BaseItemBlock assembler = null;
	public static final ItemCircuit circuit = null;
	public static final BaseItemBlock multilever = null;
	public static final BaseItemBlock potentiometer = null;
	public static final BaseItemBlock display = null;
	public static final BaseItemBlock sensor_reader = null;
	public static final BaseItemBlock oszillograph = null;
	public static final BaseItemBlock rsp_32bit = null;
	public static final ItemRSPipe rsp_basic = null;
	public static final BaseItemBlock bit_shifter = null;
	public static final BaseItemBlock fluid_valve = null;
	public static final BaseItemBlock energy_valve = null;
	public static final ItemWirelessCon wireless_con = null;

	//Items
	public static final ItemProgramm circuit_plan = null;
	public static final ItemItemSensor item_sensor = null;
	public static final ItemFluidSensor fluid_sensor = null;
	public static final ItemEnergySensor energy_sensor = null;
	public static final ItemTimeSensor time_sensor = null;

	/** creates and registers them all */
	public static void init() {
		Capabilities.registerIntern(IntegerComp.class);
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

	@SubscribeEvent
	public static void registerBlocks(RegistryEvent.Register<Block> ev) {
		ev.getRegistry().registerAll(
			OrientedBlock.create("designer", Material.ROCK, SoundType.STONE, 0, CircuitDesigner.class, PropertyOrientation.HOR_AXIS).setCreativeTab(tabCircuits),
			OrientedBlock.create("assembler", Material.ROCK, SoundType.STONE, 0, Assembler.class, PropertyOrientation.HOR_AXIS).setCreativeTab(tabCircuits),
			new BlockCircuit("circuit", Material.ROCK, SoundType.STONE, Circuit.class).setCreativeTab(tabCircuits),
			OrientedBlock.create("multilever", Material.ROCK, SoundType.STONE, 0, MultiLever.class, PropertyOrientation.HOR_AXIS).setCreativeTab(tabCircuits),
			OrientedBlock.create("potentiometer", Material.ROCK, SoundType.STONE, 0, Potentiometer.class, PropertyOrientation.HOR_AXIS).setCreativeTab(tabCircuits),
			OrientedBlock.create("display", Material.ROCK, SoundType.STONE, 0, Display8bit.class, PropertyOrientation.XY_12_ROT).setCreativeTab(tabCircuits).setLightLevel(0.375F),
			OrientedBlock.create("sensor_reader", Material.ROCK, SoundType.STONE, 3, BlockSensor.class, PropertyOrientation.ALL_AXIS).setBlockBounds(new AxisAlignedBB(0.125, 0.125, 0, 0.875, 0.875, 0.25)).setLightOpacity(0).setCreativeTab(tabCircuits),
			OrientedBlock.create("oszillograph", Material.ROCK, SoundType.STONE, 0, Oszillograph.class, PropertyOrientation.XY_12_ROT).setCreativeTab(tabCircuits).setLightLevel(0.375F),
			BlockPipe.create("rsp_32bit", Material.IRON, SoundType.METAL, IntegerPipe.class, 1).setSize(0.5).setLightOpacity(0).setCreativeTab(tabCircuits),
			BlockPipe.create("rsp_basic", Material.IRON, SoundType.METAL, BasicRSPipe.class, 3).setSize(0.25).setLightOpacity(0).setCreativeTab(tabCircuits),
			OrientedBlock.create("bit_shifter", Material.IRON, SoundType.METAL, 3, BitShifter.class, PropertyOrientation.ALL_AXIS).setBlockBounds(new AxisAlignedBB(0.25, 0.25, 0.0, 0.75, 0.75, 1.0)).setLightOpacity(0).setCreativeTab(tabCircuits),
			OrientedBlock.create("fluid_valve", Material.IRON, SoundType.METAL, 0, FluidValve.class, PropertyOrientation.ALL_AXIS).setCreativeTab(tabCircuits),
			OrientedBlock.create("energy_valve", Material.IRON, SoundType.METAL, 0, EnergyValve.class, PropertyOrientation.ALL_AXIS).setCreativeTab(tabCircuits),
			OrientedBlock.create("wireless_con", Material.IRON, SoundType.METAL, 3, WirelessConnector.class, PropertyOrientation.ALL_AXIS).setBlockBounds(new AxisAlignedBB(0.25, 0.25, 0.0, 0.75, 0.75, 0.875)).setLightOpacity(0).setCreativeTab(tabCircuits)
		);
	}

	@SubscribeEvent
	public static void registerItems(RegistryEvent.Register<Item> ev) {
		ev.getRegistry().registerAll(
			new BaseItemBlock(DESIGNER),
			new BaseItemBlock(ASSEMBLER),
			new ItemCircuit(CIRCUIT),
			new BaseItemBlock(MULTILEVER),
			new BaseItemBlock(POTENTIOMETER),
			new BaseItemBlock(DISPLAY),
			new BaseItemBlock(SENSOR_READER),
			new BaseItemBlock(OSZILLOGRAPH),
			new BaseItemBlock(RSP_32BIT),
			new ItemRSPipe(RSP_BASIC),
			new BaseItemBlock(BIT_SHIFTER),
			new BaseItemBlock(FLUID_VALVE),
			new BaseItemBlock(ENERGY_VALVE),
			new ItemWirelessCon(WIRELESS_CON),
			new ItemProgramm("circuit_plan").setCreativeTab(tabCircuits),
			new ItemItemSensor("item_sensor").setCreativeTab(tabCircuits),
			new ItemFluidSensor("fluid_sensor").setCreativeTab(tabCircuits),
			new ItemEnergySensor("energy_sensor").setCreativeTab(tabCircuits),
			new ItemTimeSensor("time_sensor").setCreativeTab(tabCircuits)
		);
	}

}
