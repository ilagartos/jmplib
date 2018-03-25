package jmplib;

import java.lang.invoke.MethodType;
import java.lang.reflect.Modifier;
import java.util.LinkedList;
import java.util.Queue;

import jmplib.exceptions.StructuralIntercessionException;
import jmplib.primitives.Primitive;
import jmplib.primitives.PrimitiveExecutor;
import jmplib.primitives.PrimitiveFactory;

/**
 * An intercessor of classes. This class is the façade of JMPlib and provides
 * all the primitive support (add method, replace method, delete method...) and
 * the methods that allow to create invokers for the new members. Contrary to
 * {@link Intercessor}, this class supports the execution of several primitives
 * simultaneously as a transaction.
 * 
 * @author Ignacio Lagartos
 * 
 */
public class IntercessorTransaction {

	private Queue<Primitive> primitives = new LinkedList<Primitive>();

	private boolean commited = false;

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
	 *  transaction.addMethod(Counter.class, "sum", mt, 
	 *  	"return this.counter += value;", "value"); </code>
	 * </pre>
	 * 
	 * <p>
	 * This call is equivalent to:
	 * </p>
	 * 
	 * <pre>
	 * <code>transaction.addMethod(Counter.class, "sum", mt, 
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
	public void addMethod(Class<?> clazz, String name, MethodType methodType,
			String body, String... parameterNames)
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
	 *  transaction.addMethod(Counter.class, "sum", mt, 
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
	public void addMethod(Class<?> clazz, String name, MethodType methodType,
			String body, int modifiers, String... parameterNames)
			throws StructuralIntercessionException {
		if (parameterNames == null) {
			parameterNames = new String[0];
		}
		// Checking params
		checkAddMethodParams(clazz, name, methodType, body, modifiers);
		// Creating the primitive
		Primitive primitive = PrimitiveFactory.createAddMethodPrimitive(clazz,
				name, methodType, parameterNames, body, modifiers);
		primitives.add(primitive);
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
	 *  transaction.addMethod(Counter.class, "sum", mt, 
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
	public void addMethod(Class<?> clazz, String name, MethodType methodType,
			String body, int modifiers, Class<?>[] exceptions,
			String... parameterNames) throws StructuralIntercessionException {
		if (parameterNames == null) {
			parameterNames = new String[0];
		}
		// / Checking params
		checkAddMethodParams(clazz, name, methodType, body, modifiers);
		// Creating the primitive
		Primitive primitive = PrimitiveFactory.createAddMethodPrimitive(clazz,
				name, methodType, parameterNames, body, modifiers, exceptions);
		primitives.add(primitive);
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
	 * transaction.replaceImplementation(Calculator.class, "sum", 
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
	public void replaceImplementation(Class<?> clazz, String name, String body)
			throws StructuralIntercessionException {
		// Check parameters
		checkReplaceImplementationParams(clazz, name, body);
		// Creating the primitive
		Primitive primitive = PrimitiveFactory.createReplaceImplementation(
				clazz, name, body);
		primitives.add(primitive);
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
	 * transaction.replaceImplementation(Calculator.class, "sum", type, 
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
	public void replaceImplementation(Class<?> clazz, String name,
			MethodType methodType, String body)
			throws StructuralIntercessionException {
		// Check parameters
		checkReplaceImplementationParams(clazz, name, methodType, body);
		// Creating the primitive
		Primitive primitive = PrimitiveFactory.createReplaceImplementation(
				clazz, name, methodType, body);
		primitives.add(primitive);
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
	 * transaction.replaceMethod(Owner.class, "addPet", newType, 
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
	public void replaceMethod(Class<?> clazz, String name,
			MethodType newMethodType, String body)
			throws StructuralIntercessionException {
		// Check parameters
		checkReplaceMethodParams(clazz, name, newMethodType, body);
		// Creating the primitive
		Primitive primitive = PrimitiveFactory.createReplaceMethodPrimitive(
				clazz, name, newMethodType, body);
		primitives.add(primitive);
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
	 * transaction.replaceMethod(Owner.class, "module10", type, newType, 
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
	public void replaceMethod(Class<?> clazz, String name,
			MethodType methodType, MethodType newMethodType, String body)
			throws StructuralIntercessionException {
		// Check parameters
		checkReplaceMethodParams(clazz, name, methodType, newMethodType, body);
		// Creating the primitive
		Primitive primitive = PrimitiveFactory.createReplaceMethodPrimitive(
				clazz, name, methodType, newMethodType, body);
		primitives.add(primitive);
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
	 * transaction.deleteMethod(Dog.class, "bark");
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
	public void deleteMethod(Class<?> clazz, String name)
			throws StructuralIntercessionException {
		// Checking the params
		checkDeleteMethodParams(clazz, name);
		// Creating the primitive
		Primitive primitive = PrimitiveFactory.createDeleteMethodPrimitive(
				clazz, name);
		primitives.add(primitive);
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
	 * transaction.deleteMethod(Calculator.class, "sum", type);
	 * transaction.commit();
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
	public void deleteMethod(Class<?> clazz, String name, MethodType methodType)
			throws StructuralIntercessionException {
		// Checking the params
		checkDeleteMethodParams(clazz, name, methodType);
		// Creating the primitive
		Primitive primitive = PrimitiveFactory.createDeleteMethodPrimitive(
				clazz, name, methodType);
		primitives.add(primitive);
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
	 * <code>transaction.addField(Person.class, String.class, "lastName");</code>
	 * </pre>
	 * <p>
	 * This call is equivalent to:
	 * </p>
	 * 
	 * <pre>
	 * <code>transaction.addField(Person.class, <b>0</b>, String.class, 
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
	public void addField(Class<?> clazz, Class<?> type, String name)
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
	 * <code>transaction.addField(Person.class, Modifier.PRIVATE, String.class, 
	 * 		"lastName");</code>
	 * </pre>
	 * <p>
	 * This call is equivalent to:
	 * </p>
	 * 
	 * <pre>
	 * <code>transaction.addField(Person.class, Modifier.PRIVATE, String.class, 
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
	public void addField(Class<?> clazz, int modifiers, Class<?> type,
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
	 * <code>transaction.addField(Person.class, Modifier.PRIVATE, String.class, 
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
	public void addField(Class<?> clazz, int modifiers, Class<?> type,
			String name, String init) throws StructuralIntercessionException {
		// Checking parameters
		checkAddFieldParams(clazz, modifiers, type, name, init);
		// Creating the primitive
		Primitive primitive = PrimitiveFactory.createAddFieldPrimitive(clazz,
				modifiers, type, name, init);
		primitives.add(primitive);
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
	 * <code>transaction.deleteField(Person.class, "lastName");</code>
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
	public void deleteField(Class<?> clazz, String name)
			throws StructuralIntercessionException {
		// Checking parameters
		checkDeleteFieldParams(clazz, name);
		// Creating the primitive
		Primitive primitive = PrimitiveFactory.createDeleteFieldPrimitive(
				clazz, name);
		primitives.add(primitive);
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
	 * <code>transaction.replaceField(Calculator.class, "lastResult", double.class);</code>
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
	public void replaceField(Class<?> clazz, String name, Class<?> newType)
			throws StructuralIntercessionException {
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
	 * <code>transaction.replaceField(Calculator.class, "lastResult", double.class, "0.0");</code>
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
	public void replaceField(Class<?> clazz, String name, Class<?> newType,
			String newInit) throws StructuralIntercessionException {
		// Checking parameters
		checkReplaceFieldParams(clazz, name, newType);
		// Creating the primitive
		Primitive primitive = PrimitiveFactory.createReplaceFieldPrimitive(
				clazz, name, newType, newInit);
		primitives.add(primitive);
	}

	/**
	 * <p>
	 * Executes all the primitives added. If any error occurr during the
	 * process, nothing is going to have effect over the application.
	 * </p>
	 * 
	 * @throws StructuralIntercessionException
	 */
	public void commit() throws StructuralIntercessionException {
		if (commited) {
			throw new StructuralIntercessionException(
					"The primitives have already been committed");
		}
		PrimitiveExecutor executor = new PrimitiveExecutor(primitives);
		executor.executePrimitives();
		commited = true;
	}

	/**
	 * Check parameters
	 */
	private void checkDeleteFieldParams(Class<?> clazz, String name) {
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
	private void checkReplaceFieldParams(Class<?> clazz, String name,
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
	private void checkAddFieldParams(Class<?> clazz, int modifiers,
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
	private void checkAddMethodParams(Class<?> clazz, String name, String body,
			int modifiers) throws IllegalArgumentException {
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
		int mask = Modifier.methodModifiers();
		if ((modifiers | mask) != mask)
			throw new IllegalArgumentException(
					"The modifier combination is incorrect for a method");
	}

	/**
	 * Check parameters
	 */
	private void checkAddMethodParams(Class<?> clazz, String name,
			MethodType methodType, String body, int modifiers)
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
		checkAddMethodParams(clazz, name, body, modifiers);
	}

	/**
	 * Check parameters
	 */
	private void checkReplaceImplementationParams(Class<?> clazz, String name,
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
	private void checkReplaceImplementationParams(Class<?> clazz, String name,
			MethodType methodType, String body) throws IllegalArgumentException {
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
	private void checkDeleteMethodParams(Class<?> clazz, String name)
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

	/**
	 * Check parameters
	 */
	private void checkDeleteMethodParams(Class<?> clazz, String name,
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
	private void checkReplaceMethodParams(Class<?> clazz, String name,
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
	private void checkReplaceMethodParams(Class<?> clazz, String name,
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
	private void checkReplaceMethodParams(Class<?> clazz, String name,
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

}
