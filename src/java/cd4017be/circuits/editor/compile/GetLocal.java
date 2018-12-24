package cd4017be.circuits.editor.compile;

import java.util.function.IntSupplier;

import cd4017be.circuits.editor.op.Pin;
import cd4017be.lib.jvm_utils.MethodAssembler;

public class GetLocal implements IOp {

	public final Pin pin;
	public final boolean primary;
	public SetLocal source;

	GetLocal(Pin pin, boolean primary) {
		this.pin = pin;
		this.primary = primary;
	}

	public void link(SetLocal src) {
		source = src;
		src.refCount++;
	}

	@Override
	public void compile(MethodAssembler ma, IntSupplier dirty) {
		ma.load(source.idx);
	}

}