package cd4017be.circuits.tileEntity;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.ArrayUtils;

import cd4017be.api.circuits.IDirectionalRedstone;
import cd4017be.api.computers.ComputerAPI;
import cd4017be.lib.block.AdvancedBlock.IRedstoneTile;
import cd4017be.lib.util.Utils;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.machine.Value;
import li.cil.oc.api.network.ComponentConnector;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraftforge.common.util.Constants.NBT;
import net.minecraftforge.fml.common.Optional.Interface;
import net.minecraftforge.fml.common.Optional.InterfaceList;
import net.minecraftforge.fml.common.Optional.Method;


/**
 * @author CD4017BE
 *
 */
@InterfaceList(value = {
	@Interface(iface = "li.cil.oc.api.network.Environment", modid = "opencomputers"),
	@Interface(iface = "net.minecraft.util.ITickable", modid = "opencomputers")})
public class OC_ADC extends SyncronousRedstoneIO implements IRedstoneTile, IDirectionalRedstone, ITickable, Environment {

	private static final String[] SIDES = {"bottom", "top", "north", "south", "west", "east"};
	public static double INIT_COST = 100, RUN_COST = 1, WRITE_COST = 1;
	public static int MAX_SAMPLES = 256;
	private Object node = ComputerAPI.newOCnode(this, "adc", true);
	private String target;
	private final int[][] active_buffer = new int[12][], back_buffer = new int[12][];
	private int[] inputCfg = ArrayUtils.EMPTY_INT_ARRAY, outputCfg = ArrayUtils.EMPTY_INT_ARRAY;
	private int step = -1, samples = 0, modified = 0;
	/** -1: start single, 0: start continuous, 1: run continuous, 2: stop */
	private byte status = 2;
	private double powerUsage = 0;

	@Override
	public int redstoneLevel(EnumFacing side, boolean strong) {
		int[] buff = active_buffer[side.ordinal() + 6];
		return strong || buff == null ? 0 : buff[step];
	}

	@Override
	public boolean connectRedstone(EnumFacing side) {
		return getRSDirection(side) != 0;
	}

	@Override
	public byte getRSDirection(EnumFacing s) {
		int i = s.ordinal();
		return (byte)((active_buffer[i] != null ? 1:0) | (active_buffer[i+6] != null ? 2:0));
	}

	@Override
	public void update() {
		if (node == null) return;
		ComputerAPI.update(this, node, timer == -1 ? -powerUsage : 0);
		super.update();
	}

	@Override
	protected void tick(int rsDirty) {
		int s = step;
		for (int i = 0; i < 6; i++) {
			int[] buff = active_buffer[i];
			if (buff != null)
				buff[s] = rsInput[i];
		}
		int s1 = s + 1;
		if (s1 == samples)
			synchronized (back_buffer) {
				int stat = status;
				if (stat > 0) endCycle();
				if (stat == 2) {
					interval = -interval;
					timer = Integer.MIN_VALUE;
					target = null;
				} else {
					startCycle();
					if (stat < 0) status = 2;
					if (target != null && node != null)
						signal(node, target);
				}
			}
		else for (int i = 6; i < 12; i++) {
			int[] buff = active_buffer[i];
			int v;
			if (buff != null && buff[s] != (v = buff[s1]))
				Utils.updateRedstoneOnSide(this, v, EnumFacing.VALUES[i - 6]);
		}
	}

	private void endCycle() {
		for (int i = 0; i < 6; i++) {
			int[] buff = active_buffer[i];
			active_buffer[i] = back_buffer[i];
			back_buffer[i] = buff;
		}
	}

