package cd4017be.circuits.tileEntity;

import java.io.IOException;

import cd4017be.api.circuits.IDirectionalRedstone;
import cd4017be.lib.BlockGuiHandler.ClientPacketReceiver;
import cd4017be.lib.Gui.DataContainer;
import cd4017be.lib.Gui.DataContainer.IGuiData;
import cd4017be.lib.block.BaseTileEntity;
import cd4017be.lib.block.AdvancedBlock.INeighborAwareTile;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

/**
 *
 * @author CD4017BE
 */
public class Display8bit extends BaseTileEntity implements INeighborAwareTile, IDirectionalRedstone, IGuiData, ClientPacketReceiver {

	public int state;
	public short dspType = 0xf82;
	public String text0 = "", text1 = "";
	public String format = "###";
	public int display = 0x03080808;

	@Override
	public void neighborBlockChange(Block b, BlockPos src) {
		if (world.isRemote) return;
		int nState = 0;
		for (EnumFacing s : EnumFacing.VALUES)
			nState |= world.getRedstonePower(pos.offset(s), s);
		if (nState != state) {
			state = nState;
			this.formatState();
			this.markUpdate();
		}
	}

	@Override
	public void neighborTileChange(BlockPos src) {
	}

	@Override
	public void onPacketFromClient(PacketBuffer data, EntityPlayer sender) throws IOException {
		byte cmd = data.readByte();
		if (cmd == 0) {
			dspType = data.readShort();
			this.formatState();
		} else if (cmd == 1) {
			format = data.readString(8);
			this.formatState();
		} else if (cmd == 2) text0 = data.readString(16);
		else if (cmd == 3) text1 = data.readString(16);
		this.markUpdate();
	}

	private void formatState() {
		int m = dspType & 3;
		long s = state;//(long)state >> (dspType >> 2 & 0x1f) & 0xffffffffL >>> (31 - (dspType >> 7 & 0x1f));
		if (m == 0 || m == 3) {
			display = (int)s;
			return;
		}
		long sgnMask = 1L << (dspType >> 7 & 0x1f);
		boolean sign = (s & sgnMask) != 0;
		if (sign && format.indexOf('+') >= 0) s = sgnMask - (s & ~sgnMask);
		display = 0;
		int n = 0;
		for (int i = format.length() - 1; i >= 0 && n < 3; i--) {
			int d;
			char c = format.charAt(i);
			switch (c) {
			case '#': 
				if (m == 1) {d = ((int)s & 0xf) + 8; s >>>= 4;}
				else {d = (int)(s % 10L) + 8; s /= 10L;}
				break;
			case '$': 
				if (m == 1) s >>>= 4;
				else s /= 10L;
				continue;
			case 's': case 'S':
				if (i <= 0 || (c = format.charAt(--i)) <= '0' || c > '9') continue;
				else if (m == 1) s >>>= (c - '0') * 4;
				else for (int j = c - '0'; j > 0; j--) s /= 10L;
				continue;
			case ':': d = 24; break;
			case '.': d = 25; break;
			case '-': d = 26; break;
			case '%': d = 27; break;
			case '_': d = 28; break;
			case '+': d = sign ? 26 : 28; break;
			default: if (c >= '0' && c <= '9') d = c - '0' + 8;
				else if (c >= 'a' && c <= 'f') d = c - 'a' + 18;
				else if (c >= 'A' && c <= 'F') d = c - 'A' + 18;
				else d = 29;
			}
			display |= (d & 0xff) << (8 * n++);
		}
		display |= n << 24;
	}

	@Override
	public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
		state = pkt.getNbtCompound().getInteger("state");
		dspType = pkt.getNbtCompound().getShort("dsp");
		text0 = pkt.getNbtCompound().getString("t0");
		text1 = pkt.getNbtCompound().getString("t1");
		format = pkt.getNbtCompound().getString("form");
		display = pkt.getNbtCompound().getInteger("td");
	}

	@Override
	public SPacketUpdateTileEntity getUpdatePacket() {
		NBTTagCompound nbt = new NBTTagCompound();
		nbt.setInteger("state", state);
		nbt.setShort("dsp", dspType);
		nbt.setString("t0", text0);
		nbt.setString("t1", text1);
		nbt.setString("form", format);
		nbt.setInteger("td", display);
		return new SPacketUpdateTileEntity(pos, -1, nbt);
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
		nbt.setInteger("state", state);
		nbt.setShort("mode", dspType);
		nbt.setString("t0", text0);
		nbt.setString("t1", text1);
		nbt.setString("form", format);
		return super.writeToNBT(nbt);
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		state = nbt.getInteger("state");
		dspType = nbt.getShort("mode");
		text0 = nbt.getString("t0");
		text1 = nbt.getString("t1");
		format = nbt.getString("form");
		this.formatState();
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
