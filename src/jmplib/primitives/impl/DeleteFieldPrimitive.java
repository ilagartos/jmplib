package jmplib.primitives.impl;

import java.lang.reflect.Modifier;

import jmplib.asm.util.ASMUtils;
import jmplib.classversions.DeleteMemberTables;
import jmplib.classversions.util.MemberKey;
import jmplib.exceptions.StructuralIntercessionException;
import jmplib.javaparser.util.JavaParserUtils;
import jmplib.primitives.FieldPrimitive;
import jmplib.sourcecode.ClassContent;

import com.github.javaparser.ParseException;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;

/**
 * Deletes a field of one class
 * 
 * @author Ignacio Lagartos
 *
 */
public class DeleteFieldPrimitive extends FieldPrimitive {

	private int modifiers;
	private Class<?> type;

	public DeleteFieldPrimitive(ClassContent classContent, String name, Class<?> type) {
		super(classContent, name);
		this.type = type;
	}

	/**
	 * Removes the declaration of one field from the source code of the class
	 */
	@Override
	protected void executePrimitive() throws StructuralIntercessionException {
		CompilationUnit unit;
		try {
			unit = JavaParserUtils.parse(classContent.getContent());
		} catch (ParseException e) {
			throw new StructuralIntercessionException(
					"An exception was thrown parsing the class. "
							+ e.getMessage(), e);
		}
		TypeDeclaration type = JavaParserUtils.searchType(unit, clazz.getName()
				+ "_NewVersion_" + classContent.getVersion());
		VariableDeclarator declarator;
		modifiers = type.getModifiers();
		try {
			declarator = JavaParserUtils.searchField(unit, clazz, name);
		} catch (NoSuchFieldException e) {
			throw new StructuralIntercessionException(e.getMessage(), e);
		}
		FieldDeclaration field = (FieldDeclaration) declarator.getParentNode();
		removeSupportMethods(unit, type);
		if (field.getVariables().size() == 1) {
			// It is needed to delete the entire field
			type.getMembers().remove(field);
			// Saving the field for undo command
			declaration = field;
		} else {
			// Only is needed to delete the variable declaration
			field.getVariables().remove(declarator);
			// Saving the field for undo command
			declaration = new FieldDeclaration(field.getModifiers(),
					field.getType(), declarator);
			declaration.setAnnotations(field.getAnnotations());
		}
		classContent.setContent(unit.toString());
	}

	/**
	 * Delete the auxiliary method of the deleted field
	 * 
	 * @param unit
	 *            {@link CompilationUnit} of the class
	 * @param type
	 *            {@link TypeDeclaration} of the class
	 */
	protected void removeSupportMethods(CompilationUnit unit, TypeDeclaration type) {
		try {
			if (Modifier.isStatic(modifiers)) {
				getter = JavaParserUtils.searchMethod(unit, clazz, "_" + name
						+ "_getter");
				setter = JavaParserUtils.searchMethod(unit, clazz, "_" + name
						+ "_setter");
			} else {
				getter = JavaParserUtils.searchMethod(unit, clazz, "_" + name
						+ "_fieldGetter");
				setter = JavaParserUtils.searchMethod(unit, clazz, "_" + name
						+ "_fieldSetter");
			}
			type.getMembers().remove(getter);
			DeleteMemberTables.delete(new MemberKey(ASMUtils.getInternalName(clazz), getter.getName(), getGetterDescriptor()));
			type.getMembers().remove(setter);
			DeleteMemberTables.delete(new MemberKey(ASMUtils.getInternalName(clazz), setter.getName(), getSetterDescriptor()));
		} catch (NoSuchMethodException e) {
			throw new RuntimeException(
					"This field must have static getter and setter", e);
		}
		try {
			unary = JavaParserUtils.searchMethod(unit, clazz, "_" + name
					+ "_unary");
			type.getMembers().remove(unary);
			DeleteMemberTables.delete(new MemberKey(ASMUtils.getInternalName(clazz), unary.getName(), getUnaryDescriptor()));
		} catch (NoSuchMethodException e) {
		}
	}

	/**
	 * Revert the changes done
	 */
	@Override
	protected void undoPrimitive() throws StructuralIntercessionException {
		CompilationUnit unit;
		try {
			unit = JavaParserUtils.parse(classContent.getContent());
		} catch (ParseException e) {
			throw new StructuralIntercessionException(
					"An exception was thrown parsing the class. "
							+ e.getMessage(), e);
		}
		TypeDeclaration type = JavaParserUtils.searchType(unit, clazz.getName()
				+ "_NewVersion_" + classContent.getVersion());
		type.getMembers().add(declaration);
		addSupportMethods(type);
		classContent.setContent(unit.toString());
	}

	/**
	 * Add deleted auxiliary methods
	 * 
	 * @param type
	 *            {@link TypeDeclaration} of the class
	 */
	protected void addSupportMethods(TypeDeclaration type) {
		if (getter == null) {
			throw new RuntimeException(
					"The support getter for a field couldn't be null");
		}
		type.getMembers().add(getter);
		DeleteMemberTables.clear(new MemberKey(ASMUtils.getInternalName(clazz), getter.getName(), getGetterDescriptor()));
		if (setter == null) {
			throw new RuntimeException(
					"The support setter for a field couldn't be null");
		}
		type.getMembers().add(setter);
		DeleteMemberTables.clear(new MemberKey(ASMUtils.getInternalName(clazz), setter.getName(), getSetterDescriptor()));
		if (unary != null) {
			type.getMembers().add(unary);
			DeleteMemberTables.clear(new MemberKey(ASMUtils.getInternalName(clazz), unary.getName(), getUnaryDescriptor()));
		}
	}

	@Override
	public boolean isSafe() {
		return false;
	}

	protected String getGetterDescriptor() {
		String descriptor = "(";
		if(Modifier.isStatic(modifiers)){
			return descriptor.concat(")")
					.concat(ASMUtils.getDescriptor(type));
		}else{
			return descriptor.concat(ASMUtils.getDescriptor(clazz))
					.concat(")")
					.concat(ASMUtils.getDescriptor(type));
		}
	}

	protected String getSetterDescriptor() {
		String descriptor = "(";
		if(Modifier.isStatic(modifiers)){
			return descriptor.concat(ASMUtils.getDescriptor(type))
					.concat(")V");
		}else{
			return descriptor.concat(ASMUtils.getDescriptor(clazz))
					.concat(ASMUtils.getDescriptor(type))
					.concat(")V");
		}
	}

	protected String getUnaryDescriptor() {
		String descriptor = "(";
		if(Modifier.isStatic(modifiers)){
			return descriptor.concat("I)")
					.concat(ASMUtils.getDescriptor(type));
		}else{
			return descriptor.concat(ASMUtils.getDescriptor(clazz))
					.concat("I)")
					.concat(ASMUtils.getDescriptor(type));
		}
	}

}
