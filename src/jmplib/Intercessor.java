package jmplib;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;

import jmplib.classversions.VersionTables;
import jmplib.compiler.ClassCompiler;
import jmplib.compiler.PolyglotAdapter;
import jmplib.exceptions.CompilationFailedException;
import jmplib.exceptions.StructuralIntercessionException;
import jmplib.primitives.Primitive;
import jmplib.primitives.PrimitiveExecutor;
import jmplib.primitives.PrimitiveFactory;
import jmplib.util.ClassPathUtil;
import jmplib.util.MemberFinder;
import jmplib.util.WrapperClassGenerator;

/**
 * An intercessor of classes. This class is the main façade of JMPlib and
 * provides all the primitive support (add method, replace method, delete
 * method...) and the methods that allow to create invokers for the new members.
 * 
 * @author Ignacio Lagartos
 * 
 */
public class Intercessor {

	private static int INVOKER_COUNTER = 0;

	/**
	 * <p>
	 * Adds new method to the specified class.
	 * </p>
	 * <p>
	 * For example:
	 * </p>
	 * 
	 * <pre>
	 * <code>// Declaring MethodType
	 *  MethodType mt = MethodType.methodType(int.class, int.class);
	 *  
	 *  // Adding method to Counter
	 *  Intercessor.addMethod(Counter.class, "sum", mt, 
	 *  	"return this.counter += value;", "value"); </code>
	 * </pre>
	 * 
	 * <p>
	 * This call is equivalent to:
	 * </p>
	 * 
	 * <pre>
	 * <code>Intercessor.addMethod(Counter.class, "sum", mt, 
	 *  	"return this.counter += value;",
	 *  	Modifier.PUBLIC, "value"); </code>
	 * </pre>
	 * 
	 * @param clazz
	 *            The class to modify
	 * @param name
	 *            The name of the new method
	 * @param methodType
	 *            The type of the new method
	 * @param body
	 *            The code of the new method
	 * @param parameterNames
	 *            Optional parameter unless the method have parameters
	 * @throws IllegalArgumentException
	 *             If the parameters are wrong
	 * @throws StructuralIntercessionException
	 *             If a problem was encountered before finishing the adding
	 *             (e.g. compilation errors)
	 */
	public static void addMethod(Class<?> clazz, String name,
			MethodType methodType, String body, String... parameterNames)
			throws StructuralIntercessionException {
		addMethod(clazz, name, methodType, body, Modifier.PUBLIC,
				parameterNames);
	}

	/**
	 * <p>
	 * Adds new method to the specified class.
	 * </p>
	 * <p>
	 * For example:
	 * </p>
	 * 
	 * <pre>
	 * <code>// Declaring MethodType
	 *  MethodType mt = MethodType.methodType(int.class, int.class);
	 *  
	 *  // Adding method to Counter
	 *  Intercessor.addMethod(Counter.class, "sum", mt, 
	 *  	"return this.counter += value;",
	 *  	Modifier.PUBLIC, "value"); </code>
	 * </pre>
	 * 
	 * @param clazz
	 *            The class to modify
	 * @param name
	 *            The name of the new method
	 * @param methodType
	 *            The type of the new method
	 * @param body
	 *            The code of the new method
	 * @param modifiers
	 *            The modifiers of the new method
	 * @param parameterNames
	 *            Optional parameter unless the method have parameters
	 * @throws IllegalArgumentException
	 *             If the parameters are wrong
	 * @throws StructuralIntercessionException
	 *             If a problem was encountered before finishing the adding
	 *             (e.g. compilation errors)
	 */
	public static void addMethod(Class<?> clazz, String name,
			MethodType methodType, String body, int modifiers,
			String... parameterNames) throws StructuralIntercessionException {
		if (parameterNames == null) {
			parameterNames = new String[0];
		}
		try {
			// Checking params
			checkAddMethodParams(clazz, name, methodType, body, modifiers);
			// Creating the primitive
			Primitive primitive = PrimitiveFactory.createAddMethodPrimitive(
					clazz, name, methodType, parameterNames, body, modifiers);
			// Executing the primitive
			PrimitiveExecutor executor = new PrimitiveExecutor(primitive);
			executor.executePrimitives();
		} catch (StructuralIntercessionException e) {
			throw new StructuralIntercessionException(
					"addMethod could not be executed due to the following reasons: "
							+ e.getMessage(), e.getCause());
		}
	}

	/**
	 * <p>
	 * Adds new method to the specified class.
	 * </p>
	 * <p>
	 * For example:
	 * </p>
	 * 
	 * <pre>
	 * <code>// Declaring MethodType
	 *  MethodType mt = MethodType.methodType(int.class, int.class);
	 *  
	 *  Class<?>[] exceptions = new Class<?>[]{IllegalArgumentException.class}; 
	 *  
	 *  // Adding method to Counter
	 *  Intercessor.addMethod(Counter.class, "sum", mt, 
	 *  	"return this.counter += value;",
	 *  	Modifier.PUBLIC, exceptions,"value"); </code>
	 * </pre>
	 * 
	 * @param clazz
	 *            The class to modify
	 * @param name
	 *            The name of the new method
	 * @param methodType
	 *            The type of the new method
	 * @param body
	 *            The code of the new method
	 * @param modifiers
	 *            The modifiers of the new method
	 * @param exceptions
	 *            The exceptions that throws the new method
	 * @param parameterNames
	 *            Optional parameter unless the method have parameters
	 * @throws IllegalArgumentException
	 *             If the parameters are wrong
	 * @throws StructuralIntercessionException
	 *             If a problem was encountered before finishing the adding
	 *             (e.g. compilation errors)
	 */
	public static void addMethod(Class<?> clazz, String name,
			MethodType methodType, String body, int modifiers,
			Class<?>[] exceptions, String... parameterNames)
			throws StructuralIntercessionException {
		if (parameterNames == null) {
			parameterNames = new String[0];
		}
		try {
			// Checking params
			checkAddMethodParams(clazz, name, methodType, body, modifiers);
			// Creating the primitive
			Primitive primitive = PrimitiveFactory.createAddMethodPrimitive(
					clazz, name, methodType, parameterNames, body, modifiers,
					exceptions);
			// Executing the primitive
			PrimitiveExecutor executor = new PrimitiveExecutor(primitive);
			executor.executePrimitives();
		} catch (StructuralIntercessionException e) {
			throw new StructuralIntercessionException(
					"addMethod could not be executed due to the following reasons: "
							+ e.getMessage(), e.getCause());
		}
	}

