package cd4017be.circuits.editor.compile;

import java.util.function.IntSupplier;

import cd4017be.circuits.editor.op.Pin;
import cd4017be.lib.jvm_utils.MethodAssembler;

public class SetLocal implements IOp {

	public final Chunk owner;
	public final Pin pin;
	public int idx, refCount;

	SetLocal(Chunk owner, Pin pin) {
		this.owner = owner;
		this.pin = pin;
	}

	@Override
	public void compile(MethodAssembler ma, IntSupplier dirty) {
		ma.store(idx = ma.newLocal(MethodAssembler.T_INT));
	}

}