package jmplib.asm.visitor;

import java.lang.reflect.Modifier;

import jmplib.annotations.AuxiliaryMethod;
import jmplib.annotations.NoRedirect;
import jmplib.asm.util.ASMUtils;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * This visitor adds _newVersion field to the class.
 * 
 * @author Ignacio Lagartos
 *
 */
public class NewVersionVisitor extends ClassVisitor implements Opcodes {

	private boolean editable = true;
	private boolean instrumented = false;
	private boolean isAbstract = false;
	private String internalName;

	public NewVersionVisitor(int api, ClassVisitor visitor) {
		super(api, visitor);
	}

	public NewVersionVisitor(int api) {
		super(api);
	}

	@Override
	public void visit(int arg0, int arg1, String arg2, String arg3,
			String arg4, String[] arg5) {
		if (Modifier.isInterface(arg1)) {
			editable = false;
		}
		isAbstract = Modifier.isAbstract(arg1);
		this.internalName = arg2;
		super.visit(arg0, arg1, arg2, arg3, arg4, arg5);
	}

	/**
	 * Checks the existence of _newVersion field
	 */
	@Override
	public FieldVisitor visitField(int access, String name, String desc,
			String signature, Object value) {
		if (name.equals("_newVersion")) {
			instrumented = true;
		}
		return super.visitField(access, name, desc, signature, value);
	}

	/**
	 * Adds _newVersion field and its getter and setter
	 */
	@Override
	public void visitEnd() {
		if (!editable || instrumented) {
			super.visitEnd();
			return;
		}
		createNewVersion();
		createCurrentInstanceVersion();
		createCurrentClassVersion();
		createNewInstance();
		createTransferState();
		createObjCreated();
		super.visitEnd();
	}

	private void createTransferState() {
		int modifiers = ACC_PUBLIC;
		MethodVisitor mv = cv.visitMethod(modifiers, "_transferState", "()V",
				null, null);
		AnnotationVisitor av = mv.visitAnnotation(
				ASMUtils.getDescriptor(AuxiliaryMethod.class), true);
		av.visitEnd();
		mv.visitCode();
		mv.visitInsn(RETURN);
		mv.visitMaxs(0, 0);
		mv.visitEnd();
	}

	private void createObjCreated() {
		if (isAbstract) {
			return;
		}
		FieldVisitor fv = cv.visitField(ACC_PRIVATE, "_objCreated",
				ASMUtils.getDescriptor(boolean.class), null, null);
		fv.visitEnd();
		// Getter and Setter _newVersion
		MethodVisitor getter = cv.visitMethod(ACC_PUBLIC,
				"get_ObjCreated",
				"()" + ASMUtils.getDescriptor(boolean.class), null, null);
		getter.visitAnnotation(ASMUtils.getDescriptor(NoRedirect.class), true);
		getter.visitAnnotation(ASMUtils.getDescriptor(AuxiliaryMethod.class),
				true);
		getter.visitCode();
		Label l0 = new Label();
		getter.visitLabel(l0);
		getter.visitVarInsn(ALOAD, 0);
		getter.visitFieldInsn(GETFIELD, internalName,
				"_objCreated", ASMUtils.getDescriptor(boolean.class));
		getter.visitInsn(IRETURN);
		Label l1 = new Label();
		getter.visitLabel(l1);
		getter.visitLocalVariable("this", "L" + internalName + ";", null, l0,
				l1, 0);
		getter.visitMaxs(1, 1);
		getter.visitEnd();
	}

	private void createNewInstance() {
		int modifiers = ACC_PROTECTED;
		if (isAbstract) {
			modifiers |= ACC_ABSTRACT;
		}
		MethodVisitor mv = cv.visitMethod(modifiers, "_createInstance",
				"()Ljava/lang/Object;", null, null);
		AnnotationVisitor av = mv.visitAnnotation(
				ASMUtils.getDescriptor(AuxiliaryMethod.class), true);
		av.visitEnd();
		if (!isAbstract) {
			mv.visitCode();
			mv.visitInsn(ACONST_NULL);
			mv.visitInsn(ARETURN);
		}
		mv.visitMaxs(1, 1);
		mv.visitEnd();
	}

	private void createCurrentClassVersion() {
		FieldVisitor fv = cv.visitField(ACC_PUBLIC | ACC_STATIC,
				"_currentClassVersion", ASMUtils.getDescriptor(int.class),
				null, null);
		fv.visitEnd();
	}

