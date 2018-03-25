package jmplib.classversions;

import java.util.HashMap;
import java.util.Map;

import jmplib.classversions.util.MemberKey;

/**
 * This class stores keys of the methods that has been deleted.
 * 
 * @author Ignacio Lagartos
 *
 */
public class DeleteMemberTables {

	private static Map<Integer, MemberKey> deletedMembers = new HashMap<Integer, MemberKey>();

	/**
	 * Checks if the method was deleted
	 * 
	 * @param key
	 *            Method identifier
	 * @return true if deleted
	 */
	public static boolean check(MemberKey key) {
		return deletedMembers.containsKey(key.hashCode());
	}

	/**
	 * Marks one method as deleted
	 * 
	 * @param key
	 *            Method identifier
	 */
	public static void delete(MemberKey key) {
		deletedMembers.put(key.hashCode(), key);
	}

	/**
	 * Marks the method as not deleted
	 * 
	 * @param key
	 *            Method identifier
	 */
	public static void clear(MemberKey key) {
		deletedMembers.remove(key.hashCode());
	}

}
