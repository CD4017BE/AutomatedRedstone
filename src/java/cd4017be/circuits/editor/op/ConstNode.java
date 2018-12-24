package cd4017be.circuits.editor.op;

import java.util.function.IntSupplier;

import cd4017be.circuits.editor.InvalidSchematicException;
import cd4017be.circuits.editor.InvalidSchematicException.ErrorType;
import cd4017be.lib.jvm_utils.MethodAssembler;

/**
 * @author CD4017BE
 *
 */
public class ConstNode extends OpNode {

	public String value = "0";

	public ConstNode(int index) {
		super(OpType.cst, index);
	}

	@Override
	public void checkValid() throws InvalidSchematicException {
		super.checkValid();
		try {
			parse(value);
		} catch (NumberFormatException e) {
			throw new InvalidSchematicException(ErrorType.invalidCfg, this, 0);
		}
	}

	public static int parse(String value) {
		if (value.isEmpty())
			throw new NumberFormatException();
		char c = value.charAt(0);
		int rad;
		if (c == 'b') {
			rad = 2;
			value = value.substring(1);
		} else if (c == 'b') {
			rad = 16;
			value = value.substring(1);
		} else rad = 10;
		return Integer.parseInt(value.replaceAll("_+", ""), rad);
	}

	@Override
	public void compile(MethodAssembler ma, IntSupplier dirty) {
		int v;
		try { v = parse(value); } catch (NumberFormatException e) { v = 0; }
		ma.pushConst(v);
	}

}
