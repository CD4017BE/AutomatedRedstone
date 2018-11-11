package cd4017be.circuits.tileEntity;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.ArrayUtils;

import cd4017be.api.circuits.IDirectionalRedstone;
import cd4017be.api.computers.ComputerAPI;
import cd4017be.circuits.Objects;
import cd4017be.lib.block.AdvancedBlock.IRedstoneTile;
import cd4017be.lib.util.Utils;
import li.cil.oc.api.Driver;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
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
		else {
			for (int i = 6; i < 12; i++) {
				int[] buff = active_buffer[i];
				int v;
				if (buff != null && buff[s] != (v = buff[s1]))
					Utils.updateRedstoneOnSide(this, v, EnumFacing.VALUES[i - 6]);
			}
			step = s1;
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
		for (int i = 6, m = modified; i < 12; i++) {
			int[] buff0 = active_buffer[i];
			if (buff0 == null) continue;
			int oldv = buff0[s-1];
			if ((m >> i & 1) != 0)
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
		for (int i : outputCfg) usedIO |= 1 << i;
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
				list.appendTag(tag);
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
	@Callback(direct = false, doc = "function(inputs:table, outputs:table, samples:number, interval:number[, phase:number]) -- Configure the device to assign channels to the given lists of input and output sides with the given sample buffer size and synchronize timing based on the given interval and phase-offset in seconds.")
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
		for (int i = 0; i < outCfg.length; i++) {
			int j = outCfg[i] + 6;
			outCfg[i] = j;
			usedIO |= 1 << j;
		}
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
		this.world.notifyNeighborsOfStateChange(pos, blockType, false);
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
	@Callback(direct = true, doc = "function():table -- Get the current configuration of the device.")
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
	@Callback(direct = false, doc = "function([singleRun:bool]):bool -- Start recording and playback of Redstone signals and if singleRun is given True automatically stop after one processing cycle has been completed. Returns whether it was already running. Also registeres the caller to receive \"buffer_swap\" signals.")
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
	@Callback(direct = false, doc = "function([force:bool]):bool -- Stop recording and playback of Redstone signals but letting the current processing cycle complete unless force is given True. Returns whether it was running.")
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
	@Callback(direct = true, limit = 6, doc = "function():number -- Get the currently processed index in the active buffers or 0 if no signal recording/playback in progress.")
	public Object[] running(Context context, Arguments args) throws Exception {
		return new Object[] {Double.valueOf(interval > 0 ? step + 1 : 0)};
	}

	@Method(modid = "opencomputers")
	@Callback(direct = true, limit = 6, doc = "function(channel:number, sampleData:table) -- Assign the given sample data to the secondary buffer of the specified output channel. It's swapped over into the active buffer when a new processing cycle starts (which triggers a \"buffer_swap\" signal).")
	public Object[] setOutput(Context context, Arguments args) throws Exception {
		int i = outputCfg[args.checkInteger(0) - 1];
		Map<?,?> data = args.checkTable(1);
		synchronized (back_buffer) {
			ComponentConnector node = (ComponentConnector)this.node;
			if (!node.tryChangeBuffer(-WRITE_COST * (double)data.size())) throw new IllegalStateException("Out of Power!");
			setStates(back_buffer[i], data);
			modified |= 1 << i;
		}
		return null;
	}

	@Method(modid = "opencomputers")
	@Callback(direct = true, limit = 6, doc = "function(channel:number):table -- Get the sample data currently stored in the secondary buffer of the specified output channel.")
	public Object[] getOutput(Context context, Arguments args) throws Exception {
		int i = outputCfg[args.checkInteger(0) - 1];
		synchronized (back_buffer) {
			return new Object[] {back_buffer[i]};
		}
	}

	@Method(modid = "opencomputers")
	@Callback(direct = true, limit = 6, doc = "function(channel:number):table -- Get the sample data in the secondary buffer of the specified input channel that was fetched from the active buffer after the last processing cycle completed (which triggered a \"buffer_swap\" signal).")
	public Object[] getInput(Context context, Arguments args) throws Exception {
		int i = inputCfg[args.checkInteger(0) - 1];
		synchronized (back_buffer) {
			return new Object[] {back_buffer[i]};
		}
	}

	private static void setStates(int[] buffer, Map<?,?> map) {
		int n = buffer.length;
		for (Entry<?,?> e : map.entrySet()) {
			int j = ((Number)e.getKey()).intValue() - 1;
			if (j >= 0 && j < n)
				buffer[j] = ((Number)e.getValue()).intValue();
		}
	}

	@Method(modid = "opencomputers")
	public static void registerAPI() {
		Driver.add((stack) -> stack.getItem() == Objects.oc_adc ? OC_ADC.class : null);
	}

}
