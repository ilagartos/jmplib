package jmplib.primitives.impl;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import jmplib.exceptions.StructuralIntercessionException;
import jmplib.javaparser.util.JavaParserUtils;
import jmplib.primitives.FieldPrimitive;
import jmplib.sourcecode.ClassContent;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.body.VariableDeclaratorId;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.type.Type;

/**
 * Replaces one field
 * 
 * @author Ignacio Lagartos
 *
 */
public class ReplaceFieldPrimitive extends FieldPrimitive {

	private Class<?> newFieldClass;
	private Expression oldInit;
	private String newInit;
	private Type oldType;

	public ReplaceFieldPrimitive(ClassContent classContent, String name,
			Class<?> newFieldClass, String newInit) {
		super(classContent, name);
		this.newFieldClass = newFieldClass;
		this.newInit = newInit;
	}

	/**
	 * Changes the type of the field and the type of the auxiliary methods
	 */
	@Override
	protected void executePrimitive() throws StructuralIntercessionException {
		Expression newInit = null;
		if (this.newInit != null) {
			try {
				newInit = JavaParser.parseExpression(this.newInit);
			} catch (ParseException e) {
				throw new StructuralIntercessionException(e.getMessage(), e);
			}
		}
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
		try {
			declarator = JavaParserUtils.searchField(unit, clazz, name);
		} catch (NoSuchFieldException e) {
			throw new StructuralIntercessionException(e.getMessage(), e);
		}
		FieldDeclaration field = (FieldDeclaration) declarator.getParentNode();
		oldType = field.getType();
		replaceSupportMethods(unit, type);
		if (field.getVariables().size() == 1) {
			// The type can be replaced directly
			field.setType(JavaParserUtils.transform(newFieldClass));
		} else {
			// Remove from that field declaration
			field.getVariables().remove(declarator);
			// creating the new FieldDeclaration to modify only the type of this
			// variable
			declaration = new FieldDeclaration(field.getModifiers(),
					JavaParserUtils.transform(newFieldClass), declarator);
			declaration.setAnnotations(field.getAnnotations());
			type.getMembers().add(declaration);
		}
		oldInit = declarator.getInit();
		if (newInit != null)
			declarator.setInit(newInit);
		classContent.setContent(unit.toString());
	}

	/**
	 * Replace the auxiliary methods to match with the new field type
	 * 
	 * @param unit
	 *            {@link CompilationUnit} of the field
	 * @param type
	 *            {@link TypeDeclaration} of the field
	 */
	private void replaceSupportMethods(CompilationUnit unit,
			TypeDeclaration type) {
		Type newType = JavaParserUtils.transform(newFieldClass);
		try {
			if (Modifier.isStatic(type.getModifiers())) {
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
			getter.setType(newType);
			List<Parameter> parameters = new ArrayList<Parameter>();
			if (!Modifier.isStatic(type.getModifiers())) {
				VariableDeclaratorId id = new VariableDeclaratorId("o");
				Parameter parameter = new Parameter(
						JavaParserUtils.transform(clazz), id);
				parameters.add(parameter);
			}
			VariableDeclaratorId id = new VariableDeclaratorId("value");
			Parameter parameter = new Parameter(newType, id);
			parameters.add(parameter);
			setter.setParameters(parameters);
		} catch (NoSuchMethodException e) {
			throw new RuntimeException(
					"This static field must have static getter and setter", e);
		}
		try {
			unary = JavaParserUtils.searchMethod(unit, clazz, "_" + name
					+ "_unary");
			unary.setType(newType);
		} catch (NoSuchMethodException e) {
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
					"An exception was thrown parsing the class. "
							+ e.getMessage(), e);
		}
		TypeDeclaration type = JavaParserUtils.searchType(unit, clazz.getName()
				+ "_NewVersion_" + classContent.getVersion());
		VariableDeclarator declarator;
		try {
			declarator = JavaParserUtils.searchField(unit, clazz, name);
		} catch (NoSuchFieldException e) {
			throw new StructuralIntercessionException(e.getMessage(), e);
		}
		FieldDeclaration field = (FieldDeclaration) declarator.getParentNode();
		restoreSupportMethods(unit, type);
		field.setType(oldType);
		declarator.setInit(oldInit);
		classContent.setContent(unit.toString());
	}

	/**
	 * Restores the old auxiliary methods
	 * 
	 * @param unit
	 *            {@link CompilationUnit} of the field
	 * @param type
	 *            {@link TypeDeclaration} of the field
	 */
	private void restoreSupportMethods(CompilationUnit unit,
			TypeDeclaration type) {
		try {
			if (Modifier.isStatic(type.getModifiers())) {
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
			getter.setType(oldType);
			List<Parameter> parameters = new ArrayList<Parameter>();
			if (!Modifier.isStatic(type.getModifiers())) {
				VariableDeclaratorId id = new VariableDeclaratorId("o");
				Parameter parameter = new Parameter(
						JavaParserUtils.transform(clazz), id);
				parameters.add(parameter);
			}
			VariableDeclaratorId id = new VariableDeclaratorId("value");
			Parameter parameter = new Parameter(oldType, id);
			parameters.add(parameter);
			setter.setParameters(parameters);
		} catch (NoSuchMethodException e) {
			throw new RuntimeException(
					"This static field must have static getter and setter", e);
		}
		try {
			unary = JavaParserUtils.searchMethod(unit, clazz, "_" + name
					+ "_unary");
			unary.setType(oldType);
		} catch (NoSuchMethodException e) {
		}
	}

}
