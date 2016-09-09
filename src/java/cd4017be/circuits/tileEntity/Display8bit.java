package cd4017be.circuits.tileEntity;

import java.io.IOException;

import cd4017be.api.circuits.IDirectionalRedstone;
import cd4017be.lib.ModTileEntity;
import cd4017be.lib.Gui.DataContainer.IGuiData;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.util.EnumFacing;

/**
 *
 * @author CD4017BE
 */
public class Display8bit extends ModTileEntity implements IDirectionalRedstone, IGuiData {

	public int state;
	public short dspType = 0xf82;
	public String text0 = "", text1 = "";
	public String format = "###";
	public int display = 0x03080808;

	@Override
	public void onNeighborBlockChange(Block b) {
		if (worldObj.isRemote) return;
		int nState = 0, rs;
		for (EnumFacing s : EnumFacing.VALUES)
			if ((rs = worldObj.getRedstonePower(pos.offset(s), s)) > nState) nState = rs;
		if (nState != state) {
			state = nState;
			this.formatState();
			this.markUpdate();
		}
	}

	@Override
	public void onPlayerCommand(PacketBuffer data, EntityPlayerMP player) throws IOException {
		byte cmd = data.readByte();
		if (cmd == 0) {
			dspType = data.readShort();
			this.formatState();
		} else if (cmd == 1) {
			format = data.readStringFromBuffer(8);
			this.formatState();
		} else if (cmd == 2) text0 = data.readStringFromBuffer(16);
		else if (cmd == 3) text1 = data.readStringFromBuffer(16);
		this.markUpdate();
	}

	private void formatState() {
		int m = dspType & 3;
		int s = state >> (dspType >> 2 & 0x1f) & 0xffffffff >>> (31 - (dspType >> 7 & 0x1f));
		if (m == 0 || m == 3) {
			display = s;
			return;
		}
		display = 0;
		int n = 0;
		for (int i = format.length() - 1; i >= 0 && n < 3; i--) {
			int d;
			char c = format.charAt(i);
			switch (c) {
			case '#': 
				if (m == 1) {d = (s & 0xf) + 8; s >>= 4;}
				else {d = s % 10 + 8; s /= 10;}
				break;
			case '$': 
				if (m == 1) s >>= 4;
				else s /= 10;
				continue;
			case 's': case 'S':
				if (i <= 0 || (c = format.charAt(--i)) <= '0' || c > '9') continue;
				else if (m == 1) s >>= (c - '0') * 4;
				else for (int j = c - '0'; j >= 0; j--) s /= 10;
				continue;
			case ':': d = 24; break;
			case '.': d = 25; break;
			case '-': d = 26; break;
			case '%': d = 27; break;
			default: if (c >= '0' && c <= '9') d = c - '0' + 8;
				else if (c >= 'a' && c <= 'f') d = c - 'a' + 18;
				else if (c >= 'A' && c <= 'F') d = c - 'A' + 18;
				else d = 28;
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

}
