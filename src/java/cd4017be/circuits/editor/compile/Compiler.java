package cd4017be.circuits.editor.compile;

import static cd4017be.lib.jvm_utils.ClassAssembler.*;
import static cd4017be.lib.jvm_utils.NBT2Class.*;

import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

import cd4017be.api.circuits.Chip;
import cd4017be.circuits.editor.InvalidSchematicException;
import cd4017be.circuits.editor.InvalidSchematicException.ErrorType;
import cd4017be.circuits.editor.op.ConstNode;
import cd4017be.circuits.editor.op.IONode;
import cd4017be.circuits.editor.op.OpNode;
import cd4017be.circuits.editor.op.OpType;
import cd4017be.circuits.editor.op.Pin;
import cd4017be.circuits.editor.op.ReadVar;
import cd4017be.circuits.editor.op.WriteVar;
import cd4017be.lib.jvm_utils.ConstantPool;
import cd4017be.lib.jvm_utils.MethodAssembler;
import static cd4017be.lib.jvm_utils.MethodAssembler.*;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.NBTTagCompound;

/**
 * @author CD4017BE
 *
 */
public class Compiler {

	public static ArrayList<IONode> filterProgramm(Collection<OpNode> program) throws InvalidSchematicException {
		HashMap<String, IONode> defined = new HashMap<>();
		ArrayList<WriteVar> writers = new ArrayList<>();
		for (OpNode node : program) {
			if (node == null) continue;
			node.check = 0;
			if (!(node instanceof IONode)) continue;
			IONode io = (IONode)node;
			String label = io.label;
			if (node instanceof WriteVar) writers.add((WriteVar)node);
			else {
				IONode confl = defined.put(label, io);
				if (confl != null)
					throw new InvalidSchematicException(confl instanceof ReadVar && node instanceof ReadVar ? ErrorType.readConflict : ErrorType.duplicateLabel, node, -1);
			}
		}
		for (WriteVar node : writers) {
			String label = node.label;
			IONode pair = defined.put(label, node);
			if (pair == null || pair instanceof ReadVar) node.link((ReadVar)pair);
			else throw new InvalidSchematicException(pair instanceof WriteVar ? ErrorType.writeConflict : ErrorType.duplicateLabel, pair, -1);
		}
		ArrayList<IONode> nodes = new ArrayList<>(defined.values());
		Collections.sort(nodes, (a, b)-> {
			int i = a.op.compareTo(b.op);
			if (i != 0) return i;
			return a.label.compareTo(b.label);
		});
		return nodes;
	}

	public static NBTTagCompound assemble(ArrayList<IONode> ioPorts) throws InvalidSchematicException {
		//Collect fields
		ArrayList<String> fields = new ArrayList<>(ioPorts.size()), labels = new ArrayList<String>();
		int numIn = 0, numOut = 0;
		for (IONode node : ioPorts) {
			node.checkValid();
			labels.add(node.label);
			fields.add(node.fieldDesc());
			if (node.op == OpType.in) numIn++;
			else if (node.op == OpType.out) numOut++;
		}
		//Assemble NBT
		NBTTagCompound nbt = new NBTTagCompound();
		ConstantPool cpt = new ConstantPool("Chip", Chip.class);
		nbt.setTag("class", wrapUp(genFields(fields, cpt), labels, numIn, numOut, genMain(groupOperations(ioPorts), cpt), cpt));
		nbt.setIntArray("state", getInitState(ioPorts, fields.size() - numIn - numOut));
		nbt.setByte("in", (byte)numIn);
		nbt.setByte("out", (byte)numOut);
		return nbt;
	}

	private static int[] getInitState(ArrayList<IONode> ioPorts, int numVal) {
		int[] state = new int[numVal];
		for (int i = ioPorts.size() - numVal, j = 0; j < numVal; i++, j++) {
			IONode node = ioPorts.get(i);
			ReadVar def = node instanceof WriteVar ?
				((WriteVar)node).getLink() : (ReadVar)node;
			if (def != null) try {
					state[j] = ConstNode.parse(def.initValue);
				} catch (NumberFormatException e) {}
		}
		return state;
	}

