package cd4017be.circuits.editor.op;

import java.util.Arrays;
import java.util.Iterator;
import java.util.function.IntSupplier;

import cd4017be.circuits.editor.compile.IOp;
import cd4017be.lib.jvm_utils.MethodAssembler;
import io.netty.buffer.ByteBuf;

/**
 * @author CD4017BE
 *
 */
public class OpNode implements IOp {

	public final OpType op;
	public final int index;
	public final Pin[] inputs, outputs;

	public OpNode(OpType type, int index) {
		this.op = type;
		this.index = index;
		int l = type.outs;
		Pin[] outputs = new Pin[l];
		for (int i = 0; i < l; i++)
			outputs[i] = new Pin(this, i);
		this.outputs = outputs;
		this.inputs = new Pin[type.ins];
	}

	public void setInput(int i, Pin p) {
		Pin pr = inputs[i];
		if (pr == p) return;
		inputs[i] = p;
		if (pr != null && inputIdx(pr) < 0)
			pr.dest.remove(this);
		if (p != null)
			p.dest.add(this);
	}

	public int inputIdx(Pin pin) {
		Pin[] inputs = this.inputs;
		int i = inputs.length - 1;
		for (; i >= 0; i--)
			if (inputs[i] == pin)
				break;
		return i;
	}

	public boolean isEnd() {
		for (Pin pin : outputs)
			if (pin.validUsers() > 0)
				return false;
		return true;
	}

	@Override
	public void compile(MethodAssembler ma, IntSupplier dirty) {
		op.compile(ma, this);
	}

	public int in() {
		int n = 0;
		for (Pin p : inputs)
			if (p != null)
				n++;
		return n;
	}

	public int out() {
		int n = 0;
		for (Pin p : outputs)
			if (p.validUsers() > 0)
				n++;
		return n;
	}

	public void read(ByteBuf data) {
		rasterX = data.readUnsignedByte();
		rasterY = data.readUnsignedByte();
		byte[] arr = new byte[data.readUnsignedByte()];
		data.readBytes(arr);
		label = new String(arr);
		if (this instanceof IConfigurable) {
			arr = new byte[data.readUnsignedByte()];
			data.readBytes(arr);
			((IConfigurable)this).setCfg(new String(arr));
		}
	}

	public void write(ByteBuf data) {
		data.writeByte(rasterX);
		data.writeByte(rasterY);
		byte[] arr = label.getBytes();
		if (arr.length > 255) arr = Arrays.copyOf(arr, 255);
		data.writeByte(arr.length);
		data.writeBytes(arr);
		if (this instanceof IConfigurable) {
			arr = ((IConfigurable)this).getCfg().getBytes();
			if (arr.length > 255) arr = Arrays.copyOf(arr, 255);
			data.writeByte(arr.length);
			data.writeBytes(arr);
		}
	}

	public void remove() {
		for (Pin p : inputs)
			if (p != null)
				p.dest.remove(this);
		for (Pin p : outputs)
			for (Iterator<OpNode> it = p.dest.iterator(); it.hasNext();) {
				OpNode op = it.next();
				it.remove();
				for (int i = 0; i < op.inputs.length; i++)
					if (op.inputs[i] == p)
						op.setInput(i, null);
			}
	}

	@Override
	public String toString() {
		return (index & 0xff) + ": Node[" + op.name() + "]";
	}

}
