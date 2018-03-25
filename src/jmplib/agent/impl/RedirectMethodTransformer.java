package jmplib.agent.impl;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;

import jmplib.agent.AbstractTransformer;
import jmplib.agent.UpdaterAgent;
import jmplib.annotations.NoRedirect;
import jmplib.asm.util.ASMUtils;
import jmplib.classversions.DeleteMemberTables;
import jmplib.classversions.VersionTables;
import jmplib.classversions.util.MemberKey;
import jmplib.util.TransferState;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * This transformer redirects the methods of the class to the new versions.
 * 
 * @author Ignacio Lagartos
 *
 */
public class RedirectMethodTransformer extends AbstractTransformer implements
		Opcodes {

	private boolean isVersion = false;
	private Class<?> originalClass;
	private String lastVersionClassName, originalClassDescriptor,
			originalClassName;

	/**
	 * It is aplicable when it is not the first load of the class, the class is
	 * inside the instrumentables collection inside the UpdaterAgent class and
	 * it has a new version.
	 */
	protected boolean instrumentableClass(String className,
			Class<?> classBeingRedefined) {
		if (classBeingRedefined == null)
			return false;
		if (!UpdaterAgent.instrumentables.containsKey(className.hashCode()))
			return false;
		if (className.contains("_NewVersion_"))
			isVersion = true;
		else
			isVersion = false;
		if (!isVersion && !VersionTables.hasNewVersion(classBeingRedefined))
			return false;
		return true;
	}

	/**
	 * Redirects methods to the new version of the class.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public byte[] transform(String className, Class<?> classBeingRedefined,
			byte[] classfileBuffer) {
		if (isVersion) {
			originalClass = VersionTables.isVersionOf(classBeingRedefined);
		} else {
			originalClass = classBeingRedefined;
		}
		Class<?> lastVersion = VersionTables.getNewVersion(originalClass);
		lastVersionClassName = ASMUtils.getInternalName(lastVersion);
		originalClassName = ASMUtils.getInternalName(originalClass);
		originalClassDescriptor = ASMUtils.getDescriptor(originalClass);

		ClassNode classNode = ASMUtils.getClassNode(classfileBuffer);
		List<MethodNode> methods = classNode.methods;
		for (MethodNode methodNode : methods) {
			if (isRedirectTarget(methodNode)) {
				if (methodNode.name.equals("_createInstance")) {
					changeCreateInstance(methodNode);
				} else if (methodNode.name.equals("_transferState")
						&& !isVersion) {
					changeTransferState(methodNode);
				} else {
					redirectMethod(methodNode);
				}
			}
		}

		ClassWriter cw = null;
		try {
			cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
			classNode.accept(cw);
		} catch (Throwable e) {
			System.err.println(e.getMessage());
		}
		byte[] bytes = cw.toByteArray();
		return bytes;
	}

	/**
	 * <p>
	 * The method is redirect target if:
	 * </p>
	 * <ul>
	 * <li>It doesn't have the {@code NoRedirect} annotation</li>
	 * <li>It isn't abstract or native</li>
	 * <li>It belongs to an original class</li>
	 * <li>It is static method</li>
	 * </ul>
	 * 
	 * @param methodNode
	 *            The method node
	 * @return {@code true} if any of the conditions is satisfied
	 */
	@SuppressWarnings("unchecked")
	private boolean isRedirectTarget(MethodNode methodNode) {
		if (methodNode.visibleAnnotations != null) {
			List<AnnotationNode> visibleAnnotations = methodNode.visibleAnnotations;
			for (AnnotationNode annotationNode : visibleAnnotations) {
				if (annotationNode.desc.equals(ASMUtils
						.getDescriptor(NoRedirect.class)))
					return false;
			}
		}
		if (!(!isVersion || Modifier.isStatic(methodNode.access)))
			return false;
		if (Modifier.isAbstract(methodNode.access))
			return false;
		if (Modifier.isNative(methodNode.access))
			return false;
		if (DeleteMemberTables.check(new MemberKey(originalClassName,
				methodNode.name, methodNode.desc))) {
			return false;
		}
		return true;
	}

	/**
	 * <p>
	 * The method is redirected to the new version
	 * </p>
	 * <ul>
	 * <li>If the method is static, it is redirected to the corresponding static
	 * method in the last version of the class</li>
	 * <li>If it is an instance method, the method is redirected to its invoker
	 * inside the most recent version</li>
	 * </ul>
	 * 
	 * @param methodNode
	 */
	private void redirectMethod(MethodNode methodNode) {
		String invokerName, invokerDesc;
		if (Modifier.isStatic(methodNode.access)) {
			invokerDesc = methodNode.desc;
			invokerName = methodNode.name;
		} else {
			invokerName = "_".concat(methodNode.name).concat("_invoker");
			invokerDesc = methodNode.desc.replaceAll("\\(", "("
					+ originalClassDescriptor);
		}

		InsnList instructions = new InsnList();
		Type[] params = Type.getArgumentTypes(invokerDesc);
		int index = 0;
		for (int i = 0; i < params.length; i++) {
			instructions.add(new VarInsnNode(ASMUtils.getVarOpcode(params[i]
					.getDescriptor()), index));
			index = ASMUtils.nextIndex(index, params[i].getDescriptor());
		}
		instructions.add(new MethodInsnNode(INVOKESTATIC, lastVersionClassName,
				invokerName, invokerDesc, false));
		instructions.add(new InsnNode(ASMUtils.getReturnOpcode(invokerDesc)));
		int maxStack = methodNode.maxStack;
		int maxLocals = methodNode.maxLocals;
		methodNode.instructions = instructions;
		methodNode.tryCatchBlocks = null;
		methodNode.maxLocals = maxLocals;
		methodNode.maxStack = maxStack;
		methodNode.localVariables = null;
	}

	private void changeCreateInstance(MethodNode methodNode) {
		InsnList instructions = new InsnList();
		instructions.add(new TypeInsnNode(NEW, lastVersionClassName));
		instructions.add(new InsnNode(DUP));
		instructions.add(new MethodInsnNode(INVOKESPECIAL,
				lastVersionClassName, "<init>", "()V", false));
		instructions.add(new InsnNode(ARETURN));
		methodNode.instructions = instructions;
		methodNode.maxLocals = 1;
		methodNode.maxStack = 2;
		methodNode.localVariables = null;
	}

	private void changeTransferState(MethodNode methodNode) {
		InsnList instructions = new InsnList();
		Field[] fields = TransferState.getFields(originalClass);
		String descriptor = "(";
		for (Field field : fields) {
			if (Modifier.isStatic(field.getModifiers())
					|| !field.getDeclaringClass().equals(originalClass))
				continue;
			String fieldDesc = ASMUtils.getDescriptor(field.getType());
			descriptor = descriptor.concat(fieldDesc);
			instructions.add(new VarInsnNode(ALOAD, 0));
			if (field.getName().equals("_newVersion")) {
				instructions.add(new MethodInsnNode(INVOKEVIRTUAL,
						originalClassName, "get_NewVersion",
						"()Ljava/lang/Object;", false));
			} else {
				instructions.add(new FieldInsnNode(GETFIELD, originalClassName,
						field.getName(), fieldDesc));
			}
		}
		descriptor = descriptor.concat(")V");
		instructions.add(new MethodInsnNode(INVOKESTATIC, lastVersionClassName,
				"_transferState", descriptor, false));
		Class<?> superClass = originalClass.getSuperclass();
		String internalSuperClassName = ASMUtils.getInternalName(superClass);
		if (isSuperCallNeeded(internalSuperClassName)) {
			instructions.add(new VarInsnNode(ALOAD, 0));
			instructions.add(new MethodInsnNode(INVOKESPECIAL,
					internalSuperClassName, "_transferState", "()V", false));
		}
		instructions.add(new InsnNode(RETURN));
		methodNode.instructions = instructions;
		methodNode.localVariables = null;
	}

	private boolean isSuperCallNeeded(String internalSuperClassName) {
		return UpdaterAgent.instrumentables.containsKey(internalSuperClassName
				.hashCode());
	}
}
