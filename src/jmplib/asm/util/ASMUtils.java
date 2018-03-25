package jmplib.asm.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Modifier;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.util.ASMifier;
import org.objectweb.asm.util.CheckClassAdapter;
import org.objectweb.asm.util.TraceClassVisitor;

/**
 * Helper class to encapsulate the most common ASM operations
 * 
 * @author Ignacio Lagartos
 * 
 */
public class ASMUtils {

	/**
	 * This method transforms a {@link Type} to {@link Class} object.
	 * 
	 * @param type
	 *            The {@link Type} object to transform.
	 * @return The {@link Class} represented by the {@link Type} object or null
	 *         if the type doesn't represent a valid {@link Class}.
	 */
	public static Class<?> getClass(Type type) {
		String className = type.getClassName();
		try {
			if (!className.contains("["))
				return Class.forName(className);
			else
				return Class.forName(type.getDescriptor());
		} catch (ClassNotFoundException e) {
			switch (className) {
			case "byte":
				return byte.class;
			case "short":
				return short.class;
			case "int":
				return int.class;
			case "long":
				return long.class;
			case "char":
				return char.class;
			case "float":
				return float.class;
			case "double":
				return double.class;
			case "boolean":
				return boolean.class;
			case "void":
				return void.class;
			default:
				throw new IllegalArgumentException(
						"The type didn't represent a class");
			}
		}
	}

