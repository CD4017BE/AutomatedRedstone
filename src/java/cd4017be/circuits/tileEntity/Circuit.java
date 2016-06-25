/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cd4017be.circuits.tileEntity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import cd4017be.api.circuits.IRedstone1bit;
import cd4017be.api.circuits.IRedstone8bit;
import cd4017be.api.computers.ComputerAPI;
import cd4017be.lib.TileContainer;
import cd4017be.lib.TileEntityData;
import cd4017be.lib.templates.AutomatedTile;
import cd4017be.lib.util.Utils;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.ICrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fml.common.Optional;
import net.minecraftforge.fml.common.Optional.Interface;

/**
 *
 * @author CD4017BE
 */
@Optional.InterfaceList(value = {@Interface(iface = "dan200.computercraft.api.peripheral.IPeripheral", modid = "ComputerCraft"), @Interface(iface = "li.cil.oc.api.network.Environment", modid = "OpenComputers")})
public class Circuit extends AutomatedTile implements IRedstone8bit, IRedstone1bit, Environment
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
    public String getName() 
    {
        return name.length() > 0 ? "\"" + name + "\"" : super.getName();
    }
    
    //CPU
    public final byte[] io = new byte[4];//byte 0,1 input; byte 2,3 output
    public final byte[] ram = new byte[16];//byte 0-7 gates; byte 8-15 counter
    //Programm
    public final byte[] obj = new byte[32];//byte 0-15 counter-bit-pointer; byte 16-31 output-bit-pointer 
    public byte[] code = new byte[0];//Programm Bytecode
    //Item
    public String name = "";
    public int var;
    private short timer = 0;
    private boolean update;
    private boolean updateCon;
    private boolean ticked = true;
    
    @Override
    public void update() 
    {
        super.update();
        if (worldObj.isRemote) return;
        if (updateCon) {
        	worldObj.notifyNeighborsOfStateChange(pos, this.getBlockType());
        	updateCon = false;
        }
        timer++;
        if (getConfig(12) == 0) timer = 0;
        if (timer >= netData.ints[0]) {
            timer = 0;
            int c;
            if (update) {
                io[0] = io[1] = 0;
                for (int i = 0; i < 6; i++) {
                    EnumFacing dir = EnumFacing.VALUES[i];
                    c = this.getConfig(i + 6);
                    if (c >= 2) continue;
                    TileEntity te = Utils.getTileOnSide(this, (byte)i);
                    if (te != null && te instanceof IRedstone8bit && ((IRedstone8bit)te).getDirection(i^1) > 0) {
                    	io[c] |= ((IRedstone8bit)te).getValue(i^1) & this.getConfig(i);
                    } else if ((te != null && te instanceof IRedstone1bit && ((IRedstone1bit)te).getBitDirection(i^1) > 0 && ((IRedstone1bit)te).getBitValue(i^1) > 0)
                        || worldObj.getRedstonePower(pos.offset(dir), dir) > 0) {
                    	io[c] |= this.getConfig(i);
                    }
                }
                update = false;
            }
            byte l2 = io[2], l3 = io[3];
            cpuTick();
            if (io[2] != l2 || io[3] != l3)
                for (int i = 0; i < 6; i++) {
                	EnumFacing dir = EnumFacing.VALUES[i];
                    if (this.getDirection(i) <= 0) continue;
                    TileEntity te = Utils.getTileOnSide(this, (byte)i);
                    if (te != null && te instanceof IRedstone8bit && ((IRedstone8bit)te).getDirection(i^1) < 0) {
                        ((IRedstone8bit)te).setValue(i^1, this.getValue(i), 1);
                    } else if (te != null && te instanceof IRedstone1bit && ((IRedstone1bit)te).getBitDirection(i^1) < 0) {
                    	((IRedstone1bit)te).setBitValue(i^1, this.getBitValue(i), 1);
                    } else {
                    	this.notifyStateChange(pos.offset(dir), dir.getOpposite());
                    }
                }
            ticked = true;
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
    	int c;
    	if (s < 6) {s *= 8; c = 0xff;}
    	else {s = s * 2 + 36; c = 3;}
        return (int)(netData.longs[0] >> s) & c;
    }
    
    public void setConfig(int s, int v)
    {
    	int c;
    	if (s < 6) {s *= 8; c = 0xff;}
    	else {s = s * 2 + 36; c = 3;}
        netData.longs[0] &= ~((long)c << s);
        netData.longs[0] |= (long)(v & c) << s;
    }

    @Override
    protected void customPlayerCommand(byte cmd, PacketBuffer dis, EntityPlayerMP player) throws IOException 
    {
        if (cmd == 2) Arrays.fill(ram, (byte)0);
        else if (cmd == 1) {
            netData.ints[0] = dis.readInt();
            if (netData.ints[0] < 1) netData.ints[0] = 1;
            else if (netData.ints[0] > 1200) netData.ints[0] = 1200;
        } else if (cmd == 0) {
        	netData.longs[0] = dis.readLong();
            updateCon = update = true;
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
        update = ticked = true;
    }

    @Override
    public ArrayList<ItemStack> dropItem(IBlockState m, int fortune) 
    {
        ArrayList<ItemStack> list = new ArrayList<ItemStack>();
        ItemStack item = new ItemStack(this.getBlockType());
        item.setTagCompound(new NBTTagCompound());
        item.getTagCompound().setByte("InOut", (byte)(var & 0xff));
        item.getTagCompound().setByte("Gates", (byte)(var >> 8 & 0xff));
        item.getTagCompound().setByte("Count", (byte)(var >> 16 & 0xff));
        if (code.length > 0) {
            NBTTagCompound nbt = new NBTTagCompound();
            nbt.setString("name", name);
            nbt.setByteArray("code", code);
            nbt.setByteArray("cnt", Arrays.copyOfRange(obj, 0, 16));
            nbt.setByteArray("out", Arrays.copyOfRange(obj, 16, 32));
            item.getTagCompound().setTag("Progr", nbt);
        }
        list.add(item);
        return list;
    }

    @Override
    public void onPlaced(EntityLivingBase entity, ItemStack item) 
    {
        if (item.getTagCompound() != null) {
            var = 0;
            var |= item.getTagCompound().getByte("InOut") & 0xff;
            var |= item.getTagCompound().getByte("Gates") << 8 & 0xff00;
            var |= item.getTagCompound().getByte("Count") << 16 & 0xff0000;
            if (item.getTagCompound().hasKey("Progr")) {
                NBTTagCompound nbt = item.getTagCompound().getCompoundTag("Progr");
                code = nbt.getByteArray("code");
                byte[] b = nbt.getByteArray("out");
                System.arraycopy(b, 0, obj, 16, Math.min(16, b.length));
                b = nbt.getByteArray("cnt");
                System.arraycopy(b, 0, obj, 0, Math.min(16, b.length));
                for (int i = 0; i < 8; i++) 
                	if (obj[i] >= 0) var |= 1 << (24 + i);
                name = nbt.getString("name");
            }
        }
    }

    @Override
	public boolean detectAndSendChanges(TileContainer container, List<ICrafting> crafters, PacketBuffer dos) throws IOException {
    	if (!ticked) {
    		dos.writeByte(0);
    		return false;
    	}
		ticked = false;
		int n = var >> 8 & 0xff;
		dos.writeByte(n);
		for (int i = 0; i < (n + 7) / 8; i++) dos.writeByte(ram[i]);
		n = var >> 24 & 0xff;
		dos.writeByte(n);
		for (int i = 0; i < 8; i++)
			if ((n >> i & 1) != 0)
				dos.writeByte(ram[i | 8]);
		dos.writeString(name);
    	return true;
	}

	@Override
	public void updateNetData(PacketBuffer dis, TileContainer container) throws IOException {
		super.updateNetData(dis, container);
		int n = dis.readByte() & 0xff;
		if (n == 0) return;
		var &= 0x00ff00ff;
		var |= n << 8;
		for (int i = 0; i < (n + 7) / 8; i++) ram[i] = dis.readByte();
		n = dis.readByte() & 0xff;
		var |= n << 24;
		for (int i = 0; i < 8; i++)
			if ((n >> i & 1) != 0)
				ram[i | 8] = dis.readByte();
		name = dis.readStringFromBuffer(32);
	}

	@Override
    public byte getValue(int s) 
    {
    	
        int c = this.getConfig(s + 6);
        if (c >= 2) return (byte)(io[c] & this.getConfig(s));
        else return 0;
    }

    @Override
    public byte getDirection(int s) 
    {
    	return (netData.longs[0] & 0xffL << (s * 8)) == 0 ? (byte)0 : (netData.longs[0] & 2L << (s * 2 + 48)) == 0 ? (byte)-1 : (byte)1;
    }

    @Override
    public void setValue(int s, byte v, int recursion) 
    {
        update = true;
    }

    @Override
    public byte getBitDirection(int s) 
    {
        return this.getDirection(s);
    }

    @Override
    public byte getBitValue(int s) 
    {
    	return this.getValue(s) != 0 ? (byte)15 : (byte)0;
    }

    @Override
    public void setBitValue(int s, byte v, int recursion) 
    {
        update = true;
    }
    
    private void notifyStateChange(BlockPos pos, EnumFacing except)
    {
        worldObj.notifyBlockOfStateChange(pos, this.getBlockType());
        worldObj.notifyNeighborsOfStateExcept(pos, this.getBlockType(), except);
    }
    
    @Override
    public int redstoneLevel(int s, boolean str) 
    {
        return this.getBitValue(s);
    }
    
    @Override
    public void onNeighborBlockChange(Block b) 
    {
        update = true;
    }

    //OpenComputers:
    
    private Object node = ComputerAPI.newOCnode(this, "RedstoneCircuits-Out8bit", false);
    
    @Override
	public void invalidate() {
		super.invalidate();
		ComputerAPI.removeOCnode(node);
	}

	@Override
	public void onChunkUnload() {
		super.onChunkUnload();
		ComputerAPI.removeOCnode(node);
	}

	@Optional.Method(modid = "OpenComputers")
	@Override
	public Node node() {
		return (Node)node;
	}

    @Optional.Method(modid = "OpenComputers")
	@Override
	public void onConnect(Node node) {}

    @Optional.Method(modid = "OpenComputers")
	@Override
	public void onDisconnect(Node node) {}

    @Optional.Method(modid = "OpenComputers")
	@Override
	public void onMessage(Message message) {}
    
    @Optional.Method(modid = "OpenComputers")
    @Callback(doc = "function(pos:int):int returns the 8 logic gate states at given byte position" ,direct = true)
    public Object[] getByte(Context cont, Arguments args) {
    	int id = args.checkInteger(0);
    	if (id >= 0 && id < ram.length) return new Object[]{(int)ram[id]};
    	else throw new IndexOutOfBoundsException();
    }
    
    @Optional.Method(modid = "OpenComputers")
    @Callback(doc = "function(pos:int, val:int[, mask:int]) sets the 8 logic gate states at given byte position. Use mask to only set specific bits" ,direct = true)
    public Object[] setByte(Context cont, Arguments args) {
    	int id = args.checkInteger(0);
    	int f = args.optInteger(2, 0xff);
    	int n = args.checkInteger(1) & f;
    	if (id >= 0 && id < ram.length) {
    		ram[id] &= ~f;
    		ram[id] |= n;
    		return new Object[0];
    	} else throw new IndexOutOfBoundsException();
    }
    
}
