package jmplib.primitives;

import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import jmplib.exceptions.StructuralIntercessionException;
import jmplib.primitives.impl.AddFieldPrimitive;
import jmplib.primitives.impl.AddMethodPrimitive;
import jmplib.primitives.impl.DeleteFieldPrimitive;
import jmplib.primitives.impl.DeleteMethodPrimitive;
import jmplib.primitives.impl.ReplaceFieldPrimitive;
import jmplib.primitives.impl.ReplaceImplementationPrimitive;
import jmplib.primitives.impl.ReplaceMethodPrimitive;
import jmplib.sourcecode.ClassContent;
import jmplib.sourcecode.SourceCodeCache;
import jmplib.util.MemberFinder;

/**
 * Factory for the creation of the intercession primitives
 * 
 * @author Ignacio Lagartos
 *
 */
public class PrimitiveFactory {

	private static SourceCodeCache sourceCodeCache = SourceCodeCache
			.getInstance();

	/**
	 * Creates {@link AddMethodPrimitive}
	 * 
	 * @param clazz
	 *            The class to modify
	 * @param name
	 *            The name of the method
	 * @param type
	 *            The type of the method
	 * @param paramNames
	 *            The parameter names
	 * @param body
	 *            The body of the method
	 * @param modifiers
	 *            The modifiers of the method
	 * @param exceptions
	 *            The exceptions of the method
	 * @return The {@link Primitive} ready to be executed
	 * @throws StructuralIntercessionException
	 */
	public static Primitive createAddMethodPrimitive(Class<?> clazz,
			String name, MethodType type, String[] paramNames, String body,
			int modifiers, Class<?>... exceptions)
			throws StructuralIntercessionException {
		ClassContent classContent = sourceCodeCache.getClassContent(clazz);
		if (paramNames.length != type.parameterCount()) {
			throw new IllegalArgumentException("The number of parameter"
					+ " names must match with the number of parameters");
		}
		Primitive primitive = new AddMethodPrimitive(classContent, name,
				type.returnType(), type.parameterArray(), exceptions,
				paramNames, body, modifiers);
		return primitive;
	}

	/**
	 * Creates {@link DeleteMethodPrimitive}
	 * 
	 * @param clazz
	 *            The class to modify
	 * @param name
	 *            The name of the method
	 * @return The {@link Primitive} ready to be executed
	 * @throws StructuralIntercessionException
	 */
	public static Primitive createDeleteMethodPrimitive(Class<?> clazz,
			String name) throws StructuralIntercessionException {
		ClassContent classContent = sourceCodeCache.getClassContent(clazz);
		Method m;
		try {
			m = MemberFinder.findMethod(clazz, name);
		} catch (NoSuchMethodException e) {
			throw new StructuralIntercessionException(e.getMessage(), e);
		}
		Primitive primitive = new DeleteMethodPrimitive(classContent, name,
				m.getReturnType(), m.getParameterTypes());
		return primitive;
	}

	/**
	 * Creates {@link DeleteMethodPrimitive}
	 * 
	 * @param clazz
	 *            The class to modify
	 * @param name
	 *            The name of the method
	 * @param type
	 *            The type of the method
	 * @return The {@link Primitive} ready to be executed
	 * @throws StructuralIntercessionException
	 */
	public static Primitive createDeleteMethodPrimitive(Class<?> clazz,
			String name, MethodType type)
			throws StructuralIntercessionException {
		ClassContent classContent = sourceCodeCache.getClassContent(clazz);
		Primitive primitive = new DeleteMethodPrimitive(classContent, name,
				type.returnType(), type.parameterArray());
		return primitive;
	}

	/**
	 * Creates {@link ReplaceImplementationPrimitive}
	 * 
	 * @param clazz
	 *            The class to modify
	 * @param name
	 *            The name of the method
	 * @param body
	 *            The new body
	 * @return The {@link Primitive} ready to be executed
	 * @throws StructuralIntercessionException
	 */
	public static Primitive createReplaceImplementation(Class<?> clazz,
			String name, String body) throws StructuralIntercessionException {
		ClassContent classContent = sourceCodeCache.getClassContent(clazz);
		Method m;
		try {
			m = MemberFinder.findMethod(clazz, name);
		} catch (NoSuchMethodException e) {
			throw new StructuralIntercessionException(e.getMessage(), e);
		}
		Primitive primitive = new ReplaceImplementationPrimitive(classContent,
				name, body, m.getReturnType(), m.getParameterTypes());
		return primitive;
	}

