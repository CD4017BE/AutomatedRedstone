package cd4017be.circuits.tileEntity;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import cd4017be.api.circuits.IDirectionalRedstone;
import cd4017be.api.circuits.IQuickRedstoneHandler;
import cd4017be.lib.BlockGuiHandler.ClientPacketReceiver;
import cd4017be.lib.TooltipInfo;
import cd4017be.lib.Gui.DataContainer;
import cd4017be.lib.Gui.DataContainer.IGuiData;
import cd4017be.lib.block.AdvancedBlock.INeighborAwareTile;
import cd4017be.lib.block.AdvancedBlock.IRedstoneTile;
import cd4017be.lib.block.AdvancedBlock.ITilePlaceHarvest;
import cd4017be.lib.block.BaseTileEntity;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.ICapabilityProvider;

/**
 *
 * @author CD4017BE
 */
public class Circuit extends BaseTileEntity implements INeighborAwareTile, IRedstoneTile, ITilePlaceHarvest, IDirectionalRedstone, IGuiData, ITickable, IQuickRedstoneHandler, ClientPacketReceiver {

	public static final int[] ClockSpeed = {20, 5, 1};
	public static final byte 
		C_NULL = 0x00,	//empty gates[0xf]
		C_IN = 0x10,	//input
		C_OR = 0x20,	//OR (bit^[0xf]) -> bit
		C_NOR = 0x30,	//NOR (bit^[0xf]) -> bit
		C_AND = 0x40,	//AND (bit^[0xf]) -> bit
		C_NAND = 0x50,	//NAND (bit^[0xf]) -> bit
		C_XOR = 0x60,	//XOR (bit^[0xf]) -> bit
		C_XNOR = 0x70,	//XNOR (bit^[0xf]) -> bit
		C_COMP = (byte)0x80,//compare[0xc] (N/C[1], N/C[2]) -> bit
		C_COMP_1 = (byte)0x81, C_COMP_2 = (byte)0x82, C_COMP_3 = (byte)0x83,
		C_CNT = (byte)0x90,	//counter (N/C[1], N/C[2], bit, bit) -> N[0xc]
		C_MUX = (byte)0xA0,	//multiplex (N/C[1], N/C[2], bit) -> N[0xc]
		C_ADD = (byte)0xB0,	//add (N/C[1], N/C[2]) -> N[0xc]
		C_SUB = (byte)0xC0,	//subtract (N/C[1], N/C[2]) -> N[0xc]
		C_MUL = (byte)0xD0,	//multiply (N/C[1], N/C[2]) -> N[0xc]
		C_DIV = (byte)0xE0,	//divide (N/C[1], N/C[2]) -> N[0xc]
		C_MOD = (byte)0xF0;	//modulo (N/C[1], N/C[2]) -> N[0xc]

	@Override
	public String getName() {
		return name.length() > 0 ? "\"" + name + "\"" : TooltipInfo.getLocFormat("gui.cd4017be.circuit.name");
	}

	public String name = "";
	/** var[0-7]: IO, var[8-15]: cap, var[16-23]: gates, var[24-31]: calc */
	public int var;
	public final IOacc[] ioacc = new IOacc[6];
	public IOcfg[] iocfg = new IOcfg[0];
	public byte[] ram = new byte[0];
	public int tickInt = ClockSpeed[0];
	private long nextUpdate = 0;
	public byte mode = 0;
	public byte memoryOfs;
	public int startIdx;

	public class IOcfg {
		public final String label;
		public final byte size, addr;
		public final boolean dir;
		public byte ofs, side;
		IOcfg(boolean dir, String label, byte size, byte addr) {
			this.dir = dir; this.label = label; this.size = size; this.addr = addr;
		}
		IOcfg(NBTTagCompound tag) {
			dir = tag.getBoolean("d");
			label = tag.getString("n");
			size = tag.getByte("s");
			addr = tag.getByte("p");
			side = tag.getByte("a");
			ofs = tag.getByte("o");
		}
		NBTTagCompound write() {
			NBTTagCompound tag = new NBTTagCompound();
			tag.setBoolean("d", dir);
			tag.setByte("p", addr);
			tag.setByte("s", size);
			tag.setString("n", label);
			tag.setByte("a", side);
			tag.setByte("o", ofs);
			return tag;
		}
	}

