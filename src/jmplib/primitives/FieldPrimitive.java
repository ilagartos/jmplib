package jmplib.primitives;

import java.lang.reflect.Modifier;

import jmplib.sourcecode.ClassContent;

import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;

/**
 * Superclass of all field primitives.
 * 
 * @author Ignacio Lagartos
 *
 */
public abstract class FieldPrimitive extends AbstractPrimitive {

	protected String name;

	protected FieldDeclaration declaration;
	protected MethodDeclaration getter, setter, unary;

	public FieldPrimitive(ClassContent classContent, String name) {
		super(classContent);
		this.name = name;
	}

	/**
	 * Obtains the visibility of the auxiliary methods
	 * 
	 * @param fieldModifiers
	 *            The field modifiers
	 * @return The visibility of the methodsS
	 */
	protected int getAuxiliaryMethodVisibility(int fieldModifiers) {
		int mask = Modifier.PRIVATE | Modifier.PROTECTED | Modifier.PUBLIC;
		return fieldModifiers & mask;
	}

}
