package cd4017be.circuits.editor;

import cd4017be.circuits.editor.op.OpNode;

/**
 * @author CD4017BE
 *
 */
public class InvalidSchematicException extends Exception {
	private static final long serialVersionUID = 1L;

	public final ErrorType type;
	public final OpNode node;
	public final int pin;

	public InvalidSchematicException(ErrorType type, OpNode node, int pin) {
		this.type = type;
		this.node = node;
		this.pin = pin;
	}

	public int compact() {
		return (type == null ? -1 : type.ordinal()) & 0xff
			| (node == null ? -1 : node.index << 8) & 0xffff00
			| pin << 24 & 0xff000000;
	}

	public static final int NO_ERROR = -1;

	public enum ErrorType {
		noCircuitBoard,
		missingMaterial,
		invalidLabel,
		invalidCfg,
		duplicateLabel,
		readConflict,
		writeConflict,
		causalLoop,
		missingInput;

		public static ErrorType get(int code) {
			code &= 0xff;
			ErrorType[] arr = values();
			return code < arr.length ? arr[code] : null;
		}
	}

}
