/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cd4017be.circuits.tileEntity;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import cd4017be.api.circuits.IRedstone1bit;
import cd4017be.api.circuits.IRedstone8bit;
import cd4017be.lib.TileEntityData;
import cd4017be.lib.templates.AutomatedTile;
import cd4017be.lib.util.Utils;
import net.minecraft.block.Block;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

/**
 *
 * @author CD4017BE
 */
public class Circuit extends AutomatedTile implements IRedstone8bit, IRedstone1bit
{
    
    public Circuit()
    {
        /**
         * int: tickrate
         * long: config
         */
        netData = new TileEntityData(1, 1, 0, 0);
        netData.ints[0] = 1;
    }

    @Override
    public String getInventoryName() 
    {
        return super.getInventoryName() + (name.length() > 0 ? " (".concat(name).concat(")") : "");
    }
    
    //CPU
    private final byte[] io = new byte[4];//byte 0,1 input; byte 2,3 output
    private final byte[] ram = new byte[16];//byte 0-7 gates; byte 8-15 counter
    //Programm
    private final byte[] obj = new byte[32];//byte 0-15 counter-bit-pointer; byte 16-31 output-bit-pointer 
    private byte[] code = new byte[0];//Programm Bytecode
    //Item
    public String name = "";
    private int var;
    private short timer = 0;
    private boolean update;
    
    @Override
    public void updateEntity() 
    {
        super.updateEntity();
        if (worldObj.isRemote) return;
        timer++;
        if (getConfig(6) == 0) timer = 0;
        if (timer >= netData.ints[0]) {
            timer = 0;
            byte d8, d1;
            int c;
            if (update) {
                io[0] = io[1] = 0;
                for (int i = 0; i < 6; i++) {
                    ForgeDirection dir = ForgeDirection.getOrientation(i);
                    d1 = this.getBitDirection(i);
                    d8 = this.getDirection(i);
                    if (d1 > -1 && d8 > -1) continue;
                    c = this.getConfig(i);
                    TileEntity te = Utils.getTileOnSide(this, (byte)i);
                    if (d8 < 0 && te != null && te instanceof IRedstone8bit && ((IRedstone8bit)te).getDirection(i^1) > 0) {
                        io[c >> 4] |= ((IRedstone8bit)te).getValue(i^1);
                    } else if (d1 < 0 && ((te != null && te instanceof IRedstone1bit && ((IRedstone1bit)te).getBitDirection(i^1) > 0 && ((IRedstone1bit)te).getBitValue(i^1))
                        || worldObj.getIndirectPowerOutput(xCoord + dir.offsetX, yCoord + dir.offsetY, zCoord + dir.offsetZ, i))) {
                        io[c >> 4] |= 1 << ((c & 0xf) - 2);
                    }
                }
                update = false;
            }
            byte l2 = io[2], l3 = io[3];
            cpuTick();
            if (io[2] != l2 || io[3] != l3)
                for (int i = 0; i < 6; i++)
                {
                    ForgeDirection dir = ForgeDirection.getOrientation(i);
                    d1 = this.getBitDirection(i);
                    d8 = this.getDirection(i);
                    if (d1 < 1 && d8 < 1) continue;
                    c = this.getConfig(i);
                    TileEntity te = Utils.getTileOnSide(this, (byte)i);
                    if (d8 > 0 && te != null && te instanceof IRedstone8bit && ((IRedstone8bit)te).getDirection(i^1) < 0) {
                        ((IRedstone8bit)te).setValue(i^1, io[c >> 4], 1);
                    } else if (d1 > 0) {
                        if (te != null && te instanceof IRedstone1bit && ((IRedstone1bit)te).getBitDirection(i^1) < 0) {
                            ((IRedstone1bit)te).setBitValue(i^1, (io[c >> 4] >> ((c & 0xf) - 2) & 1) != 0, 1);
                        } else {
                            this.notifyStateChange(xCoord + dir.offsetX, yCoord + dir.offsetY, zCoord + dir.offsetZ);
                        }
                    }
                }
        }
    }
    
