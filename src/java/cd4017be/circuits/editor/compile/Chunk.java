package cd4017be.circuits.editor.compile;

import java.util.ArrayList;
import java.util.Map;
import java.util.function.IntSupplier;

import cd4017be.circuits.editor.op.OpNode;
import cd4017be.circuits.editor.op.Pin;
import cd4017be.lib.jvm_utils.MethodAssembler;
import io.netty.buffer.ByteBuf;

import static cd4017be.lib.jvm_utils.ClassAssembler.*;

/**
 * @author CD4017BE
 *
 */
public class Chunk implements Comparable<Chunk>{

	public final ArrayList<GetLocal> inputs = new ArrayList<>();
	public final ArrayList<SetLocal> outputs = new ArrayList<>();
	public final ArrayList<IOp> operations = new ArrayList<>();
	public int order = -1;

	public Chunk(OpNode head) {
		addOp(head, -1);
	}

	private void addOp(OpNode node, int out) {
		Pin[] ins = node.inputs;
		int n = ins.length;
		byte[] multi = new byte[n];
		for (int j = 0; j < n; j++) {
			Pin pin = ins[j];
			if (pin == null) continue;
			int k = node.inputIdx(pin);
			int m = multi[k] |= 1 << (k - j);
			OpNode src = pin.src;
			int l = pin.index;
			boolean primary = true;
			for (int i = 0; i < l; i++)
				if (src.outputs[i].validUsers() > 0) {
					primary = false;
					break;
				}
			if (primary && pin.validUsers() <= 1 && m < 16) {
				if (j == k) {
					addOp(src, l);
					if (m > 1) operations.add(new Dup(m));
				}
				continue;
			}
			GetLocal load = new GetLocal(pin, primary);
			inputs.add(load);
			operations.add(load);
		}
		operations.add(node);
		Pin[] pins = node.outputs;
		for (int l = pins.length - 1; l > out; l--) {
			Pin pin = pins[l];
			if (pin.validUsers() > 0) {
				SetLocal store = new SetLocal(this, pin);
				outputs.add(store);
				operations.add(store);
			}
		}
	}

	public void map(Map<Pin, SetLocal> map) {
		for (SetLocal out : outputs)
			map.put(out.pin, out);
	}

	public int order() {
		int o = order;
		if (o >= 0) return o;
		for (GetLocal get : inputs) {
			SetLocal set = get.source;
			if (set == null) return -1;
			int o1 = set.owner.order();
			if (o1 < 0) return -1;
			if (o1 > o) o = o1;
		}
		return order = o + 1;
	}

	@Override
	public int compareTo(Chunk c) {
		return this.order() - c.order();
	}

	static class Dup implements IOp {
		final byte mask;
		Dup(int mask) { this.mask = (byte)mask; }
		@Override
		public void compile(MethodAssembler ma, IntSupplier dirty) {
			ByteBuf b = ma.code;
			int m = mask;
			if ((m & 2) != 0) {
				b.writeByte(_dup);
				ma.push(MethodAssembler.T_INT);
			}
			if ((m & 4) != 0) {
				b.writeByte(_dup_x1);
				ma.push(MethodAssembler.T_INT);
			}
			if ((m & 8) != 0) {
				b.writeByte(_dup_x2);
				ma.push(MethodAssembler.T_INT);
			}
		}
	}

}