	/**
	 * <p>
	 * Replaces the code of one method.
	 * </p>
	 * <p>
	 * For example, modifying the method to use a new field called lastResult:
	 * </p>
	 * 
	 * <pre>
	 * <code>// Moify a the method code
	 * Intercessor.replaceImplementation(Calculator.class, "sum", 
	 * 		"this.lastResult = a + b;" 
	 * 		+ "return this.lastResult;");</code>
	 * </pre>
	 * 
	 * @param clazz
	 *            The class to modify
	 * @param name
	 *            The name of the method to modify
	 * @param body
	 *            The new code
	 * @throws IllegalArgumentException
	 *             If the parameters are wrong
	 * @throws StructuralIntercessionException
	 *             If a problem was encountered before finishing or the method
	 *             is overload
	 */
	public static void replaceImplementation(Class<?> clazz, String name,
			String body) throws StructuralIntercessionException {
		try {
			// Check parameters
			checkReplaceImplementationParams(clazz, name, body);
			// Creating the primitive
			Primitive primitive = PrimitiveFactory.createReplaceImplementation(
					clazz, name, body);
			// Execute primitive
			PrimitiveExecutor executor = new PrimitiveExecutor(primitive);
			executor.executePrimitives();
		} catch (StructuralIntercessionException e) {
			throw new StructuralIntercessionException(
					"replaceImplementation could not be executed due to the following reasons: "
							+ e.getMessage(), e.getCause());
		}
	}

	/**
	 * <p>
	 * Replaces the code of one method. The MethodType allows to replace the
	 * correct method when the methods are overloaded.
	 * </p>
	 * <p>
	 * For example, modifying the method to use a new field called lastResult:
	 * </p>
	 * 
	 * <pre>
	 * <code>// Creating MethodType
	 * MethodType type = MethodType.methodType(double.class, double.class, double.class);
	 * 
	 * // Modifiying the method
	 * Intercessor.replaceImplementation(Calculator.class, "sum", type, 
	 * 		"this.lastResult = a + b;" 
	 * 		+ "return this.lastResult;");
	 * </pre>
	 * 
	 * @param clazz
	 *            The class to modify
	 * @param name
	 *            The name of the method to modify
	 * @param methodType
	 *            The type of the method
	 * @param body
	 *            The new code
	 * @throws IllegalArgumentException
	 *             If the parameters are wrong
	 * @throws StructuralIntercessionException
	 *             If a problem was encountered before finishing
	 */
	public static void replaceImplementation(Class<?> clazz, String name,
			MethodType methodType, String body)
			throws StructuralIntercessionException {
		try {
			// Check parameters
			checkReplaceImplementationParams(clazz, name, methodType, body);
			// Creating the primitive
			Primitive primitive = PrimitiveFactory.createReplaceImplementation(
					clazz, name, methodType, body);
			// Execute primitive
			PrimitiveExecutor executor = new PrimitiveExecutor(primitive);
			executor.executePrimitives();
		} catch (StructuralIntercessionException e) {
			throw new StructuralIntercessionException(
					"replaceImplementation could not be executed due to the following reasons: "
							+ e.getMessage(), e.getCause());
		}
	}

	/**
	 * Replaces the code and signature of one method.
	 * <p>
	 * For example, modifying the method to acept a more generic type (Dog ->
	 * Animal):
	 * </p>
	 * 
	 * <pre>
	 * <code>// Creating MethodType
	 * MethodType newType = MethodType.methodType(void.class, Pet.class);
	 * 
	 * // Modifying the method
	 * Intercessor.replaceMethod(Owner.class, "addPet", newType, 
	 * 		"this.pet = dog;");</code>
	 * </pre>
	 * 
	 * <p>
	 * <b>IMPORTANT:</b> the name of the parameters doesn't change.
	 * </p>
	 * 
	 * @param clazz
	 *            The class to modify
	 * @param name
	 *            The name of the method
	 * @param newMethodType
	 *            The new type
	 * @param body
	 *            The new code
	 * @throws IllegalArgumentException
	 *             If the parameters are wrong
	 * @throws StructuralIntercessionException
	 *             If a problem was encountered before finishing or the method
	 *             is overloaded
	 */
	public static void replaceMethod(Class<?> clazz, String name,
			MethodType newMethodType, String body)
			throws StructuralIntercessionException {
		try {
			// Check parameters
			checkReplaceMethodParams(clazz, name, newMethodType, body);
			// Creating the primitive
			Primitive primitive = PrimitiveFactory
					.createReplaceMethodPrimitive(clazz, name, newMethodType,
							body);
			// Execute primitive
			PrimitiveExecutor executor = new PrimitiveExecutor(primitive);
			executor.executePrimitives();
		} catch (StructuralIntercessionException e) {
			throw new StructuralIntercessionException(
					"replaceMethod could not be executed due to the following reasons: "
							+ e.getMessage(), e.getCause());
		}

	}

