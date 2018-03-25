package jmplib.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.List;

import jmplib.classversions.VersionTables;
import jmplib.exceptions.StructuralIntercessionException;

/**
 * Helper class to find methods inside functional interfaces and resolve generic
 * info.
 * 
 * @author Ignacio Lagartos
 *
 */
public class MemberFinder {

	/**
	 * Obtains the main method of the functional interface
	 * 
	 * @param interfaceClass
	 *            Functional interface
	 * @return The method data
	 */
	public static Method getMethod(Class<?> interfaceClass) {
		if (!interfaceClass.isInterface())
			throw new IllegalArgumentException("The class is not an interface");
		Method result = null;
		Method[] methods = interfaceClass.getMethods();
		for (Method method : methods) {
			if (Modifier.isAbstract(method.getModifiers())) {
				if (result != null) {
					throw new IllegalArgumentException(
							"More than one abstract method inside the interface");
				}
				result = method;
			}
		}
		if (result == null) {
			throw new IllegalArgumentException(
					"There is no method inside the interface");
		}
		return result;
	}

	/**
	 * Find if one method belongs a class
	 * 
	 * @param declaring
	 *            The class owner
	 * @param name
	 *            The name of the method
	 * @return The method data
	 * @throws StructuralIntercessionException
	 *             There are more than one method
	 * @throws NoSuchMethodException
	 *             If the method doesn't exist
	 */
	public static Method findMethod(Class<?> declaring, String name)
			throws StructuralIntercessionException, NoSuchMethodException {
		Class<?> clazz = VersionTables.getNewVersion(declaring);
		// Get all methods
		Method[] methods = clazz.getDeclaredMethods();
		List<Method> methodWithTheSameName = new ArrayList<Method>();
		// Add the method with the name
		for (Method method : methods) {
			if (method.getName().equals(name)) {
				methodWithTheSameName.add(method);
			}
		}
		// Check the number of methods
		if (methodWithTheSameName.size() == 0) {
			throw new NoSuchMethodException("The method ".concat(name)
					.concat(" does not exist in the class ")
					.concat(declaring.getName()));
		}
		if (methodWithTheSameName.size() > 1) {
			throw new StructuralIntercessionException(
					"There are more than one method named ".concat(name)
							.concat(" in the class ")
							.concat(declaring.getName()));
		}
		// Analyze the method
		return methodWithTheSameName.get(0);
	}

	/**
	 * Find if one field belongs a class
	 * 
	 * @param declaring
	 *            The class owner
	 * @param name
	 *            The name of the field
	 * @return The field data
	 * @throws NoSuchFieldException
	 *             If the field doesn't exist
	 */
	public static Field findField(Class<?> declaring, String name)
			throws NoSuchFieldException {
		Class<?> clazz = VersionTables.getNewVersion(declaring);
		return clazz.getDeclaredField(name);
	}

	/**
	 * Resolves the generic return type of the method
	 * 
	 * @param m
	 *            Generic method
	 * @param parametrizationClasses
	 *            Parametrization classes
	 * @return Type of the return
	 */
	public static Type resolveGenericReturn(Method m,
			Class<?>[] parametrizationClasses) {
		// Class<?> result = null;
		Type t = m.getGenericReturnType();
		if (t instanceof TypeVariable<?>) {
			TypeVariable<?>[] variables = m.getDeclaringClass()
					.getTypeParameters();
			TypeVariable<?> var = (TypeVariable<?>) m.getGenericReturnType();
			for (int i = 0; i < variables.length; i++) {
				if (variables[i].equals(var))
					try {
						return parametrizationClasses[i];
					} catch (ArrayIndexOutOfBoundsException e) {
						throw new IllegalArgumentException(
								"Incorrect number of parametrization classes");
					}
			}
		}
		return t;
	}

	/**
	 * Resolves the generic parameter types of the method
	 * 
	 * @param m
	 *            Generic method
	 * @param parametrizationClasses
	 *            Parametrization classes
	 * @return Type of the parameters
	 */
	public static Type[] resolveGenericParameters(Method m,
			Class<?>[] parametrizationClasses) {
		Type[] parameters = m.getGenericParameterTypes();
		Class<?> result[] = new Class<?>[parameters.length];
		TypeVariable<?>[] variables = m.getDeclaringClass().getTypeParameters();
		for (int i = 0; i < result.length; i++) {
			if (parameters[i] instanceof TypeVariable<?>) {
				TypeVariable<?> var = (TypeVariable<?>) parameters[i];
				for (int j = 0; j < variables.length; j++) {
					if (variables[j].equals(var))
						result[i] = parametrizationClasses[j];
				}
			} else {
				result[i] = (Class<?>) parameters[i];
			}
		}
		return result;
	}

	/**
	 * Resolves the genericity of the method to Class types
	 * 
	 * @param m
	 *            Generic method
	 * @param parametrizationClasses
	 *            Parametrization classes
	 * @return The parametrized parameters
	 */
	public static Class<?>[] resolveGenericParametersToClass(Method m,
			Class<?>[] parametrizationClasses) {
		Type[] parameters = m.getGenericParameterTypes();
		Class<?>[] result = new Class<?>[parameters.length];
		TypeVariable<?>[] variables = m.getDeclaringClass().getTypeParameters();
		for (int i = 0; i < result.length; i++) {
			if (parameters[i] instanceof TypeVariable<?>) {
				TypeVariable<?> var = (TypeVariable<?>) parameters[i];
				for (int j = 0; j < variables.length; j++) {
					if (variables[j].equals(var))
						result[i] = parametrizationClasses[j];
				}
			} else {
				result[i] = (Class<?>) parameters[i];
			}
		}
		return result;
	}

}
