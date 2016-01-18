/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cd4017be.circuits;

import cd4017be.api.computers.ComputerAPI;
import cd4017be.circuits.block.BlockInvConnector;
import cd4017be.circuits.block.BlockRSPipe1;
import cd4017be.circuits.block.BlockRSPipe8;
import cd4017be.circuits.item.ItemCircuit;
import cd4017be.circuits.item.ItemProgramm;
import cd4017be.circuits.item.ItemWireless8bit;
import cd4017be.circuits.tileEntity.Assembler;
import cd4017be.circuits.tileEntity.Circuit;
import cd4017be.circuits.tileEntity.Display8bit;
import cd4017be.circuits.tileEntity.InvConnector;
import cd4017be.circuits.tileEntity.InvReader;
import cd4017be.circuits.tileEntity.ItemTranslocator;
import cd4017be.circuits.tileEntity.Lever8bit;
import cd4017be.circuits.tileEntity.Programmer;
import cd4017be.circuits.tileEntity.RSPipe1;
import cd4017be.circuits.tileEntity.RSPipe8;
import cd4017be.circuits.tileEntity.RedstoneInterface;
import cd4017be.circuits.tileEntity.Wireless8bit;
import cd4017be.lib.BlockGuiHandler;
import cd4017be.lib.BlockItemRegistry;
import cd4017be.lib.DefaultItemBlock;
import cd4017be.lib.TileBlock;
import cd4017be.lib.TileBlockRegistry;
import cd4017be.lib.TileContainer;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;

/**
 *
 * @author CD4017BE
 */
@Mod(modid="AutomatedRedstone", name="Automated Redstone", version="2.1.0")
public class RedstoneCircuits 
{
    // The instance of your mod that Forge uses.
    @Instance("AutomatedRedstone")
    public static RedstoneCircuits instance;
    
    // Says where the client and server 'proxy' code is loaded.
    @SidedProxy(clientSide="cd4017be.circuits.ClientProxy", serverSide="cd4017be.circuits.CommonProxy")
    public static CommonProxy proxy;
    
    public static CreativeTabs tabCircuits;
    
    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) 
    {
        BlockItemRegistry.setMod("circuits");
        tabCircuits = new CreativeTabCircuits("circuits");
        initBlocks();
        initItems();
    }
    
    @Mod.EventHandler
    public void load(FMLInitializationEvent event) 
    {
        BlockGuiHandler.registerMod(this);
        proxy.registerRenderers();
        proxy.registerBlocks();
        proxy.registerRecipes();
        ComputerAPI.register();
    }
    
    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event)
    {
        
    }
    
    private void initBlocks()
    {
        TileBlockRegistry.register((TileBlock)(new TileBlock("programmer", Material.wood, DefaultItemBlock.class, 0x2).setCreativeTab(tabCircuits).setHardness(1.5F).setResistance(10F).setStepSound(Block.soundTypeWood)), Programmer.class, TileContainer.class);
        TileBlockRegistry.register((TileBlock)(new TileBlock("assembler", Material.rock, DefaultItemBlock.class, 0x2).setCreativeTab(tabCircuits).setHardness(1.5F).setResistance(10F).setStepSound(Block.soundTypeStone)), Assembler.class, TileContainer.class);
        TileBlockRegistry.register((TileBlock)(new TileBlock("circuit", Material.rock, ItemCircuit.class, 0x51).setCreativeTab(tabCircuits).setHardness(1.5F).setResistance(10F).setStepSound(Block.soundTypeStone)), Circuit.class, TileContainer.class);
        TileBlockRegistry.register((TileBlock)(new BlockRSPipe8("rsp8bit", Material.iron).setCreativeTab(tabCircuits).setHardness(1.0F).setResistance(10F).setStepSound(Block.soundTypeMetal)), RSPipe8.class, null);
        TileBlockRegistry.register((TileBlock)(new BlockRSPipe1("rsp1bit", Material.iron).setCreativeTab(tabCircuits).setHardness(1.0F).setResistance(10F).setStepSound(Block.soundTypeMetal)), RSPipe1.class, null);
        TileBlockRegistry.register((TileBlock)(new TileBlock("lever8bit", Material.rock, DefaultItemBlock.class, 0x3).setCreativeTab(tabCircuits).setHardness(1.5F).setResistance(10F).setStepSound(Block.soundTypeStone)), Lever8bit.class, null);
        TileBlockRegistry.register((TileBlock)(new TileBlock("display8bit", Material.rock, DefaultItemBlock.class, 0x3).setCreativeTab(tabCircuits).setHardness(1.5F).setResistance(10F).setStepSound(Block.soundTypeStone)), Display8bit.class, null);
        TileBlockRegistry.register((TileBlock)(new TileBlock("rsInterface", Material.rock, DefaultItemBlock.class, 0x11).setCreativeTab(tabCircuits).setHardness(1.5F).setResistance(10F).setStepSound(Block.soundTypeStone)), RedstoneInterface.class, TileContainer.class);
        TileBlockRegistry.register((TileBlock)(new TileBlock("wireless8bit", Material.iron, ItemWireless8bit.class, 0x51).setCreativeTab(tabCircuits).setHardness(2.0F).setResistance(20F).setStepSound(Block.soundTypeMetal)), Wireless8bit.class, null);
        TileBlockRegistry.register((TileBlock)(new TileBlock("invReader", Material.rock, DefaultItemBlock.class, 0x1).setCreativeTab(tabCircuits).setHardness(1.5F).setResistance(10F).setStepSound(Block.soundTypeStone)), InvReader.class, TileContainer.class);
        TileBlockRegistry.register((TileBlock)(new TileBlock("itemTranslocator", Material.rock, DefaultItemBlock.class, 0x1).setCreativeTab(tabCircuits).setHardness(1.5F).setResistance(10F).setStepSound(Block.soundTypeStone)), ItemTranslocator.class, TileContainer.class);
        TileBlockRegistry.register((TileBlock)(new BlockInvConnector("invConnector", Material.glass).setCreativeTab(tabCircuits).setHardness(0.5F).setResistance(10F).setStepSound(Block.soundTypeGlass)), InvConnector.class, null);
    }
    
    private void initItems()
    {
        String path = BlockItemRegistry.texPath();
        new ItemProgramm("circuitPlan", path+"circuitPlan");
    }
}