	/**
	 * Replaces the code and signature of one method. The MethodType allows to
	 * replace the correct method when the methods are overloaded.
	 * <p>
	 * For example, modifying the method to acept a more generic type (int ->
	 * double):
	 * </p>
	 * 
	 * <pre>
	 * <code>// Creating MethodType
	 * MethodType type = MethodType.methodType(int.class, int.class);
	 * 
	 * // Creating the new MethodType
	 * MethodType newType = MethodType.methodType(int.class, double.class);
	 * 
	 * // Modifying the method
	 * Intercessor.replaceMethod(Owner.class, "module10", type, newType, 
	 * 		"return number % 10;");</code>
	 * </pre>
	 * 
	 * <p>
	 * <b>IMPORTANT:</b> the name of the parameters doesn't change.
	 * </p>
	 * 
	 * @param clazz
	 *            The class to modify
	 * @param name
	 *            The name of the method
	 * @param methodType
	 *            The type of the method to modify
	 * @param newMethodType
	 *            The new type
	 * @param body
	 *            The new code
	 * @throws IllegalArgumentException
	 *             If the parameters are wrong
	 * @throws StructuralIntercessionException
	 *             If a problem was encountered before finishing
	 */
	public static void replaceMethod(Class<?> clazz, String name,
			MethodType methodType, MethodType newMethodType, String body)
			throws StructuralIntercessionException {
		try {
			// Check parameters
			checkReplaceMethodParams(clazz, name, methodType, newMethodType,
					body);
			// Creating the primitive
			Primitive primitive = PrimitiveFactory
					.createReplaceMethodPrimitive(clazz, name, methodType,
							newMethodType, body);
			// Execute primitive
			PrimitiveExecutor executor = new PrimitiveExecutor(primitive);
			executor.executePrimitives();
		} catch (StructuralIntercessionException e) {
			throw new StructuralIntercessionException(
					"replaceMethod could not be executed due to the following reasons: "
							+ e.getMessage(), e.getCause());
		}

	}

	/**
	 * <p>
	 * Deletes a method from the specified class.
	 * </p>
	 * <p>
	 * For example:
	 * </p>
	 * 
	 * <pre>
	 * <code>// Deleting method
	 * Intercessor.deleteMethod(Dog.class, "bark");
	 * 
	 * Dog dog = new Dog();
	 * dog.bark(); // Throws RuntimeException</code>
	 * </pre>
	 * 
	 * @param clazz
	 *            The class to modify
	 * @param name
	 *            The name of the method
	 * @throws IllegalArgumentException
	 *             If the parameters are wrong
	 * @throws StructuralIntercessionException
	 *             If a problem was encountered before finishing or the method
	 *             is overloaded
	 */
	public static void deleteMethod(Class<?> clazz, String name)
			throws StructuralIntercessionException {
		try {
			// Checking the params
			checkDeleteMethodParams(clazz, name);
			// Creating the primitive
			Primitive primitive = PrimitiveFactory.createDeleteMethodPrimitive(
					clazz, name);
			// Executing the primitive
			PrimitiveExecutor executor = new PrimitiveExecutor(primitive);
			executor.executePrimitives();
		} catch (StructuralIntercessionException e) {
			throw new StructuralIntercessionException(
					"deleteMethod could not be executed due to the following reasons: "
							+ e.getMessage(), e.getCause());
		}
	}

	/**
	 * <p>
	 * Deletes a method from the specified class. The MethodType allows to
	 * delete overloaded methods.
	 * </p>
	 * <p>
	 * For example:
	 * </p>
	 * 
	 * <pre>
	 * <code>// Creating MethodType
	 * MethodType type = MethodType.methodType(int.class, int.class, int.class);
	 * 
	 * // Deleting method
	 * Intercessor.deleteMethod(Calculator.class, "sum", type);
	 * 
	 * Calculator c = new Calculator();
	 * int sum = c.sum(1, 1); // Throws RuntimeException</code>
	 * </pre>
	 * 
	 * @param clazz
	 *            The class to modify
	 * @param name
	 *            The name of the method
	 * @throws IllegalArgumentException
	 *             If the parameters are wrong
	 * @throws StructuralIntercessionException
	 *             If a problem was encountered before finishing
	 */
	public static void deleteMethod(Class<?> clazz, String name,
			MethodType methodType) throws StructuralIntercessionException {
		try {
			// Checking the params
			checkDeleteMethodParams(clazz, name, methodType);
			// Creating the primitive
			Primitive primitive = PrimitiveFactory.createDeleteMethodPrimitive(
					clazz, name, methodType);
			// Executing the primitive
			PrimitiveExecutor executor = new PrimitiveExecutor(primitive);
			executor.executePrimitives();
		} catch (StructuralIntercessionException e) {
			throw new StructuralIntercessionException(
					"deleteMethod could not be executed due to the following reasons: "
							+ e.getMessage(), e.getCause());
		}
	}

	/**
	 * <p>
	 * Adds a new field to the specified class
	 * </p>
	 * <p>
	 * For example:
	 * </p>
	 * 
	 * <pre>
	 * <code>Intercessor.addField(Person.class, String.class, "lastName");</code>
	 * </pre>
	 * <p>
	 * This call is equivalent to:
	 * </p>
	 * 
	 * <pre>
	 * <code>Intercessor.addField(Person.class, <b>0</b>, String.class, 
	 * 		"lastName", <b>null</b>);</code>
	 * </pre>
	 * 
	 * @param clazz
	 *            The class to modify
	 * @param type
	 *            The type of the new field
	 * @param name
	 *            The name of the new field
	 * @throws IllegalArgumentException
	 *             If the parameters are wrong
	 * @throws StructuralIntercessionException
	 *             If a problem was encountered before finishing
	 */
	public static void addField(Class<?> clazz, Class<?> type, String name)
			throws StructuralIntercessionException {
		addField(clazz, 0, type, name, null);
	}

	/**
	 * <p>
	 * Adds a new field to the specified class
	 * </p>
	 * <p>
	 * For example:
	 * </p>
	 * 
	 * <pre>
	 * <code>Intercessor.addField(Person.class, Modifier.PRIVATE, String.class, 
	 * 		"lastName");</code>
	 * </pre>
	 * <p>
	 * This call is equivalent to:
	 * </p>
	 * 
	 * <pre>
	 * <code>Intercessor.addField(Person.class, Modifier.PRIVATE, String.class, 
	 * 		"lastName", <b>null</b>);</code>
	 * </pre>
	 * 
	 * @param clazz
	 *            The class to modify
	 * @param modifiers
	 *            The modifiers of the field
	 * @param type
	 *            The type of the new field
	 * @param name
	 *            The name of the new field
	 * @throws IllegalArgumentException
	 *             If the parameters are wrong
	 * @throws StructuralIntercessionException
	 *             If a problem was encountered before finishing
	 */
	public static void addField(Class<?> clazz, int modifiers, Class<?> type,
			String name) throws StructuralIntercessionException {
		addField(clazz, modifiers, type, name, null);
	}