	class IOacc {
		int stateI, stateO;
		IQuickRedstoneHandler te;
		final EnumFacing side;
		byte dir;
		IOacc(int s) {side = EnumFacing.VALUES[s];}
		void update(int state) {
			if (state != stateO) {
				stateO = state;
				if (te == null || te.invalid()) {
					ICapabilityProvider t = getTileOnSide(side);
					if (t instanceof IQuickRedstoneHandler) te = (IQuickRedstoneHandler)t;
				}
				if (te != null) te.onRedstoneStateChange(side.getOpposite(), state, Circuit.this);
				else world.neighborChanged(pos.offset(side), blockType, pos);
			}
			if ((dir & 4) != 0) {
				state = world.getRedstonePower(pos.offset(side), side);
				if (state != stateI) setIn(state);
				dir &= 3;
			}
		}
		void setIn(int state) {
			long t = world.getTotalWorldTime();
			if (t != nextUpdate) {
				stateI = state;
				if (mode >= 2 && t > nextUpdate) {
					nextUpdate += tickInt + 1;
					if (t >= nextUpdate) nextUpdate = t + 1L;
				}
			} else dir |= 4;
		}
	}

	@Override
	public void update() {
		if (world.isRemote || mode == 0 || ram.length == 0) return;
		long t = world.getTotalWorldTime();
		if (t == nextUpdate) {
			if (mode == 1) nextUpdate += tickInt;
			else if (mode == 3) {nextUpdate++; mode = 7;}
			else {nextUpdate--; mode &= 3;}
			try {
				int[] out = cpuTick();
				for (IOacc acc : ioacc)
					if (acc != null)
						acc.update(out[acc.side.ordinal()]);
			} catch (Exception e) {
				e.printStackTrace();
				ram = new byte[0];
				name = "§cERROR: invalid code!§8";
			}
		}
	}

