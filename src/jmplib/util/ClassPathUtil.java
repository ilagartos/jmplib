package jmplib.util;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

import jmplib.exceptions.StructuralIntercessionException;

/**
 * This class manages the classpath of the application adding the generated_bin
 * folder to it. This allows the Java Compiler to use the new auxiliary members
 * in the instrumented classes.
 * 
 * @author Ignacio Lagartos
 *
 */
public final class ClassPathUtil {

	private static final String JAVA_HOME = "java.home";
	private static final String PROPERTY_FILE_NAME = "config.properties";

	private static List<File> classPath = null;

	private ClassPathUtil() {
	}

	/**
	 * Provides the modified classpath with generated_bin folder included
	 * 
	 * @return The classpath of the application
	 */
	public static List<File> getApplicationClassPath() {
		return classPath;
	}

	static {
		if (!new File(PROPERTY_FILE_NAME).exists()) {
			throw new RuntimeException(
					"Cannot found the config.properties file");
		}
		String propertie;
		try {
			propertie = FileUtils.getProperty(JAVA_HOME, PROPERTY_FILE_NAME);
		} catch (StructuralIntercessionException e) {
			throw new RuntimeException("Cannot obtain the propertie", e);
		}
		System.setProperty(JAVA_HOME, propertie);
		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		StandardJavaFileManager fileManager = compiler.getStandardFileManager(
				null, null, null);
		if (classPath == null) {
			Iterable<? extends File> iterable = fileManager
					.getLocation(StandardLocation.CLASS_PATH);
			@SuppressWarnings("unchecked")
			Iterator<File> iterator = (Iterator<File>) iterable.iterator();
			classPath = new ArrayList<File>();
			while (iterator.hasNext()) {
				File file = (File) iterator.next();
				if (file.getAbsolutePath().contains("\\bin")) {
					file = new File(
							(file.getParent() != null ? file.getParent() + "\\"
									: "") + PathConstants.MODIFIED_CLASS_PATH);
					classPath.add(0, file);
					continue;
				}
				classPath.add(file);
			}
		}
	}

}
