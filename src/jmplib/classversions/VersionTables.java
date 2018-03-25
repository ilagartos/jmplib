package jmplib.classversions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class stores a map composed by a class (using its full name) and the
 * class that represent the most up-to-date version of this class that is in use
 * in a concrete moment.
 * 
 * @author Computational Reflection Research Group. University of Oviedo
 * 
 */
public class VersionTables {
	private static final Map<Integer, Class<?>> versions = new HashMap<Integer, Class<?>>();
	private static final Map<Integer, Class<?>> versionOf = new HashMap<Integer, Class<?>>();
	private static final Map<Integer, List<Class<?>>> allVersions = new HashMap<Integer, List<Class<?>>>();
	public static final boolean DEBUG = false;

	/**
	 * Determined if an instance has a new version available, which means that
	 * either: - The class has a new version associated and the instance has not
	 * (meaning that it is the first time that this instance have been used
	 * since the class has a new version) - The instance has a new version, but
	 * it is not of the same type of the new version of its class (meaning that
	 * the instance has an older version object instance that belongs to a
	 * previous class version type).
	 * 
	 * @param instance
	 *            The instance that maybe has new version
	 * @return if the instance have new versions
	 */
	public static boolean instanceHasNewVersion(Object instance) {

		Class<?> classVersion = versions.get(instance.getClass().getName()
				.hashCode());

		if (DEBUG)
			System.out.println("instanceHasNewVersion: Class version = "
					+ classVersion);

		// The class has no new versions
		if (classVersion == null)
			return false;

		// Use reflection to obtain version object (that is added at load-time).
		Object newVersion = null;
		try {
			newVersion = instance.getClass().getField("_newVersion")
					.get(instance);
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		}

		// The class has a new version, but this instance does not, or the new
		// version of this instance is not of the
		// same class that the new version of its class
		if ((classVersion != null && newVersion == null)
				|| (classVersion != newVersion.getClass()))
			return true;

		return false;
	}

	/**
	 * Obtains the new version of the class
	 * 
	 * @param clazz
	 *            The class
	 * @return The new version or the same class
	 */
	public static Class<?> getNewVersion(Class<?> clazz) {
		Class<?> ret = versions.get(clazz.getName().hashCode());
		if (ret != null)
			return ret;
		return clazz;

	}

	/**
	 * Obtains the new version of the class
	 * 
	 * @param className
	 *            The full name of the class
	 * @return The new version or null
	 */
	public static Class<?> getNewVersion(String className) {
		Class<?> ret = versions.get(className.hashCode());
		if (ret != null)
			return ret;
		return null;

	}

	/**
	 * Determines if a class has a new version or not
	 * 
	 * @param clazz
	 *            The class
	 * @return Returns if a class has a new version or not
	 */
	public static boolean hasNewVersion(Class<?> clazz) {
		return versions.get(clazz.getName().hashCode()) != null;
	}

	/**
	 * Determines if a class has a new version or not
	 * 
	 * @param className
	 *            The full name of the class.
	 * @return Returns if a class has a new version or not
	 */
	public static boolean hasNewVersion(String className) {
		return versions.get(className.hashCode()) != null;
	}

	/**
	 * Adds a new version of one class
	 * 
	 * @param original
	 *            The original class
	 * @param version
	 *            The new version
	 */
	public static void addNewVersion(Class<?> original, Class<?> version) {
		int code = original.getName().hashCode();
		versionOf.put(version.getName().hashCode(), original);
		versions.put(code, version);
		List<Class<?>> versionsList = allVersions.get(code);
		if (versionsList == null)
			versionsList = new ArrayList<Class<?>>();
		for (Class<?> clazz : versionsList)
			versions.put(clazz.getName().hashCode(), version);
		versionsList.add(version);
		allVersions.put(code, versionsList);

	}

	/**
	 * Obtains all version of one class
	 * 
	 * @param clazz
	 *            The class
	 * @return All versions of the class
	 */
	public static List<Class<?>> getVersions(Class<?> clazz) {
		int code = clazz.getName().hashCode();
		List<Class<?>> versions = allVersions.get(code);
		if (versions == null)
			versions = new ArrayList<Class<?>>();
		return versions;
	}

	/**
	 * Obtains the original class that is versioned by the class provided
	 * 
	 * @param clazz
	 *            The version class
	 * @return The original class
	 */
	public static Class<?> isVersionOf(Class<?> clazz) {
		return versionOf.get(clazz.getName().hashCode());
	}

	/**
	 * Obtains the original class that is versioned by the class provided
	 * 
	 * @param clazz
	 *            The full name of the version class
	 * @return The original class
	 */
	public static Class<?> isVersionOf(String className) {
		return versionOf.get(className.hashCode());
	}
}
