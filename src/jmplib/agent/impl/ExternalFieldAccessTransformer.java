package jmplib.agent.impl;

import java.util.List;

import jmplib.agent.AbstractTransformer;
import jmplib.agent.UpdaterAgent;
import jmplib.annotations.AuxiliaryMethod;
import jmplib.asm.util.ASMUtils;
import jmplib.classversions.VersionTables;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * This transformer changes the external field access inside the original access
 * with method calls to its invoker methods.
 * 
 * @author Ignacio Lagartos
 *
 */
public class ExternalFieldAccessTransformer extends AbstractTransformer
		implements Opcodes {

	private boolean modified = false;

	/**
	 * It is aplicable when it is not the first load of the class, the class is
	 * inside the instrumentables collection inside the UpdaterAgent class, the
	 * class is not a version class and the class does not have a new version.
	 */
	protected boolean instrumentableClass(String className,
			Class<?> classBeingRedefined) {
		if (!UpdaterAgent.instrumentables.containsKey(className.hashCode()))
			return false;
		if (className.contains("_NewVersion_"))
			return false;
		if (classBeingRedefined == null)
			return false;
		if (VersionTables.hasNewVersion(classBeingRedefined))
			return false;
		return true;

	}

	/**
	 * Replaces the external access with auxiliar method calls.
	 */
	@Override
	public byte[] transform(String className, Class<?> classBeingRedefined,
			byte[] classfileBuffer) {
		ClassNode classNode = ASMUtils.getClassNode(classfileBuffer);
		@SuppressWarnings("unchecked")
		List<MethodNode> methods = classNode.methods;
		for (MethodNode methodNode : methods) {
			checkMethod(methodNode, className);
		}
		if (!modified)
			return null;
		modified = false;
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
	 * Returns false if the annotation {@link AuxiliaryMethod} is present in the
	 * annotation list
	 * 
	 * @param visibleAnnotations
	 *            Annotation list
	 * @return {@code false} if the list contains {@link AuxiliaryMethod}
	 *         annotation and {@code true} in other case
	 */
	private boolean isRedirectTarget(List<AnnotationNode> visibleAnnotations) {
		if (visibleAnnotations == null)
			return true;
		for (AnnotationNode annotationNode : visibleAnnotations) {
			if (annotationNode.desc.equals(ASMUtils
					.getDescriptor(AuxiliaryMethod.class)))
				return false;
		}
		return true;
	}

	/**
	 * Checks the method to discover external field accesses
	 * 
	 * @param methodNode
	 *            The method node
	 * @param className
	 *            The class name
	 */
	@SuppressWarnings("unchecked")
	private void checkMethod(MethodNode methodNode, String className) {
		if (!isRedirectTarget(methodNode.visibleAnnotations))
			return;
		for (int i = 0; i < methodNode.instructions.size(); i++) {
			AbstractInsnNode instructionNode = methodNode.instructions.get(i);
			FieldInsnNode fieldNode = null;
			switch (instructionNode.getOpcode()) {
			case GETFIELD:
				fieldNode = (FieldInsnNode) instructionNode;
				if (!className.equals(fieldNode.owner)
						&& UpdaterAgent.instrumentables
								.containsKey(fieldNode.owner.hashCode())) {
					replaceGetInsnIndy(methodNode, fieldNode, i);
					modified = true;
				}
				break;
			case PUTFIELD:
				fieldNode = (FieldInsnNode) instructionNode;
				if (!className.equals(fieldNode.owner)
						&& UpdaterAgent.instrumentables
								.containsKey(fieldNode.owner.hashCode())) {
					replaceSetInsnIndy(methodNode, fieldNode, i);
					modified = true;
				}
				break;

			default:
				break;
			}
		}
	}

	/**
	 * Replaces the access of the field by a call to its fieldGetter method
	 * 
	 * @param methodNode
	 *            The method node where the access is
	 * @param fieldNode
	 *            The access node
	 * @param index
	 *            The index of the instruction
	 */
	private void replaceGetInsnIndy(MethodNode methodNode,
			FieldInsnNode fieldNode, int index) {
		AbstractInsnNode insn = new MethodInsnNode(INVOKESTATIC,
				fieldNode.owner, "_" + fieldNode.name + "_fieldGetter", "(L"
						+ fieldNode.owner + ";)" + fieldNode.desc, false);
		methodNode.instructions.insert(fieldNode, insn);
		methodNode.instructions.remove(fieldNode);
	}

	/**
	 * Replaces the assign of the field by a call to its fieldSetter method
	 * 
	 * @param methodNode
	 *            The method node where the assign is
	 * @param fieldNode
	 *            The assign node
	 * @param index
	 *            The index of the instruction
	 */
	private void replaceSetInsnIndy(MethodNode methodNode,
			FieldInsnNode fieldNode, int index) {
		AbstractInsnNode insn = new MethodInsnNode(INVOKESTATIC,
				fieldNode.owner, "_" + fieldNode.name + "_fieldSetter", "(L"
						+ fieldNode.owner + ";" + fieldNode.desc + ")V", false);
		methodNode.instructions.insert(fieldNode, insn);
		methodNode.instructions.remove(fieldNode);
	}
}
