package cd4017be.circuits.editor.op;

import static cd4017be.lib.jvm_utils.ClassAssembler.*;

import cd4017be.lib.jvm_utils.MethodAssembler;
import static cd4017be.lib.jvm_utils.MethodAssembler.*;
import io.netty.buffer.ByteBuf;

/**
 * @author CD4017BE
 *
 */
public enum OpType {

	in(0x01) {
		@Override
		public OpNode node(int idx) { return new IONode(this, idx); }
	}, out(0x10) {
		@Override
		public OpNode node(int idx) { return new IONode(this, idx); }
	}, read(0x01) {
		@Override
		public OpNode node(int idx) { return new ReadVar(idx); }
	}, write(0x31) {
		@Override
		public OpNode node(int idx) { return new WriteVar(idx); }
	}, cst(0x01) {
		@Override
		public OpNode node(int idx) { return new ConstNode(idx); }
	},
	not(0x11, "i-", _iconst_m1, _ixor),
	or(0x21, "-", _ior),
	nor(0x21, "-", _ior, _iconst_m1, _ixor),
	and(0x21, "-", _iand),
	nand(0x21, "-", _iand, _iconst_m1, _ixor),
	xor(0x21, "-", _ixor),
	xnor(0x21, "-", _ixor, _iconst_m1, _ixor),
	not0(0x11, "i-6#|", _dup, _ifeq, (byte)0, (byte)5, _pop, _iconst_m1),
	is0(0x11, "-7#|i#|", _ifeq, (byte)0, (byte)7, _iconst_0, _goto, (byte)0, (byte)4, _iconst_m1),
	nsgn(0x11, "-7#|i#|", _iflt, (byte)0, (byte)7, _iconst_0, _goto, (byte)0, (byte)4, _iconst_m1),
	psgn(0x11, "-7#|i#|", _ifge, (byte)0, (byte)7, _iconst_0, _goto, (byte)0, (byte)4, _iconst_m1),
	eq(0x21, "2-7#|i#|", _if_icmpeq, (byte)0, (byte)7, _iconst_0, _goto, (byte)0, (byte)4, _iconst_m1),
	neq(0x21, "2-7#|i#|", _if_icmpne, (byte)0, (byte)7, _iconst_0, _goto, (byte)0, (byte)4, _iconst_m1),
	ls(0x21, "2-7#|i#|", _if_icmplt, (byte)0, (byte)7, _iconst_0, _goto, (byte)0, (byte)4, _iconst_m1),
	geq(0x21, "2-7#|i#|", _if_icmpge, (byte)0, (byte)7, _iconst_0, _goto, (byte)0, (byte)4, _iconst_m1),
	inc(0x11, "i-", _iconst_1, _iadd),
	dec(0x11, "i-", _iconst_1, _isub),
	neg(0x11, "", _ineg),
	abs(0x11, "i-5#|", _dup, _ifge, (byte)0, (byte)4, _ineg),
	add(0x21, "-", _iadd),
	sub(0x21, "-", _isub),
	mul(0x21, "-", _imul),
	div(0x21, "i-6#c3#|-#|", _dup, _ifne, (byte)0, (byte)10, pop2, _ldc_w, (byte)0x80, (byte)0, (byte)0, (byte)0, _goto, (byte)0, (byte)4, _idiv),
	mod(0x21, "i-9#|-#|", _dup, _ifne, (byte)0, (byte)8, pop2, _iconst_0, _goto, (byte)0, (byte)4, _irem),
	bsl(0x21, "-", _ishl),
	bsr(0x21, "-", _ishr),
	usr(0x21, "-", _iushr),
	max(0x21, "ii2-5#|-", _dup2, _if_icmpge, (byte)0, (byte)4, _swap, _pop),
	min(0x21, "ii2-5#|-", _dup2, _if_icmple, (byte)0, (byte)4, _swap, _pop),
	sort(0x22, "ii2-5#|", _dup2, _if_icmpge, (byte)0, (byte)4, _swap) {
		@Override
		public void compile(MethodAssembler ma, OpNode node) {
			Pin[] outs = node.outputs;
			if (outs[0].validUsers() == 0) min.compile(ma, node);
			else if (outs[1].validUsers() == 0) max.compile(ma, node);
			else super.compile(ma, node);
		}
	},
	swt(0x331, "-4#|-", _ifeq, (byte)0, (byte)4, _swap, _pop),
	swt_(0x331, "-4#|-", _ifne, (byte)0, (byte)4, _swap, _pop),//inverted switch (internal only)
	frg(0x332, "-4#|", _ifeq, (byte)0, (byte)4, _swap) {
		@Override
		public void compile(MethodAssembler ma, OpNode node) {
			Pin[] outs = node.outputs;
			if (outs[0].validUsers() == 0) swt_.compile(ma, node);
			else if (outs[1].validUsers() == 0) swt.compile(ma, node);
			else super.compile(ma, node);
		}
	}, bsplt(0x108) {
		@Override
		public void compile(MethodAssembler ma, OpNode node) {
			ByteBuf b = ma.code;
			short mask = 1;
			int n = 1, l = node.out();
			for (Pin pin : node.outputs) {
				if (pin.validUsers() > 0) {
					if (n > 1) b.writeByte(_swap);
					if (n < l) {
						b.writeByte(_dup);
						ma.push(T_INT);
					}
					ma.pushConst(mask);
					b.writeBytes(new byte[] {_iand, _dup, _ifeq, 0, 5, _pop, _iconst_m1});
					ma.pop(1); ma.frame();
					n++;
				}
				mask <<= 1;
			}
			ma.push(n - 1);
			ma.pop(1);
		}
	}, bcomb(0x801) {
		@Override
		public boolean requiresInput(int i) { return false; }
		@Override
		public void compile(MethodAssembler ma, OpNode node) {
			ByteBuf b = ma.code;
			boolean last = false;
			for (int i = node.inputs.length - 1; i >= 0; i--) {
				if (node.inputs[i] == null) continue;
				if (last) b.writeByte(_swap);
				ma.push(T_INT);
				b.writeBytes(new byte[] {_dup, _ifeq, 0, (byte)(i < 3 ? 5 : i < 7 ? 6 : 7), _pop}).writeBytes(_iconst_((short)(1 << i)));
				ma.pop(1); ma.frame();
				if (last) {
					b.writeByte(_ior);
					ma.pop(1);
				} else last = true;
			}
		}
	};

