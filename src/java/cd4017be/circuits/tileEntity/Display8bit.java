package cd4017be.circuits.tileEntity;

import java.io.IOException;

import cd4017be.api.circuits.IDirectionalRedstone;
import cd4017be.lib.BlockGuiHandler.ClientPacketReceiver;
import cd4017be.lib.Gui.DataContainer;
import cd4017be.lib.Gui.DataContainer.IGuiData;
import cd4017be.lib.block.AdvancedBlock.INeighborAwareTile;
import cd4017be.lib.tileentity.BaseTileEntity;
import cd4017be.lib.util.Orientation;
import cd4017be.lib.util.Utils;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 *
 * @author CD4017BE
 */
public class Display8bit extends BaseTileEntity implements INeighborAwareTile, IDirectionalRedstone, IGuiData, ClientPacketReceiver {

	private static final int OVERLOAD = 0x3_1E_2B_2B;

	public int state;
	/**bits[0,1]: mode, bit2: slave, {bit[4,5]: size, bit[6,7]: pos | bit[4,5,6]: pos + 4}, bits[8-11]: color */
	public short dspMode = 0xe02;
	public String text0 = "", text1 = "";
	public String format = "###";
	/**0xCN332211: C=color, N=symbol count, 123=symbols */
	public int display = 0xe3000000;

	@Override
	public void neighborBlockChange(Block b, BlockPos src) {
		if (world.isRemote) return;
		int nState = 0;
		for (EnumFacing s : EnumFacing.VALUES)
			nState |= world.getRedstonePower(pos.offset(s), s);
		if (nState != state) {
			state = nState;
			if ((dspMode & 4) == 0)
				this.markUpdate();
		}
	}

	@Override
	public void neighborTileChange(BlockPos src) {
	}

	@Override
	public void onPacketFromClient(PacketBuffer data, EntityPlayer sender) throws IOException {
		byte cmd = data.readByte();
		switch(cmd) {
		case 0:
			short prev = dspMode;
			dspMode &= 0xfff0;
			dspMode |= data.readByte() & 7;
			scanNeighbors(prev);
			return;
		case 1: format = data.readString(8); break;
		case 2: text0 = data.readString(32); break;
		case 3: text1 = data.readString(32); break;
		case 4:
			dspMode &= 0xf0ff;
			dspMode |= (data.readByte() & 15) << 8;
			break;
		}
		this.markUpdate();
	}

	private void scanNeighbors(short prev) {
		boolean update = (prev & 4) != 0;
		Orientation o = getOrientation();
		EnumFacing left = o.rotate(EnumFacing.EAST);
		if ((dspMode & 4) != 0) {
			update &= ((dspMode ^ prev) & 1) != 0;
			int dir = (dspMode << 1 & 2) - 1;
			int p = 0;
			for (int i = 1; i < 4; i++) {
				TileEntity te = Utils.getTileAt(world, pos.offset(left, i * dir));
				if (te instanceof Display8bit) {
					Display8bit dsp = (Display8bit)te;
					p = i * dir;
					if ((dsp.dspMode & 4) == 0) dsp.addSlave(p);
					else if ((dsp.dspMode & 1) == (dspMode & 1)) continue;
				} else p = (i - 1) * dir;
				break;
			}
			dspMode &= 0xff0f;
			dspMode |= (p + 4) << 4;
		} else {
			int p = 0;
			for (int i = 1; i < 4; i++) {
				TileEntity te = Utils.getTileAt(world, pos.offset(left, -i));
				if (te instanceof Display8bit) {
					Display8bit dsp = (Display8bit)te;
					if ((dsp.dspMode & 4) == 0 || (dsp.dspMode & 1) == 0) break;
					p = i;
				} else break;
			}
			int s = 0;
			for (int i = 1; i < 4 - p; i++) {
				TileEntity te = Utils.getTileAt(world, pos.offset(left, i));
				if (te instanceof Display8bit) {
					Display8bit dsp = (Display8bit)te;
					if ((dsp.dspMode & 4) == 0 || (dsp.dspMode & 1) != 0) break;
					s = i + p;
				} else break;
			}
			dspMode &= 0xff0f;
			dspMode |= s << 4 | p << 6;
		}
		if (update) {
			TileEntity te = Utils.getTileAt(world, pos.offset(left, (prev >> 4 & 7) - 4));
			if (te instanceof Display8bit) {
				Display8bit dsp = (Display8bit)te;
				if ((dsp.dspMode & 4) == 0) dsp.scanNeighbors(dsp.dspMode);
			}
		}
		markUpdate();
	}

	private void addSlave(int pos) {
		int p = dspMode >> 6 & 3, s = (dspMode >> 4 & 3) + 1;
		if (pos > p) {
			s += pos - p;
			p = pos;
		} else if (pos <= p - s) {
			s = p - pos + 1;
		} else return;
		if (--s > 3) s = 3;
		if (p > s) p = s;
		dspMode &= 0xff0f;
		dspMode |= s << 4 | p << 6;
		markUpdate();
	}

	private void updateFor(int pos) {
		if ((dspMode & 4) != 0) return;
		int p = dspMode >> 6 & 3, s = (dspMode >> 4 & 3) + 1;
		p -= pos;
		if (p >= 0 && p < s) formatState();
	}

