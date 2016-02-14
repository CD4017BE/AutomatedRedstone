/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cd4017be.circuits.tileEntity;

import java.io.IOException;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import cd4017be.circuits.item.ItemCircuit;
import cd4017be.lib.BlockItemRegistry;
import cd4017be.lib.TileContainer;
import cd4017be.lib.TileEntityData;
import cd4017be.lib.templates.AutomatedTile;
import cd4017be.lib.templates.IAutomatedInv;
import cd4017be.lib.templates.Inventory;
import cd4017be.lib.templates.Inventory.Component;
import cd4017be.lib.templates.SlotOutput;

/**
 *
 * @author CD4017BE
 */
public class Assembler extends AutomatedTile implements IAutomatedInv
{
    
    public Assembler()
    {
        netData = new TileEntityData(1, 3, 0, 0);
        inventory = new Inventory(this, 8, new Component(0, 1, -1), new Component(2, 3, 1), new Component(3, 6, 0));
    }

    @Override
    protected void customPlayerCommand(byte cmd, PacketBuffer dis, EntityPlayerMP player) throws IOException 
    {
        if (cmd == 0 && inventory.items[7] == null && inventory.items[6] != null && inventory.items[6].getItem() instanceof ItemCircuit) {
            ItemStack item = this.decrStackSize(6, 1);
            if (item.getTagCompound() == null) item.setTagCompound(new NBTTagCompound());
            int n = item.getTagCompound().getByte("InOut") & 0xff;
            int x = this.getInOut(n);
            item.getTagCompound().setByte("InOut", (byte)(n + x));
            this.removeInOut(x);
            n = item.getTagCompound().getByte("Gates") & 0xff;
            x = this.getGates(n);
            item.getTagCompound().setByte("Gates", (byte)(n + x));
            this.removeGates(x);
            n = item.getTagCompound().getByte("Count") & 0xff;
            x = this.getCount(n);
            item.getTagCompound().setByte("Count", (byte)(n + x));
            this.removeCount(x);
            this.setInventorySlotContents(7, item);
        }
    }

