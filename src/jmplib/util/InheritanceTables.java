package jmplib.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Holds the inheritance information of the modificable classes
 * 
 * @author Ignacio Lagartos
 *
 */
public class InheritanceTables {

	private static Map<Integer, List<Class<?>>> subclassesCache = new HashMap<Integer, List<Class<?>>>();

	/**
	 * Add inheritance information to the inheritance tree
	 * 
	 * @param mother
	 *            The mother class
	 * @param son
	 *            The child class
	 */
	public static void put(Class<?> mother, Class<?> son) {
		Integer hascode = mother.getName().hashCode();
		List<Class<?>> subclasses = subclassesCache.get(hascode);
		if (subclasses == null) {
			subclasses = new ArrayList<Class<?>>();
			subclassesCache.put(hascode, subclasses);
		}
		subclasses.add(son);
	}

	/**
	 * Provides direct sublclasses of one
	 * 
	 * @param clazz
	 *            The super class
	 * @return The direct subclasses
	 */
	public static List<Class<?>> getSubclasses(Class<?> clazz) {
		Integer hascode = clazz.getName().hashCode();
		List<Class<?>> subclasses = subclassesCache.get(hascode);
		if (subclasses == null) {
			subclasses = new ArrayList<Class<?>>();
		}
		return subclasses;
	}

}
