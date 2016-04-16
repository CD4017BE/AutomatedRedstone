/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cd4017be.circuits.tileEntity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import cd4017be.circuits.item.ItemCircuit;
import cd4017be.lib.BlockItemRegistry;
import cd4017be.lib.TileContainer;
import cd4017be.lib.TileEntityData;
import cd4017be.lib.templates.AutomatedTile;
import cd4017be.lib.templates.Inventory;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;

/**
 *
 * @author CD4017BE
 */
public class Programmer extends AutomatedTile implements ISidedInventory
{
    public static final byte C_OR = 0x00;
    public static final byte C_NOR = 0x10;
    public static final byte C_AND = 0x20;
    public static final byte C_NAND = 0x30;
    public static final byte C_XOR = 0x40;
    public static final byte C_XNOR = 0x50;
    public static final byte C_IN = 0x60;
    public static final byte C_EQ = 0x70;
    public static final byte C_LO = (byte)0x80;
    public static final byte C_HI = (byte)0x90;
    private static final byte[] defaultArray = new byte[16];
    static {
        Arrays.fill(defaultArray, (byte)0xff);
    }
    
    public byte[] outputs = Arrays.copyOf(defaultArray, defaultArray.length);
    public String[] gates = new String[]{""};
    public byte[] counter = Arrays.copyOf(defaultArray, defaultArray.length);
    public String name = "";
    public String message = "";
    
    public Programmer()
    {
        netData = new TileEntityData(1, 0, 0, 0);
        inventory = new Inventory(this, 1);
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) 
    {
        super.writeToNBT(nbt);
        this.save(nbt);
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) 
    {
        super.readFromNBT(nbt);
        this.load(nbt);
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) 
    {
        this.load(pkt.getNbtCompound());
        message = pkt.getNbtCompound().getString("msg");
    }

    @Override
    public Packet getDescriptionPacket() 
    {
        NBTTagCompound nbt = new NBTTagCompound();
        this.save(nbt);
        nbt.setString("msg", message);
        return new SPacketUpdateTileEntity(pos, -1, nbt);
    }

    @Override
    protected void customPlayerCommand(byte cmd, PacketBuffer dis, EntityPlayerMP player) throws IOException 
    {
        message = "";
        if (cmd == 0 && inventory.items[0] != null) {
            if (inventory.items[0].getItem() == Items.paper) {
                outputs = Arrays.copyOf(defaultArray, defaultArray.length);
                counter = Arrays.copyOf(defaultArray, defaultArray.length);
                gates = new String[]{""};
                name = "";
            } else if (inventory.items[0].getItem() == BlockItemRegistry.itemId("circuitPlan") && inventory.items[0].getTagCompound() != null) {
                this.load(inventory.items[0].getTagCompound());
                message = "Programm Loaded";
            }
        } else if (cmd == 1 && inventory.items[0] != null) {
            if (inventory.items[0].getItem() == Items.paper) inventory.items[0] = BlockItemRegistry.stack("item.circuitPlan", inventory.items[0].stackSize);
            else if (inventory.items[0].getItem() instanceof ItemCircuit) {
                if (inventory.items[0].getTagCompound() == null) inventory.items[0].setTagCompound(new NBTTagCompound());
                NBTTagCompound code = this.write();
                if (!message.equals("Compiling successfull")) {
                    this.markUpdate();
                    return;
                }
                int n = code.getTagList("Src", 8).tagCount();
                if ((inventory.items[0].getTagCompound().getByte("Gates") & 0xff) < n) {
                    message = "Not enought Gates: " + n + " needed!";
                    this.markUpdate();
                    return;
                } n = 0;
                for (byte b : code.getByteArray("Out"))
                    if (b >= 0) n++;
                if ((inventory.items[0].getTagCompound().getByte("InOut") & 0xff) < n) {
                    message = "Not enought IO-Ports: " + n + " needed!";
                    this.markUpdate();
                    return;
                } n = 0;
                byte[] b = code.getByteArray("Count");
                for (int j = 0; j < 8; j++) {
                    if (b[j] >= 0 || b[j + 8] >= 0) n++;
                }
                if ((inventory.items[0].getTagCompound().getByte("Count") & 0xff) < n) {
                    message = "Not enought Counter: " + n + " needed!";
                    this.markUpdate();
                    return;
                }
                NBTTagCompound progr = new NBTTagCompound();
                progr.setString("name", code.getString("name"));
                progr.setByteArray("code", code.getByteArray("Data"));
                progr.setByteArray("out", code.getByteArray("Out"));
                progr.setByteArray("cnt", code.getByteArray("Count"));
                inventory.items[0].getTagCompound().setTag("Progr", progr);
            }
            else if (inventory.items[0].getItem() != BlockItemRegistry.itemId("circuitPlan")) return;
            else inventory.items[0].setTagCompound(this.write());
        } else if (cmd == 2) { //setLine
            byte l = dis.readByte();
            if (l >= 0 && l < gates.length) gates[l] = dis.readStringFromBuffer(40);
        } else if (cmd == 3) { //addLine
            byte l = dis.readByte();
            if (l >= 0 && l <= gates.length){
                String[] buff = new String[gates.length + 1];
                if (l > 0) System.arraycopy(gates, 0, buff, 0, l);
                if (l < gates.length) System.arraycopy(gates, l, buff, l + 1, gates.length - l);
                gates = buff;
                gates[l] = "";
            }
        } else if (cmd == 4) { //deleteLine
            byte l = dis.readByte();
            if (l >= 0 && l < gates.length) {
                String[] buff = new String[gates.length - 1];
                if (l > 0) System.arraycopy(gates, 0, buff, 0, l);
                if (l < gates.length - 1) System.arraycopy(gates, l + 1, buff, l, gates.length - l - 1);
                gates = buff;
                if (gates.length == 0) gates = new String[]{""};
            }
        } else if (cmd == 5) { //setOutput
            byte l = dis.readByte();
            if (l >= 0 && l < outputs.length) outputs[l] = dis.readByte();
        } else if (cmd == 6) { //setCounter
            byte l = dis.readByte();
            if (l >= 0 && l < counter.length) counter[l] = dis.readByte();
        } else if (cmd == 7) {
            name = dis.readStringFromBuffer(40);
        }
        this.markUpdate();
    }
    