	private int[] cpuTick() {
		int m = 0;
		for (int n = 0, i = startIdx; i < ram.length; i++) {
			byte cmd = ram[i];
			int con = cmd & 0x0f;
			int p = n >> 3, x;
			cmd &= 0xf0;
			switch (cmd) {
			case C_NULL:
				n += con + 1;
				continue;
			case C_IN: {
				IOcfg cfg = iocfg[m++];
				x = ioacc[cfg.side].stateI >> cfg.ofs;
				if (cfg.size < 8) {
					int f = n & 7;
					int mask = 0xff >> (8 - cfg.size) << f;
					x <<= f;
					ram[p] = (byte)((ram[p] & ~mask) | (x & mask));
					n += cfg.size;
					continue;
				}
			} break;
			case C_OR: {
				int j = i; i += con;
				boolean s = true;
				while(s && j < i) s = !getBit(++j);
				int mask = 1 << (n & 7);
				if (s) ram[p] &= ~mask;
				else ram[p] |= mask;
			} n++; continue;
			case C_NOR: {
				int j = i; i += con;
				boolean s = true;
				while(s && j < i) s = !getBit(++j);
				int mask = 1 << (n & 7);
				if (s) ram[p] |= mask;
				else ram[p] &= ~mask;
			} n++; continue;
			case C_AND: {
				int j = i; i += con;
				boolean s = true;
				while(s && j < i) s = getBit(++j);
				int mask = 1 << (n & 7);
				if (s) ram[p] |= mask;
				else ram[p] &= ~mask;
			} n++; continue;
			case C_NAND: {
				int j = i; i += con;
				boolean s = true;
				while(s && j < i) s = getBit(++j);
				int mask = 1 << (n & 7);
				if (s) ram[p] &= ~mask;
				else ram[p] |= mask;
			} n++; continue;
			case C_XOR: {
				int j = i; i += con;
				boolean s = false;
				while(j < i) s ^= getBit(++j);
				int mask = 1 << (n & 7);
				if (s) ram[p] |= mask;
				else ram[p] &= ~mask;
			} n++; continue;
			case C_XNOR: {
				int j = i; i += con;
				boolean s = false;
				while(j < i) s ^= getBit(++j);
				int mask = 1 << (n & 7);
				if (s) ram[p] &= ~mask;
				else ram[p] |= mask;
			} n++; continue;
			case C_COMP: {
				boolean s;
				int a = param(ram[++i], con >> 2), b = param(ram[++i], con >> 3);
				switch(con & 3) {
				case 0: s = a < b; break;
				case 1: s = a >= b; break;
				case 2: s = a == b; break;
				default: s = a != b;
				}
				int mask = 1 << (n & 7);
				if (s) ram[p] |= mask;
				else ram[p] &= ~mask;
			} n++; continue;
			case C_CNT: {
				i += 4;
				if (getBit(i - 2)) x = param(ram[i], con >> 3);
				else if (getBit(i - 3)) x = param(p | (con & 3) << 6, 0) + param(ram[i - 1], con >> 2);
				else {n += (con & 3) * 8 + 8; continue;}
			} break;
			case C_MUX:
				x = getBit(++i) ? param(ram[i + 2], con >> 3) : param(ram[i + 1], con >> 2);
				i += 2;
				break;
			case C_ADD:
				x = param(ram[++i], con >> 2) + param(ram[++i], con >> 3);
				break;
			case C_SUB:
				x = param(ram[++i], con >> 2) - param(ram[++i], con >> 3);
				break;
			case C_MUL:
				x = param(ram[++i], con >> 2) * param(ram[++i], con >> 3);
				break;
			case C_DIV: {
				int a = param(ram[++i], con >> 2), b = param(ram[++i], con >> 3);
				x = b == 0 ? -1 : a / b;
			} break;
			case C_MOD: {
				int a = param(ram[++i], con >> 2), b = param(ram[++i], con >> 3);
				x = b == 0 ? -1 : a % b;
			} break;
			default: continue;//won't ever be called
			}
			con &= 3;
			n += con * 8 + 8;
			switch(con) {//store result
			case 3: ram[p + 3] = (byte)(x >> 24);
			case 2: ram[p + 2] = (byte)(x >> 16);
			case 1: ram[p + 1] = (byte)(x >> 8);
			default: ram[p] = (byte)x;
			}
		}
		int[] rsOut = new int[6];
		while (m < iocfg.length) {
			IOcfg cfg = iocfg[m++];
			int p = cfg.addr >> 3 & 0x1f, x;
			if (cfg.size < 8) {
				x = ram[p] >> (cfg.addr & 0x7);
				x &= 0xff >> (8 - cfg.size);
			} else {
				x = 0;
				switch(cfg.size) {
				case 32: x |= ram[p + 3] << 24;
				case 24: x |= (ram[p + 2] & 0xff) << 16;
				case 16: x |= (ram[p + 1] & 0xff) << 8;
				default: x |= ram[p] & 0xff;
				}
			}
			rsOut[cfg.side] |= x << cfg.ofs;
		}
		return rsOut;
	}

	private boolean getBit(int i) {
		int p = ram[i] & 0xff;
		return (ram[p >> 3 & 0x1f] & 1 << (p & 7)) != 0;
	}

	/**
	 * @param p bit[0-5]: index, bit[6,7]: size bit[8]: signed
	 * @return number
	 */
	private int param(int p, int sign) {
		int t = p & 0x3f;
		p |= sign << 8;
		switch(p & 0x1c0) {
		case 0x00: return ram[t] & 0xff;
		case 0x40: return (ram[t++] & 0xff) | (ram[t] & 0xff) << 8;
		case 0x80: return (ram[t++] & 0xff) | (ram[t++] & 0xff) << 8 | (ram[t] & 0xff) << 16;
		case 0x100: return ram[t];
		case 0x140: return (ram[t++] & 0xff) | ram[t] << 8;
		case 0x180: return (ram[t++] & 0xff) | (ram[t++] & 0xff) << 8 | ram[t] << 16;
		default: return (ram[t++] & 0xff) | (ram[t++] & 0xff) << 8 | (ram[t++] & 0xff) << 16 | ram[t] << 24;
		}
	}

