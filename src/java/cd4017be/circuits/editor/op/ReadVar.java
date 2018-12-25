package cd4017be.circuits.editor.op;

import cd4017be.circuits.editor.InvalidSchematicException;
import cd4017be.circuits.editor.InvalidSchematicException.ErrorType;

/**
 * @author CD4017BE
 *
 */
public class ReadVar extends IONode implements IConfigurable {

	public String initValue = "0";

	public ReadVar(int index) {
		super(OpType.read, index);
	}

	@Override
	public void checkValid() throws InvalidSchematicException {
		super.checkValid();
		try {
			ConstNode.parse(initValue);
		} catch (NumberFormatException e) {
			throw new InvalidSchematicException(ErrorType.invalidCfg, this, 0);
		}
	}

	@Override
	public String getCfg() {
		return initValue;
	}

	@Override
	public void setCfg(String cfg) {
		initValue = cfg;
	}

	@Override
	public String cfgTooltip() {
		return "gui.circuits.opCfg.ivalue";
	}

}
