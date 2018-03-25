package jmplib;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

import jmplib.agent.UpdaterAgent;
import jmplib.compiler.ClassCompiler;
import jmplib.compiler.PolyglotAdapter;
import jmplib.exceptions.CompilationFailedException;
import jmplib.exceptions.StructuralIntercessionException;
import jmplib.javaparser.util.JavaParserUtils;
import jmplib.sourcecode.SourceCodeCache;
import jmplib.util.ClassPathUtil;
import jmplib.util.EnvironmentSetUp;
import jmplib.util.InheritanceTables;
import jmplib.util.JavaSourceFromString;
import jmplib.util.PathConstants;
import jmplib.util.WrapperClassGenerator;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.stmt.BlockStmt;

/**
 * An evaluator of dynamic code and dynamic classes. This class provides support
 * for the addition of new classes to the application and the creation of
 * dynamic executable blocks of code by {@code FunctionalInterface}.
 * 
 * @author Ignacio Lagartos
 *
 */
public class Evaluator {

	private static int evalVersion = 0;

	/**
	 * <p>
	 * Exec method allows the addition of new classes at runtime from its source
	 * code to the application. These classes are incorporated to the specified
	 * package as they were there before the application start running, and can
	 * be used by other classes as normal.
	 * </p>
	 * <p>
	 * The following example shows how does it works:
	 * </p>
	 * 
	 * <pre>
	 * <code>Class<?> clazz = Evaluator.exec("package pack.example; public class Foo {}");</code>
	 * </pre>
	 * <p>
	 * This sentence adds a new class called {@code Foo} to the package
	 * {@code package.example} of the application. This new class is compatible
	 * with JMPlib modifications because a copy of its source code is located
	 * inside the source code folder.
	 * 
	 * <p>
	 * <b>IMPORTANT:</b> If you stop the application make sure you delete the
	 * added classes before launching it again.
	 * </p>
	 * 
	 * @param classSource
	 *            source code of the class
	 * @return The class reference
	 * @throws StructuralIntercessionException
	 */
	public static Class<?> exec(String classSource)
			throws StructuralIntercessionException {
		File source = null;
		try {
			boolean inheritance = false;
			CompilationUnit unit = JavaParserUtils.parse(classSource);
			String pack = unit.getPackage().getName().toString();
			TypeDeclaration type = unit.getTypes().get(0);
			if (type instanceof ClassOrInterfaceDeclaration) {
				ClassOrInterfaceDeclaration decl = (ClassOrInterfaceDeclaration) type;
				inheritance = decl.getExtends() != null
						&& decl.getExtends().size() > 0;
			}
			String name = pack + "." + type.getName();
			source = new File(PathConstants.ORIGINAL_SRC_PATH
					+ name.replace('.', '/') + ".java");
			if (!source.exists()) {
				source.createNewFile();
			} else {
				throw new StructuralIntercessionException(
						"The class already exist in the src folder");
			}
			FileWriter writer = new FileWriter(source);
			if (!inheritance) {
				writer.write(classSource);
			} else {
				setEmptyBodies(unit);
				writer.write(unit.toStringWithoutComments());
			}
			writer.close();
			JavaSourceFromString[] instrumented = PolyglotAdapter
					.instrument(source);
			ClassCompiler.getInstance().compile(
					ClassPathUtil.getApplicationClassPath(), instrumented);
			Class<?> clazz = Class.forName(name);
			if (!clazz.isInterface()) {
				if (inheritance) {
					writer = new FileWriter(source);
					writer.write(classSource);
					writer.close();
				}
				SourceCodeCache.getInstance().getClassContent(clazz);
				InheritanceTables.put(clazz.getSuperclass(), clazz);
				UpdaterAgent.updateClass(clazz);
				Intercessor.addField(clazz, boolean.class, "__newclass__");
			}
			source.delete();
			return clazz;
		} catch (ParseException | IOException | CompilationFailedException
				| ClassNotFoundException | StructuralIntercessionException e) {
			if (source != null) {
				source.delete();
			}
			throw new StructuralIntercessionException(
					"exec could not be executed due to the following reasons: "
							+ e.getMessage(), e.getCause());
		}
	}