	/**
	 * This method obtain the {@link ClassNode} of the given class.
	 * 
	 * @param clazz
	 *            The class which {@link ClassNode} have to be returned.
	 * @return The {@link ClassNode} of the given class.
	 */
	public static ClassNode getClassNode(Class<?> clazz) {
		ClassNode classNode = new ClassNode();
		ClassReader reader = null;
		try {
			reader = new ClassReader(Type.getInternalName(clazz));
		} catch (IOException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
		reader.accept(classNode, 0);
		return classNode;
	}

	/**
	 * This method obtain the {@link ClassNode} of the given class bytes.
	 * 
	 * @param bytes
	 *            The class which {@link ClassNode} have to be returned.
	 * @return The {@link ClassNode} of the given class.
	 */
	public static ClassNode getClassNode(byte[] bytes) {
		ClassNode classNode = new ClassNode();
		ClassReader reader = null;
		reader = new ClassReader(bytes);
		reader.accept(classNode, 0);
		return classNode;
	}

	/**
	 * This method obtain the {@link ClassNode} from a {@link InputStream}.
	 * 
	 * @param is
	 *            The stream to read the bytes
	 * @return The {@link ClassNode} of the given class.
	 * @throws IOException
	 *             If there are problems with the stream
	 */
	public static ClassNode getClassNode(InputStream is) throws IOException {
		ClassNode classNode = new ClassNode();
		ClassReader reader = null;
		reader = new ClassReader(is);
		reader.accept(classNode, 0);
		return classNode;
	}

	/**
	 * This method obtain the {@link ClassNode} from the internal name of the
	 * class .
	 * 
	 * @param internalName
	 *            The internal name of the class
	 * @return The {@link ClassNode} of the given class.
	 * @throws IOException
	 *             If there are problems reading the class
	 */
	public static ClassNode getClassNode(String internalName)
			throws IOException {
		ClassNode classNode = new ClassNode();
		ClassReader reader = null;
		reader = new ClassReader(internalName);
		reader.accept(classNode, 0);
		return classNode;
	}

	/**
	 * Returns the ASMified code of the class
	 * 
	 * @param clazz
	 *            The class to ASMified
	 * @return The code to generate the class
	 */
	public static String getASMified(Class<?> clazz) {
		ClassNode classNode = getClassNode(clazz);
		ASMifier asMifier = new ASMifier();
		TraceClassVisitor visitor = new TraceClassVisitor(null, asMifier, null);
		classNode.accept(visitor);
		return asMifier.getText().toString();
	}

	/**
	 * Returns the ASMified code of the class
	 * 
	 * @param bytes
	 *            The bytes of the class to ASMified
	 * @return The code to generate the class
	 */
	public static String getASMified(byte[] bytes) {
		ClassNode classNode = getClassNode(bytes);
		ASMifier asMifier = new ASMifier();
		TraceClassVisitor visitor = new TraceClassVisitor(null, asMifier, null);
		classNode.accept(visitor);
		return asMifier.getText().toString();
	}

	/**
	 * Returns the ASMified code of the class
	 * 
	 * @param is
	 *            The {@link InputStream} to read the bytes of the class to
	 *            ASMified
	 * @return The code to generate the class
	 */
	public static String getASMified(InputStream is) throws IOException {
		ClassNode classNode = getClassNode(is);
		ASMifier asMifier = new ASMifier();
		TraceClassVisitor visitor = new TraceClassVisitor(null, asMifier, null);
		classNode.accept(visitor);
		return asMifier.getText().toString();
	}

	/**
	 * Return the bytecode of the class
	 * 
	 * @param clazz
	 *            The class to obtain the bytecode
	 * @return The bytecode of the class
	 */
	public static String byteCodeSpy(Class<?> clazz) {
		ClassNode classNode = getClassNode(clazz);
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		TraceClassVisitor visitor = new TraceClassVisitor(new PrintWriter(os));
		classNode.accept(visitor);
		return os.toString();
	}

	/**
	 * Return the bytecode of the class
	 * 
	 * @param clazz
	 *            The bytes of the class to obtain the bytecode
	 * @return The bytecode of the class
	 */
	public static String byteCodeSpy(byte[] bytes) {
		ClassNode classNode = getClassNode(bytes);
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		TraceClassVisitor visitor = new TraceClassVisitor(new PrintWriter(os));
		classNode.accept(visitor);
		return os.toString();
	}

	/**
	 * Return the bytecode of the class
	 * 
	 * @param clazz
	 *            The {@link InputStream} to read the bytes of the class to
	 *            obtain the bytecode
	 * @return The bytecode of the class
	 */
	public static String byteCodeSpy(InputStream is) throws IOException {
		ClassNode classNode = getClassNode(is);
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		TraceClassVisitor visitor = new TraceClassVisitor(new PrintWriter(os));
		classNode.accept(visitor);
		return os.toString();
	}

	/**
	 * Return the max stack size for a method
	 * 
	 * @param methodNode
	 *            Method to calculate max stack
	 * @return The max stack of the method
	 */
	public static int getMaxStack(MethodNode methodNode) {
		return Type.getArgumentsAndReturnSizes(methodNode.desc);
	}

	/**
	 * Return the argument types of the method
	 * 
	 * @param methodNode
	 *            The method node
	 * @return The argument types
	 */
	public static Type[] getArgumentTypes(MethodNode methodNode) {
		return Type.getArgumentTypes(methodNode.desc);
	}

	/**
	 * Create a {@link VarInsnNode} from a {@link Type} and the index of the
	 * parameter
	 * 
	 * @param type
	 *            Type of the var
	 * @param number
	 *            Index of the var
	 * @return The {@link VarInsnNode}
	 */
	public static VarInsnNode getParamInsn(Type type, int number) {
		VarInsnNode varNode = new VarInsnNode(
				getVarOpcode(type.getDescriptor()), number);
		return varNode;
	}

	/**
	 * Obtains a static {@link MethodInsnNode}
	 * 
	 * @param className
	 *            The internal name class that owns the method
	 * @param methodName
	 *            The name of the method
	 * @param descriptor
	 *            The descriptor of the method
	 * @return The node of the static call
	 */
	public static MethodInsnNode getMethodInsnStatic(String className,
			String methodName, String descriptor) {
		return new MethodInsnNode(Opcodes.INVOKESTATIC, className, methodName,
				descriptor, false);
	}

	/**
	 * Obtains the return node for one method
	 * 
	 * @param methodNode
	 *            The method node
	 * @return The return node
	 */
	public static InsnNode getReturnInsn(MethodNode methodNode) {
		InsnNode returnNode = new InsnNode(getReturnOpcode(methodNode.desc));
		return returnNode;
	}

	/**
	 * Checks of the method is static
	 * 
	 * @param methodNode
	 *            The method
	 * @return return if it is static
	 */
	public static boolean isStatic(MethodNode methodNode) {
		return Modifier.isStatic(methodNode.access);
	}

	/**
	 * Obtains the descriptor of the class
	 * 
	 * @param clazz
	 *            The class
	 * @return The descriptor of the class
	 */
	public static String getDescriptor(Class<?> clazz) {
		return Type.getDescriptor(clazz);
	}

	/**
	 * Obtains the internal name of the class
	 * 
	 * @param clazz
	 *            The class
	 * @return The internal name of the class
	 */
	public static String getInternalName(Class<?> clazz) {
		return clazz.getName().replace('.', '/');
	}

	/**
	 * Return the return opcode for one method descriptor
	 * 
	 * @param methodDesc
	 *            Method descriptor
	 * @return The return opcode
	 */
	public static int getReturnOpcode(String methodDesc) {
		switch (Type.getReturnType(methodDesc).getClassName()) {
		case "void":
			return Opcodes.RETURN;
		case "short":
		case "byte":
		case "boolean":
		case "char":
		case "int":
			return Opcodes.IRETURN;
		case "long":
			return Opcodes.LRETURN;
		case "float":
			return Opcodes.FRETURN;
		case "double":
			return Opcodes.DRETURN;
		default:
			return Opcodes.ARETURN;
		}
	}

	/**
	 * Obtains the var opcode for a descriptor
	 * 
	 * @param varDesc
	 *            Descriptor of the var
	 * @return The opcode
	 */
	public static int getVarOpcode(String varDesc) {
		switch (Type.getType(varDesc).getClassName()) {
		case "short":
		case "byte":
		case "boolean":
		case "char":
		case "int":
			return Opcodes.ILOAD;
		case "long":
			return Opcodes.LLOAD;
		case "float":
			return Opcodes.FLOAD;
		case "double":
			return Opcodes.DLOAD;
		default:
			return Opcodes.ALOAD;
		}
	}

	/**
	 * Obtains the {@link InsnList} with the load of all parameters of the
	 * method.
	 * 
	 * @param desc
	 *            Descriptor of the method
	 * @return The list with the {@link VarInsnNode} of the parameters
	 */
	public static InsnList getVarInsnList(String desc) {
		InsnList instructions = new InsnList();
		Type[] params = Type.getArgumentTypes(desc);
		int index = 1;
		for (Type type : params) {
			instructions.add(new VarInsnNode(ASMUtils.getVarOpcode(type
					.getDescriptor()), index));
			index = nextIndex(index, type.getDescriptor());
		}
		return instructions;
	}

	/**
	 * Obtains the {@link InsnList} with the load of the parameters of the
	 * method avoiding the first parameter.
	 * 
	 * @param desc
	 *            The descriptor of the method
	 * @return The list with the {@link VarInsnNode} of the parameters
	 */
	public static InsnList getVarInvokerInsnList(String desc) {
		InsnList instructions = new InsnList();
		Type[] params = Type.getArgumentTypes(desc);
		int index = 1;
		for (int i = 1; i < params.length; i++) {
			instructions.add(new VarInsnNode(ASMUtils.getVarOpcode(params[i]
					.getDescriptor()), index));
			index = nextIndex(index, params[i].getDescriptor());
		}
		return instructions;
	}

	/**
	 * Calculates the next index
	 * 
	 * @param index
	 *            Actual index
	 * @param desc
	 *            Descriptor of the actual variable
	 * @return The next index
	 */
	public static int nextIndex(int index, String desc) {
		if (desc.equals("D") || desc.equals("J")) {
			return index + 2;
		} else {
			return index + 1;
		}
	}

	/**
	 * Check if the class have errors
	 * 
	 * @param bytes
	 *            Bytes of the class
	 * @return Errores found
	 */
	public static String checkClass(byte[] bytes) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		CheckClassAdapter.verify(new ClassReader(bytes), false, pw);
		return sw.toString();
	}

	public static int getFirstAvailableIndex(String methodDesc, int index) {
		Type[] vars = Type.getArgumentTypes(methodDesc);
		for (Type type : vars) {
			index = nextIndex(index, type.getDescriptor());
		}
		return index;
	}

}
