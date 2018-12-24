package cd4017be.circuits.editor.op;

/**
 * @author CD4017BE
 *
 */
public class ReadVar extends IONode {

	public String initValue = "0";

	public ReadVar(int index) {
		super(OpType.read, index);
	}

}
