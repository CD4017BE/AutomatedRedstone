package cd4017be.circuits.editor.compile;

import java.util.function.IntSupplier;

import cd4017be.lib.jvm_utils.MethodAssembler;

/**
 * @author CD4017BE
 *
 */
public interface IOp {

	/**
	 * @param ma the update method
	 * @param access to the dirty flag
	 */
	void compile(MethodAssembler ma, IntSupplier dirtyIdx);

}
