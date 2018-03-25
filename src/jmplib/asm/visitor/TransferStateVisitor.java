package jmplib.asm.visitor;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

import jmplib.annotations.AuxiliaryMethod;
import jmplib.annotations.NoRedirect;
import jmplib.asm.util.ASMUtils;
import jmplib.util.TransferState;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * This visitor adds _newVersion field to the class.
 * 
 * @author Ignacio Lagartos
 *
 */
public class TransferStateVisitor extends ClassVisitor implements Opcodes {

	private boolean editable = true;
	private boolean instrumented = false;
	private String internalName;
	private Map<String, String> fields = new HashMap<String, String>();

	public TransferStateVisitor(int api, ClassVisitor visitor) {
		super(api, visitor);
	}

	public TransferStateVisitor(int api) {
		super(api);
	}

	@Override
	public void visit(int arg0, int arg1, String arg2, String arg3,
			String arg4, String[] arg5) {
		if (Modifier.isInterface(arg1)) {
			editable = false;
		}
		this.internalName = arg2;
		super.visit(arg0, arg1, arg2, arg3, arg4, arg5);
	}

	/**
	 * Checks the existence of _newVersion field
	 */
	@Override
	public FieldVisitor visitField(int access, String name, String desc,
			String signature, Object value) {
		fields.put(name, desc);
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
		try {
			createTransferState();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		super.visitEnd();
	}

	private void createTransferState() throws ClassNotFoundException {
		int modifiers = ACC_PUBLIC | ACC_STATIC;
		String originalClassName = internalName.replaceAll(
				"_NewVersion_(\\d+)", "").replace('/', '.');
		Class<?> originalClass = Class.forName(originalClassName);
		Field[] fields = TransferState.getFields(originalClass);
		int newVersionIndex = 0, currentIndex = 0;
		String descriptor = "(";
		Map<String, Integer> matchingFieldsIndexes = new HashMap<String, Integer>();
		for (Field field : fields) {
			if (Modifier.isStatic(field.getModifiers())
					|| !field.getDeclaringClass().equals(originalClass)) {
				continue;
			}
			String fieldDesc = ASMUtils.getDescriptor(field.getType());
			descriptor = descriptor.concat(fieldDesc);
			if (field.getName().equals("_newVersion")) {
				newVersionIndex = currentIndex;
			}
			if (this.fields.containsKey(field.getName())) {
				matchingFieldsIndexes.put(field.getName(), currentIndex);
			}
			currentIndex = ASMUtils.nextIndex(currentIndex, fieldDesc);
		}
		descriptor = descriptor.concat(")V");
		MethodVisitor mv = cv.visitMethod(modifiers, "_transferState",
				descriptor, null, null);
		AnnotationVisitor av = mv.visitAnnotation(
				ASMUtils.getDescriptor(AuxiliaryMethod.class), true);
		av.visitEnd();
		av = mv.visitAnnotation(ASMUtils.getDescriptor(NoRedirect.class), true);
		av.visitEnd();
		mv.visitCode();
		mv.visitVarInsn(ALOAD, newVersionIndex);
		mv.visitTypeInsn(CHECKCAST, internalName);
		int castObjectIndex = ASMUtils.getFirstAvailableIndex(descriptor, 0);
		mv.visitVarInsn(ASTORE, castObjectIndex);
		for (Field field : fields) {
			if (!field.getDeclaringClass().getName().equals(originalClassName)) {
				continue;
			}
			String fieldDesc = ASMUtils.getDescriptor(field.getType());
			if (matchingFieldsIndexes.containsKey(field.getName())) {
				String versionDesc = this.fields.get(field.getName());
				if (fieldDesc.equals(versionDesc)) {
					visitEqualDescAssign(matchingFieldsIndexes, mv,
							castObjectIndex, field, fieldDesc, versionDesc);
				}
			}
		}
		mv.visitInsn(RETURN);
		mv.visitEnd();
	}

	private void visitEqualDescAssign(
			Map<String, Integer> matchingFieldsIndexes, MethodVisitor mv,
			int castObjectIndex, Field field, String fieldDesc,
			String versionDesc) {
		Integer paramIndex = matchingFieldsIndexes.get(field.getName());
		mv.visitVarInsn(ALOAD, castObjectIndex);
		mv.visitVarInsn(ASMUtils.getVarOpcode(fieldDesc), paramIndex);
		mv.visitFieldInsn(PUTFIELD, internalName, field.getName(), versionDesc);
	}

}
