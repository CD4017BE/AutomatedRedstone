package cd4017be.circuits.editor;

import cd4017be.circuits.editor.op.OpNode;

public class PinRef {

	public final OpNode source;
	public final int index;

	public PinRef(OpNode src, int idx) {
		this.source = src;
		this.index = idx;
	}

}
