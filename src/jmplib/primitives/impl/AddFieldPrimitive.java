package jmplib.primitives.impl;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jmplib.annotations.AuxiliaryMethod;
import jmplib.asm.util.ASMUtils;
import jmplib.exceptions.StructuralIntercessionException;
import jmplib.javaparser.util.JavaParserUtils;
import jmplib.primitives.FieldPrimitive;
import jmplib.sourcecode.ClassContent;
import jmplib.util.Templates;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.body.VariableDeclaratorId;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;

/**
 * This primitive class adds a new field and its auxiliar methods to one class
 * 
 * @author Ignacio Lagartos
 *
 */
public class AddFieldPrimitive extends FieldPrimitive {

	private static Map<Integer, Class<?>> numericTypes = new HashMap<Integer, Class<?>>();
	private int modifiers;
	private Class<?> fieldClass;
	private String init;

	public AddFieldPrimitive(ClassContent classContent, int modifiers,
			Class<?> type, String name, String init) {
		super(classContent, name);
		this.fieldClass = type;
		this.modifiers = modifiers;
		this.init = init;
	}

	/**
	 * Adds the field to the source code and the auxiliar methods needed
	 */
	@Override
	protected void executePrimitive() throws StructuralIntercessionException {
		try {
			generateNewMembers(init);
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
		td.getMembers().add(getter);
		if (!Modifier.isFinal(modifiers)) {
			td.getMembers().add(setter);
			if (unary != null)
				td.getMembers().add(unary);
		}
		classContent.setContent(unit.toString());
	}

	/**
	 * Revert the changes
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
		TypeDeclaration td = JavaParserUtils.searchType(unit, clazz.getName()
				+ "_NewVersion_" + classContent.getVersion());
		int numberOfMethods = td.getMembers().size();
		int expectedNumber = 0;
		td.getMembers().remove(declaration);
		td.getMembers().remove(getter);
		if (!Modifier.isFinal(modifiers)) {
			td.getMembers().remove(setter);
			if (unary != null) {
				td.getMembers().remove(unary);
				expectedNumber = numberOfMethods - 4;
			} else {
				expectedNumber = numberOfMethods - 3;
			}
		} else {
			expectedNumber = numberOfMethods - 2;
		}
		if (expectedNumber != td.getMembers().size()) {
			throw new RuntimeException(
					"An error occurred performing the undo action. "
							+ "The field didn't remove correctly.");
		}
		classContent.setContent(unit.toString());
	}

	/**
	 * Generates the new members of the class
	 * 
	 * @param init
	 *            Initialization sequence
	 * @throws ParseException
	 */
	private void generateNewMembers(String init) throws ParseException {
		Type type = JavaParserUtils.transform(this.fieldClass);
		VariableDeclarator vd = new VariableDeclarator(
				new VariableDeclaratorId(name));
		declaration = new FieldDeclaration(modifiers, type, vd);
		if (init != null) {
			vd.setInit(JavaParser.parseExpression(init));
		}
		if (Modifier.isStatic(modifiers)) {
			generateStaticFieldGetterAndSetter(type);
			if (numericTypes.containsKey(fieldClass.getName().hashCode())) {
				generateStaticUnaryMethod(type);
			}
		} else {
			generateInstanceFieldGetterAndSetter(type);
			if (numericTypes.containsKey(fieldClass.getName().hashCode())) {
				generateInstanceUnaryMethod(type);
			}
		}
	}

	/**
	 * Generates the getter and setter for static fields
	 * 
	 * @param type
	 *            The type of the field
	 * @throws ParseException
	 */
	private void generateStaticFieldGetterAndSetter(Type type)
			throws ParseException {
		getter = new MethodDeclaration(getAuxiliaryMethodVisibility(modifiers)
				| Modifier.STATIC, type, "_" + name + "_getter");
		List<AnnotationExpr> annotations = new ArrayList<AnnotationExpr>();
		NameExpr exp = JavaParserUtils.classToNameExpr(AuxiliaryMethod.class);
		annotations.add(new NormalAnnotationExpr(exp, null));
		getter.setAnnotations(annotations);
		List<Parameter> parameter = new ArrayList<Parameter>();
		parameter
				.add(new Parameter(type, new VariableDeclaratorId("newValue")));
		setter = new MethodDeclaration(getAuxiliaryMethodVisibility(modifiers)
				| Modifier.STATIC, JavaParserUtils.transform(void.class), "_"
				+ name + "_setter", parameter);
		setter.setAnnotations(annotations);
		getter.setBody(JavaParser.parseBlock("{ return " + name + "; }"));
		setter.setBody(JavaParser.parseBlock("{" + name + " = newValue; }"));
	}

	/**
	 * Generates the getter and setter for instance fields
	 * 
	 * @param type
	 *            The type of the field
	 * @throws ParseException
	 */
	private void generateInstanceFieldGetterAndSetter(Type type)
			throws ParseException {
		List<Parameter> getterParams = new ArrayList<Parameter>();
		getterParams.add(new Parameter(JavaParserUtils.transform(clazz),
				new VariableDeclaratorId("o")));
		List<Parameter> setterParams = new ArrayList<Parameter>();
		setterParams.add(new Parameter(JavaParserUtils.transform(clazz),
				new VariableDeclaratorId("o")));
		setterParams
				.add(new Parameter(type, new VariableDeclaratorId("value")));

		List<AnnotationExpr> annotations = new ArrayList<AnnotationExpr>();
		NameExpr exp = JavaParserUtils.classToNameExpr(AuxiliaryMethod.class);
		annotations.add(new NormalAnnotationExpr(exp, null));

		getter = new MethodDeclaration(getAuxiliaryMethodVisibility(modifiers)
				| Modifier.STATIC, type, "_" + name + "_fieldGetter",
				getterParams);
		setter = new MethodDeclaration(getAuxiliaryMethodVisibility(modifiers)
				| Modifier.STATIC, JavaParserUtils.transform(void.class), "_"
				+ name + "_fieldSetter", setterParams);

		getter.setAnnotations(annotations);
		setter.setAnnotations(annotations);

		Object[] args = {
				name,
				clazz.getSimpleName()
						+ "_NewVersion_"
						+ (classContent.isUpdated() ? classContent.getVersion() - 1
								: classContent.getVersion()),
				clazz.getSimpleName() };

		String bodyGetter = String.format(
				Templates.FIELD_GETTER_TEMPLATE, args);
		String bodySetter = String.format(
				Templates.FIELD_SETTER_TEMPLATE, args);

		getter.setBody(JavaParser.parseBlock(bodyGetter));
		setter.setBody(JavaParser.parseBlock(bodySetter));
	}

	/**
	 * Generates the unary method for static fields
	 * 
	 * @param fieldType
	 *            The type of the field
	 * @throws ParseException
	 */
	private void generateStaticUnaryMethod(Type fieldType) {
		ClassOrInterfaceType intClassOrInterfaceType = new ClassOrInterfaceType(
				int.class.getName());

		List<Parameter> unaryParams = new ArrayList<Parameter>();
		unaryParams.add(new Parameter(intClassOrInterfaceType,
				new VariableDeclaratorId("type")));

		List<AnnotationExpr> annotations = new ArrayList<AnnotationExpr>();
		annotations.add(new NormalAnnotationExpr(new NameExpr(
				AuxiliaryMethod.class.getName()),
				new ArrayList<MemberValuePair>()));

		unary = new MethodDeclaration(getAuxiliaryMethodVisibility(modifiers)
				| Modifier.STATIC, fieldType, "_" + name + "_unary",
				unaryParams);

		unary.setAnnotations(annotations);

		Object[] args = { name, clazz.getSimpleName() };

		String bodyUnary = String.format(Templates.STATIC_FIELD_UNARY_TEMPLATE,
				args);

		try {
			unary.setBody(JavaParser.parseBlock(bodyUnary));
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Generates the unary method for instance fields
	 * 
	 * @param fieldType
	 *            The type of the field
	 * @throws ParseException
	 */
	private void generateInstanceUnaryMethod(Type fieldType) {
		ClassOrInterfaceType intClassOrInterfaceType = new ClassOrInterfaceType(
				int.class.getName());

		List<Parameter> unaryParams = new ArrayList<Parameter>();
		unaryParams.add(new Parameter(
				new ClassOrInterfaceType(clazz.getName()),
				new VariableDeclaratorId("o")));
		unaryParams.add(new Parameter(intClassOrInterfaceType,
				new VariableDeclaratorId("type")));

		List<AnnotationExpr> annotations = new ArrayList<AnnotationExpr>();
		annotations.add(new NormalAnnotationExpr(new NameExpr(
				AuxiliaryMethod.class.getName()),
				new ArrayList<MemberValuePair>()));

		unary = new MethodDeclaration(getAuxiliaryMethodVisibility(modifiers)
				| Modifier.STATIC, fieldType, "_" + name + "_unary",
				unaryParams);

		unary.setAnnotations(annotations);

		Object[] args = {
				name,
				clazz.getSimpleName()
						+ "_NewVersion_"
						+ (classContent.isUpdated() ? classContent.getVersion() - 1
								: classContent.getVersion()),
				clazz.getSimpleName() };

		String bodyUnary = String.format(
				Templates.INSTANCE_FIELD_UNARY_TEMPLATE, args);

		try {
			unary.setBody(JavaParser.parseBlock(bodyUnary));
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}

	static {
		numericTypes.put(ASMUtils.getDescriptor(int.class).hashCode(),
				int.class);
		numericTypes.put(ASMUtils.getDescriptor(Integer.class).hashCode(),
				Integer.class);
		numericTypes.put(ASMUtils.getDescriptor(short.class).hashCode(),
				short.class);
		numericTypes.put(ASMUtils.getDescriptor(Short.class).hashCode(),
				Short.class);
		numericTypes.put(ASMUtils.getDescriptor(long.class).hashCode(),
				long.class);
		numericTypes.put(ASMUtils.getDescriptor(Long.class).hashCode(),
				Long.class);
		numericTypes.put(ASMUtils.getDescriptor(float.class).hashCode(),
				float.class);
		numericTypes.put(ASMUtils.getDescriptor(Float.class).hashCode(),
				Float.class);
		numericTypes.put(ASMUtils.getDescriptor(double.class).hashCode(),
				double.class);
		numericTypes.put(ASMUtils.getDescriptor(Double.class).hashCode(),
				Double.class);
		numericTypes.put(ASMUtils.getDescriptor(byte.class).hashCode(),
				byte.class);
		numericTypes.put(ASMUtils.getDescriptor(Byte.class).hashCode(),
				Byte.class);
		numericTypes.put(ASMUtils.getDescriptor(char.class).hashCode(),
				char.class);
		numericTypes.put(ASMUtils.getDescriptor(Character.class).hashCode(),
				Character.class);
	}
}