	/**
	 * <p>
	 * Adds a new field to the specified class
	 * </p>
	 * <p>
	 * For example:
	 * </p>
	 * 
	 * <pre>
	 * <code>Intercessor.addField(Person.class, Modifier.PRIVATE, String.class, 
	 * 		"lastName", "Doe");</code>
	 * </pre>
	 * 
	 * @param clazz
	 *            The class to modify
	 * @param modifiers
	 *            The modifiers of the field
	 * @param type
	 *            The type of the new field
	 * @param name
	 *            The name of the new field
	 * @param init
	 *            The initialization sequence
	 * @throws IllegalArgumentException
	 *             If the parameters are wrong
	 * @throws StructuralIntercessionException
	 *             If a problem was encountered before finishing
	 */
	public static void addField(Class<?> clazz, int modifiers, Class<?> type,
			String name, String init) throws StructuralIntercessionException {
		try {
			// Checking parameters
			checkAddFieldParams(clazz, modifiers, type, name, init);
			// Creating the primitive
			Primitive primitive = PrimitiveFactory.createAddFieldPrimitive(
					clazz, modifiers, type, name, init);
			// Executing the primitive
			PrimitiveExecutor executor = new PrimitiveExecutor(primitive);
			executor.executePrimitives();
		} catch (StructuralIntercessionException e) {
			throw new StructuralIntercessionException(
					"addField could not be executed due to the following reasons: "
							+ e.getMessage(), e.getCause());
		}
	}

	/**
	 * <p>
	 * Deletes one field from the specified class.
	 * </p>
	 * <p>
	 * For example:
	 * </p>
	 * 
	 * <pre>
	 * <code>Intercessor.deleteField(Person.class, "lastName");</code>
	 * </pre>
	 * 
	 * @param clazz
	 *            The class to modfy
	 * @param name
	 *            The name of the field
	 * @throws IllegalArgumentException
	 *             If the parameters are wrong
	 * @throws StructuralIntercessionException
	 *             If a problem was encountered before finishing or the field is
	 *             in use
	 */
	public static void deleteField(Class<?> clazz, String name)
			throws StructuralIntercessionException {
		try {
			// Checking parameters
			checkDeleteFieldParams(clazz, name);
			// Creating the primitive
			Primitive primitive = PrimitiveFactory.createDeleteFieldPrimitive(
					clazz, name);
			// Executing the primitive
			PrimitiveExecutor executor = new PrimitiveExecutor(primitive);
			executor.executePrimitives();
		} catch (StructuralIntercessionException e) {
			throw new StructuralIntercessionException(
					"deleteField could not be executed due to the following reasons: "
							+ e.getMessage(), e.getCause());
		}
	}

	/**
	 * <p>
	 * Replaces the type of one field.
	 * </p>
	 * <p>
	 * For example:
	 * </p>
	 * 
	 * <pre>
	 * <code>Intercessor.replaceField(Calculator.class, "lastResult", double.class);</code>
	 * </pre>
	 * 
	 * @param clazz
	 *            The class to modify
	 * @param name
	 *            The name of the field
	 * @param newType
	 *            The new type of the field
	 * @throws IllegalArgumentException
	 *             If the parameters are wrong
	 * @throws StructuralIntercessionException
	 *             If a problem was encountered before finishing
	 */
	public static void replaceField(Class<?> clazz, String name,
			Class<?> newType) throws StructuralIntercessionException {
		replaceField(clazz, name, newType, null);
	}

	/**
	 * <p>
	 * Replaces the type and the initialization sequence of one field.
	 * </p>
	 * <p>
	 * For example:
	 * </p>
	 * 
	 * <pre>
	 * <code>Intercessor.replaceField(Calculator.class, "lastResult", double.class, "0.0");</code>
	 * </pre>
	 * 
	 * @param clazz
	 *            The class to modify
	 * @param name
	 *            The name of the field
	 * @param newType
	 *            The new type of the field
	 * @param newInit
	 *            The new initialization sequence
	 * @throws IllegalArgumentException
	 *             If the parameters are wrong
	 * @throws StructuralIntercessionException
	 *             If a problem was encountered before finishing
	 */
	public static void replaceField(Class<?> clazz, String name,
			Class<?> newType, String newInit)
			throws StructuralIntercessionException {
		try {
			// Checking parameters
			checkReplaceFieldParams(clazz, name, newType);
			// Creating the primitive
			Primitive primitive = PrimitiveFactory.createReplaceFieldPrimitive(
					clazz, name, newType, newInit);
			// Executing the primitive
			PrimitiveExecutor executor = new PrimitiveExecutor(primitive);
			executor.executePrimitives();
		} catch (StructuralIntercessionException e) {
			throw new StructuralIntercessionException(
					"replaceField could not be executed due to the following reasons: "
							+ e.getMessage(), e.getCause());
		}
	}

	/**
	 * <p>
	 * Generates an instance of the specified interface to invoke one instance
	 * method. The first parameter of the interface must be the class owner
	 * type, meaning, the class where the method is. This parameter is going to
	 * be the instance over the invokations are called.
	 * </p>
	 * 
	 * @param clazz
	 *            The class where the method is
	 * @param name
	 *            The name of the method
	 * @param functionalInterface
	 *            The functional interface
	 * @param parametrizationClasses
	 *            The parametrization classes if the interface is a generic
	 *            interface
	 * @return The populated interface with the invoker
	 * @throws IllegalArgumentException
	 *             If the parameters are wrong
	 * @throws StructuralIntercessionException
	 *             If a problem was encountered before finishing
	 */
	public static <T> T getInvoker(Class<?> clazz, String name,
			Class<T> functionalInterface, Class<?>... parametrizationClasses)
			throws StructuralIntercessionException {
		return getInvoker(clazz, name, functionalInterface, 0,
				parametrizationClasses);
	}

