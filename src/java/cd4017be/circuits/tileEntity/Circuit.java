package cd4017be.circuits.tileEntity;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import cd4017be.api.circuits.IDirectionalRedstone;
import cd4017be.api.circuits.IQuickRedstoneHandler;
import cd4017be.circuits.tileEntity.CircuitDesigner.ModuleType;
import cd4017be.lib.BlockGuiHandler.ClientPacketReceiver;
import cd4017be.lib.Gui.DataContainer;
import cd4017be.lib.Gui.DataContainer.IGuiData;
import cd4017be.lib.block.AdvancedBlock.INeighborAwareTile;
import cd4017be.lib.block.AdvancedBlock.IRedstoneTile;
import cd4017be.lib.block.AdvancedBlock.ITilePlaceHarvest;
import cd4017be.lib.tileentity.BaseTileEntity;
import cd4017be.lib.util.TooltipUtil;
import cd4017be.lib.util.Utils;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
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
	private static final Random RANDOM = new Random();
	private static final int[] LIMIT = {Byte.MIN_VALUE, Short.MIN_VALUE, 0xff800000, Integer.MIN_VALUE,
										Byte.MAX_VALUE, Short.MAX_VALUE, 0x007fffff, Integer.MAX_VALUE};
	public static final byte 
		C_NULL = 0,	//padding
		C_IN = 1,	//input
		C_OR = 2,	//OR-gate
		C_NOR = 3,	//inverted OR-gate
		C_AND = 4,	//AND-gate
		C_NAND = 5,	//inverted AND-gate
		C_XOR = 6,	//XOR-gate
		C_NXOR = 7,	//inverted XOR-gate
		C_REP = 8,	//repeater
		C_NOT = 9,	//NOT-gate
		C_LS = 10,	//< comparator
		C_NLS = 11,	//>= comparator
		C_EQ = 12,	//== comparator
		C_NEQ = 13,	//!= comparator
		C_NEG = 14,	//negate operator
		C_ABS = 15,	//absolute function
		C_ADD = 16,	//add operator
		C_SUB = 17,	//subtract operator
		C_MUL = 18,	//multiply operator
		C_DIV = 19,	//division operator
		C_MOD = 20,	//modulo operator
		C_MIN = 21,	//minimum function
		C_MAX = 22,	//maximum function
		C_SWT = 23,	//binary switch
		C_CNT = 24,	//simple counter
		C_CNT_ = 25,//advanced counter
		C_RNG = 26,	//random generator
		C_SQRT = 27,//square root
		C_BSR = 28,	//>> operator
		C_BSL = 29,	//<< operator
		C_COMB = 30,//bit combiner
		C_FRG = 31,	//Fredkin gate
		C_RD = 32,	//mem reader
		C_WR = 33,	//mem writer
		C_SKIP = 63;//large padding

	public String name = "";
	/** var[0-7]: IO, var[8-15]: cap, var[16-23]: gates, var[24-31]: calc */
	public int var;
	public final IOacc[] ioacc = new IOacc[6];
	public IOcfg[] iocfg = new IOcfg[0];
	public byte[] ram = new byte[0];
	public int tickInt = ClockSpeed[0];
	private long nextUpdate = 0;
	public long usedAddr;
	public byte mode = 0;
	public int startIdx;
	private int readIdx;

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
				startIdx = 0;
				name = TooltipUtil.translate("gui.cd4017be.circuit.error");
			}
			markDirty();
		}
	}

	private int[] cpuTick() {
		int idxIO = 0, n = 0, x, s;
		for (readIdx = startIdx; readIdx < ram.length; readIdx++) {
			byte cmd = ram[readIdx];
			s = cmd >> 6 & 3;
			switch(cmd & 0x3f) {
			case C_NULL: n += s + 1; continue;
			case C_IN:
				IOcfg cfg = iocfg[idxIO++];
				x = ioacc[cfg.side].stateI >> cfg.ofs;
				break;
			case C_OR:
				for (x = 0; s >= 0; s--) x |= read8bit();
				ram[n++] = (byte)x;
				continue;
			case C_NOR:
				for (x = 0; s >= 0; s--) x |= read8bit();
				ram[n++] = (byte)~x;
				continue;
			case C_AND:
				for (x = 0xff; s >= 0; s--) x &= read8bit();
				ram[n++] = (byte)x;
				continue;
			case C_NAND:
				for (x = 0xff; s >= 0; s--) x &= read8bit();
				ram[n++] = (byte)~x;
				continue;
			case C_XOR:
				for (x = 0; s >= 0; s--) x ^= read8bit();
				ram[n++] = (byte)x;
				continue;
			case C_NXOR:
				for (x = 0xff; s >= 0; s--) x ^= read8bit();
				ram[n++] = (byte)x;
				continue;
			case C_REP: x = getNum(ram[++readIdx]); break;
			case C_NOT: ram[n++] = (byte)~read8bit(); continue;
			case C_LS: ram[n++] = getNum(ram[++readIdx]) < getNum(ram[++readIdx]) ? (byte)0xff : 0; continue;
			case C_NLS: ram[n++] = getNum(ram[++readIdx]) >= getNum(ram[++readIdx]) ? (byte)0xff : 0; continue;
			case C_EQ: ram[n++] = getNum(ram[++readIdx]) == getNum(ram[++readIdx]) ? (byte)0xff : 0; continue;
			case C_NEQ: ram[n++] = getNum(ram[++readIdx]) != getNum(ram[++readIdx]) ? (byte)0xff : 0; continue;
			case C_NEG: x = -getNum(ram[++readIdx]); break;
			case C_ABS: x = Math.abs(getNum(ram[++readIdx])); break;
			case C_ADD: x = getNum(ram[++readIdx]) + getNum(ram[++readIdx]); break;
			case C_SUB:	x = getNum(ram[++readIdx]) - getNum(ram[++readIdx]); break;
			case C_MUL: x = getNum(ram[++readIdx]) * getNum(ram[++readIdx]); break;
			case C_DIV: {
				int a = getNum(ram[++readIdx]), b = getNum(ram[++readIdx]);
				x = b != 0 ? Math.floorDiv(a, b) : LIMIT[s | (a >= 0 ? 4 : 0)];
			} break;
			case C_MOD: {
				int a = getNum(ram[++readIdx]), b = getNum(ram[++readIdx]);
				x = b != 0 ? Math.floorMod(a, b) : 0;
			} break;
			case C_MIN: x = Math.min(getNum(ram[++readIdx]), getNum(ram[++readIdx])); break;
			case C_MAX: x = Math.max(getNum(ram[++readIdx]), getNum(ram[++readIdx])); break;
			case C_SWT: x = getNum(readBool() ? ram[readIdx+2] : ram[readIdx+1]); readIdx+=2; break;
			case C_CNT: {
				boolean incr = readBool(), reset = readBool();
				if (reset) x = 0;
				else if (incr) x = getNum(n | s << 6) + 1;
				else { n += s + 1; continue; }
			} break;
			case C_CNT_: {
				boolean incr = readBool(), reset = readBool();
				readIdx+=2;
				if (reset) x = getNum(ram[readIdx]);
				else if (incr) x = getNum(n | s << 6) + getNum(ram[readIdx-1]);
				else { n += s + 1; continue; }
			} break;
			case C_RNG: {
				int a = getNum(ram[++readIdx]);
				x = a > 0 ? RANDOM.nextInt(a) : a < 0 ? -RANDOM.nextInt(-a) : 0;
			} break;
			case C_SQRT: {
				int a = getNum(ram[++readIdx]);
				x = a >= 0 ? sqrt(a) : -sqrt(-a);
			} break;
			case C_BSR: x = getNumU(ram[++readIdx]) >>> getNum(ram[++readIdx]); break;
			case C_BSL: x = getNumU(ram[++readIdx]) << getNum(ram[++readIdx]); break;
			case C_COMB: x = 0; s = s * 2 + 1;
				for (int mask = 1; s >= 0; s--, mask <<= 1) x |= read8bit() & mask;
				ram[n++] = (byte)x;
				continue;
			case C_FRG: {
				int a = read8bit(), b = read8bit(), c = read8bit() & (a ^ b);
				ram[n++] = (byte) (a ^ c);
				ram[n++] = (byte) (b ^ c);
			} continue;
			case C_RD: {
				s++;
				int i, i1 = ram[++readIdx] & 0x3f;
				if (i1 < n) {i = i1; i1 = n - s;}
				else {i = n + s; i1 -= s - 1;}
				i += (ram[ram[++readIdx] & 0x3f] & 0x7f) * s;
				if (i <= i1)
					for (; s > 0; s--)
						ram[n++] = ram[i++];
				else
					for (; s > 0; s--)
						ram[n++] = 0;
			} continue;
			case C_WR: {
				s++;
				int i, i1 = ram[++readIdx] & 0x3f;
				if (i1 < n) {i = i1; i1 = n;}
				else {i = n; i1 -= s - 1;}
				i += (ram[ram[++readIdx] & 0x3f] & 0xff) * s;
				readIdx++;
				n += s;
				if (i <= i1)
					for (x = getNum(ram[readIdx]); s > 0; s--, x >>= 8)
						ram[i++] = (byte)x;
			} continue;
			case C_SKIP: n += ram[++readIdx] & 0x3f; continue;
			default: throw new IllegalArgumentException("invalid command byte:" + cmd);
			}
			for (; s > 0; s--, x >>= 8) ram[n++] = (byte)x;
			ram[n++] = (byte)x;
		}
		int[] rsOut = new int[6];
		while (idxIO < iocfg.length) {
			IOcfg cfg = iocfg[idxIO++];
			int p = cfg.addr & 0x3f;
			x = 0;
			switch(cfg.addr & 0xc0) {
			case 0xc0: x |= ram[p + 3] << 24;
			case 0x80: x |= (ram[p + 2] & 0xff) << 16;
			case 0x40: x |= (ram[p + 1] & 0xff) << 8;
			default: x |= ram[p] & 0xff;
			}
			rsOut[cfg.side] |= x << cfg.ofs;
		}
		return rsOut;
	}

	private static int sqrt(int x) {
		int y = x, z = 4;
		while(y >= z) {
			y >>= 1;
			z <<= 1;
		}
		z >>= 2;
		while(y > z + 1) {
			y = (y + z) >> 1;
			z = x / y;
		}
		return y;
	}

	private boolean readBool() {
		int idx = ram[++readIdx];
		byte x = ram[idx & 0x3f];
		return (idx & 0xc0) < 0x80 ? x != 0 : (x & ram[++readIdx]) != 0;
	}

	private int read8bit() {
		int idx = ram[++readIdx];
		byte x = ram[idx & 0x3f];
		switch(idx & 0xc0) {
		default: x &= ram[++readIdx];
		case 0: return x != 0 ? 0xff : 0;
		case 0x40: return x;
		}
	}

	public int getNum(int idx) {
		int p = idx & 0x3f;
		switch(idx & 0xc0) {
		case 0x00: return ram[p];
		case 0x40: return ram[p] & 0xff | ram[++p] << 8;
		case 0x80: return ram[p] & 0xff | ram[++p] << 8 & 0xff00 | ram[++p] << 16;
		default: return ram[p] & 0xff | ram[++p] << 8 & 0xff00 | ram[++p] << 16 & 0xff0000 | ram[++p] << 24;
		}
	}

	public int getNumU(int idx) {
		int p = idx & 0x3f;
		switch(idx & 0xc0) {
		case 0x00: return ram[p] & 0xff;
		case 0x40: return ram[p] & 0xff | ram[++p] << 8 & 0xff00;
		case 0x80: return ram[p] & 0xff | ram[++p] << 8 & 0xff00 | ram[++p] << 16 & 0xff0000;
		default: return ram[p] & 0xff | ram[++p] << 8 & 0xff00 | ram[++p] << 16 & 0xff0000 | ram[++p] << 24;
		}
	}

	public int getMinInterval() {
		return ClockSpeed[getBlockMetadata() % ClockSpeed.length];
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
				neighborBlockChange(blockType, pos);
			}
			byte ofs = data.readByte();
			if (ofs < 0) cfg.ofs = 0;
			else if (ofs > 32 - cfg.size()) cfg.ofs = (byte)(32 - cfg.size());
			else cfg.ofs = ofs;
		} else if (cmd == 1) {
			mode = (byte)(data.readByte() & 3);
			if (mode == 1) {
				long t = world.getTotalWorldTime();
				nextUpdate = t + (long)tickInt - t % (long)tickInt;
			}
		} else if (cmd == 2) {
			int min = getMinInterval();
			tickInt = data.readInt();
			if (tickInt < min) tickInt = min;
			else if (tickInt > 1200) tickInt = 1200;
			if (mode == 1) {
				long t = world.getTotalWorldTime();
				nextUpdate = t + (long)tickInt - t % (long)tickInt;
			}
		} else if (cmd == 3) {
			for (int i = startIdx, j = 0; i < ram.length; i++) {
				//get op
				byte b = ram[i];
				if (b == C_SKIP) {
					j += ram[++i] & 0x3f;
					continue;
				}
				ModuleType mt = ModuleType.values()[b & 0x3f];
				//skip extra data
				int w = mt.varInAm ? ((b >> 6 & 3) + 1) * mt.group : mt.cons();
				for (int k = 0; k < w; k++)
					if (Assembler.extraByte(ram[++i], mt.conType(k))) i++;
				//get size
				int k = mt.isNum ? j + (b >> 6 & 3) : j + mt.size - 1;
				if (mt.cons() == 0) j = k + 1; //don't reset constants
				else {//reset bytes
					if (k >= startIdx) k = startIdx - 1;
					while (j <= k) ram[j++] = 0;
				}
			}
		} else if (cmd == 4) {
			int i = data.readByte() & 0x3f;
			if (i < startIdx) {
				ram[i] = data.readByte();
				long t = world.getTotalWorldTime();
				if (t > nextUpdate && mode > 1) {
					nextUpdate += tickInt + 1;
					if (t >= nextUpdate) nextUpdate = t + 1L;
				}
			}
		}
		markDirty();
	}

	private NBTTagCompound write(NBTTagCompound nbt) {
		nbt.setByte("IO", (byte)(var));
		nbt.setByte("Cap", (byte)(var >> 8));
		nbt.setByte("Gate", (byte)(var >> 16));
		nbt.setByte("Calc", (byte)(var >> 24));
		nbt.setByteArray("data", Arrays.copyOf(ram, ram.length));//Don't let other things use the same reference to this array
		nbt.setString("name", name);
		nbt.setLong("used", usedAddr);
		NBTTagList list = new NBTTagList();
		for (IOcfg cfg : iocfg) list.appendTag(cfg.write());
		nbt.setTag("io", list);
		return nbt;
	}

	private void read(NBTTagCompound nbt) {
		var = (nbt.getByte("IO") & 0xff) 
			| (nbt.getByte("Cap") & 0xff) << 8 
			| (nbt.getByte("Gate") & 0xff) << 16 
			| (nbt.getByte("Calc") & 0xff) << 24;
		ram = nbt.getByteArray("data");
		name = nbt.getString("name");
		usedAddr = nbt.getLong("used");
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
		startIdx = Math.min((var >> 8 & 0xff), ram.length);
		//backward compatibility:
		if (Long.bitCount(usedAddr) != startIdx)
			usedAddr = startIdx == 0 ? 0L : 0xffffffffffffffffL >>> (64 - startIdx);
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
		if (!world.isRemote && item.hasTagCompound()) {
			read(item.getTagCompound());
			markUpdate();
		}
	}

	@Override
	public SPacketUpdateTileEntity getUpdatePacket() {
		NBTTagCompound nbt = new NBTTagCompound();
		write(nbt);
		return new SPacketUpdateTileEntity(pos, -1, nbt);
	}

	@Override
	public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
		read(pkt.getNbtCompound());
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
	public void neighborTileChange(TileEntity te, EnumFacing side) {
	}

	@Override
	public byte getRSDirection(EnumFacing s) {
		IOacc acc = ioacc[s.ordinal()];
		return acc == null ? (byte)0 : (byte)(acc.dir & 3);
	}

	@Override
	public void initContainer(DataContainer container) {
		if (world.isRemote) Arrays.fill(ram, 0, startIdx, (byte)0);
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
		long chng1 = 0;
		for (int i = 0; i < startIdx; i++)
			if (ram[i] != ls.ram[i]) chng1 |= 1L << i;
		if (chng1 != 0) {
			dos.writeLong(chng1);
			chng |= 128;
			for (int i = 0; chng1 != 0; i++, chng1 >>>= 1)
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
			long chng1 = dis.readLong();
			for (int i = 0; chng1 != 0 && i < ram.length; i++, chng1 >>>= 1)
				if ((chng1 & 1) != 0) ram[i] = dis.readByte();
		}
	}

	@Override
	public int[] getSyncVariables() {
		return null;
	}

	@Override
	public void setSyncVariable(int i, int v) {
	}

	@Override
	public String getName() {
		return name.length() > 0 ? "\"" + name + "\"" : TooltipUtil.translate("gui.cd4017be.circuit.name");
	}

	private static final class LastState {
		int tickInt;
		byte mode;
		final short[] iocfg = new short[6];
		final byte[] ram = new byte[64];
	}

	public class IOcfg {
		public final String label;
		/**bit[0-5]: pos, bit[6-7]: size */
		public final byte addr;
		public final boolean dir;
		public byte ofs, side;
		IOcfg(boolean dir, String label, byte addr) {
			this.dir = dir; this.label = label; this.addr = addr;
		}
		IOcfg(NBTTagCompound tag) {
			dir = tag.getBoolean("d");
			label = tag.getString("n");
			addr = tag.getByte("p");
			side = tag.getByte("a");
			ofs = tag.getByte("o");
		}
		NBTTagCompound write() {
			NBTTagCompound tag = new NBTTagCompound();
			tag.setBoolean("d", dir);
			tag.setByte("p", addr);
			tag.setString("n", label);
			tag.setByte("a", side);
			tag.setByte("o", ofs);
			return tag;
		}
		public int size() {return (addr >> 3 & 24) + 8;}
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
					ICapabilityProvider t = Utils.neighborTile(Circuit.this, side);
					if (t instanceof IQuickRedstoneHandler) te = (IQuickRedstoneHandler)t;
				}
				if (te != null) te.onRedstoneStateChange(side.getOpposite(), state, Circuit.this);
				else world.neighborChanged(pos.offset(side), getBlockType(), pos);
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
			markDirty();
		}
	}

}