	private void createCurrentInstanceVersion() {
		FieldVisitor fv = cv.visitField(ACC_PUBLIC, "_currentInstanceVersion",
				ASMUtils.getDescriptor(int.class), null, null);
		fv.visitEnd();
		// Getter and Setter _newVersion
		MethodVisitor getter = cv.visitMethod(ACC_PUBLIC,
				"get_CurrentInstanceVersion",
				"()" + ASMUtils.getDescriptor(int.class), null, null);
		getter.visitAnnotation(ASMUtils.getDescriptor(NoRedirect.class), true);
		getter.visitAnnotation(ASMUtils.getDescriptor(AuxiliaryMethod.class),
				true);
		getter.visitCode();
		Label l0 = new Label();
		getter.visitLabel(l0);
		getter.visitVarInsn(ALOAD, 0);
		getter.visitFieldInsn(GETFIELD, internalName,
				"_currentInstanceVersion", ASMUtils.getDescriptor(int.class));
		getter.visitInsn(IRETURN);
		Label l1 = new Label();
		getter.visitLabel(l1);
		getter.visitLocalVariable("this", "L" + internalName + ";", null, l0,
				l1, 0);
		getter.visitMaxs(1, 1);
		getter.visitEnd();
		MethodVisitor setter = cv.visitMethod(ACC_PUBLIC,
				"set_CurrentInstanceVersion",
				"(" + ASMUtils.getDescriptor(int.class) + ")V", null, null);
		setter.visitAnnotation(ASMUtils.getDescriptor(NoRedirect.class), true);
		setter.visitAnnotation(ASMUtils.getDescriptor(AuxiliaryMethod.class),
				true);
		setter.visitCode();
		l0 = new Label();
		setter.visitLabel(l0);
		setter.visitLineNumber(43, l0);
		setter.visitVarInsn(ALOAD, 0);
		setter.visitVarInsn(ILOAD, 1);
		setter.visitFieldInsn(PUTFIELD, internalName,
				"_currentInstanceVersion", ASMUtils.getDescriptor(int.class));
		l1 = new Label();
		setter.visitLabel(l1);
		setter.visitLineNumber(44, l1);
		setter.visitInsn(RETURN);
		Label l2 = new Label();
		setter.visitLabel(l2);
		setter.visitLocalVariable("this", "L" + internalName + ";", null, l0,
				l2, 0);
		setter.visitLocalVariable("o", ASMUtils.getDescriptor(int.class), null,
				l0, l2, 1);
		setter.visitMaxs(2, 2);
		setter.visitEnd();
	}

	private void createNewVersion() {
		FieldVisitor fv = cv.visitField(ACC_PUBLIC, "_newVersion",
				ASMUtils.getDescriptor(Object.class), null, null);
		fv.visitEnd();
		// Getter and Setter _newVersion
		MethodVisitor getter = cv.visitMethod(ACC_PUBLIC, "get_NewVersion",
				"()" + ASMUtils.getDescriptor(Object.class), null, null);
		getter.visitAnnotation(ASMUtils.getDescriptor(NoRedirect.class), true);
		getter.visitAnnotation(ASMUtils.getDescriptor(AuxiliaryMethod.class),
				true);
		getter.visitCode();
		Label l0 = new Label();
		getter.visitLabel(l0);
		getter.visitVarInsn(ALOAD, 0);
		getter.visitFieldInsn(GETFIELD, internalName, "_newVersion",
				ASMUtils.getDescriptor(Object.class));
		getter.visitInsn(ARETURN);
		Label l1 = new Label();
		getter.visitLabel(l1);
		getter.visitLocalVariable("this", "L" + internalName + ";", null, l0,
				l1, 0);
		getter.visitMaxs(1, 1);
		getter.visitEnd();
		MethodVisitor setter = cv.visitMethod(ACC_PUBLIC, "set_NewVersion", "("
				+ ASMUtils.getDescriptor(Object.class) + ")V", null, null);
		setter.visitAnnotation(ASMUtils.getDescriptor(NoRedirect.class), true);
		setter.visitAnnotation(ASMUtils.getDescriptor(AuxiliaryMethod.class),
				true);
		setter.visitCode();
		l0 = new Label();
		setter.visitLabel(l0);
		setter.visitLineNumber(43, l0);
		setter.visitVarInsn(ALOAD, 0);
		setter.visitVarInsn(ALOAD, 1);
		setter.visitFieldInsn(PUTFIELD, internalName, "_newVersion",
				ASMUtils.getDescriptor(Object.class));
		l1 = new Label();
		setter.visitLabel(l1);
		setter.visitLineNumber(44, l1);
		setter.visitInsn(RETURN);
		Label l2 = new Label();
		setter.visitLabel(l2);
		setter.visitLocalVariable("this", "L" + internalName + ";", null, l0,
				l2, 0);
		setter.visitLocalVariable("o", ASMUtils.getDescriptor(Object.class),
				null, l0, l2, 1);
		setter.visitMaxs(2, 2);
		setter.visitEnd();
	}

}
