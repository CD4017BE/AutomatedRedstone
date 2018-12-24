package cd4017be.circuits.editor.op;

import static cd4017be.lib.jvm_utils.ClassAssembler.*;

import java.util.function.IntSupplier;

import cd4017be.lib.jvm_utils.MethodAssembler;
import io.netty.buffer.ByteBuf;

/**
 * @author CD4017BE
 *
 */
public class WriteVar extends IONode {

	public WriteVar(int index) {
		super(OpType.write, index);
	}

	public void link(ReadVar src) {
		if (src != null) {
			setInput(2, src.outputs[0]);
			inputs[1] = inputs[0];
		} else {
			setInput(2, null);
			inputs[1] = null;
		}
	}

	public ReadVar getLink() {
		Pin pin = inputs[2];
		if (pin != null) {
			OpNode node = pin.src;
			if (node instanceof ReadVar) return (ReadVar)node;
		}
		return null;
	}

	@Override
	public void compile(MethodAssembler ma, IntSupplier dirty) {
		ByteBuf b = ma.code;
		short field = ma.cpt.putFieldMethod(null, fieldDesc());
		boolean out = outputs[0].validUsers() > 0;
		if (inputs[2] != null) {
			int di = dirty.getAsInt();
			int p = b.writerIndex();
			b.writeByte(_if_icmpeq).writeShort(0);
			b.writeByte(_dup).writeByte(_aload_0).writeByte(_swap);
			b.writeByte(_putfield).writeShort(field);
			b.writeByte(_iconst_1);
			ma.pop(1);
			ma.store(di);
			b.setShort(p + 1, b.writerIndex() - p); ma.frame();
			if (!out) {
				b.writeByte(_pop);
				ma.pop(1);
			}
		} else {
			if (out) {
				b.writeByte(_dup);
				ma.push(MethodAssembler.T_INT);
			}
			ma.load(0);
			b.writeByte(_swap);
			b.writeByte(_putfield).writeShort(field);
			ma.pop(2);
		}
	}

}
