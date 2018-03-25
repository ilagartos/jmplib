package jmplib.util;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class TransferState {
	// Statically create a cache to avoid O(n) search on destination class
	// fields.
	static Map<String, Field> destFiels = new HashMap<>();
	static Map<Integer, Field[]> fieldCache = new HashMap<Integer, Field[]>();
	static Map<Integer, Class<?>> superClasses = new HashMap<Integer, Class<?>>();

	// Declared here to save memory
	static Field[] destFields;
	static Field[] srcFields;

	public static void transferState(Object src, Object dest) {
		copyFields(src, dest, src.getClass(), dest.getClass());
		// Clear cache to free it for the next transfer state. Done here to
		// perform this only once per execution.
		TransferState.destFiels.clear();
	}

	/**
	 * Exception handling are stripped from this methods for performance reasons
	 * 
	 * @param src
	 * @param dest
	 * @param srcClass
	 * @param destClass
	 * @throws IllegalAccessException
	 * @throws NoSuchFieldException
	 */
	private static void copyFields(Object src, Object dest, Class<?> srcClass,
			Class<?> destClass) {
		// Populate destionation field map cache to avoid O(n) seaches. Effect
		// is more dramatic the more attributes
		// a certain class has

		// Avoid multiple nested refencing (x.y)
		Map<String, Field> destFieldsMap = TransferState.destFiels;
		destFields = getFields(destClass);
		for (Field f : destFields)
			destFieldsMap.put(f.getName(), f);

		// Run over all the source class fields
		srcFields = getFields(srcClass);
		// Avoid multiple nested referencing (x.y)
		Field fdest;
		for (Field f : srcFields) {
			// setAccessible(true) on both fields is key to obtain good
			// performance6
			f.setAccessible(true);

			// Read value
			fdest = destFieldsMap.get(f.getName());
			if (fdest == null)
				continue;
			fdest.setAccessible(true);

			// Write on the destination class with type-specialized writers
			// This seems to have a slight performance advantage over just
			// calling the Object one (default)
			Class<?> t = f.getType();
			try {
				if (t == short.class)
					fdest.setShort(dest, f.getShort(src));
				else if (t == int.class)
					fdest.setInt(dest, f.getInt(src));
				else if (t == float.class)
					fdest.setFloat(dest, f.getFloat(src));
				else if (t == double.class)
					fdest.setDouble(dest, f.getDouble(src));
				else if (t == char.class)
					fdest.setChar(dest, f.getChar(src));
				else if (t == byte.class)
					fdest.setByte(dest, f.getByte(src));
				else if (t == long.class)
					fdest.setLong(dest, f.getLong(src));
				else
					fdest.set(dest, f.get(src)); // Default write (Object type)
			} catch (Exception e) {
			}
		}

		// Iterate superclasses on both classes. We suppose that the Object
		// class has no attribute worth copying1
		srcClass = srcClass.getSuperclass();
		if (srcClass != Object.class) {
			copyFields(src, dest, srcClass, destClass.getSuperclass());
		}
	}

	public static Field[] getFields(Class<?> clazz) {
		Integer key = clazz.getName().hashCode();
		Field[] fields = fieldCache.get(key);
		if (fields == null) {
			fields = clazz.getDeclaredFields();
			fieldCache.put(key, fields);
		}
		return fields;
	}

}