package jmplib.agent.impl;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jmplib.agent.AbstractTransformer;
import jmplib.agent.UpdaterAgent;
import jmplib.annotations.AuxiliaryMethod;
import jmplib.annotations.DefaultMethod;
import jmplib.asm.util.ASMUtils;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * This transformer overrides methods of the superclasses. This allows to
 * redefine this methods by version classes.
 * 
 * @author Ignacio Lagartos
 *
 */
public class DefaultMethodTransformer extends AbstractTransformer implements
		Opcodes {

	private Map<Integer, MethodHolder> methods;
	private Map<Integer, FieldHolder> fields;

	private boolean isVersion = false;
	private String originalClassName = null;

	/**
	 * It is aplicable when it is the first load of the class and the class is
	 * inside the instrumentables collection inside the UpdaterAgent class.
	 */
	protected boolean instrumentableClass(String className,
			Class<?> classBeingRedefined) {
		if (UpdaterAgent.instrumentables.containsKey(className.hashCode())
				&& classBeingRedefined == null)
			return true;
		else
			return false;
	}

	/**
	 * Redefine superclass methods and adds auxiliar methods to the class.
	 */
	@Override
	public byte[] transform(String className, Class<?> classBeingRedefined,
			byte[] classfileBuffer) {
		if (className.contains("_NewVersion_")) {
			isVersion = true;
			originalClassName = className
					.replaceAll("(_NewVersion_)(\\d+)", "");
		} else {
			isVersion = false;
		}
		ClassNode classNode = ASMUtils.getClassNode(classfileBuffer);
		if (Modifier.isInterface(classNode.access))
			return classfileBuffer;
		List<ClassNode> superClasses = getSuperClasses(classNode.superName);
		getSuperMembers(superClasses);
		calculateDefaults(classNode);
		addMembers(classNode);
		ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
		try {
			classNode.accept(cw);
		} catch (Throwable e) {
			e.printStackTrace();
		}
		byte[] bytes = cw.toByteArray();
		return bytes;
	}

	/**
	 * Adds the methods to the class node. If the class is a version class, the
	 * invoker methods are added too.
	 * 
	 * @param classNode
	 */
	private void addMethods(ClassNode classNode) {
		for (MethodHolder holder : methods.values()) {
			if (holder.name.equals("finalize")) {
				// Do not add this method
			} else if (isVersion) {
				if (holder.name.equals("equals")) {
					addEqualsMethodNodeImproved(classNode, holder);
				} else {
					addMethodNode(classNode, holder);
				}
				addInvokerImproved(classNode, holder);
			} else {
				addMethodNode(classNode, holder);
			}
		}
	}

	/**
	 * Adds the method to the class node, the body of the method contains a call
	 * to the super class method.
	 * 
	 * @param classNode
	 * @param holder
	 */
	@SuppressWarnings("unchecked")
	private void addMethodNode(ClassNode classNode, MethodHolder holder) {
		MethodNode methodNode = new MethodNode(holder.access, holder.name,
				holder.desc, holder.signature, holder.exceptions);
		InsnList instructions = new InsnList();
		instructions.add(new VarInsnNode(ALOAD, 0));
		instructions.add(ASMUtils.getVarInsnList(holder.desc));
		instructions.add(new MethodInsnNode(INVOKESPECIAL, holder.owner,
				holder.name, holder.desc, false));
		instructions.add(ASMUtils.getReturnInsn(methodNode));
		methodNode.instructions = instructions;
		AnnotationNode defaultMethod = new AnnotationNode(
				ASMUtils.getDescriptor(DefaultMethod.class));
		List<AnnotationNode> annotations = new ArrayList<AnnotationNode>();
		annotations.add(defaultMethod);
		methodNode.visibleAnnotations = annotations;
		classNode.methods.add(methodNode);
	}

	/**
	 * Redefine the default equals in the version class. Inside the
	 * {@code equals} method, if the obj is an original type then the
	 * {@code equals} calls {@code creator} and calls
	 * {@code super.equals(obj.get_NewVersion())}, else returns {@code false}.
	 * 
	 * @param classNode
	 * @param holder
	 */
	@SuppressWarnings("unchecked")
	private void addEqualsMethodNodeImproved(ClassNode classNode,
			MethodHolder holder) {
		MethodNode methodNode = new MethodNode(holder.access, holder.name,
				holder.desc, holder.signature, holder.exceptions);
		InsnList instructions = new InsnList();
		Label l0 = new Label();
		instructions.add(new LabelNode(l0));
		instructions.add(new VarInsnNode(ALOAD, 1));
		instructions.add(new TypeInsnNode(INSTANCEOF, originalClassName));
		Label l1 = new Label();
		LabelNode ln1 = new LabelNode(l1);
		instructions.add(new JumpInsnNode(IFEQ, ln1));
		Label l2 = new Label();
		instructions.add(new LabelNode(l2));
		instructions.add(new VarInsnNode(ALOAD, 1));
		instructions.add(new TypeInsnNode(CHECKCAST, originalClassName));
		instructions.add(new VarInsnNode(ASTORE, 2));
		Label l3 = new Label();
		instructions.add(new LabelNode(l3));
		instructions.add(new VarInsnNode(ALOAD, 2));
		instructions.add(new MethodInsnNode(INVOKEVIRTUAL, originalClassName,
				"get_CurrentInstanceVersion", "()I", false));
		instructions.add(new FieldInsnNode(GETSTATIC, originalClassName,
				"_currentClassVersion", "I"));
		Label l4 = new Label();
		instructions.add(new JumpInsnNode(IF_ICMPEQ, new LabelNode(l4)));
		Label l5 = new Label();
		instructions.add(new LabelNode(l5));
		instructions.add(new VarInsnNode(ALOAD, 2));
		instructions.add(new MethodInsnNode(INVOKESTATIC, holder.owner,
				"_creator", "(L" + originalClassName + ";)V", false));
		instructions.add(new LabelNode(l4));
		instructions.add(new FrameNode(F_APPEND, 1,
				new Object[] { originalClassName }, 0, null));
		instructions.add(new VarInsnNode(ALOAD, 0));
		instructions.add(new VarInsnNode(ALOAD, 2));
		instructions.add(new MethodInsnNode(INVOKEVIRTUAL, originalClassName,
				"get_NewVersion", "()Ljava/lang/Object;", false));
		instructions.add(new MethodInsnNode(INVOKESPECIAL, holder.owner,
				"equals", "(Ljava/lang/Object;)Z", false));
		instructions.add(new InsnNode(IRETURN));
		instructions.add(ln1);
		instructions.add(new FrameNode(F_CHOP, 1, null, 0, null));
		instructions.add(new InsnNode(ICONST_0));
		instructions.add(new InsnNode(IRETURN));
		methodNode.instructions = instructions;
		List<AnnotationNode> annotations = new ArrayList<AnnotationNode>();
		AnnotationNode defaultMethod = new AnnotationNode(
				ASMUtils.getDescriptor(DefaultMethod.class));
		annotations.add(defaultMethod);
		methodNode.visibleAnnotations = annotations;
		classNode.methods.add(methodNode);
	}

	/**
	 * Adds helper methods in the class to provide access to the super class
	 * fields
	 * 
	 * @param classNode
	 *            The class node
	 */
	private void addFieldInvokers(ClassNode classNode) {
		String originalName = getOriginalClassName(classNode.name);
		for (FieldHolder holder : fields.values()) {
			if (isVersion) {
				addFieldGetterVersionImproved(classNode, holder, originalName);
				addFieldSetterVersionImproved(classNode, holder, originalName);
			} else {
				addFieldGetter(classNode, holder, originalName);
				addFieldSetter(classNode, holder, originalName);
			}
		}
	}

	/**
	 * Adds a getter for the field of the superclass
	 * 
	 * @param classNode
	 *            The class node
	 * @param holder
	 *            The field data
	 * @param originalName
	 *            The original class name
	 */
	@SuppressWarnings("unchecked")
	private void addFieldGetterVersionImproved(ClassNode classNode,
			FieldHolder holder, String originalName) {
		String getterDesc = "(L" + originalName + ";)" + holder.desc;
		MethodNode getter = new MethodNode(holder.access | Modifier.STATIC, "_"
				+ holder.name + "_fieldGetter", getterDesc, null, null);
		InsnList instructions = new InsnList();
		Label l0 = new Label();
		instructions.add(new LabelNode(l0));
		instructions.add(new VarInsnNode(ALOAD, 0));
		instructions.add(new MethodInsnNode(INVOKEVIRTUAL, originalName,
				"get_CurrentInstanceVersion", "()I", false));
		instructions.add(new FieldInsnNode(GETSTATIC, originalName,
				"_currentClassVersion", "I"));
		Label l1 = new Label();
		instructions.add(new JumpInsnNode(IF_ICMPEQ, new LabelNode(l1)));
		Label l2 = new Label();
		instructions.add(new LabelNode(l2));
		instructions.add(new VarInsnNode(ALOAD, 0));
		instructions.add(new MethodInsnNode(INVOKESTATIC, classNode.name,
				"_creator", "(L" + originalName + ";)V", false));
		instructions.add(new LabelNode(l1));
		instructions.add(new FrameNode(F_SAME, 0, null, 0, null));
		instructions.add(new VarInsnNode(ALOAD, 0));
		instructions.add(new MethodInsnNode(INVOKEVIRTUAL, originalName,
				"get_NewVersion", "()Ljava/lang/Object;", false));
		instructions.add(new TypeInsnNode(CHECKCAST, classNode.name));
		instructions.add(new FieldInsnNode(GETFIELD, holder.owner, holder.name,
				holder.desc));
		instructions.add(ASMUtils.getReturnInsn(getter));
		getter.instructions = instructions;
		AnnotationNode auxiliaryMethod = new AnnotationNode(
				ASMUtils.getDescriptor(AuxiliaryMethod.class));
		List<AnnotationNode> annotations = new ArrayList<AnnotationNode>();
		annotations.add(auxiliaryMethod);
		getter.visibleAnnotations = annotations;
		classNode.methods.add(getter);
	}

	/**
	 * Adds a setter for the field of the superclass
	 * 
	 * @param classNode
	 *            The class node
	 * @param holder
	 *            The field data
	 * @param originalName
	 *            The original class name
	 */
	@SuppressWarnings("unchecked")
	private void addFieldSetterVersionImproved(ClassNode classNode,
			FieldHolder holder, String originalName) {
		String setterDesc = "(L" + originalName + ";" + holder.desc + ")V";
		MethodNode setter = new MethodNode(holder.access | Modifier.STATIC, "_"
				+ holder.name + "_fieldSetter", setterDesc, null, null);
		InsnList instructions = new InsnList();
		Label l0 = new Label();
		instructions.add(new LabelNode(l0));
		instructions.add(new VarInsnNode(ALOAD, 0));
		instructions.add(new MethodInsnNode(INVOKEVIRTUAL, originalName,
				"get_CurrentInstanceVersion", "()I", false));
		instructions.add(new FieldInsnNode(GETSTATIC, originalName,
				"_currentClassVersion", "I"));
		Label l1 = new Label();
		instructions.add(new JumpInsnNode(IF_ICMPEQ, new LabelNode(l1)));
		Label l2 = new Label();
		instructions.add(new LabelNode(l2));
		instructions.add(new VarInsnNode(ALOAD, 0));
		instructions.add(new MethodInsnNode(INVOKESTATIC, classNode.name,
				"_creator", "(L" + originalName + ";)V", false));
		instructions.add(new LabelNode(l1));
		instructions.add(new FrameNode(F_SAME, 0, null, 0, null));
		instructions.add(new VarInsnNode(ALOAD, 0));
		instructions.add(new MethodInsnNode(INVOKEVIRTUAL, originalName,
				"get_NewVersion", "()Ljava/lang/Object;", false));
		instructions.add(new TypeInsnNode(CHECKCAST, classNode.name));
		instructions.add(ASMUtils.getVarInvokerInsnList(setter.desc));
		instructions.add(new FieldInsnNode(PUTFIELD, holder.owner, holder.name,
				holder.desc));
		instructions.add(ASMUtils.getReturnInsn(setter));
		setter.instructions = instructions;
		AnnotationNode auxiliaryMethod = new AnnotationNode(
				ASMUtils.getDescriptor(AuxiliaryMethod.class));
		List<AnnotationNode> annotations = new ArrayList<AnnotationNode>();
		annotations.add(auxiliaryMethod);
		setter.visibleAnnotations = annotations;
		classNode.methods.add(setter);
	}

	/**
	 * Adds a getter for the field of the superclass
	 * 
	 * @param classNode
	 *            The class node
	 * @param holder
	 *            The field data
	 * @param originalName
	 *            The original class name
	 */
	@SuppressWarnings("unchecked")
	private void addFieldGetter(ClassNode classNode, FieldHolder holder,
			String originalName) {
		MethodNode getter = new MethodNode(holder.access | Modifier.STATIC, "_"
				+ holder.name + "_fieldGetter", "(L" + originalName + ";)"
				+ holder.desc, null, null);
		InsnList instructions = new InsnList();
		instructions.add(new VarInsnNode(ALOAD, 0));
		instructions.add(new FieldInsnNode(GETFIELD, holder.owner, holder.name,
				holder.desc));
		instructions.add(ASMUtils.getReturnInsn(getter));
		getter.instructions = instructions;
		AnnotationNode auxiliaryMethod = new AnnotationNode(
				ASMUtils.getDescriptor(AuxiliaryMethod.class));
		List<AnnotationNode> annotations = new ArrayList<AnnotationNode>();
		annotations.add(auxiliaryMethod);
		getter.visibleAnnotations = annotations;
		classNode.methods.add(getter);

	}

	/**
	 * Adds a setter for the field of the super class
	 * 
	 * @param classNode
	 *            The class node
	 * @param holder
	 *            The field data
	 * @param originalName
	 *            The original class name
	 */
	@SuppressWarnings("unchecked")
	private void addFieldSetter(ClassNode classNode, FieldHolder holder,
			String originalName) {
		MethodNode setter = new MethodNode(holder.access | Modifier.STATIC, "_"
				+ holder.name + "_fieldSetter", "(L" + originalName + ";"
				+ holder.desc + ")V", null, null);
		InsnList instructions = new InsnList();
		instructions.add(new VarInsnNode(ALOAD, 0));
		instructions
				.add(new VarInsnNode(ASMUtils.getVarOpcode(holder.desc), 1));
		instructions.add(new FieldInsnNode(PUTFIELD, holder.owner, holder.name,
				holder.desc));
		instructions.add(ASMUtils.getReturnInsn(setter));
		setter.instructions = instructions;
		AnnotationNode auxiliaryMethod = new AnnotationNode(
				ASMUtils.getDescriptor(AuxiliaryMethod.class));
		List<AnnotationNode> annotations = new ArrayList<AnnotationNode>();
		annotations.add(auxiliaryMethod);
		setter.visibleAnnotations = annotations;
		classNode.methods.add(setter);

	}

	/**
	 * Add the default members to the class
	 * 
	 * @param classNode
	 *            The class node
	 */
	private void addMembers(ClassNode classNode) {
		addMethods(classNode);
		addFieldInvokers(classNode);
	}

	/**
	 * Adds invoker method to the class
	 * 
	 * @param classNode
	 *            The class node
	 * @param holder
	 *            The data of the method
	 */
	@SuppressWarnings("unchecked")
	private void addInvokerImproved(ClassNode classNode, MethodHolder holder) {
		String originalName = getOriginalClassName(classNode.name);
		String invokerDesc = holder.desc.replaceAll("\\(", "\\(L"
				+ originalName + ";");
		MethodNode methodNode = new MethodNode(holder.access | Modifier.STATIC,
				"_" + holder.name + "_invoker", invokerDesc, null,
				holder.exceptions);
		InsnList instructions = new InsnList();
		Label l0 = new Label();
		instructions.add(new LabelNode(l0));
		instructions.add(new VarInsnNode(ALOAD, 0));
		instructions.add(new MethodInsnNode(INVOKEVIRTUAL, originalName,
				"get_CurrentInstanceVersion", "()I", false));
		instructions.add(new FieldInsnNode(GETSTATIC, originalName,
				"_currentClassVersion", "I"));
		Label l1 = new Label();
		instructions.add(new JumpInsnNode(IF_ICMPEQ, new LabelNode(l1)));
		Label l2 = new Label();
		instructions.add(new LabelNode(l2));
		instructions.add(new VarInsnNode(ALOAD, 0));
		instructions.add(new MethodInsnNode(INVOKESTATIC, classNode.name,
				"_creator", "(L" + originalName + ";)V", false));
		instructions.add(new LabelNode(l1));
		instructions.add(new FrameNode(F_SAME, 0, null, 0, null));
		instructions.add(new VarInsnNode(ALOAD, 0));
		instructions.add(new MethodInsnNode(INVOKEVIRTUAL, originalName,
				"get_NewVersion", "()Ljava/lang/Object;", false));
		instructions.add(new TypeInsnNode(CHECKCAST, classNode.name));
		instructions.add(ASMUtils.getVarInvokerInsnList(invokerDesc));
		instructions.add(new MethodInsnNode(INVOKEVIRTUAL, classNode.name,
				holder.name, holder.desc, false));
		Label l3 = new Label();
		instructions.add(new LabelNode(l3));
		instructions.add(ASMUtils.getReturnInsn(methodNode));
		Label l4 = new Label();
		instructions.add(new LabelNode(l4));
		methodNode.instructions = instructions;
		AnnotationNode auxiliaryMethod = new AnnotationNode(
				ASMUtils.getDescriptor(AuxiliaryMethod.class));
		List<AnnotationNode> annotations = new ArrayList<AnnotationNode>();
		annotations.add(auxiliaryMethod);
		methodNode.visibleAnnotations = annotations;
		classNode.methods.add(methodNode);
	}

	/**
	 * Delete the class version ending to return the original class name
	 * 
	 * @param className
	 *            The class version name
	 * @return The original class name
	 */
	private String getOriginalClassName(String className) {
		String original = className.replaceAll("(_NewVersion_)(\\d+)", "");
		return original;
	}

	/**
	 * Calculates which methods of the superclasses do not exist in the actual
	 * class
	 * 
	 * @param classNode
	 */
	private void calculateDefaultMethods(ClassNode classNode) {
		for (Object node : classNode.methods) {
			MethodNode methodNode = (MethodNode) node;
			methods.remove((methodNode.name + methodNode.desc).hashCode());
		}
	}

	/**
	 * Calculates which fields of the superclasses do not exist in the actual
	 * class
	 * 
	 * @param classNode
	 */
	private void calculateDefaultFieldInvokers(ClassNode classNode) {
		for (Object node : classNode.fields) {
			FieldNode fieldNode = (FieldNode) node;
			fields.remove(fieldNode.name.hashCode());
		}
	}

	/**
	 * Calculates which members of the superclasses do not exist in the actual
	 * class
	 * 
	 * @param classNode
	 */
	private void calculateDefaults(ClassNode classNode) {
		calculateDefaultFieldInvokers(classNode);
		calculateDefaultMethods(classNode);
	}

	/**
	 * Obtains all methods in the superclasses
	 * 
	 * @param superClasses
	 */
	private void getSuperMethods(List<ClassNode> superClasses) {
		methods = new HashMap<Integer, MethodHolder>();
		for (ClassNode classNode : superClasses) {
			for (Object node : classNode.methods) {
				MethodNode m = (MethodNode) node;
				if (!Modifier.isFinal(m.access) && !Modifier.isStatic(m.access)
						&& !Modifier.isPrivate(m.access)
						&& !Modifier.isAbstract(m.access)
						&& !m.name.equals("<init>")
						&& !m.name.equals("<clinit>")) {
					MethodHolder holder = new MethodHolder(m.access
							& ~Modifier.NATIVE, m.name, m.desc, m.signature,
							exceptionsToArray(m.exceptions), classNode.name);
					methods.put((m.name + m.desc).hashCode(), holder);
				} else {
					methods.remove((m.name + m.desc).hashCode());
				}
			}
		}
	}

	/**
	 * Obtains all fields in the superclasses
	 * 
	 * @param superClasses
	 */
	private void getSuperFields(List<ClassNode> superClasses) {
		fields = new HashMap<Integer, FieldHolder>();
		for (ClassNode classNode : superClasses) {
			for (Object node : classNode.fields) {
				FieldNode f = (FieldNode) node;
				if (!Modifier.isPrivate(f.access)
						&& !Modifier.isStatic(f.access)
						&& !"_oldVersion".equals(f.name)) {
					FieldHolder holder = new FieldHolder(f.access, f.name,
							f.desc, classNode.name);
					fields.put(f.name.hashCode(), holder);
				} else {
					fields.remove(f.name.hashCode());
				}
			}
		}
	}

	/**
	 * Obtains all members in the superclasses
	 * 
	 * @param superClasses
	 */
	private void getSuperMembers(List<ClassNode> superClasses) {
		getSuperFields(superClasses);
		getSuperMethods(superClasses);
	}

	/**
	 * Obtains the {@link ClassNode} of the superclass and all superclasses
	 * 
	 * @param superName
	 *            The name of the superclass
	 * @return Collection of all superclasses
	 */
	private List<ClassNode> getSuperClasses(String superName) {
		List<ClassNode> superClasses = new ArrayList<ClassNode>();
		if (superName == null)
			return superClasses;
		try {
			ClassNode classNode = ASMUtils.getClassNode(superName);
			superClasses.addAll(getSuperClasses(classNode.superName));
			superClasses.add(classNode);
			return superClasses;
		} catch (IOException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	/**
	 * Transform a list of exceptions to String array
	 * 
	 * @param exceptions
	 *            Exception list
	 * @return {@code String[] with the exception names}
	 */
	@SuppressWarnings("rawtypes")
	private String[] exceptionsToArray(List exceptions) {
		String[] result = new String[exceptions.size()];
		for (int i = 0; i < result.length; i++) {
			result[i] = (String) exceptions.get(i);
		}
		return result;
	}

	private class MethodHolder {
		int access;
		String name;
		String desc;
		String signature;
		String[] exceptions;
		String owner;

		public MethodHolder(int access, String name, String desc,
				String signature, String[] exceptions, String owner) {
			super();
			this.access = access;
			this.name = name;
			this.desc = desc;
			this.signature = signature;
			this.exceptions = exceptions;
			this.owner = owner;
		}

	}

	private class FieldHolder {
		int access;
		String name;
		String desc;
		String owner;

		public FieldHolder(int access, String name, String desc, String owner) {
			super();
			this.access = access;
			this.name = name;
			this.desc = desc;
			this.owner = owner;
		}

	}

}
