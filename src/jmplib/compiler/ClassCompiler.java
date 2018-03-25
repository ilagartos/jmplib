package jmplib.compiler;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.List;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

import jmplib.exceptions.CompilationFailedException;
import jmplib.exceptions.StructuralIntercessionException;
import jmplib.util.FileUtils;
import jmplib.util.PathConstants;

/**
 * This class is used for compile java files at runtime. The class implements a
 * singleton pattern so all the constructors are private. It is possible to
 * create an instance through the {@link ClassCompiler#getInstance()} method.
 * 
 * @author Ignacio Lagartos
 * 
 */
public class ClassCompiler {

	private static final String JAVA_HOME = "java.home";
	private static final String PROPERTY_FILE_NAME = "config.properties";
	private static ClassCompiler _instance = new ClassCompiler();

	private ClassCompiler() {
	}

	/**
	 * This method compiles multiple java files at runtime. If anyone of those
	 * has compilation errors, no one would be compiled.
	 * 
	 * @param files
	 *            The java files to be compiled
	 * @param classes
	 *            The names of the classes that will be compiled
	 * @throws IOException
	 * @throws CompilationFailedException
	 *             It's thrown when the file have source code errors.
	 * @throws StructuralIntercessionException
	 */
	public void compile(List<File> classPath, JavaFileObject... files)
			throws CompilationFailedException, IOException,
			StructuralIntercessionException {
		System.setProperty(JAVA_HOME,
				FileUtils.getProperty(JAVA_HOME, PROPERTY_FILE_NAME));
		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		StandardJavaFileManager fileManager = compiler.getStandardFileManager(
				null, null, null);
		boolean compiled = false;
		Writer errors = new StringWriter();
		fileManager.setLocation(StandardLocation.CLASS_OUTPUT,
				Arrays.asList(new File(PathConstants.ORIGINAL_CLASS_PATH)));
		fileManager.setLocation(StandardLocation.CLASS_PATH, classPath);
		// Compile the file
		compiled = compiler.getTask(errors, fileManager, null,
				Arrays.asList("-g"), null, Arrays.asList(files)).call();
		fileManager.close();
		if (!compiled) {
			throw new CompilationFailedException(
					"The compilation of the classes failed.\n"
							+ errors.toString(), errors.toString());
		}
	}

	/**
	 * Obtains the instance of the ClassCompiler
	 * 
	 * @return {@link ClassCompiler}
	 */
	public static ClassCompiler getInstance() {
		return _instance;
	}

}