	private OpType(int io) {
		this(io, null);
	}

	/**
	 * @param io = 0xIO with I number of input pins and O number of output pins
	 * @param stack stack operations performed by instructions:<br>
	 * "i": push int on stack<br>
	 * "n-": pop n elements from stack<br>
	 * "n#": write n bytes of code<br>
	 * "|": insert stack map frame here<br>
	 * "c": insert int constant reference from cpt
	 * @param instr instruction data
	 */
	private OpType(int io, String stack, byte... instr) {
		this.ins = io >> 4 & 15;
		this.outs = io & 15;
		this.instr = instr;
		this.stack = stack;
	}

	final byte[] instr;

	public final int ins, outs;
	
	private final String stack;

	public OpNode node(int idx) {
		return new OpNode(this, idx);
	}

	public void compile(MethodAssembler ma, OpNode node) {
		byte[] instr = this.instr;
		int bi = 0, v = 0;
		for (char c : stack.toCharArray())
			switch(c) {
			case '|':
				ma.frame();
				break;
			case 'i':
				ma.push(T_INT);
				break;
			case '-':
				ma.pop(v <= 0 ? 1 : v);
				v = 0;
				break;
			case '#':
				if (v <= 0) v = 1;
				ma.code.writeBytes(instr, bi, v);
				bi += v;
				v = 0;
				break;
			case 'c':
				ma.code.writeShort(ma.cpt.putInt(
						(instr[bi++] & 0xff) << 24 |
						(instr[bi++] & 0xff) << 16 |
						(instr[bi++] & 0xff) << 8 |
						(instr[bi++] & 0xff)));
				break;
			default:
				if (c >= '0' && c <= '9')
					v = v * 10 + c - '0';
			}
		v = instr.length - bi;
		if (v > 0) ma.code.writeBytes(instr, bi, v);
	}

	public boolean requiresInput(int i) {
		return i < Integer.bitCount(pinsI);
	}

	public static OpNode newOp(int id, int idx) {
		OpType[] values = values();
		return id < values.length ? values[id].node(idx) : null;
	}

}