	private static ArrayList<Chunk> groupOperations(ArrayList<IONode> ioPorts) {
		HashMap<Pin, SetLocal> provided = new HashMap<>();
		ArrayDeque<GetLocal> required = new ArrayDeque<>();
		ArrayList<Chunk> chunks = new ArrayList<>();
		for (IONode node : ioPorts) {
			if (!node.isEnd()) continue;
			Chunk chunk = new Chunk(node);
			chunk.map(provided);
			chunks.add(chunk);
			required.addAll(chunk.inputs);
		}
		while (!required.isEmpty()) {
			GetLocal get = required.remove();
			Pin pin = get.pin;
			SetLocal set = provided.get(pin);
			if (set != null) get.link(set);
			else if (get.primary) {
				Chunk chunk = new Chunk(pin.src);
				chunk.map(provided);
				required.addAll(chunk.inputs);
				chunks.add(chunk);
				get.link(provided.get(pin));
			} else required.add(get);
		}
		Collections.sort(chunks);
		return chunks;
	}

	private static byte[] genMain(ArrayList<Chunk> chunks, ConstantPool cpt) {
		MethodAssembler ma = new MethodAssembler(cpt, T_THIS);
		DirtyField dirty = new DirtyField(ma);
		for (Chunk c : chunks)
			for (IOp op : c.operations)
				op.compile(ma, dirty);
		//handle dirty flag
		ByteBuf b = ma.code;
		ma.load(0);
		int i = dirty.idx;
		if (i >= 0) ma.load(i);
		else ma.pushConst(0);
		b.writeByte(_putfield).writeShort(cpt.putFieldMethod(Chip.class, "Z dirty"));
		b.writeByte(_return);
		//generate
		return genMethod(1, cpt.putUtf8("update"), cpt.putUtf8("()V"), ma.generate());
	}

	static class DirtyField implements IntSupplier {
		private final MethodAssembler ma;
		private int idx = -1;
		public DirtyField(MethodAssembler ma) { this.ma = ma; }
		@Override
		public int getAsInt() {
			int i = idx;
			if (i < 0) {
				MethodAssembler ma = this.ma;
				idx = i = ma.newLocal(T_INT);
				ma.pushConst(0);
				ma.store(i);
			}
			return i;
		}
	}

	private static NBTTagCompound wrapUp(int[] fields, ArrayList<String> labels, int numIn, int numOut, byte[] main, ConstantPool cpt) {
		int l = fields.length,
			varOfs = numIn + numOut,
			numVar = l - varOfs;
		ArrayList<byte[]> methods = new ArrayList<>();
		methods.add(main);
		{	//input pins
			short[] inputs = new short[numIn],
					inputL = new short[numIn];
			for (int i = 0; i < numIn; i++) {
				inputs[i] = refField(fields[i], cpt);
				inputL[i] = cpt.putString(labels.get(i));
			}
			methods.add(genMethod(1, cpt.putUtf8("inputs"), cpt.putUtf8("()[Ljava/lang/String;"),
					mkArray(inputL, cpt.putClass(String.class.getName()), cpt)));
			methods.add(genMethod(1, cpt.putUtf8("connectInput"), cpt.putUtf8("(ILjava/util/function/IntSupplier;)V"),
					fieldSetSwitch(inputs, T_OBJECT + cpt.putClass(IntSupplier.class.getName()), cpt)));
		} {	//output pins
			short[] outputs = new short[numOut],
					outputL = new short[numOut];
			for (int i = 0, j = numIn; j < varOfs; j++, i++) {
				outputs[i] = refField(fields[j], cpt);
				outputL[i] = cpt.putString(labels.get(j));
			}
			methods.add(genMethod(1, cpt.putUtf8("outputs"), cpt.putUtf8("()[Ljava/lang/String;"),
					mkArray(outputL, cpt.putClass(String.class.getName()), cpt)));
			methods.add(genMethod(1, cpt.putUtf8("connectOutput"), cpt.putUtf8("(ILjava/util/function/IntConsumer;)V"),
					fieldSetSwitch(outputs, T_OBJECT + cpt.putClass(IntConsumer.class.getName()), cpt)));
		} {	//variables
			short[] vars = new short[numVar],
					varL = new short[numVar];
			for (int i = 0, j = varOfs; j < l; j++, i++) {
				vars[i] = refField(fields[j], cpt);
				varL[i] = cpt.putString(labels.get(j));
			}
			methods.add(genMethod(1, cpt.putUtf8("getLabels"), cpt.putUtf8("()[Ljava/lang/String;"),
					mkArray(varL, cpt.putClass(String.class.getName()), cpt)));
			methods.add(genMethod(1, cpt.putUtf8("getStates"), cpt.putUtf8("()[I"),
					mkArray(vars, (short)0, cpt)));
			methods.add(genMethod(1, cpt.putUtf8("setParam"), cpt.putUtf8("(II)V"),
					fieldSetSwitch(vars, T_INT, cpt)));
		}
		return writeNBT(cpt, fields, methods);
	}

