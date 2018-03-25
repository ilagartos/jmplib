package jmplib.primitives.impl;

import jmplib.exceptions.StructuralIntercessionException;
import jmplib.javaparser.util.JavaParserUtils;
import jmplib.primitives.MethodPrimitive;
import jmplib.sourcecode.ClassContent;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.BlockStmt;

/**
 * Replace the implementation of one method
 * 
 * @author Ignacio Lagartos
 *
 */
public class ReplaceImplementationPrimitive extends MethodPrimitive {

	private String name;
	private BlockStmt newBody, oldBody;

	private String body;

	public ReplaceImplementationPrimitive(ClassContent classContent,
			String name, String body, Class<?> returnClass,
			Class<?>[] parameterClasses) {
		super(classContent, returnClass, parameterClasses);
		this.name = name;
		this.body = body;
	}

	/**
	 * Changes the method body
	 */
	@Override
	protected void executePrimitive() throws StructuralIntercessionException {
		try {
			this.newBody = JavaParser.parseBlock("{" + body + "}");
		} catch (ParseException e) {
			throw new StructuralIntercessionException(e.getMessage(), e);
		}
		CompilationUnit unit;
		try {
			unit = JavaParserUtils.parse(classContent.getContent());
		} catch (ParseException e) {
			throw new StructuralIntercessionException(
					"An exception was thrown parsing the class. "
							+ e.getMessage(), e);
		}
		MethodDeclaration declaration;
		try {
			declaration = JavaParserUtils.searchMethod(unit, clazz, name,
					parameterClasses, returnClass);
		} catch (NoSuchMethodException e) {
			throw new StructuralIntercessionException(e.getMessage(), e);
		}
		oldBody = declaration.getBody();
		declaration.setBody(newBody);
		classContent.setContent(unit.toString());
	}

	/**
	 * Restores the old body
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
		MethodDeclaration declaration;
		try {
			declaration = JavaParserUtils.searchMethod(unit, clazz, name,
					parameterClasses, returnClass);
		} catch (NoSuchMethodException e) {
			throw new StructuralIntercessionException(e.getMessage(), e);
		}
		declaration.setBody(oldBody);
		classContent.setContent(unit.toString());
	}

}