	/**
	 * <p>
	 * This method generates classes that implements functional interfaces. That
	 * allows to enclose the code given and invoke it by the interface instance
	 * returned.
	 * </p>
	 * 
	 * @param code
	 *            the code to enclose in the interface method
	 * @param functionalInterface
	 *            a {@link FunctionalInterface}
	 * @param paramNames
	 *            the name of the parameters that will receive the method
	 * @return An instance of the functional interface that allows to invoke the
	 *         code given
	 * @throws StructuralIntercessionException
	 */
	public static <T> T generateEvalInvoker(String code,
			Class<T> functionalInterface, String... paramNames)
			throws StructuralIntercessionException {
		return generateEvalInvoker(code, functionalInterface, paramNames,
				new Class<?>[0]);
	}

	/**
	 * <p>
	 * This method generates classes that implements generic functional
	 * interfaces. That allows to enclose the code given and invoke it by the
	 * interface instance returned.
	 * </p>
	 * 
	 * @param code
	 *            the code to enclose in the interface method
	 * @param functionalInterface
	 *            a {@link FunctionalInterface}
	 * @param paramNames
	 *            the name of the parameters that will receive the method
	 * @param parametrizationClasses
	 *            the parametrization classes of the interface
	 * @return An instance of the functional interface that allows to invoke the
	 *         code given
	 * @throws StructuralIntercessionException
	 */
	public static <T> T generateEvalInvoker(String code,
			Class<T> functionalInterface, String[] paramNames,
			Class<?>... parametrizationClasses)
			throws StructuralIntercessionException {
		String name = "Generated_Eval_Class_" + ++evalVersion;
		File file = null;
		try {
			file = WrapperClassGenerator.generateEvalClass(name, code,
					functionalInterface, paramNames, parametrizationClasses);
		} catch (StructuralIntercessionException e) {
			throw new StructuralIntercessionException(
					"generateEvalInvoker could not be executed due to the following reasons: "
							+ e.getMessage(), e.getCause());
		}
		T evalInvoker;
		try {
			evalInvoker = compileFile(file, name,
					WrapperClassGenerator.GENERATED_EVAL_PACKAGE);
		} catch (CompilationFailedException e) {
			throw new StructuralIntercessionException(
					"generateEvalInvoker could not be executed due to the following reasons: "
							+ e.getMessage(), e);
		}
		return evalInvoker;
	}

	/**
	 * <p>
	 * This method generates classes that implements functional interfaces, that
	 * have to extend {@link EnvironmentSetUp} interface. It allows to enclose
	 * the code given and invoke it by the interface instance returned.
	 * </p>
	 * <p>
	 * Additionally, this method define the global variables of the generated
	 * class using the map {@code environment} . All this variables are common
	 * for each invokation and have to be initialized by the method
	 * {@code EnvironmentSetUp.setEnvironment}.
	 * </p>
	 * 
	 * @param code
	 *            the code to enclose in the interface method
	 * @param functionalInterface
	 *            a {@link FunctionalInterface}
	 * @param environment
	 *            the map with the names and types of the global variables in
	 *            the generated class
	 * @param paramNames
	 *            the name of the parameters that will receive the method
	 * @return An instance of the functional interface that allows to invoke the
	 *         code given
	 * @throws StructuralIntercessionException
	 */
	public static <T extends EnvironmentSetUp> T generateEvalInvoker(
			String code, Class<T> functionalInterface,
			Map<String, Class<?>> environment, String... paramNames)
			throws StructuralIntercessionException {
		return generateEvalInvoker(code, functionalInterface, environment,
				paramNames, new Class<?>[0]);
	}

