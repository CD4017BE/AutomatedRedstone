package cd4017be.circuits.multiblock;

import java.util.HashMap;
import java.util.HashSet;

import cd4017be.lib.TickRegistry;
import cd4017be.lib.TickRegistry.IUpdatable;
import cd4017be.lib.templates.SharedNetwork;

/**
 * 
 * @author CD4017BE
 */
public class RedstoneNetwork extends SharedNetwork<RedstoneNode, RedstoneNetwork> implements IUpdatable {

	public static final short AllIn = 0x5555, AllOut = (short)0xaaaa;
	public HashSet<RedstoneNode> inputs = new HashSet<RedstoneNode>();
	public HashSet<RedstoneNode> outputs = new HashSet<RedstoneNode>();
	public int outputState = 0;
	public boolean updateState;

	protected RedstoneNetwork(HashMap<Long, RedstoneNode> comps) {
		super(comps);
	}

	public RedstoneNetwork(RedstoneNode comp) {
		super(comp);
		if ((comp.rsIO & AllIn) != 0) inputs.add(comp);
		if ((comp.rsIO & AllOut) != 0) outputs.add(comp);
		outputState = comp.inputState;
	}

	@Override
	public void onMerged(RedstoneNetwork network) {
		super.onMerged(network);
		outputState &= network.outputState;
		markStateDirty();
		inputs.addAll(network.inputs);
		outputs.addAll(network.outputs);
	}

	@Override
	public void remove(RedstoneNode comp) {
		super.remove(comp);
		if (inputs.remove(comp)) markStateDirty();
		outputs.remove(comp);
	}

	@Override
	public RedstoneNetwork onSplit(HashMap<Long, RedstoneNode> comps) {
		RedstoneNetwork si = new RedstoneNetwork(comps);
		si.outputState = outputState;
		for (RedstoneNode c : comps.values()) {
			this.inputs.remove(c);
			if ((c.rsIO & AllIn) != 0) {
				si.inputs.add(c);
				markStateDirty();
			}
			this.outputs.remove(c);
			if ((c.rsIO & AllOut) != 0) {
				si.outputs.add(c);
			}
		}
		si.markStateDirty();
		return si;
	}

	public void setIO(RedstoneNode c, short con) {
		if ((con & AllIn) != 0) inputs.add(c);
		else inputs.remove(c);
		if ((con & AllOut) != 0) outputs.add(c);
		else outputs.remove(c);
		c.rsIO = con;
		if (c.digital)
			for (int i = 0; i < 6; i++)
				if ((con >> (i << 1) & 3) != 0 && (c.con >> i & 1) != 0)
					c.setConnect((byte)i, false);
		markStateDirty();
	}

	@Override
	public void markDirty() {
		if (!update) {
			update = true;
			TickRegistry.instance.updates.add(this);
		}
	}

	public void markStateDirty() {
		if (!updateState) {
			updateState = true;
			TickRegistry.instance.updates.add(this);
		}
	}

	@Override
	public void process() {
		if (update) {
			if (core == null)
				for (RedstoneNode c : components.values()) { core = c; break; }
			reassembleNetwork();
			update = false;
		}
		if (updateState) { 
			int newState = 0;
			for (RedstoneNode c : inputs)
				if (c.digital) newState |= c.inputState;
				else if ((newState += c.inputState) >= 255) {
					newState = 255;
					break;
				}
			if (newState != outputState) {
				outputState = newState;
				for (RedstoneNode c : outputs) c.onStateChange();
			}
			updateState = false;
		}
	}

}