	private void startCycle() {
		step = 0;
		int s = samples;
		for (int i = 6, m = modified; i < 12; i++, m >>>= 1) {
			int[] buff0 = active_buffer[i];
			if (buff0 == null) continue;
			int oldv = buff0[s-1];
			if ((m & 1) != 0)
				System.arraycopy(back_buffer[i], 0, buff0, 0, s);
			int newv = buff0[0];
			if (newv != oldv)
				Utils.updateRedstoneOnSide(this, newv, EnumFacing.VALUES[i - 6]);
		}
		modified = 0;
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		if (node != null) ComputerAPI.readNode(node, nbt);
		if (nbt.hasKey("event", NBT.TAG_STRING))
			target = nbt.getString("event");
		else target = null;
		this.inputCfg = nbt.getIntArray("inCfg");
		this.outputCfg = nbt.getIntArray("outCfg");
		this.powerUsage = RUN_COST * (double)(inputCfg.length + outputCfg.length);
		int v = nbt.getShort("samples");
		if (v < 0) samples = 0;
		else if (v > MAX_SAMPLES) samples = MAX_SAMPLES;
		else samples = v;
		v = nbt.getShort("step");
		if (v < 0 || v >= samples) step = samples - 1;
		else step = v;
		this.status = nbt.getByte("status");
		int usedIO = 0;
		for (int i : inputCfg) usedIO |= 1 << i;
		for (int i : outputCfg) usedIO |= 0x40 << i;
		NBTTagList list = nbt.getTagList("buffers", NBT.TAG_COMPOUND);
		for (int i = 0, j = 0; i < 12; i++)
			if ((usedIO >> i & 1) != 0) {
				NBTTagCompound tag = list.getCompoundTagAt(j++);
				int[] buff = tag.getIntArray("a");
				if (buff.length != samples) buff = Arrays.copyOf(buff, samples);
				active_buffer[i] = buff;
				buff = tag.getIntArray("b");
				if (buff.length != samples) buff = Arrays.copyOf(buff, samples);
				back_buffer[i] = buff;
			} else {
				active_buffer[i] = null;
				back_buffer[i] = null;
			}
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
		if (node != null) ComputerAPI.saveNode(node, nbt);
		if (target != null) nbt.setString("event", target);
		nbt.setIntArray("inCfg", inputCfg);
		nbt.setIntArray("outCfg", outputCfg);
		nbt.setShort("step", (short)step);
		nbt.setShort("samples", (short)samples);
		nbt.setByte("status", status);
		NBTTagList list = new NBTTagList();
		for (int i = 0; i < 12; i++) {
			int[] buff = active_buffer[i];
			if (buff != null) {
				NBTTagCompound tag = new NBTTagCompound();
				tag.setIntArray("a", buff);
				tag.setIntArray("b", back_buffer[i]);
			}
		}
		nbt.setTag("buffers", list);
		return super.writeToNBT(nbt);
	}

	@Override
	public Node node() {
		return (Node) node;
	}

	@Override
	public void onConnect(Node node) {
	}

	@Override
	public void onDisconnect(Node node) {
	}

	@Override
	public void onMessage(Message message) {
	}

	@Method(modid = "opencomputers")
	@Callback(direct = false, doc = "")
	public Object[] configure(Context context, Arguments args) throws Exception {
		int[] inCfg = parse(args.checkTable(0));
		int[] outCfg = parse(args.checkTable(1));
		int samples = args.checkInteger(2);
		int interv = (int)(args.checkDouble(3) * 20D);
		int phase = (int)(args.optDouble(4, 0) * 20D);
		if (samples > MAX_SAMPLES) samples = MAX_SAMPLES;
		if (interv < 1) interv = 1;
		int usedIO = 0;
		for (int i : inCfg) usedIO |= 1 << i;
		for (int i : outCfg) usedIO |= 0x40 << i;
		synchronized (back_buffer) {
			ComponentConnector node = (ComponentConnector)this.node;
			if (!node.tryChangeBuffer(-INIT_COST))
				throw new IllegalStateException("Out of Power!");
			for (int i = 0; i < 12; i++)
				if ((usedIO >> i & 1) != 0) {
					active_buffer[i] = new int[samples];
					back_buffer[i] = new int[samples];
				} else {
					active_buffer[i] = null;
					back_buffer[i] = null;
				}
			this.inputCfg = inCfg;
			this.outputCfg = outCfg;
			this.samples = samples;
			this.interval = -interv;
			this.phase = phase;
			this.step = samples - 1;
			this.setupData();
			this.powerUsage = RUN_COST * (double)(inputCfg.length + outputCfg.length);
		}
		return null;
	}

	private int[] parse(Map<?,?> map) {
		int[] arr = new int[map.size()];
		for (Entry<?,?> e : map.entrySet()) {
			Object k = e.getKey(), v = e.getValue();
			int i, j;
			if (k instanceof Number) i = ((Number)k).intValue();
			else throw new IllegalArgumentException("Table keys must be numbers!");
			if (i <= 0 || i > arr.length)
				throw new IllegalArgumentException("Table key " + i + " out of range 1 ... " + arr.length);
			if (v instanceof Double) j = ((Double)v).intValue();
			else if (v instanceof String) {
				String s = ((String)v).toLowerCase();
				for (j = 0; j < SIDES.length; j++)
					if (SIDES[j].startsWith(s)) break;
			} else j = -1;
			if (j < 0 || j >= 6)
				throw new IllegalArgumentException("Invalide side specifier: " + v);
			arr[i - 1] = j;
		}
		return arr;
	}

	@Method(modid = "opencomputers")
	@Callback(direct = true, doc = "")
	public Object[] getConfig(Context context, Arguments args) throws Exception {
		HashMap<String, Object> map = new HashMap<>();
		map.put("samples", Double.valueOf(samples));
		map.put("interval", Double.valueOf(interval / 20D));
		map.put("phase", Double.valueOf(phase / 20D));
		map.put("inputs", Double.valueOf(inputCfg.length));
		map.put("outputs", Double.valueOf(outputCfg.length));
		return new Object[] {map};
	}

