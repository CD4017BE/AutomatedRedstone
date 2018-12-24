package cd4017be.circuits.editor.op;

import java.util.HashSet;
import java.util.Set;

/**
 * @author CD4017BE
 *
 */
public class Pin {

	public final OpNode src;
	public final int index;
	public Set<OpNode> dest = new HashSet<OpNode>();
	public int listingIdx;

	public Pin(OpNode src, int idx) {
		this.src = src;
		this.index = idx;
	}

	public int validUsers() {
		int n = 0;
		for (OpNode node : dest)
			if (node.check > 0) n++;
		return n > 1 && src instanceof ConstNode ? 1 : n;//constants are easy to compute
	}

	@Override
	public String toString() {
		return src + " #" + index;
	}

}
