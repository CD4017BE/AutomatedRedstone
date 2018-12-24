package cd4017be.circuits.editor.op;

import static cd4017be.lib.jvm_utils.ClassAssembler.*;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

import cd4017be.circuits.editor.InvalidSchematicException;
import cd4017be.circuits.editor.InvalidSchematicException.ErrorType;
import cd4017be.lib.jvm_utils.ConstantPool;
import cd4017be.lib.jvm_utils.MethodAssembler;
import io.netty.buffer.ByteBuf;

/**
 * @author CD4017BE
 *
 */
public class IONode extends OpNode {

	public IONode(OpType type, int index) {
		super(type, index);
	}

	@Override
	public void checkValid() throws InvalidSchematicException {
		super.checkValid();
		String s = label;
		if (s.isEmpty() || !Character.isJavaIdentifierStart(s.charAt(0)))
			throw new InvalidSchematicException(ErrorType.invalidLabel, this, -1);
		for (int i = 1, l = s.length(); i < l; i++)
			if (!Character.isJavaIdentifierPart(s.charAt(i)))
				throw new InvalidSchematicException(ErrorType.invalidLabel, this, -1);
	}

	public String fieldDesc() {
		switch (op) {
		case in: return "Ljava.util.function.IntSupplier; " + label;
		case out: return "Ljava.util.function.IntConsumer; " + label;
		default: return "I " + label;
		}
	}

	@Override
	public void compile(MethodAssembler ma, IntSupplier dirty) {
		ByteBuf b = ma.code;
		ConstantPool cpt = ma.cpt;
		b.writeByte(_aload_0);
		b.writeByte(_getfield).writeShort(cpt.putFieldMethod(null, fieldDesc()));
		ma.push(MethodAssembler.T_INT);
		switch (op) {
		case out:
			b.writeByte(_swap);
			b.writeByte(_invokeinterface).writeShort(cpt.putFieldMethod(IntConsumer.class, "accept(I)V")).writeByte(2).writeByte(0);
			ma.pop(2);
			break;
		case in:
			b.writeByte(_invokeinterface).writeShort(cpt.putFieldMethod(IntSupplier.class, "getAsInt()I")).writeByte(1).writeByte(0);
		default:
		}
	}

}