	@Method(modid = "opencomputers")
	@Callback(direct = false, doc = "")
	public Object[] start(Context context, Arguments args) throws Exception {
		boolean single = args.optBoolean(0, false);
		boolean running = true;
		if (interval < 0) {
			interval = - interval;
			setupData();
			running = false;
		}
		step = samples - 1;
		status = (byte)(single ? -1 : 0);
		target = context.node().address();
		return new Object[] {running};
	}

	@Method(modid = "opencomputers")
	private void signal(Object node, String address) {
		((Node)node).sendToAddress(address, "computer.signal", "buffer_swap");
	}

	@Method(modid = "opencomputers")
	@Callback(direct = false, doc = "")
	public Object[] stop(Context context, Arguments args) throws Exception {
		boolean force = args.optBoolean(0, false);
		boolean running = status != 2;
		status = 2;
		if (force && interval > 0)
			synchronized (back_buffer) {
				endCycle();
				interval = -interval;
				timer = Integer.MIN_VALUE;
				target = null;
				return new Object[] {true};
			}
		return new Object[] {running};
	}

	@Method(modid = "opencomputers")
	@Callback(direct = true, limit = 6, doc = "")
	public Object[] running(Context context, Arguments args) throws Exception {
		return new Object[] {Double.valueOf(interval > 0 ? step + 1 : 0)};
	}

	@Method(modid = "opencomputers")
	@Callback(direct = true, limit = 6, doc = "")
	public Object[] setOutput(Context context, Arguments args) throws Exception {
		int i = outputCfg[args.checkInteger(0) - 1], n = samples;
		Object data = args.checkAny(1);
		ComponentConnector node = (ComponentConnector)this.node;
		synchronized (back_buffer) {
			int[] buff = back_buffer[i];
			if (data instanceof SampleData) {
				SampleData sd = (SampleData)data;
				int l = Math.min(sd.data.length, n);
				if (!node.tryChangeBuffer(-WRITE_COST * (double)l)) throw new IllegalStateException("Out of Power!");
				System.arraycopy(sd.data, 0, buff, 0, l);
			} else if (data instanceof Map) {
				Map<?,?> map = (Map<?,?>)data;
				if (!node.tryChangeBuffer(-WRITE_COST * (double)map.size())) throw new IllegalStateException("Out of Power!");
				for (Entry<?,?> e : map.entrySet()) {
					int j = ((Number)e.getKey()).intValue() - 1;
					if (j >= 0 && j < n)
						buff[j] = ((Number)e.getValue()).intValue();
				}
			} else throw new IllegalArgumentException("table or sample data expected in arg 1");
			modified |= 1 << i;
		}
		return null;
	}

	@Method(modid = "opencomputers")
	@Callback(direct = true, limit = 6, doc = "")
	public Object[] getOutput(Context context, Arguments args) throws Exception {
		int i = outputCfg[args.checkInteger(0) - 1];
		SampleData data = new SampleData();
		synchronized (back_buffer) {
			data.data = back_buffer[i].clone();
		}
		return new Object[] {data};
	}

	@Method(modid = "opencomputers")
	@Callback(direct = true, limit = 6, doc = "")
	public Object[] getInput(Context context, Arguments args) throws Exception {
		int i = inputCfg[args.checkInteger(0) - 1];
		SampleData data = new SampleData();
		synchronized (back_buffer) {
			data.data = back_buffer[i].clone();
		}
		return new Object[] {data};
	}

	@Interface(iface = "li.cil.oc.api.machine.Value", modid = "opencomputers")
	public static class SampleData implements Value {

		public int[] data;

		public SampleData() {}

		public SampleData(int size) {
			this.data = new int[size];
		}

		@Override
		public void load(NBTTagCompound nbt) {
			data = nbt.getIntArray("d");
		}

		@Override
		public void save(NBTTagCompound nbt) {
			nbt.setIntArray("d", data);
		}

		@Override
		public Object apply(Context context, Arguments args) {
			if (args.isInteger(0)) {
				int i = args.checkInteger(0);
				return i < 0 || i >= data.length ? Double.NaN : Double.valueOf(data[i]);
			} else if (args.isString(0) && args.checkString(0).equals("size")) {
				return Double.valueOf(data.length);
			} else return null;
		}

		@Override
		public void unapply(Context context, Arguments args) {
			if (args.isInteger(0)) {
				int i = args.checkInteger(0);
				if (i >= 0 && i < data.length)
					data[i] = args.checkInteger(1);
			}
		}

		@Override
		public Object[] call(Context context, Arguments args) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void dispose(Context arg0) {}

	}
}
