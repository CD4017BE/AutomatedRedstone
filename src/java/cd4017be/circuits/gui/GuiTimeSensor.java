package cd4017be.circuits.gui;

import java.util.Calendar;

import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import cd4017be.lib.BlockGuiHandler;
import cd4017be.lib.Gui.DataContainer;
import cd4017be.lib.Gui.AdvancedGui;
import static java.util.Calendar.*;

public class GuiTimeSensor extends AdvancedGui {

	private final InventoryPlayer inv;
	
	public GuiTimeSensor(DataContainer cont) {
		super(cont);
		this.MAIN_TEX = new ResourceLocation("circuits", "textures/gui/fluid_sensor.png");
		this.bgTexY = 150;
		this.inv = cont.player.inventory;
	}

	@Override
	public void initGui() {
		this.xSize = 112;
		this.ySize = 76;
		super.initGui();
		guiComps.add(new Button(0, 7, 15, 18, 18, 0).texture(112, 150).setTooltip("timeSensor.mode#"));
		guiComps.add(new Button(1, 25, 15, 80, 18, -1).setTooltip("timeSensor.srcT#"));
		guiComps.add(new TextField(2, 8, 43, 96, 7, 20).color(0xff004080, 0xffff4040));
		guiComps.add(new TextField(3, 8, 61, 96, 7, 20).color(0xff004080, 0xffff4040));
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
		super.drawGuiContainerBackgroundLayer(partialTicks, mouseX, mouseY);
		NBTTagCompound nbt = this.getTag();
		this.drawLocString(guiLeft + 26, guiTop + 16, 8, 0xff000080, "timeSensor.src" + nbt.getByte("src"));
		this.drawLocString(guiLeft + 8, guiTop + 34, 8, 0xff404040, "timeSensor.ref");
		this.drawLocString(guiLeft + 8, guiTop + 52, 8, 0xff404040, "timeSensor.int");
	}

	private NBTTagCompound getTag() {
		NBTTagCompound nbt = inv.mainInventory.get(inv.currentItem).getTagCompound();
		return nbt != null ? nbt : new NBTTagCompound();
	}

	@Override
	protected Object getDisplVar(int id) {
		NBTTagCompound nbt = this.getTag();
		switch(id) {
		case 0: return (int)nbt.getByte("mode");
		case 1: return (int)nbt.getByte("src"); 
		case 2: return this.formatTime(nbt.getLong("ref"), nbt.getByte("src"));
		case 3: return this.formatTime(nbt.getLong("int"), (byte)0);
		default: return null;
		}
	}
	
	private String formatTime(long t, byte src) {
		if (src == 0) {
			String s = String.format("%02d", (int)(t % 60000L) / 1000);
			if (t % 1000 >= 10) s += String.format(".%02d", (t % 1000) / 10);
			if (t >= 60000) s = String.format("%02d:", (int)(t % 3600000L) / 60000) + s;
			if (t >= 3600000) s = String.format("%02d:", (int)(t % 86400000L) / 3600000) + s;
			if (t >= 86400000) s = String.format("%dd ", (int)(t / 86400000L)) + s;
			return s;
		} else if (src == 1) {
			return String.format("day %d %d:%02d.%02d", (int)(t / 1200000L), (int)(t %= 1200000L) / 60000, (int)(t %= 60000L) / 1000, (int)(t % 1000L) / 10);
		} else {
			Calendar c = getInstance();
			c.setTimeInMillis(t);
			return String.format("%d.%02d.%d %d:%02d:%02d", c.get(DAY_OF_MONTH), c.get(MONTH) + 1, c.get(YEAR), c.get(HOUR_OF_DAY), c.get(MINUTE), c.get(SECOND));
		}
	}
	
	private long parseTime(String s, byte src) {
		s = s.trim();
		if (src == 0 || src == 1) {
			long t = 0;
			int p;
			if (s.startsWith("day")) {
				p = s.indexOf(' ', 4);
				if (p < 0) p = s.length();
				t = (long)Integer.parseInt(s.substring(3, p).trim()) * 1200000L;
				s = s.substring(p).trim();
			} else if ((p = s.indexOf('d')) >= 0) {
				t += (long)Integer.parseInt(s.substring(0, p)) * 86400000L;
				s = s.substring(p + 1).trim();
			}
			String[] s1 = s.split(":");
			t += (long)(Float.parseFloat(s1[s1.length - 1]) * 1000F);
			if (s1.length >= 2) t += Integer.parseInt(s1[s1.length - 2]) * 60000;
			if (s1.length >= 3) t += Integer.parseInt(s1[s1.length - 3]) * 3600000;
			return t;
		} else {
			String[] s1 = s.split("[\\. :]");
			Calendar c = getInstance();
			c.set(Integer.parseInt(s1[2]), Integer.parseInt(s1[1]) - 1, Integer.parseInt(s1[0]), Integer.parseInt(s1[3]), Integer.parseInt(s1[4]), Integer.parseInt(s1[5]));
			return c.getTimeInMillis();
		}
	}

	@Override
	protected void setDisplVar(int id, Object obj, boolean send) {
		NBTTagCompound nbt = this.getTag();
		PacketBuffer dos = BlockGuiHandler.getPacketForItem(inv.currentItem);
		switch(id) {
		case 0: dos.writeByte(1).writeByte((nbt.getByte("mode") + ((Integer)obj == 0 ? 1 : 3)) % 4); break;
		case 1: dos.writeByte(0).writeByte((nbt.getByte("src") + ((Integer)obj == 0 ? 1 : 2)) % 3); break;
		case 2: try {dos.writeByte(2).writeLong(this.parseTime((String)obj, nbt.getByte("src"))); break;} catch(Exception e) {return;}
		case 3: try {dos.writeByte(3).writeLong(this.parseTime((String)obj, (byte)0)); break;} catch(Exception e) {return;}
		}
		if (send) BlockGuiHandler.sendPacketToServer(dos);
	}

}
