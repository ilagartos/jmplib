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
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.type.Type;

/**
 * Replaces one method
 * 
 * @author Ignacio Lagartos
 *
 */
public class ReplaceMethodPrimitive extends MethodPrimitive {

	private String name;
	private String body;

	private BlockStmt newBody, oldBody;
	private List<Parameter> parameters;
	private List<NameExpr> exceptions;

	protected MethodDeclaration declaration = null, invoker = null;

	private Class<?> newReturnClass;
	private Class<?>[] newParameterClasses;

	public ReplaceMethodPrimitive(ClassContent classContent, String name,
			String body, Class<?> returnType, Class<?>[] parameterClasses,
			Class<?> newReturnType, Class<?>[] newParameterClasses) {
		super(classContent, returnType, parameterClasses);
		this.name = name;
		this.body = body;
		this.newReturnClass = newReturnType;
		this.newParameterClasses = newParameterClasses;
	}

	/**
	 * Generate the new method and new auxiliary methods
	 * 
	 * @throws StructuralIntercessionException
	 */
	private void initialize() throws StructuralIntercessionException {
		if (parameterClasses.length != newParameterClasses.length)
			throw new StructuralIntercessionException(
					"The number of parameters have to match");
		obtainMethodData();
		createBody();
		generateMethod(body);
		generateInvoker();
	}

	/**
	 * Generates the new method
	 * 
	 * @param body
	 *            new body of the method
	 * @throws StructuralIntercessionException
	 */
	private void generateMethod(String body)
			throws StructuralIntercessionException {
		Type returnType = JavaParserUtils.transform(newReturnClass);
		if (newParameterClasses == null || newParameterClasses.length == 0) {
			declaration = new MethodDeclaration(modifiers, returnType, name);
		} else {
			generateParams(name, returnType);
		}
		if (exceptions != null) {
			declaration.setThrows(exceptions);
		}
		try {
			generateBody(body);
		} catch (ParseException e) {
			throw new StructuralIntercessionException(e.getMessage(), e);
		}
	}

	/**
	 * Generate the parameter list and creates the method declaration
	 * 
	 * @param name
	 *            Name of the method
	 * @param returnType
	 *            Return type of the method
	 * @throws StructuralIntercessionException
	 */
	private void generateParams(String name, Type returnType)
			throws StructuralIntercessionException {
		List<Parameter> parameters = new ArrayList<Parameter>();
		for (int i = 0; i < newParameterClasses.length; i++) {
			parameters.add(new Parameter(JavaParserUtils
					.transform(newParameterClasses[i]), this.parameters.get(i)
					.getId()));
		}
		declaration = new MethodDeclaration(modifiers, returnType, name,
				parameters);
	}

	/**
	 * Parses the body code
	 * 
	 * @param body
	 *            Code of the body
	 * @throws ParseException
	 */
	private void generateBody(String body) throws ParseException {
		if (Modifier.isAbstract(modifiers) || Modifier.isNative(modifiers)) {
			// Do nothing
		} else {
			declaration.setBody(JavaParser.parseBlock("{" + body + "}"));
		}
	}

	/**
	 * Generates the invoker method
	 * 
	 * @throws StructuralIntercessionException
	 */
	private void generateInvoker() throws StructuralIntercessionException {
		Type returnType = JavaParserUtils.transform(newReturnClass);
		List<Parameter> parameter = new ArrayList<Parameter>();
		parameter.add(new Parameter(JavaParserUtils.transform(clazz),
				new VariableDeclaratorId("o")));
		String paramsNames = "";
		for (int i = 0; i < newParameterClasses.length; i++) {
			parameter.add(new Parameter(JavaParserUtils
					.transform(newParameterClasses[i]),
					new VariableDeclaratorId("param" + i)));
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
		if (exceptions != null) {
			invoker.setThrows(exceptions);
		}

		Object[] args = {
				clazz.getSimpleName(),
				name,
				clazz.getSimpleName()
						+ "_NewVersion_"
						+ (classContent.isUpdated() ? classContent.getVersion() - 1
								: classContent.getVersion()), paramsNames,
				(newReturnClass.getName().equals("void") ? "" : "return ") };

		String bodyInvoker = String.format(Templates.INVOKER_BODY_TEMPLATE,
				args);

		try {
			invoker.setBody(JavaParser.parseBlock(bodyInvoker));
		} catch (ParseException e) {
			throw new StructuralIntercessionException(e.getMessage(), e);
		}
	}

	/**
	 * Gets the data from the old method
	 * 
	 * @throws StructuralIntercessionException
	 */
	private void obtainMethodData() throws StructuralIntercessionException {
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
		parameters = declaration.getParameters();
		modifiers = declaration.getModifiers();
	}

	/**
	 * Creates the new body for the old method, this body will redirect the
	 * calls to the new method
	 * 
	 * @throws StructuralIntercessionException
	 */
	private void createBody() throws StructuralIntercessionException {
		StringBuilder bodyBuilder = new StringBuilder();
		bodyBuilder.append("{");
		if (!void.class.equals(returnClass))
			bodyBuilder.append("return ");
		bodyBuilder.append(name);
		bodyBuilder.append("(");
		for (int i = 0; i < parameters.size(); i++) {
			bodyBuilder.append("(");
			bodyBuilder.append(newParameterClasses[i].getName());
			bodyBuilder.append(")");
			bodyBuilder.append(parameters.get(i).getId().getName());
			bodyBuilder.append(",");
		}
		bodyBuilder.deleteCharAt(bodyBuilder.length() - 1);
		bodyBuilder.append(");");
		bodyBuilder.append("}");
		try {
			newBody = JavaParser.parseBlock(bodyBuilder.toString());
		} catch (ParseException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	/**
	 * Adds the replace method and redirects the old method to the new one
	 */
	@Override
	protected void executePrimitive() throws StructuralIntercessionException {
		initialize();
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
		MethodDeclaration declaration;
		try {
			declaration = JavaParserUtils.searchMethod(unit, clazz, name,
					parameterClasses, returnClass);
		} catch (NoSuchMethodException e) {
			throw new StructuralIntercessionException(e.getMessage(), e);
		}
		oldBody = declaration.getBody();
		declaration.setBody(newBody);
		this.declaration.setModifiers(declaration.getModifiers());
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
		MethodDeclaration declaration;
		try {
			declaration = JavaParserUtils.searchMethod(unit, clazz, name,
					parameterClasses, returnClass);
		} catch (NoSuchMethodException e) {
			throw new StructuralIntercessionException(e.getMessage(), e);
		}
		declaration.setBody(oldBody);
		if (expectedNumber != td.getMembers().size()) {
			throw new RuntimeException(
					"An error occurred performing the undo action. "
							+ "The methods didn't remove correctly.");
		}
		classContent.setContent(unit.toString());
	}

}