	/**
	 * <p>
	 * Generates an instance of the specified interface to invoke one static
	 * method.
	 * </p>
	 * 
	 * @param clazz
	 *            The class where the method is
	 * @param name
	 *            The name of the method
	 * @param functionalInterface
	 *            The functional interface
	 * @param parametrizationClasses
	 *            The parametrization classes if the interface is a generic
	 *            interface
	 * @return The populated interface with the invoker
	 * @throws IllegalArgumentException
	 *             If the parameters are wrong
	 * @throws StructuralIntercessionException
	 *             If a problem was encountered before finishing
	 */
	public static <T> T getStaticInvoker(Class<?> clazz, String name,
			Class<T> functionalInterface, Class<?>... parametrizationClasses)
			throws StructuralIntercessionException {
		return getInvoker(clazz, name, functionalInterface, Modifier.STATIC,
				parametrizationClasses);
	}

	/**
	 * <p>
	 * Generates an instance of the specified interface to invoke one method. If
	 * it is an instance method, the first parameter of the interface must be
	 * the class owner type, meaning, the class where the method is. This
	 * parameter is going to be the instance over the invokations are called.
	 * </p>
	 * 
	 * @param clazz
	 *            The class where the method is
	 * @param name
	 *            The name of the method
	 * @param functionalInterface
	 *            The functional interface
	 * @param modifiers
	 *            Indicates if the method is static
	 * @param parametrizationClasses
	 *            The parametrization classes if the interface is a generic
	 *            interface
	 * @return The populated interface with the invoker
	 * @throws IllegalArgumentException
	 *             If the parameters are wrong
	 * @throws StructuralIntercessionException
	 *             If a problem was encountered before finishing
	 */
	public static <T> T getInvoker(Class<?> clazz, String name,
			Class<T> functionalInterface, int modifiers,
			Class<?>... parametrizationClasses)
			throws StructuralIntercessionException {
		if (parametrizationClasses == null)
			parametrizationClasses = new Class[0];
		// Checking params
		checkGetInvokerParams(clazz, name, functionalInterface, modifiers);
		try {
			checkVisibility(clazz, name, functionalInterface, modifiers,
					parametrizationClasses);
		} catch (NoSuchMethodException e) {
			throw new StructuralIntercessionException(
					"getInvoker could not be executed due to the following reasons: "
							+ e.getMessage(), e);
		}
		boolean isStatic = Modifier.isStatic(modifiers);
		String className = "Generated_Invoker_Class_" + ++INVOKER_COUNTER;
		File file = null;
		try {
			file = WrapperClassGenerator.generateMethodInvoker(className,
					clazz, name, functionalInterface, parametrizationClasses,
					isStatic);
		} catch (StructuralIntercessionException e) {
			throw new StructuralIntercessionException(
					"getInvoker could not be executed due to the following reasons: "
							+ e.getMessage(), e.getCause());
		}
		T invoker = null;
		try {
			invoker = compileFile(file, className,
					WrapperClassGenerator.GENERATED_INVOKER_PACKAGE);
		} catch (CompilationFailedException e) {
			throw new StructuralIntercessionException(
					"getInvoker could not be executed due to the following reasons: "
							+ e.getMessage(), e);
		}
		return invoker;
	}

	/**
	 * <p>
	 * Generates an instance of the specified interface to provide one instance
	 * attribute getter. The first parameter of the interface must be the class
	 * owner type, meaning, the class where the attribute is. This parameter is
	 * going to be the instance where the attribute is requested.
	 * </p>
	 * 
	 * @param clazz
	 *            The class where the attribute is
	 * @param name
	 *            The name of the attribute
	 * @param functionalInterface
	 *            The functional interface
	 * @param parametrizationClasses
	 *            The parametrization classes if the interface is a generic
	 *            interface
	 * @return The populated interface with the getter
	 * @throws IllegalArgumentException
	 *             If the parameters are wrong
	 * @throws StructuralIntercessionException
	 *             If a problem was encountered before finishing
	 */
	public static <T> T getAttributeInvoker(Class<?> clazz, String name,
			Class<T> attributeInterface, Class<?>... parametrizationClasses)
			throws StructuralIntercessionException {
		return getAttributeInvoker(clazz, name, attributeInterface, 0,
				parametrizationClasses);
	}

	/**
	 * <p>
	 * Generates an instance of the specified interface to provide one static
	 * attribute getter.
	 * </p>
	 * 
	 * @param clazz
	 *            The class where the attribute is
	 * @param name
	 *            The name of the attribute
	 * @param functionalInterface
	 *            The functional interface
	 * @param parametrizationClasses
	 *            The parametrization classes if the interface is a generic
	 *            interface
	 * @return The populated interface with the getter
	 * @throws IllegalArgumentException
	 *             If the parameters are wrong
	 * @throws StructuralIntercessionException
	 *             If a problem was encountered before finishing
	 */
	public static <T> T getStaticAttributeInvoker(Class<?> clazz, String name,
			Class<T> attributeInterface, Class<?>... parametrizationClasses)
			throws StructuralIntercessionException {
		return getAttributeInvoker(clazz, name, attributeInterface,
				Modifier.STATIC, parametrizationClasses);
	}