    private void cpuTick()
    {
        boolean x = false;
        int cmd, con;
        byte n = 0;
        for (int i = 0, j = 0; i < code.length; j = ++i) {
            cmd = code[i] & 0xf0;
            con = code[i] & 0x0f;
            switch (cmd) {
                case Programmer.C_OR:
                    for (x = false; i < j + con;) x |= get(code[++i]);
                break;
                case Programmer.C_NOR:
                    for (x = false; i < j + con;) x |= get(code[++i]);
                    x = !x;
                break;
                case Programmer.C_AND:
                    for (x = true; i < j + con;) x &= get(code[++i]);
                break;
                case Programmer.C_NAND:
                    for (x = true; i < j + con;) x &= get(code[++i]);
                    x = !x;
                break;
                case Programmer.C_XOR:
                    for (x = false; i < j + con;) x ^= get(code[++i]);
                break;
                case Programmer.C_XNOR:
                    for (x = true; i < j + con;) x ^= get(code[++i]);
                break;
                case Programmer.C_IN:
                    x = (io[con >> 3] & (1 << (con & 7))) != 0;
                break;
                case Programmer.C_EQ:
                    x = ram[con] == code[++i];
                break;
                case Programmer.C_LO:
                    x = ram[con] < code[++i];
                break;
                case Programmer.C_HI:
                    x = ram[con] > code[++i];
                break;
            }
            set(n++, x);
        }
        for (int i = 0; i < 8; i++) {
            if (get(obj[i | 8])) ram[i | 8] = 0;
            else if (get(obj[i])) ram[i | 8]++;
        }
        io[2] = io[3] = 0;
        for (int i = 0; i < 16; i++) {
            if (get(obj[i | 16])) io[i >> 3 | 2] |= 1 << (i & 7);
        }
    }
    
    private boolean get(byte i)
    {
        return i >= 0 && (ram[i >> 3] & (1 << (i & 7))) != 0;
    }
    
    private void set(byte i, boolean s)
    {
        if (s) ram[i >> 3] |= 1 << (i & 7);
        else ram[i >> 3] &= ~(1 << (i & 7));
    }
    
    public int getConfig(int s)
    {
        return (int)(netData.longs[0] >> ((long)s * 6L) & 0x3fL);
    }
    
    public void setConfig(int s, int v)
    {
        netData.longs[0] &= ~(0x3fL << ((long)s * 6L));
        netData.longs[0] |= ((long)v & 0x3fL) << ((long)s * 6L);
    }