	public void formatState() {
		Orientation o = getOrientation();
		EnumFacing left = o.rotate(EnumFacing.EAST);
		//handle slave mode
		if ((dspMode & 4) != 0) {
			int p = (dspMode >> 4 & 7) - 4;
			if (p == 0) return;
			BlockPos pos = this.pos.offset(left, p);
			TileEntity te = Utils.getTileAt(world, pos);
			if (te instanceof Display8bit) {
				Display8bit dsp = (Display8bit)te;
				if (dsp.getOrientation() == o)
					dsp.updateFor(p);
			}
			return;
		}
		//scan connected displays
		int p = dspMode >> 6 & 3, s = (dspMode >> 4 & 3) + 1;
		String format = "";
		Display8bit[] dsps = new Display8bit[s];
		int min = 0;
		for (int i = min; i < s; i++) {
			if (i == p) {
				dsps[i] = this;
				format = this.format + "\n" + format;
			} else {
				TileEntity te = Utils.getTileAt(world, pos.offset(left, i - p));
				if (te instanceof Display8bit) {
					dsps[i] = (Display8bit)te;
					format = dsps[i].format + "\n" + format;
				} else if (i < p) {
					min = i + 1;
					format = "";
				} else s = i;
			}
		}
		//handle mode
		int mode = dspMode & 3;
		int color = (dspMode & 0xf00) << 20;
		if (mode == 0 || mode == 3) {
			int st = state;
			for (int i = s - 1; i >= min; i--) {
				Display8bit dsp = dsps[i];
				int d = color | 0x4000000;
				for (int j = 0; j < 4; j++)
					d |= (st >> j & 1) | (st >> (j + 3) & 2) << (j * 4);
				dsp.display = d;
				st >>>= 8;
			}
			return;
		}
		long st = state & 0xffffffffL;
		//handle sign
		{
			int i, bit;
			if ((i = format.indexOf('+')) >= 0) bit = 24;
			else if ((i = format.indexOf('*')) >= 0) bit = 16;
			else if ((i = format.indexOf('~')) >= 0) bit = 8;
			else if ((i = format.indexOf('?')) >= 0) bit = 0;
			else bit = -1;
			if (bit >= 0) {
				long mask = 0xffffffff_ff_ff_ff_80L << bit;
				char sign;
				if ((st & mask) == 0) sign = '_';
				else {sign = '-'; st = -(st | mask);}
				format = format.substring(0, i) + sign + format.substring(i + 1);
			}
		}
		//parse string
		int base = mode == 2 ? 10 : 16;
		Display8bit dsp = null;
		int n = 0;
		for (int i = format.length() - 1; i >= 0; i--) {
			int d;
			char c = format.charAt(i);
			switch(c) {
			case '\n':
				if (dsp != null) dsp.display |= n << 24 | color;
				dsp = dsps[min++];
				dsp.display = 0;
				n = 0;
				continue;
			case '#':
				d = (int)(st % base) + 16;
				st /= base;
				break;
			case '$':
				st /= base;
				continue;
			case '-': d = 0x34; break;
			case '.': case ',': d = 0x35; break;
			case '"': case '\'': d = 0x36; break;
			case ':': case ';': d = 0x37; break;
			case '=': d = 0x38; break;
			case '<': d = 0x39; break;
			case '>': d = 0x3a; break;
			case '/': d = 0x3b; break;
			case '\\': d = 0x3c; break;
			case '%': d = 0x3d; break;
			case '^': case '\u00b0': d = 0x3e; break;
			case '_': d = 0x3f; break;
			default:
				if (c >= '0' && c <= '9') d = c - '0' + 0x10;
				else if (c >= 'a' && c <= 'z') d = c - 'a' + 0x1a;
				else if (c >= 'A' && c <= 'Z') d = c - 'A' + 0x1a;
				else d = 0x0f;
			}
			if (n < 3) dsp.display |= (d & 0xff) << (8 * n++);
		}
		if (dsp != null)
			if (st > 0) dsp.display = OVERLOAD | color;
			else dsp.display |= n << 24 | color;
	}

	@Override
	public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
		state = pkt.getNbtCompound().getInteger("state");
		dspMode = pkt.getNbtCompound().getShort("dsp");
		text0 = pkt.getNbtCompound().getString("t0");
		text1 = pkt.getNbtCompound().getString("t1");
		format = pkt.getNbtCompound().getString("form");
		formatState();
	}

	@Override
	public SPacketUpdateTileEntity getUpdatePacket() {
		NBTTagCompound nbt = new NBTTagCompound();
		nbt.setInteger("state", state);
		nbt.setShort("dsp", dspMode);
		nbt.setString("t0", text0);
		nbt.setString("t1", text1);
		nbt.setString("form", format);
		return new SPacketUpdateTileEntity(pos, -1, nbt);
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
		nbt.setInteger("state", state);
		nbt.setShort("mode", dspMode);
		nbt.setString("t0", text0);
		nbt.setString("t1", text1);
		nbt.setString("form", format);
		return super.writeToNBT(nbt);
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		state = nbt.getInteger("state");
		dspMode = nbt.getShort("mode");
		text0 = nbt.getString("t0");
		text1 = nbt.getString("t1");
		format = nbt.getString("form");
		display = -1;
	}

	@Override
	public void setWorld(World worldIn) {
		super.setWorld(worldIn);
		
	}

	@Override
	public byte getRSDirection(EnumFacing s) {
		return 1;
	}

	@Override
	public void initContainer(DataContainer container) {
	}

	@Override
	public int[] getSyncVariables() {
		return null;
	}

	@Override
	public void setSyncVariable(int i, int v) {
	}

	@Override
	public boolean detectAndSendChanges(DataContainer container, PacketBuffer dos) {
		return false;
	}

	@Override
	public void updateClientChanges(DataContainer container, PacketBuffer dis) {
	}

}