    private void load(NBTTagCompound nbt)
    {
        name = nbt.getString("name");
        outputs = nbt.getByteArray("Out");
        counter = nbt.getByteArray("Count");
        NBTTagList list = nbt.getTagList("Src", 8);
        gates = new String[list.tagCount()];
        for (int i = 0; i < gates.length; i++) gates[i] = list.getStringTagAt(i);
        if (gates.length == 0) gates = new String[]{""};
    }
    
    private void save(NBTTagCompound nbt)
    {
        nbt.setString("name", name);
        nbt.setByteArray("Out", outputs);
        nbt.setByteArray("Count", counter);
        NBTTagList list = new NBTTagList();
        for (String s : gates) list.appendTag(new NBTTagString(s));
        nbt.setTag("Src", list);
    }
    
    private NBTTagCompound write()
    {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setString("name", name);
        nbt.setByteArray("Out", outputs);
        nbt.setByteArray("Count", counter);
        NBTTagList list = new NBTTagList();
        for (String s : gates) list.appendTag(new NBTTagString(s));
        nbt.setTag("Src", list);
        int l = 0;
        try {
        ArrayList<Byte> data = new ArrayList<Byte>();
        for (l = 0; l < gates.length; l++) {
            String s0 = gates[l].substring(1);
            switch (gates[l].charAt(0)) {
                case '+': this.addParameters(data, C_OR, s0); break;
                case '-': this.addParameters(data, C_NOR, s0); break;
                case '&': this.addParameters(data, C_AND, s0); break;
                case '*': this.addParameters(data, C_NAND, s0); break;
                case '/': this.addParameters(data, C_XOR, s0); break;
                case '\\': this.addParameters(data, C_XNOR, s0); break;
                case '%':  data.add((byte)(C_IN | Byte.parseByte(s0.trim()))); break;
                case '#': this.compare(data, s0); break;
            }
        }
        byte[] b = new byte[data.size()];
        for (int i = 0; i < b.length; i++) {
            b[i] = data.get(i);
        }
        nbt.setByteArray("Data", b);
        message = "Compiling successfull";
        } catch(Exception e) {
            message = "Compile Error in line " + l;
        }
        return nbt;
    }
    
    private void addParameters(ArrayList<Byte> data, byte cmd, String s)
    {
        if (s.length() == 0) {
            data.add(cmd);
            return;
        }
        String[] s1 = s.split(",");
        data.add((byte)(cmd | s1.length));
        for (String s2 : s1) {
            data.add(Byte.parseByte(s2.trim()));
        }
    }
    
    private void compare(ArrayList<Byte> data, String s)
    {
        byte cmd = 0;
        int p0 = s.indexOf("=");
        int p1 = s.indexOf("<");
        int p2 = s.indexOf(">");
        if (p0 > 0) {
            cmd = C_EQ;
        } else if (p1 > 0) {
            p0 = p1;
            cmd = C_LO;
        } else if (p2 > 0) {
            p0 = p2;
            cmd = C_HI;
        }
        data.add((byte)(cmd | Byte.parseByte(s.substring(0, p0))));
        data.add((byte)Short.parseShort(s.substring(p0 + 1)));
    }

    @Override
    public void initContainer(TileContainer container) 
    {
        container.addEntitySlot(new Slot(this, 0, 152, 55));
        container.addPlayerInventory(8, 107);
    }
    
}
