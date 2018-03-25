package jmplib.primitives.impl;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import jmplib.annotations.AuxiliaryMethod;
import jmplib.exceptions.StructuralIntercessionException;
import jmplib.javaparser.util.JavaParserUtils;
import jmplib.primitives.MethodPrimitive;
import jmplib.sourcecode.ClassContent;
import jmplib.util.Templates;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclaratorId;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.type.Type;

/**
 * This class adds new methods to existing class source code. This class doesn't
 * compile this code, only generates it.
 * 
 * @author Ignacio Lagartos
 * 
 */
public class AddMethodPrimitive extends MethodPrimitive {

	protected MethodDeclaration declaration = null, invoker = null;

	private String name, body;
	private String[] paramNames;

	public AddMethodPrimitive(ClassContent classContent, String name,
			Class<?> returnClass, Class<?>[] parameterClasses,
			Class<?>[] exceptions, String[] paramNames, String body,
			int modifiers) {
		super(classContent, returnClass, parameterClasses, exceptions,
				modifiers);
		this.body = body;
		this.name = name;
		this.paramNames = paramNames;
	}

	public AddMethodPrimitive(ClassContent classContent, String name,
			Class<?> returnClass, Class<?>[] parameterClasses,
			String[] paramNames, String body, int modifiers) {
		super(classContent, returnClass, parameterClasses, modifiers);
		this.body = body;
		this.name = name;
		this.paramNames = paramNames;
	}

	/**
	 * Generates the members needed in the primitive execution
	 * 
	 * @param name
	 *            The name of the method
	 * @param paramNames
	 *            Parameter names
	 * @param body
	 *            Body of the method
	 * @throws ParseException
	 * @throws StructuralIntercessionException
	 */
	protected void initializeFields(String name, String[] paramNames,
			String body) throws ParseException, StructuralIntercessionException {
		generateInvoker(name);
		generateMethod(name, paramNames, body);
	}

	/**
	 * Generates invoker method
	 * 
	 * @param name
	 *            The name of the method
	 * @throws ParseException
	 */
	protected void generateInvoker(String name) throws ParseException {
		Type returnType = JavaParserUtils.transform(returnClass);
		List<Parameter> parameter = new ArrayList<Parameter>();
		parameter.add(new Parameter(JavaParserUtils.transform(clazz),
				new VariableDeclaratorId("o")));
		String paramsNames = "";
		for (int i = 0; i < parameterClasses.length; i++) {
			parameter.add(new Parameter(JavaParserUtils
					.transform(parameterClasses[i]), new VariableDeclaratorId(
					"param" + i)));
			paramsNames += "param" + i + ", ";
		}
		if (!paramsNames.isEmpty())
			paramsNames = paramsNames.substring(0, paramsNames.length() - 2);
		List<AnnotationExpr> annotations = new ArrayList<AnnotationExpr>();
		NameExpr exp = JavaParserUtils.classToNameExpr(AuxiliaryMethod.class);
		annotations.add(new NormalAnnotationExpr(exp, null));
		invoker = new MethodDeclaration(Modifier.PUBLIC | Modifier.STATIC,
				returnType, "_" + name + "_invoker", parameter);
		invoker.setAnnotations(annotations);
		if (exceptionClasses != null) {
			List<NameExpr> list = new ArrayList<NameExpr>();
			for (Class<?> exception : exceptionClasses) {
				list.add(new NameExpr(exception.getName()));
			}
			invoker.setThrows(list);
		}

		Object[] args = {
				clazz.getSimpleName(),
				name,
				clazz.getSimpleName()
						+ "_NewVersion_"
						+ (classContent.isUpdated() ? classContent.getVersion() - 1
								: classContent.getVersion()), paramsNames,
				(returnClass.getName().equals("void") ? "" : "return ") };

		String bodyInvoker = String.format(Templates.INVOKER_BODY_TEMPLATE,
				args);

		invoker.setBody(JavaParser.parseBlock(bodyInvoker));
	}

	/**
	 * Generates the method declaration
	 * 
	 * @param name
	 *            The name of the method
	 * @param paramNames
	 *            Parameter names
	 * @param body
	 *            Body of the method
	 * @throws ParseException
	 * @throws StructuralIntercessionException
	 */
	protected void generateMethod(String name, String[] paramNames, String body)
			throws ParseException, StructuralIntercessionException {
		Type returnType = JavaParserUtils.transform(returnClass);
		if (parameterClasses == null || parameterClasses.length == 0) {
			declaration = new MethodDeclaration(modifiers, returnType, name);
		} else {
			generateParams(name, paramNames, returnType);
		}
		if (exceptionClasses != null) {
			List<NameExpr> list = new ArrayList<NameExpr>();
			for (Class<?> exception : exceptionClasses) {
				list.add(new NameExpr(exception.getName()));
			}
			declaration.setThrows(list);
		}
		generateBody(body);
	}

	/**
	 * Generates the parameter list of the method and creates the declaration
	 * 
	 * @param name
	 *            Name of the method
	 * @param paramNames
	 *            Parameter names
	 * @param returnType
	 *            The return type
	 * @throws StructuralIntercessionException
	 */
	private void generateParams(String name, String[] paramNames,
			Type returnType) throws StructuralIntercessionException {
		List<Parameter> parameters = new ArrayList<Parameter>();
		if (Modifier.isStatic(modifiers)
				&& paramNames.length != parameterClasses.length) {
			throw new StructuralIntercessionException(
					"Adding static methods, the number of params "
							+ "in the interface must match with the parameter names");
		}
		for (int i = 0; i < parameterClasses.length; i++) {
			parameters.add(new Parameter(JavaParserUtils
					.transform(parameterClasses[i]), new VariableDeclaratorId(
					paramNames[i])));
		}
		declaration = new MethodDeclaration(modifiers, returnType, name,
				parameters);
	}

	/**
	 * Parse the body of the method
	 * 
	 * @param body
	 *            Source code of the method
	 * @throws ParseException
	 */
	protected void generateBody(String body) throws ParseException {
		if (Modifier.isAbstract(modifiers) || Modifier.isNative(modifiers)) {
			// Do nothing
		} else {
			declaration.setBody(JavaParser.parseBlock("{" + body + "}"));
		}
	}

	/**
	 * Adds the method and the invoker to the source code of the class
	 */
	@Override
	protected void executePrimitive() throws StructuralIntercessionException {
		// Create the new methods
		try {
			initializeFields(name, paramNames, body);
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
		TypeDeclaration td = JavaParserUtils.searchType(unit, clazz.getName()
				+ "_NewVersion_" + classContent.getVersion());
		td.getMembers().add(declaration);
		if (!Modifier.isStatic(modifiers)) {
			td.getMembers().add(invoker);
		}
		classContent.setContent(unit.toString());
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
		TypeDeclaration td = JavaParserUtils.searchType(unit, clazz.getName()
				+ "_NewVersion_" + classContent.getVersion());
		int numberOfMethods = td.getMembers().size();
		int expectedNumber = 0;
		td.getMembers().remove(declaration);
		if (!Modifier.isStatic(modifiers)) {
			td.getMembers().remove(invoker);
			expectedNumber = numberOfMethods - 2;
		} else {
			expectedNumber = numberOfMethods - 1;
		}
		if (expectedNumber != td.getMembers().size()) {
			throw new RuntimeException(
					"An error occurred performing the undo action. "
							+ "The methods didn't remove correctly.");
		}
		classContent.setContent(unit.toString());
	}
}
