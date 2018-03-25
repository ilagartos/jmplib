package jmplib.primitives.impl;

import java.lang.reflect.Modifier;

import jmplib.asm.util.ASMUtils;
import jmplib.classversions.DeleteMemberTables;
import jmplib.classversions.util.MemberKey;
import jmplib.exceptions.StructuralIntercessionException;
import jmplib.javaparser.util.JavaParserUtils;
import jmplib.primitives.MethodPrimitive;
import jmplib.sourcecode.ClassContent;

import com.github.javaparser.ParseException;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;

/**
 * Deletes one method from the class
 * 
 * @author Nacho
 *
 */
public class DeleteMethodPrimitive extends MethodPrimitive {

	private String name;

	protected MethodDeclaration declaration, invoker;

	public DeleteMethodPrimitive(ClassContent classContent, String name,
			Class<?> returnClass, Class<?>[] parameterClasses) {
		super(classContent, returnClass, parameterClasses);
		this.name = name;
	}

	/**
	 * Deletes one method and its invoker from the source code of the class
	 */
	@Override
	protected void executePrimitive() throws StructuralIntercessionException {
		CompilationUnit unit;
		try {
			unit = JavaParserUtils.parse(classContent.getContent());
		} catch (ParseException e) {
			throw new StructuralIntercessionException(
					"An exception was thrown parsing the class", e);
		}
		try {
			declaration = JavaParserUtils.searchMethod(unit, clazz, name,
					parameterClasses, returnClass);
		} catch (NoSuchMethodException e) {
			throw new StructuralIntercessionException(
					"No such method in the class "
							+ classContent.getClazz().getName(), e);
		}
		TypeDeclaration type = JavaParserUtils.searchType(unit, clazz.getName()
				+ "_NewVersion_" + classContent.getVersion());
		type.getMembers().remove(declaration);
		if (!getIsOverrideMethod()) {
			DeleteMemberTables.delete(new MemberKey(ASMUtils
					.getInternalName(classContent.getClazz()), name,
					getDescriptor()));
		}
		if (!Modifier.isStatic(modifiers))
			deleteInvoker(unit, type);
		classContent.setContent(unit.toString());
	}

	private void deleteInvoker(CompilationUnit unit, TypeDeclaration type)
			throws StructuralIntercessionException {
		Class<?>[] params = new Class<?>[parameterClasses.length + 1];
		System.arraycopy(parameterClasses, 0, params, 1,
				parameterClasses.length);
		params[0] = classContent.getClazz();
		String invokerName = "_".concat(name).concat("_invoker");
		try {
			invoker = JavaParserUtils.searchMethod(unit, clazz, invokerName,
					params, returnClass);
		} catch (NoSuchMethodException e) {
			throw new StructuralIntercessionException(
					"No such method in the class "
							+ classContent.getClazz().getName(), e);
		}
		type.getMembers().remove(invoker);
		if (!getIsOverrideMethod()) {
			DeleteMemberTables.delete(new MemberKey(ASMUtils
					.getInternalName(classContent.getClazz()), invokerName,
					getInvokerDescriptor()));
		}
	}

	/**
	 * Reverts the changes
	 */
	@Override
	protected void undoPrimitive() throws StructuralIntercessionException {
		CompilationUnit unit;
		try {
			unit = JavaParserUtils.parse(classContent.getContent());
		} catch (ParseException e) {
			throw new StructuralIntercessionException(
					"An exception was thrown parsing the class", e);
		}
		TypeDeclaration type = JavaParserUtils.searchType(unit, clazz.getName()
				+ "_NewVersion_" + classContent.getVersion());
		type.getMembers().add(declaration);
		DeleteMemberTables.clear(new MemberKey(ASMUtils
				.getInternalName(classContent.getClazz()), name,
				getDescriptor()));
		if (!Modifier.isStatic(modifiers)) {
			type.getMembers().add(invoker);
			DeleteMemberTables.clear(new MemberKey(ASMUtils
					.getInternalName(classContent.getClazz()), "_".concat(name)
					.concat("_invoker"), getInvokerDescriptor()));
		}
		classContent.setContent(unit.toString());
	}

	@Override
	public boolean isSafe() {
		return false;
	}

	/**
	 * Checks if the method overrides another method
	 * 
	 * @return {@code true} if overrides other method
	 * @throws SecurityException
	 */
	private boolean getIsOverrideMethod() throws SecurityException {
		Class<?> superClass = clazz.getSuperclass();
		if (superClass == null)
			return false;
		try {
			superClass.getMethod(name, parameterClasses);
			return true;
		} catch (NoSuchMethodException e) {
			return false;
		}
	}

}