	/**
	 * <p>
	 * This method generates classes that implements generic functional
	 * interfaces, that have to extend {@link EnvironmentSetUp} interface. It
	 * allows to enclose the code given and invoke it by the interface instance
	 * returned.
	 * </p>
	 * <p>
	 * Additionally, this method define the global variables of the generated
	 * class using the map {@code environment} . All this variables are common
	 * for each invokation and have to be initialized by the method
	 * {@code EnvironmentSetUp.setEnvironment}.
	 * </p>
	 * 
	 * @param code
	 *            the code to enclose in the interface method
	 * @param functionalInterface
	 *            a {@link FunctionalInterface}
	 * @param environment
	 *            the map with the names and types of the global variables in
	 *            the generated class
	 * @param paramNames
	 *            the name of the parameters that will receive the method
	 * @param parametrizationClasses
	 *            the parametrization classes of the interface
	 * @return An instance of the functional interface that allows to invoke the
	 *         code given
	 * @throws StructuralIntercessionException
	 */
	public static <T extends EnvironmentSetUp> T generateEvalInvoker(
			String code, Class<T> functionalInterface,
			Map<String, Class<?>> environment, String[] paramNames,
			Class<?>... parametrizationClasses)
			throws StructuralIntercessionException {
		String name = "Generated_Eval_Class_" + ++evalVersion;
		File file = null;
		try {
			file = WrapperClassGenerator.generateEvalClass(name, code,
					functionalInterface, paramNames, parametrizationClasses,
					environment);
		} catch (StructuralIntercessionException e) {
			throw new StructuralIntercessionException(
					"generateEvalInvoker could not be executed due to the following reasons: "
							+ e.getMessage(), e.getCause());
		}
		T evalInvoker;
		try {
			evalInvoker = compileFile(file, name,
					WrapperClassGenerator.GENERATED_EVAL_PACKAGE);
		} catch (CompilationFailedException e) {
			throw new StructuralIntercessionException(
					"generateEvalInvoker could not be executed due to the following reasons: "
							+ e.getMessage(), e);
		}
		return evalInvoker;
	}

	/**
	 * Compiles a class file and return an instance of the compiled class. Each
	 * class compiled is instrumented by Polyglot. The name and the package of
	 * the class are needed to load the {@link Class} instance.
	 * 
	 * @param file
	 *            The file of the class
	 * @param name
	 *            The name of the class
	 * @param packageName
	 *            The package of the class
	 * @return The instance of the compiled class
	 * @throws CompilationFailedException
	 *             If the class have compilation errors
	 * @throws StructuralIntercessionException
	 *             If some errors ocurrs
	 * @throws RuntimeException
	 *             If there are errors accesing the files, obtaining the class
	 *             or creating the instance
	 */
	@SuppressWarnings("unchecked")
	private static <T> T compileFile(File file, String name, String packageName)
			throws CompilationFailedException, StructuralIntercessionException {
		try {
			ClassCompiler.getInstance().compile(
					ClassPathUtil.getApplicationClassPath(),
					PolyglotAdapter.instrument(file));
		} catch (IOException e) {
			throw new RuntimeException("Errors compiling the code: "
					+ e.getMessage(), e);
		}
		Class<?> evalClass;
		try {
			evalClass = Class.forName(packageName + "." + name);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException("Errors obtaining the class: "
					+ e.getMessage(), e);
		}
		T evalInvoker;
		try {
			evalInvoker = (T) evalClass.newInstance();
		} catch (InstantiationException e) {
			throw new RuntimeException("Errors instantiating the class: "
					+ e.getMessage(), e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException("Errors accesing the class: "
					+ e.getMessage(), e);
		}
		return evalInvoker;
	}

	/**
	 * Changes all method bodies of the {@link CompilationUnit} for empty
	 * bodies. Each method that returns any value is changed to return the
	 * default value.
	 * 
	 * @param unit
	 *            The {@link CompilationUnit} that contains the methods
	 */
	private static void setEmptyBodies(CompilationUnit unit) {
		TypeDeclaration type = unit.getTypes().get(0);
		for (BodyDeclaration decl : type.getMembers()) {
			if (decl instanceof MethodDeclaration) {
				MethodDeclaration m = (MethodDeclaration) decl;
				String typeName = m.getType().toString();
				String defaultBody = "{return " + getDefaultValue(typeName)
						+ ";}";
				try {
					BlockStmt defaultParsedBody = JavaParser
							.parseBlock(defaultBody);
					m.setBody(defaultParsedBody);
				} catch (ParseException e) {
				}
			}
		}
	}

	/**
	 * Return the default value for the provided type. The value is returned as
	 * String to append to the method body.
	 * 
	 * @param type
	 *            The type to check the default value
	 * @return The String with the default value
	 */
	private static String getDefaultValue(String type) {
		switch (type.toLowerCase()) {
		case "int":
		case "short":
		case "byte":
		case "char":
		case "long":
			return "0";
		case "float":
		case "double":
			return "0.0";
		case "void":
			return "";
		default:
			return "null";
		}
	}

}
