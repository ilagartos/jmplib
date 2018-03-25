package jmplib.util;

import static java.lang.System.out;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * This utility class dumps a class structure to System.out
 * 
 * @author Redondo
 *
 */
public class ClassSpy {

	/**
	 * Prints class data
	 * 
	 * @param c
	 *            The class to print
	 */
	public static void spy(Class<?> c) {

		out.format("Class:%n  %s%n%n", c.getCanonicalName());

		Package p = c.getPackage();
		out.format("Package:%n  %s%n%n", (p != null ? p.getName()
				: "-- No Package --"));

		out.println("Superclass: " + c.getSuperclass());
		printMembers(c.getConstructors(), "Constructor");
		printMembers(getFields(c), "Fields");
		printMembers(c.getMethods(), "Methods");
		printClasses(c);

	}

	/**
	 * Provides the declared fields inside the class and its super classes
	 * 
	 * @param clazz
	 *            The class
	 * @return All class fields
	 */
	public static Field[] getFields(Class<?> clazz) {
		Map<String, Field> fields = new HashMap<String, Field>();
		while (clazz != null) {
			for (Field field : clazz.getDeclaredFields()) {
				if (!fields.containsKey(field.getName())) {
					fields.put(field.getName(), field);
				}
			}

			clazz = clazz.getSuperclass();
		}

		Collection<Field> returnCollection = fields.values();

		return returnCollection.<Field> toArray(new Field[0]);
	}

	/**
	 * Formatted print of the class members
	 * 
	 * @param mbrs
	 *            Members to print
	 * @param s
	 *            Type of the members
	 */
	private static void printMembers(Member[] mbrs, String s) {
		out.format("%s:%n", s);
		for (Member mbr : mbrs) {
			if (mbr instanceof Field)
				out.format("  %s%n", ((Field) mbr).toGenericString());
			else if (mbr instanceof Constructor)
				out.format("  %s%n", ((Constructor<?>) mbr).toGenericString());
			else if (mbr instanceof Method)
				out.format("  %s%n", ((Method) mbr).toGenericString());
		}
		if (mbrs.length == 0)
			out.format("  -- No %s --%n", s);
		out.format("%n");
	}

	/**
	 * Prints the classes that are members of the specified class
	 * 
	 * @param c
	 *            Class to print data
	 */
	private static void printClasses(Class<?> c) {
		out.format("Classes:%n");
		Class<?>[] clss = c.getClasses();
		for (Class<?> cls : clss)
			out.format("  %s%n", cls.getCanonicalName());
		if (clss.length == 0)
			out.format("  -- No member interfaces, classes, or enums --%n");
		out.format("%n");
	}
}
