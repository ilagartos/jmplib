package jmplib.primitives;

import java.util.Set;

import jmplib.exceptions.StructuralIntercessionException;
import jmplib.sourcecode.ClassContent;

/**
 * This interface is used to implement the command pattern.
 * 
 * @author Ignacio Lagartos
 * 
 */
public interface Primitive {

	/**
	 * Revert all the changes.
	 * 
	 * @throws StructuralIntercessionException
	 */
	public void undo() throws StructuralIntercessionException;

	/**
	 * Apply the primitive over the {@link ClassContent}
	 * 
	 * @return A set of {@link ClassContent} modified by the primitive
	 * @throws StructuralIntercessionException
	 *             If there are errors while performing the modificactions
	 */
	public Set<ClassContent> execute() throws StructuralIntercessionException;

	/**
	 * Show if the primitive could provoke errors in the application
	 * 
	 * @return false, if the application needs to recompile, true otherwise
	 */
	public boolean isSafe();

}