	@Override
	public void onPacketFromClient(PacketBuffer data, EntityPlayer sender) throws IOException {
		byte cmd = data.readByte();
		if (cmd == 0) {
			IOcfg cfg = iocfg[data.readByte()];
			byte side = data.readByte();
			if (side != cfg.side) {
				byte dir = 0, prev = cfg.side;
				cfg.side = side;
				for (IOcfg c : iocfg)
					if (c.side == prev)
						dir |= c.dir ? 2 : 1;
				if (dir == 0) ioacc[prev] = null;
				else ioacc[prev].dir &= dir | 4;
				IOacc acc = ioacc[side];
				if (acc == null) ioacc[side] = acc = new IOacc(side);
				acc.dir |= cfg.dir ? 2 : 1;
			}
			byte ofs = data.readByte();
			if (ofs < 0) cfg.ofs = 0;
			else if (ofs > 32 - cfg.size) cfg.ofs = (byte)(32 - cfg.size);
			else cfg.ofs = ofs;
		} else if (cmd == 1) {
			mode = (byte)(data.readByte() & 3);
			if (mode == 1) {
				long t = world.getTotalWorldTime();
				nextUpdate = t + (long)tickInt - t % (long)tickInt;
			}
		} else if (cmd == 2) {
			int min = ClockSpeed[getBlockMetadata() % ClockSpeed.length];
			tickInt = data.readInt();
			if (tickInt < min) tickInt = min;
			else if (tickInt > 1200) tickInt = 1200;
			if (mode == 1) {
				long t = world.getTotalWorldTime();
				nextUpdate = t + (long)tickInt - t % (long)tickInt;
			}
		} else if (cmd == 3) {
			Arrays.fill(ram, 0, memoryOfs, (byte)0);
		} else if (cmd == 4) {
			int i = data.readByte() & 0xff;
			if (i < startIdx) ram[i >> 3] ^= 1 << (i & 7);
		}
	}

	private NBTTagCompound write(NBTTagCompound nbt) {
		nbt.setByte("IO", (byte)(var));
		nbt.setByte("Cap", (byte)(var >> 8));
		nbt.setByte("Gate", (byte)(var >> 16));
		nbt.setByte("Calc", (byte)(var >> 24));
		nbt.setByteArray("data", Arrays.copyOf(ram, ram.length));//Don't let other things use the same reference to this array
		nbt.setString("name", name);
		NBTTagList list = new NBTTagList();
		for (IOcfg cfg : iocfg) list.appendTag(cfg.write());
		nbt.setTag("io", list);
		nbt.setByte("ofs", memoryOfs);
		return nbt;
	}

	private void read(NBTTagCompound nbt) {
		var = (nbt.getByte("IO") & 0xff) 
			| (nbt.getByte("Cap") & 0xff) << 8 
			| (nbt.getByte("Gate") & 0xff) << 16 
			| (nbt.getByte("Calc") & 0xff) << 24
			| nbt.getInteger("var");//TODO can be removed in later version
		ram = nbt.getByteArray("data");
		name = nbt.getString("name");
		NBTTagList list = nbt.getTagList("io", 10);
		Arrays.fill(ioacc, null);
		iocfg = new IOcfg[list.tagCount()];
		for (int i = 0; i < iocfg.length; i++) {
			IOcfg cfg = new IOcfg(list.getCompoundTagAt(i));
			iocfg[i] = cfg;
			IOacc acc = ioacc[cfg.side];
			if (acc == null) ioacc[cfg.side] = acc = new IOacc(cfg.side);
			acc.dir |= cfg.dir ? 2 : 1;
		}
		memoryOfs = nbt.getByte("ofs");
		startIdx = Math.min((var >> 8 & 0xff), ram.length);
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
		write(nbt);
		int[] cache = new int[12];
		for (IOacc acc : ioacc)
			if (acc != null) {
				int i = acc.side.ordinal() * 2;
				cache[i] = acc.stateI;
				cache[i + 1] = acc.stateO;
			}
		nbt.setIntArray("out", cache);
		nbt.setByte("mode", mode);
		nbt.setInteger("tick", tickInt);
		return super.writeToNBT(nbt);
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		read(nbt);
		int[] o = nbt.getIntArray("out");
		for (IOacc acc : ioacc)
			if (acc != null) {
				int i = acc.side.ordinal() * 2;
				acc.stateI = o[i];
				acc.stateO = o[i + 1];
			}
		mode = nbt.getByte("mode");
		tickInt = nbt.getInteger("tick");
	}

	@Override
	public void setWorld(World world) {
		super.setWorld(world);
		if (mode == 1) {
			long t = world.getTotalWorldTime();
			nextUpdate = t + (long)tickInt - t % (long)tickInt;
		}
	}