	/**
	 * <p>
	 * Generates an instance of the specified interface to provide one static
	 * attribute getter. If it is an instance attribute, the first parameter of
	 * the interface must be the class owner type, meaning, the class where the
	 * attribute is. This parameter is going to be the instance where the
	 * attribute is requested.
	 * </p>
	 * 
	 * @param clazz
	 *            The class where the attribute is
	 * @param name
	 *            The name of the attribute
	 * @param functionalInterface
	 *            The functional interface
	 * @param parametrizationClasses
	 *            The parametrization classes if the interface is a generic
	 *            interface
	 * @return The populated interface with the getter
	 * @throws IllegalArgumentException
	 *             If the parameters are wrong
	 * @throws StructuralIntercessionException
	 *             If a problem was encountered before finishing
	 */
	public static <T> T getAttributeInvoker(Class<?> clazz, String name,
			Class<T> functionalInterface, int modifiers,
			Class<?>... parametrizationClasses)
			throws StructuralIntercessionException {
		if (parametrizationClasses == null)
			parametrizationClasses = new Class[0];
		// Checking params
		checkGetInvokerParams(clazz, name, functionalInterface, modifiers);
		try {
			checkVisibility(clazz, name);
		} catch (NoSuchFieldException e) {
			throw new StructuralIntercessionException(
					"getAttributeInvoker could not be executed due to the following reasons: "
							+ e.getMessage(), e);
		}
		boolean isStatic = Modifier.isStatic(modifiers);
		String className = "Generated_Invoker_Class_" + ++INVOKER_COUNTER;
		File file = null;
		try {
			file = WrapperClassGenerator
					.generateFieldGetter(className, clazz, name,
							functionalInterface, parametrizationClasses,
							isStatic);
		} catch (StructuralIntercessionException e) {
			throw new StructuralIntercessionException(
					"getAttributeInvoker could not be executed due to the following reasons: "
							+ e.getMessage(), e.getCause());
		}
		T invoker = null;
		try {
			invoker = compileFile(file, className,
					WrapperClassGenerator.GENERATED_INVOKER_PACKAGE);
		} catch (CompilationFailedException e) {
			throw new StructuralIntercessionException(
					"getAttributeInvoker could not be executed due to the following reasons: "
							+ e.getMessage(), e);
		}
		return invoker;
	}

	/**
	 * <p>
	 * Generates an instance of the specified interface to provide one instance
	 * attribute setter. The first parameter of the interface must be the class
	 * owner type, meaning, the class where the attribute is. This parameter is
	 * going to be the instance where the attribute is modified.
	 * </p>
	 * 
	 * @param clazz
	 *            The class where the attribute is
	 * @param name
	 *            The name of the attribute
	 * @param functionalInterface
	 *            The functional interface
	 * @param parametrizationClasses
	 *            The parametrization classes if the interface is a generic
	 *            interface
	 * @return The populated interface with the setter
	 * @throws IllegalArgumentException
	 *             If the parameters are wrong
	 * @throws StructuralIntercessionException
	 *             If a problem was encountered before finishing
	 */
	public static <T> T setAttributeInvoker(Class<?> clazz, String name,
			Class<T> attributeInterface, Class<?>... parametrizationClasses)
			throws StructuralIntercessionException {
		return setAttributeInvoker(clazz, name, attributeInterface, 0,
				parametrizationClasses);
	}

	/**
	 * <p>
	 * Generates an instance of the specified interface to provide one static
	 * attribute setter.
	 * </p>
	 * 
	 * @param clazz
	 *            The class where the attribute is
	 * @param name
	 *            The name of the attribute
	 * @param functionalInterface
	 *            The functional interface
	 * @param parametrizationClasses
	 *            The parametrization classes if the interface is a generic
	 *            interface
	 * @return The populated interface with the setter
	 * @throws IllegalArgumentException
	 *             If the parameters are wrong
	 * @throws StructuralIntercessionException
	 *             If a problem was encountered before finishing
	 */
	public static <T> T setStaticAttributeInvoker(Class<?> clazz, String name,
			Class<T> attributeInterface, Class<?>... parametrizationClasses)
			throws StructuralIntercessionException {
		return setAttributeInvoker(clazz, name, attributeInterface, 0,
				parametrizationClasses);
	}

	/**
	 * <p>
	 * Generates an instance of the specified interface to provide one static
	 * attribute setter. If it is an instance attribute, the first parameter of
	 * the interface must be the class owner type, meaning, the class where the
	 * attribute is. This parameter is going to be the instance where the
	 * attribute is requested.
	 * </p>
	 * 
	 * @param clazz
	 *            The class where the attribute is
	 * @param name
	 *            The name of the attribute
	 * @param functionalInterface
	 *            The functional interface
	 * @param parametrizationClasses
	 *            The parametrization classes if the interface is a generic
	 *            interface
	 * @return The populated interface with the setter
	 * @throws IllegalArgumentException
	 *             If the parameters are wrong
	 * @throws StructuralIntercessionException
	 *             If a problem was encountered before finishing
	 */
	public static <T> T setAttributeInvoker(Class<?> clazz, String name,
			Class<T> functionalInterface, int modifiers,
			Class<?>... parametrizationClasses)
			throws StructuralIntercessionException {
		if (parametrizationClasses == null)
			parametrizationClasses = new Class[0];
		// Checking params
		checkGetInvokerParams(clazz, name, functionalInterface, modifiers);
		try {
			checkVisibility(clazz, name);
		} catch (NoSuchFieldException e) {
			throw new StructuralIntercessionException(
					"setAttributeInvoker could not be executed due to the following reasons: "
							+ e.getMessage(), e);
		}
		boolean isStatic = Modifier.isStatic(modifiers);
		String className = "Generated_Invoker_Class_" + ++INVOKER_COUNTER;
		File file = null;
		try {
			file = WrapperClassGenerator
					.generateFieldSetter(className, clazz, name,
							functionalInterface, parametrizationClasses,
							isStatic);
		} catch (StructuralIntercessionException e) {
			throw new StructuralIntercessionException(
					"setAttributeInvoker could not be executed due to the following reasons: "
							+ e.getMessage(), e.getCause());
		}
		T invoker = null;
		try {
			invoker = compileFile(file, className,
					WrapperClassGenerator.GENERATED_INVOKER_PACKAGE);
		} catch (CompilationFailedException e) {
			throw new StructuralIntercessionException(
					"setAttributeInvoker could not be executed due to the following reasons: "
							+ e.getMessage(), e);
		}
		return invoker;
	}

