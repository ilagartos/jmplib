package jmplib.asm.visitor;

import jmplib.classversions.VersionClass;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;
import org.objectweb.asm.commons.Method;

/**
 * This visitor instruments the constructor to add the transfer state support
 * @author Nacho
 *
 */
public class ConstructorBodyInstrumentor extends AdviceAdapter {

	private Type ownerType;

	public ConstructorBodyInstrumentor(int api, Type owner, int access,
			String name, String desc, MethodVisitor mv) {
		super(Opcodes.ASM5, mv, access, name, desc);
		this.ownerType = owner;

	}

	@Override
	protected void onMethodExit(int opcode) {
		if (opcode != Opcodes.ATHROW) {
			loadThis();
			push(true);
			putField(ownerType, "_objCreated", Type.BOOLEAN_TYPE);
			loadThis();
			invokeVirtual(ownerType, new Method("get_ObjCreated", "()Z"));
			Label end = new Label();
			ifZCmp(EQ, end);
			getStatic(ownerType, "_currentClassVersion", Type.INT_TYPE);
			push(0);
			ifCmp(Type.INT_TYPE, LE, end);
			loadThis();
			getStatic(ownerType, "_currentClassVersion", Type.INT_TYPE);
			putField(ownerType, "_currentInstanceVersion", Type.INT_TYPE);
			loadThis();
			loadThis();
			invokeVirtual(ownerType, new Method("_createInstance", "()Ljava/lang/Object;"));
			invokeVirtual(ownerType, new Method("set_NewVersion", "(Ljava/lang/Object;)V"));
			loadThis();
			invokeVirtual(ownerType, new Method("get_NewVersion", "()Ljava/lang/Object;"));
			checkCast(Type.getType(VersionClass.class));
			loadThis();
			invokeInterface(Type.getType(VersionClass.class), new Method("set_OldVersion", "(Ljava/lang/Object;)V"));
			loadThis();
			invokeVirtual(ownerType, new Method("_transferState", "()V"));
			mark(end);
			mv.visitFrame(Opcodes.F_FULL, 1, new Object[] {ownerType.getInternalName()}, 0, new Object[] {});
		}
		super.onMethodExit(opcode);
	}

}