	private static Map<Short, byte[]> mkArray(short[] values, short type, ConstantPool cpt) {
		int n = values.length;
		ByteBuffer b = ByteBuffer.allocate((type != 0 ? 7 : 6 + n) + 6 * n + (n > 5 ? n - 5 : 0));
		b.put(_iconst_((short)n));
		int p;
		if (type != 0) {
			p = 0;
			b.put(_anewarray).putShort(type); //new type[]
		} else {
			p = 1;
			b.put(_newarray).put((byte)10); //new int[]
		}
		b.put((byte)(_astore_0 + p));
		for (int i = 0; i < n; i++) {
			b.put((byte)(_aload_0 + p));
			short l = values[i];
			if (type != 0) {
				b.put(_iconst_((short)i)); //_iconst_i
				b.put(_ldc_w).putShort(l);
				b.put(_aastore);
			} else {
				b.put(_iconst_((short)i)); //_iconst_i
				b.put(_aload_0).put(_getfield).putShort(l);
				b.put(_iastore);
			}
		}
		b.put((byte)(_aload_0 + p));
		b.put(_areturn);
		return genCode(1 + p, 3, b.array(), null, null, cpt);
	}

	private static Map<Short, byte[]> fieldSetSwitch(short[] fields, int type, ConstantPool cpt) {
		int n = fields.length;
		switch(n) {
		case 0: return genCode(3, 0, new byte[] {_return}, null, null, cpt);
		case 1: 
			short f = fields[0];
			return genCode(3, 2, new byte[] {
				_aload_0, type == T_INT ? _iload_2 : _aload_2,
				_putfield, (byte)(f>>8), (byte)f, _return
			}, null, null, cpt);
		case 2: {
			MethodAssembler ma = new MethodAssembler(cpt, T_THIS, T_INT, type);
			ByteBuf b = ma.code;
			ma.load(0); ma.load(2); ma.load(1);
			b.writeByte(_ifne).writeShort(7);
			b.writeByte(_putfield).writeShort(fields[0]).writeByte(_return);
			ma.pop(1); ma.frame();
			b.writeByte(_putfield).writeShort(fields[1]).writeByte(_return);
			return ma.generate();
		}
		default:
			MethodAssembler ma = new MethodAssembler(cpt, T_THIS, T_INT, type);
			ByteBuf b = ma.code;
			ma.load(0); ma.load(2); ma.load(1);
			b.writeByte(_tableswitch);
			int ofs = 9 + 4 * n;
			b.writeInt(ofs + 4 * n - 4); //default_pc
			b.writeInt(0).writeInt(n - 1); //min, max
			for (int i = 0; i < n - 1; i++)
				b.writeInt(ofs + 4 * i); //case_pc
			ma.pop(1);
			for (int i = 0; i < n; i++) {
				ma.frame();
				b.writeByte(_putfield).writeShort(fields[i]).writeByte(_return);
			}
			return ma.generate();
		}
	}

}