	/**
	 * Compiles a class file and return an instance of the compiled class. Each
	 * class compiled is instrumented by Polyglot. The name and the package of
	 * the class are needed to load the {@link Class} instance.
	 * 
	 * @param file
	 *            The file of the class
	 * @param name
	 *            The name of the class
	 * @param packageName
	 *            The package of the class
	 * @return The instance of the compiled class
	 * @throws CompilationFailedException
	 *             If the class have compilation errors
	 * @throws StructuralIntercessionException
	 *             If some errors ocurrs
	 * @throws RuntimeException
	 *             If there are errors accesing the files, obtaining the class
	 *             or creating the instance
	 */
	@SuppressWarnings("unchecked")
	private static <T> T compileFile(File file, String name, String packageName)
			throws CompilationFailedException, StructuralIntercessionException {
		try {
			ClassCompiler.getInstance().compile(
					ClassPathUtil.getApplicationClassPath(),
					PolyglotAdapter.instrument(file));
		} catch (IOException e) {
			throw new RuntimeException("Errors compiling the code: "
					+ e.getMessage(), e);
		}
		Class<?> invokerClass;
		try {
			invokerClass = Class.forName(packageName + "." + name);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException("Errors obtaining the class: "
					+ e.getMessage(), e);
		}
		T invoker;
		try {
			invoker = (T) invokerClass.newInstance();
		} catch (InstantiationException e) {
			throw new RuntimeException("Errors instantiating the class: "
					+ e.getMessage(), e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException("Errors accesing the class: "
					+ e.getMessage(), e);
		}
		return invoker;
	}

	/**
	 * Checks if any method matches the provided functional interface method in
	 * the last version of the class.
	 * 
	 * @param clazz
	 *            The original class
	 * @param name
	 *            The name of the method
	 * @param methodInterface
	 *            The functional interface
	 * @param modifiers
	 *            The modifiers of the method
	 * @param parametrizationClasses
	 *            The classes that parametrize the interface
	 * @throws NoSuchMethodException
	 *             If no method matches the functional interface method in the
	 *             last version of the class
	 */
	private static void checkVisibility(Class<?> clazz, String name,
			Class<?> methodInterface, int modifiers,
			Class<?>[] parametrizationClasses) throws NoSuchMethodException {
		Class<?> lastVersion = VersionTables.getNewVersion(clazz);
		Method m = null, interfaceMethod = MemberFinder
				.getMethod(methodInterface);
		Class<?>[] parameters = interfaceMethod.getParameterTypes();
		parameters = MemberFinder.resolveGenericParametersToClass(
				interfaceMethod, parametrizationClasses);
		if (!Modifier.isStatic(modifiers)) {
			parameters = Arrays.copyOfRange(parameters, 1, parameters.length);
		}
		try {
			m = lastVersion.getMethod(name, parameters);
		} catch (NoSuchMethodException e) {
			throw new NoSuchMethodException("The method "
					+ name
					+ " "
					+ MethodType.methodType(interfaceMethod.getReturnType(),
							parameters).toString()
					+ " does not exist in the class " + clazz.getName());
		}
		if (!Modifier.isPublic(m.getModifiers())) {
			throw new IllegalArgumentException("The method " + m.toString()
					+ " is not visible");
		}
	}

	/**
	 * Checks if there is any attribute in the last version of the class that
	 * matches with the data provided and its visibility is public.
	 * 
	 * @param clazz
	 *            The original class
	 * @param name
	 *            The name of the field
	 * @throws NoSuchFieldException
	 *             If the field isn't public or doesn't exist
	 */
	private static void checkVisibility(Class<?> clazz, String name)
			throws NoSuchFieldException {
		Class<?> lastVersion = VersionTables.getNewVersion(clazz);
		Field f = null;
		try {
			f = lastVersion.getField(name);
		} catch (NoSuchFieldException e) {
			throw new NoSuchFieldException("The field " + name
					+ " does not exist in the class " + clazz.getName());
		}
		if (!Modifier.isPublic(f.getModifiers())) {
			throw new IllegalArgumentException("The field " + f.toString()
					+ " is not visible");
		}
	}

	/**
	 * Check parameters
	 */
	private static void checkDeleteFieldParams(Class<?> clazz, String name) {
		if (clazz == null)
			throw new IllegalArgumentException(
					"The class parameter cannot be null");
		if (name == null)
			throw new IllegalArgumentException(
					"The name parameter cannot be null");
		if ("".equals(name))
			throw new IllegalArgumentException(
					"The name parameter cannot be empty");
	}

	/**
	 * Check parameters
	 */
	private static void checkReplaceFieldParams(Class<?> clazz, String name,
			Class<?> newFieldClass) {
		if (clazz == null)
			throw new IllegalArgumentException(
					"The class parameter cannot be null");
		if (name == null)
			throw new IllegalArgumentException(
					"The name parameter cannot be null");
		if ("".equals(name))
			throw new IllegalArgumentException(
					"The name parameter cannot be empty");
		if (newFieldClass == null)
			throw new IllegalArgumentException(
					"The newFieldClass parameter cannot be null");
	}

	/**
	 * Check parameters
	 */
	private static void checkAddFieldParams(Class<?> clazz, int modifiers,
			Class<?> type, String name, String init) {
		if (clazz == null)
			throw new IllegalArgumentException(
					"The class parameter cannot be null");
		if (type == null)
			throw new IllegalArgumentException(
					"The type parameter cannot be null");
		if (name == null)
			throw new IllegalArgumentException(
					"The name parameter cannot be null");
		if (init != null && init == "")
			throw new IllegalArgumentException(
					"The init parameter cannot be empty");
		if ("".equals(name))
			throw new IllegalArgumentException(
					"The name parameter cannot be empty");
		int mask = Modifier.fieldModifiers();
		if ((modifiers | mask) != mask)
			throw new IllegalArgumentException(
					"The modifier combination is incorrect for a field");
	}

	/**
	 * Check parameters
	 */
	private static void checkAddMethodParams(Class<?> clazz, String name,
			MethodType methodType, String body, int modifiers)
			throws IllegalArgumentException {
		if (clazz == null)
			throw new IllegalArgumentException(
					"The class parameter cannot be null");
		if (name == null)
			throw new IllegalArgumentException(
					"The name parameter cannot be null");
		if (name.length() == 0)
			throw new IllegalArgumentException(
					"The name parameter cannot be empty");
		if (methodType == null)
			throw new IllegalArgumentException(
					"The methodType parameter cannot be null");
		if (methodType.returnType() == null)
			throw new IllegalArgumentException(
					"The returnType of the methodType parameter cannot be null");
		if (methodType.parameterArray() == null)
			throw new IllegalArgumentException(
					"The parameterArray of the methodType parameter cannot be null");
		if (body == null)
			throw new IllegalArgumentException(
					"The body parameter cannot be null");
		int mask = Modifier.methodModifiers();
		if ((modifiers | mask) != mask)
			throw new IllegalArgumentException(
					"The modifier combination is incorrect for a method");
	}

	/**
	 * Check parameters
	 */
	private static void checkGetInvokerParams(Class<?> clazz, String name,
			Class<?> methodInterface, int modifiers)
			throws IllegalArgumentException {
		if (clazz == null)
			throw new IllegalArgumentException(
					"The class parameter cannot be null");
		if (name == null)
			throw new IllegalArgumentException(
					"The name parameter cannot be null");
		if (name.length() == 0)
			throw new IllegalArgumentException(
					"The name parameter cannot be empty");
		if (methodInterface == null)
			throw new IllegalArgumentException(
					"The methodInterface parameter cannot be null");
		int mask = Modifier.methodModifiers();
		if ((modifiers | mask) != mask)
			throw new IllegalArgumentException(
					"The modifier combination is incorrect for a method");
	}

	/**
	 * Check parameters
	 */
	private static void checkReplaceImplementationParams(Class<?> clazz,
			String name, String body) throws IllegalArgumentException {
		if (clazz == null)
			throw new IllegalArgumentException(
					"The class parameter cannot be null");
		if (name == null)
			throw new IllegalArgumentException(
					"The name parameter cannot be null");
		if (name.length() == 0)
			throw new IllegalArgumentException(
					"The name parameter cannot be empty");
		if (body == null)
			throw new IllegalArgumentException(
					"The body parameter cannot be null");
	}

	/**
	 * Check parameters
	 */
	private static void checkReplaceImplementationParams(Class<?> clazz,
			String name, MethodType methodType, String body)
			throws IllegalArgumentException {
		if (methodType == null)
			throw new IllegalArgumentException(
					"The methodType parameter cannot be null");
		if (methodType.returnType() == null)
			throw new IllegalArgumentException(
					"The returnType of the methodType parameter cannot be null");
		if (methodType.parameterArray() == null)
			throw new IllegalArgumentException(
					"The parameterArray of the methodType parameter cannot be null");
		checkReplaceImplementationParams(clazz, name, body);
	}

	/**
	 * Check parameters
	 */
	private static void checkReplaceMethodParams(Class<?> clazz, String name,
			String body) throws IllegalArgumentException {
		if (clazz == null)
			throw new IllegalArgumentException(
					"The class parameter cannot be null");
		if (name == null)
			throw new IllegalArgumentException(
					"The name parameter cannot be null");
		if (name.length() == 0)
			throw new IllegalArgumentException(
					"The name parameter cannot be empty");
		if (body == null)
			throw new IllegalArgumentException(
					"The body parameter cannot be null");
	}

	/**
	 * Check parameters
	 */
	private static void checkReplaceMethodParams(Class<?> clazz, String name,
			MethodType newMethodType, String body)
			throws IllegalArgumentException {
		checkReplaceMethodParams(clazz, name, body);
		if (newMethodType == null)
			throw new IllegalArgumentException(
					"The newMethodType parameter cannot be null");
		if (newMethodType.returnType() == null)
			throw new IllegalArgumentException(
					"The returnType of the newMethodType parameter cannot be null");
		if (newMethodType.parameterArray() == null)
			throw new IllegalArgumentException(
					"The parameterArray of the newMethodType parameter cannot be null");
	}

	/**
	 * Check parameters
	 */
	private static void checkReplaceMethodParams(Class<?> clazz, String name,
			MethodType methodType, MethodType newMethodType, String body)
			throws IllegalArgumentException {
		checkReplaceMethodParams(clazz, name, body);
		if (methodType == null)
			throw new IllegalArgumentException(
					"The methodType parameter cannot be null");
		if (methodType.returnType() == null)
			throw new IllegalArgumentException(
					"The returnType of the methodType parameter cannot be null");
		if (methodType.parameterArray() == null)
			throw new IllegalArgumentException(
					"The parameterArray of the methodType parameter cannot be null");
		if (newMethodType == null)
			throw new IllegalArgumentException(
					"The newMethodType parameter cannot be null");
		if (newMethodType.returnType() == null)
			throw new IllegalArgumentException(
					"The returnType of the newMethodType parameter cannot be null");
		if (newMethodType.parameterArray() == null)
			throw new IllegalArgumentException(
					"The parameterArray of the newMethodType parameter cannot be null");
	}

	/**
	 * Check parameters
	 */
	private static void checkDeleteMethodParams(Class<?> clazz, String name,
			MethodType methodType) throws IllegalArgumentException {
		if (methodType == null)
			throw new IllegalArgumentException(
					"The methodType parameter cannot be null");
		if (methodType.returnType() == null)
			throw new IllegalArgumentException(
					"The returnType of the methodType parameter cannot be null");
		if (methodType.parameterArray() == null)
			throw new IllegalArgumentException(
					"The parameterArray of the methodType parameter cannot be null");
		checkDeleteMethodParams(clazz, name);
	}

	/**
	 * Check parameters
	 */
	private static void checkDeleteMethodParams(Class<?> clazz, String name)
			throws IllegalArgumentException {
		if (clazz == null)
			throw new IllegalArgumentException(
					"The class parameter cannot be null");
		if (name == null)
			throw new IllegalArgumentException(
					"The name parameter cannot be null");
		if (name.length() == 0)
			throw new IllegalArgumentException(
					"The name parameter cannot be empty");
	}

}