    @Override
    public void update() 
    {
        super.update();
        if (worldObj.isRemote) return;
        if (inventory.items[1] == null && inventory.items[0] != null && inventory.items[0].getItem() instanceof ItemCircuit) inventory.items[1] = this.decrStackSize(0, 1);
        ItemStack item = inventory.items[1];
        if (item != null) {
            int n0, n1, n2;
            if (item.getTagCompound() != null) {
                int n;
                n0 = item.getTagCompound().getByte("Gates") & 0xff;
                if (n0 > 0) {
                    if (inventory.items[3] == null) {
                        n = Math.min(n0, 64);
                        inventory.items[3] = new ItemStack(Items.redstone, n);
                    } else if (inventory.items[3].getItem() == Items.redstone) {
                        n = Math.min(n0, 64 - inventory.items[3].stackSize);
                        inventory.items[3].stackSize += n;
                    } else n = 0;
                    n0 -= n;
                    item.getTagCompound().setByte("Gates", (byte)n0);
                }
                n1 = item.getTagCompound().getByte("InOut") & 0xff;
                if (n1 > 0) {
                    if (inventory.items[4] == null) {
                        n = Math.min(n1, 64);
                        inventory.items[4] = new ItemStack(Blocks.lever, n);
                    } else if (inventory.items[4].getItem() == Item.getItemFromBlock(Blocks.lever)) {
                        n = Math.min(n1, 64 - inventory.items[4].stackSize);
                        inventory.items[4].stackSize += n;
                    } else n = 0;
                    n1 -= n;
                    item.getTagCompound().setByte("InOut", (byte)n1);
                }
                n2 = item.getTagCompound().getByte("Count") & 0xff;
                if (n2 > 0) {
                    if (inventory.items[5] == null) {
                        n = Math.min(n2, 32);
                        inventory.items[5] = new ItemStack(Items.quartz, n * 2);
                    } else if (inventory.items[5].getItem() == Items.quartz) {
                        n = Math.min(n2, (64 - inventory.items[4].stackSize) / 2);
                        inventory.items[4].stackSize += n * 2;
                    } else n = 0;
                    n2 -= n;
                    item.getTagCompound().setByte("Count", (byte)n2);
                }
                this.slotChange(inventory.items[6], inventory.items[6], 6);
            } else n0 = n1 = n2 = 0;
            if (n0 <= 0 && n1 <= 0 && n2 <= 0) {
                if (inventory.items[2] == null) {
                    inventory.items[2] = BlockItemRegistry.stack("tile.circuit", 1);
                    inventory.items[1] = null;
                } else if (inventory.items[2].stackSize < 64 && inventory.items[2].getItem() == Item.getItemFromBlock(BlockItemRegistry.blockId("circuit"))) {
                    inventory.items[2].stackSize++;
                    inventory.items[1] = null;
                }
            }
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) 
    {
        super.readFromNBT(nbt);
        this.slotChange(null, inventory.items[6], 6);
    }

    @Override
    public void initContainer(TileContainer container) 
    {
        container.addEntitySlot(new Slot(this, 0, 8, 16));
        container.addEntitySlot(new SlotOutput(this, 2, 8, 52));
        container.addEntitySlot(new Slot(this, 3, 44, 16));
        container.addEntitySlot(new Slot(this, 4, 44, 34));
        container.addEntitySlot(new Slot(this, 5, 44, 52));
        container.addEntitySlot(new Slot(this, 6, 152, 16));
        container.addEntitySlot(new SlotOutput(this, 7, 152, 52));
        container.addPlayerInventory(8, 86);
    }

    @Override
    public int[] stackTransferTarget(ItemStack item, int s, TileContainer container) 
    {
        int[] pi = container.getPlayerInv();
        if (s < pi[0] || s >= pi[1]) return pi;
        else if (item.getItem() == Items.redstone) return new int[]{2, 3};
        else if (item.getItem() == Item.getItemFromBlock(Blocks.lever)) return new int[]{3, 4};
        else if (item.getItem() == Items.quartz) return new int[]{4, 5};
        else if (item.getItem() instanceof ItemCircuit) return new int[]{5, 6};
        else return null;
    }
    
    @Override
    public boolean canInsert(ItemStack item, int cmp, int i) 
    {
        if (item == null) return true;
        else if (i == 3) return item.getItem() == Items.redstone;
        else if (i == 4) return item.getItem() == Item.getItemFromBlock(Blocks.lever);
        else if (i == 5) return item.getItem() == Items.quartz;
        else return true;
    }

    @Override
    public boolean canExtract(ItemStack item, int cmp, int i) 
    {
        return true;
    }

    @Override
    public boolean isValid(ItemStack item, int cmp, int i) 
    {
        return true;
    }

    @Override
    public void slotChange(ItemStack oldItem, ItemStack newItem, int i) 
    {
        if (i >= 3 && i <= 6) {
            if (inventory.items[6] != null && inventory.items[6].getItem() instanceof ItemCircuit && inventory.items[6].getTagCompound() != null) {
                netData.ints[0] = inventory.items[6].getTagCompound().getByte("InOut") & 0xff;
                netData.ints[1] = inventory.items[6].getTagCompound().getByte("Gates") & 0xff;
                netData.ints[2] = inventory.items[6].getTagCompound().getByte("Count") & 0xff;
            } else {
                netData.ints[0] = 0;//InOut
                netData.ints[1] = 0;//Gates
                netData.ints[2] = 0;//Count
            }
            netData.ints[0] += this.getInOut(netData.ints[0]);
            netData.ints[1] += this.getGates(netData.ints[1]);
            netData.ints[2] += this.getCount(netData.ints[2]);
        }
    }
    
    private int getInOut(int x)
    {
        int n = inventory.items[4] != null && inventory.items[4].getItem() == Item.getItemFromBlock(Blocks.lever) ? inventory.items[4].stackSize : 0;
        if (n + x > 16) n = 16 - x;
        return n;
    }
    
    private void removeInOut(int n)
    {
        this.decrStackSize(4, n);
    }
    
    private int getGates(int x)
    {
        int n = inventory.items[3] != null && inventory.items[3].getItem() == Items.redstone ? inventory.items[3].stackSize : 0;
        if (n + x > 128) n = 128 - x;
        return n;
    }
    
    private void removeGates(int n)
    {
        this.decrStackSize(3, n);
    }
    
    private int getCount(int x)
    {
        int n = inventory.items[5] != null && inventory.items[5].getItem() == Items.quartz ? inventory.items[5].stackSize / 2 : 0;
        if (n + x > 8) n = 8 - x;
        return n;
    }
    
    private void removeCount(int n)
    {
        this.decrStackSize(5, n * 2);
    }
    
}