	/**
	 * Creates {@link ReplaceImplementationPrimitive}
	 * 
	 * @param clazz
	 *            The class to modify
	 * @param name
	 *            The name of the method
	 * @param type
	 *            The type of the method
	 * @param body
	 *            The new body
	 * @return The {@link Primitive} ready to be executed
	 * @throws StructuralIntercessionException
	 */
	public static Primitive createReplaceImplementation(Class<?> clazz,
			String name, MethodType type, String body)
			throws StructuralIntercessionException {
		ClassContent classContent = sourceCodeCache.getClassContent(clazz);
		Primitive primitive = new ReplaceImplementationPrimitive(classContent,
				name, body, type.returnType(), type.parameterArray());
		return primitive;
	}

	/**
	 * Creates {@link ReplaceMethodPrimitive}
	 * 
	 * @param clazz
	 *            The class to modify
	 * @param name
	 *            The name of the method
	 * @param newMethodType
	 *            The new method type
	 * @param body
	 *            The new body
	 * @return The {@link Primitive} ready to be executed
	 * @throws StructuralIntercessionException
	 */
	public static Primitive createReplaceMethodPrimitive(Class<?> clazz,
			String name, MethodType newMethodType, String body)
			throws StructuralIntercessionException {
		ClassContent classContent = sourceCodeCache.getClassContent(clazz);
		Method original;
		try {
			original = MemberFinder.findMethod(clazz, name);
		} catch (NoSuchMethodException e) {
			throw new StructuralIntercessionException(e.getMessage(), e);
		}
		Primitive primitive = new ReplaceMethodPrimitive(classContent, name,
				body, original.getReturnType(), original.getParameterTypes(),
				newMethodType.returnType(), newMethodType.parameterArray());
		return primitive;
	}

	/**
	 * Creates {@link ReplaceMethodPrimitive}
	 * 
	 * @param clazz
	 *            The class to modify
	 * @param name
	 *            The name of the method
	 * @param methodType
	 *            The method type
	 * @param newMethodType
	 *            The new method type
	 * @param body
	 *            The new body
	 * @return The {@link Primitive} ready to be executed
	 * @throws StructuralIntercessionException
	 */
	public static Primitive createReplaceMethodPrimitive(Class<?> clazz,
			String name, MethodType methodType, MethodType newMethodType,
			String body) throws StructuralIntercessionException {
		ClassContent classContent = sourceCodeCache.getClassContent(clazz);
		Primitive primitive = new ReplaceMethodPrimitive(classContent, name,
				body, methodType.returnType(), methodType.parameterArray(),
				newMethodType.returnType(), newMethodType.parameterArray());
		return primitive;
	}

	/**
	 * Creates {@link AddFieldPrimitive}
	 * 
	 * @param clazz
	 *            The class to modify
	 * @param modifiers
	 *            The modifiers of the field
	 * @param type
	 *            The type of the field
	 * @param name
	 *            The name of the field
	 * @param init
	 *            The initialization sequence of the field
	 * @return The {@link Primitive} ready to be executed
	 * @throws StructuralIntercessionException
	 */
	public static Primitive createAddFieldPrimitive(Class<?> clazz,
			int modifiers, Class<?> type, String name, String init)
			throws StructuralIntercessionException {
		ClassContent classContent = sourceCodeCache.getClassContent(clazz);
		Primitive primitive = new AddFieldPrimitive(classContent, modifiers,
				type, name, init);
		return primitive;
	}

	/**
	 * Creates {@link DeleteFieldPrimitive}
	 * 
	 * @param clazz
	 *            The class to modify
	 * @param name
	 *            The name of the field
	 * @return The {@link Primitive} ready to be executed
	 * @throws StructuralIntercessionException
	 */
	public static Primitive createDeleteFieldPrimitive(Class<?> clazz,
			String name) throws StructuralIntercessionException {
		ClassContent classContent = sourceCodeCache.getClassContent(clazz);
		Field f;
		try {
			f = MemberFinder.findField(clazz, name);
		} catch (NoSuchFieldException e) {
			String error = String.format("The field \"%s\" does not exist in the class %s", e.getMessage(),
					clazz.getName());
			throw new StructuralIntercessionException(error, e);
		}
		Primitive primitive = new DeleteFieldPrimitive(classContent, name, f.getType());
		return primitive;
	}

	/**
	 * Creates {@link ReplaceFieldPrimitive}
	 * 
	 * @param clazz
	 *            The class to modify
	 * @param name
	 *            The name of the field
	 * @param newType
	 *            The new type
	 * @param newInit
	 *            The new initialization sequence
	 * @return The {@link Primitive} ready to be executed
	 * @throws StructuralIntercessionException
	 */
	public static Primitive createReplaceFieldPrimitive(Class<?> clazz,
			String name, Class<?> newType, String newInit)
			throws StructuralIntercessionException {
		ClassContent classContent = sourceCodeCache.getClassContent(clazz);
		Primitive primitive = new ReplaceFieldPrimitive(classContent, name,
				newType, newInit);
		return primitive;
	}

}