	@Override
	public List<ItemStack> dropItem(IBlockState m, int fortune) {
		return makeDefaultDrops(write(new NBTTagCompound()));
	}

	@Override
	public void onPlaced(EntityLivingBase entity, ItemStack item) {
		if (item.hasTagCompound()) read(item.getTagCompound());
	}

	@Override
	public int redstoneLevel(EnumFacing side, boolean strong) {
		if (strong) return 0;
		IOacc acc = ioacc[side.ordinal()];
		return acc != null ? acc.stateO : 0;
	}

	@Override
	public boolean connectRedstone(EnumFacing side) {
		return getRSDirection(side) != 0;
	}

	@Override
	public void onRedstoneStateChange(EnumFacing side, int value, TileEntity src) {
		IOacc acc = ioacc[side.ordinal()];
		if (acc != null && value != acc.stateI && (acc.dir & 1) != 0) acc.setIn(value);
	}

	@Override
	public void neighborBlockChange(Block b, BlockPos src) {
		for (IOacc acc : ioacc)
			if (acc != null && (acc.dir & 1) != 0) {
				int rs = world.getRedstonePower(pos.offset(acc.side), acc.side);
				if (rs != acc.stateI) acc.setIn(rs);
			}
	}

	@Override
	public void neighborTileChange(BlockPos src) {
	}

	@Override
	public byte getRSDirection(EnumFacing s) {
		IOacc acc = ioacc[s.ordinal()];
		return acc == null ? (byte)0 : (byte)(acc.dir & 3);
	}

	@Override
	public void initContainer(DataContainer container) {
		if (world.isRemote) Arrays.fill(ram, 0, memoryOfs, (byte)0);
		else container.extraRef = new LastState();
	}

	@Override
	public boolean detectAndSendChanges(DataContainer container, PacketBuffer dos) {
		LastState ls = (LastState)container.extraRef;
		int p = dos.writerIndex();
		dos.writeByte(0);
		byte chng = 0;
		if (tickInt != ls.tickInt || mode != ls.mode) {
			dos.writeShort(ls.tickInt = tickInt);
			dos.writeByte(ls.mode = mode);
			chng |= 1;}
		for (int i = 0; i < iocfg.length; i++) {
			IOcfg cfg = iocfg[i];
			short x = (short)(cfg.ofs | cfg.side << 8);
			if (x != ls.iocfg[i]) {
				dos.writeShort(ls.iocfg[i] = x);
				chng |= 2 << i;
			}
		}
		int chng1 = 0;
		for (int i = 0; i < startIdx; i++)
			if (ram[i] != ls.ram[i]) chng1 |= 1 << i;
		if (chng1 != 0) {
			dos.writeInt(chng1);
			chng |= 128;
			for (int i = 0; chng1 != 0; i++, chng1 >>= 1)
				if ((chng1 & 1) != 0) dos.writeByte(ls.ram[i] = ram[i]);
		}
		if (chng == 0) return false;
		dos.setByte(p, chng);
		return true;
	}

	@Override
	public void updateClientChanges(DataContainer container, PacketBuffer dis) {
		byte chng = dis.readByte();
		if ((chng & 1) != 0) {
			tickInt = dis.readShort();
			mode = dis.readByte();
		}
		for (int i = 0; i < iocfg.length; i++)
			if ((chng >> i & 2) != 0) {
				IOcfg cfg = iocfg[i];
				short x = dis.readShort();
				cfg.ofs = (byte)x;
				cfg.side = (byte)(x >> 8);
			}
		if ((chng & 128) != 0) {
			int chng1 = dis.readInt();
			for (int i = 0; chng1 != 0 && i < ram.length; i++, chng1 >>= 1)
				if ((chng1 & 1) != 0) ram[i] = dis.readByte();
		}
	}

	private static final class LastState {
		int tickInt;
		byte mode;
		final short[] iocfg = new short[6];
		final byte[] ram = new byte[32];
	}

	@Override
	public boolean canPlayerAccessUI(EntityPlayer player) {
		return !player.isDead;
	}

	@Override
	public int[] getSyncVariables() {
		return null;
	}

	@Override
	public void setSyncVariable(int i, int v) {
	}

}