    @Override
    protected void customPlayerCommand(byte cmd, DataInputStream dis, EntityPlayerMP player) throws IOException 
    {
        if (cmd == 0) Arrays.fill(ram, (byte)0);
        else if (cmd == 1) {
            netData.ints[0] = dis.readInt();
            if (netData.ints[0] < 1) netData.ints[0] = 1;
            else if (netData.ints[0] > 1200) netData.ints[0] = 1200;
        } else if (cmd == 2) {
            netData.longs[0] = dis.readLong();
            update = true;
            worldObj.notifyBlocksOfNeighborChange(xCoord, yCoord, zCoord, this.getBlockType());
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) 
    {
        super.writeToNBT(nbt);
        nbt.setByteArray("ram", ram);
        nbt.setByteArray("io", io);
        nbt.setByteArray("obj", obj);
        nbt.setByteArray("code", code);
        nbt.setInteger("var", var);
        nbt.setString("name", name);
        nbt.setInteger("tick", netData.ints[0]);
        nbt.setLong("cfg", netData.longs[0]);
        nbt.setShort("timer", timer);
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) 
    {
        super.readFromNBT(nbt);
        byte[] b = nbt.getByteArray("ram");
        System.arraycopy(b, 0, ram, 0, Math.min(ram.length, b.length));
        b = nbt.getByteArray("io");
        System.arraycopy(b, 0, io, 0, Math.min(io.length, b.length));
        b = nbt.getByteArray("obj");
        System.arraycopy(b, 0, obj, 0, Math.min(obj.length, b.length));
        code = nbt.getByteArray("code");
        var = nbt.getInteger("var");
        name = nbt.getString("name");
        timer = nbt.getShort("timer");
        netData.ints[0] = nbt.getInteger("tick");
        netData.longs[0] = nbt.getLong("cfg");
        update = true;
    }

    @Override
    public ArrayList<ItemStack> dropItem(int m, int fortune) 
    {
        ArrayList<ItemStack> list = new ArrayList<ItemStack>();
        ItemStack item = new ItemStack(this.getBlockType());
        item.stackTagCompound = new NBTTagCompound();
        item.stackTagCompound.setByte("InOut", (byte)(var & 0xff));
        item.stackTagCompound.setByte("Gates", (byte)(var >> 8 & 0xff));
        item.stackTagCompound.setByte("Count", (byte)(var >> 16 & 0xff));
        if (code.length > 0) {
            NBTTagCompound nbt = new NBTTagCompound();
            nbt.setString("name", name);
            nbt.setByteArray("code", code);
            nbt.setByteArray("cnt", Arrays.copyOfRange(obj, 0, 16));
            nbt.setByteArray("out", Arrays.copyOfRange(obj, 16, 32));
            item.stackTagCompound.setTag("Progr", nbt);
        }
        list.add(item);
        return list;
    }

    @Override
    public void onPlaced(EntityLivingBase entity, ItemStack item) 
    {
        if (item.stackTagCompound != null) {
            var = 0;
            var |= item.stackTagCompound.getByte("InOut") & 0xff;
            var |= item.stackTagCompound.getByte("Gates") << 8 & 0xff00;
            var |= item.stackTagCompound.getByte("Count") << 16 & 0xff0000;
            if (item.stackTagCompound.hasKey("Progr")) {
                NBTTagCompound nbt = item.stackTagCompound.getCompoundTag("Progr");
                code = nbt.getByteArray("code");
                byte[] b = nbt.getByteArray("out");
                System.arraycopy(b, 0, obj, 16, Math.min(16, b.length));
                b = nbt.getByteArray("cnt");
                System.arraycopy(b, 0, obj, 0, Math.min(16, b.length));
                name = nbt.getString("name");
            }
        }
    }

    @Override
    public byte getValue(int s) 
    {
        int c = this.getConfig(s);
        int c1 = c & 0xf, c2 = c >> 4;
        if (c2 >= 2 && c1 == 1) return io[c2];
        else return 0;
    }

    @Override
    public byte getDirection(int s) 
    {
        int c = this.getConfig(s);
        if ((c & 0xf) != 1) return 0;
        else return (byte)(c < 0x20 ? -1 : 1);
    }

    @Override
    public void setValue(int s, byte v, int recursion) 
    {
        update = true;
    }

    @Override
    public byte getBitDirection(int s) 
    {
        int c = this.getConfig(s);
        if ((c & 0xf) >= 2) return c >= 0x20 ? (byte)1 : (byte)-1;
        else return 0;
    }

    @Override
    public boolean getBitValue(int s) 
    {
        int c = this.getConfig(s);
        int c1 = c & 0xf, c2 = c >> 4;
        if (c2 >= 2 && c1 >= 2) return (io[c2] & (1 << (c - 2))) != 0;
        else return false;
    }

    @Override
    public void setBitValue(int s, boolean v, int recursion) 
    {
        update = true;
    }
    
    private void notifyStateChange(int x, int y, int z)
    {
        worldObj.notifyBlockOfNeighborChange(x, y, z, this.getBlockType());
        worldObj.notifyBlocksOfNeighborChange(x, y, z, this.getBlockType());
    }
    
    @Override
    public int redstoneLevel(int s, boolean str) 
    {
        return this.getBitValue(s) ? 15 : 0;
    }
    
    @Override
    public void onNeighborBlockChange(Block b) 
    {
        update = true;
    }
    
}
