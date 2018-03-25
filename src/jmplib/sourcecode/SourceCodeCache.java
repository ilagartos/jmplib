package jmplib.sourcecode;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jmplib.annotations.AuxiliaryMethod;
import jmplib.annotations.NoRedirect;
import jmplib.asm.visitor.ClassCacherVisitor;
import jmplib.classversions.VersionClass;
import jmplib.exceptions.ClassNotEditableException;
import jmplib.exceptions.StructuralIntercessionException;
import jmplib.util.FileUtils;
import jmplib.util.PathConstants;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
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

/**
 * This class is a cache of the source code of the application classes.
 * 
 * @author Ignacio Lagartos
 * 
 */
public class SourceCodeCache {

	private static SourceCodeCache _instance = null;
	private static Map<Integer, ClassContent> cache = new HashMap<Integer, ClassContent>();
	private static final String PROPERTY_FILE_NAME = "config.properties";
	private static final String SOURCE_PATH = "source.path";

	/**
	 * The constructor creates the folder where the generated versions of the
	 * classes will be located.
	 */
	private SourceCodeCache() {
		// Pointing to gen folder
		File file = new File(PathConstants.MODIFIED_SRC_PATH);
		if (file.exists()) {
			// deleting the previous executions contents
			FileUtils.deleteFile(file);
		}
		// creating again
		file.mkdir();
	}

	/**
	 * Returns the unique instance of the class ready to use.
	 * 
	 * @return Returns the unique instance of the class.
	 */
	public static SourceCodeCache getInstance() {
		// Singleton pattern
		if (_instance == null) {
			_instance = new SourceCodeCache();
		}
		return _instance;
	}

	/**
	 * Obtains the {@link ClassContent} of the specified class from the cache.
	 * If this class content isn't in the cache, then it is added and returned.
	 * 
	 * @param clazz
	 *            to obtain the {@link ClassContent}
	 * @return the {@link ClassContent} of the class given
	 * @throws StructuralIntercessionException
	 */
	public ClassContent getClassContent(Class<?> clazz)
			throws StructuralIntercessionException {
		int hashcode = clazz.getName().hashCode();
		ClassContent classContent = cache.get(hashcode);
		if (classContent != null)
			return classContent;
		try {
			addClass(clazz);
		} catch (ClassNotEditableException e) {
			throw new StructuralIntercessionException(e.getMessage(), e);
		}
		return cache.get(hashcode);
	}

	/**
	 * Provides the current version of one class
	 * 
	 * @param className
	 *            The full name of the class
	 * @return The actual version of the class. If the class doesn't have
	 *         versions returns 0. If the class is not in the cache returns -1.
	 */
	public int getVersion(String className) {
		int hashcode = className.hashCode();
		ClassContent classContent = cache.get(hashcode);
		if (classContent != null)
			return classContent.getVersion();
		else
			return -1;
	}

	/**
	 * This method caches the class into the cache.
	 * 
	 * @param clazz
	 *            The class to be cached.
	 * @throws StructuralIntercessionException
	 * @throws ClassNotEditableException
	 *             When the class doesn't have accessible source code file.
	 */
	private void addClass(Class<?> clazz) throws ClassNotEditableException,
			StructuralIntercessionException {
		if (clazz.isInterface()) {
			throw new ClassNotEditableException("Interfaces are not editable");
		}
		// Loads the file
		File file = loadJavaFile(clazz);
		String newName = clazz.getSimpleName() + "_NewVersion_0";
		// gets the content and adding auxiliary methods and fields
		CompilationUnit unit = null;
		try {
			unit = JavaParser.parse(file);
		} catch (ParseException e) {
			throw new ClassNotEditableException("The class cannot be parsed", e);
		} catch (IOException e) {
			throw new ClassNotEditableException(
					"The class "
							+ clazz.getName()
							+ " cannot be modified because the source file is not accesible",
					e);
		}
		TypeDeclaration declaration = unit.getTypes().get(0);
		declaration.getMembers().addAll(getAuxiliaryDeclarations(clazz));
		if (declaration instanceof ClassOrInterfaceDeclaration) {
			ClassOrInterfaceType interfaceType = new ClassOrInterfaceType(
					VersionClass.class.getName());
			((ClassOrInterfaceDeclaration) declaration).getImplements().add(
					interfaceType);
		}

		/*
		 * Changes the name of the class to preliminary-version (version 0)
		 * name, this is important because this name will be changed by regex
		 * for each subsequent version
		 */
		String newContent = unit.toStringWithoutComments();
		newContent = newContent.replaceFirst(clazz.getSimpleName(), newName);
		newContent = newContent.replaceAll(
				"(private|public|protected|package)( )+"
						+ clazz.getSimpleName() + "\\(", "public " + newName
						+ "(");
		// Change superclass name
		newContent = newContent.replaceAll("(extends)( )+"
				+ clazz.getSuperclass().getSimpleName(), "extends "
				+ clazz.getSuperclass().getSimpleName() + "_NewVersion_0");
		// Creates the folders and the file with the example path
		File sourceFile = createFile(clazz, newName);
		// Creates the wrapper to store the information
		ClassContent classContent = new ClassContent();
		classContent.setContent(newContent);
		classContent.setUpdated(false);
		classContent.setPath(sourceFile.getAbsolutePath());
		classContent.setVersion(0);
		classContent.setClazz(clazz);
		// Caches it
		cache.put(clazz.getName().hashCode(), classContent);
	}

	/**
	 * Creates all auxiliary members to support the JMPlib functionality
	 * 
	 * @param clazz
	 *            The class to instrument
	 * @return The list of members needed to support JMPlib versioning
	 */
	private Collection<BodyDeclaration> getAuxiliaryDeclarations(Class<?> clazz) {
		Collection<BodyDeclaration> declarations = new ArrayList<BodyDeclaration>();
		// Auxiliary methods
		try {
			ClassReader reader = new ClassReader(Type.getInternalName(clazz));
			ClassCacherVisitor visitor = new ClassCacherVisitor(Opcodes.ASM4,
					clazz);
			reader.accept(visitor, 0);
			declarations.addAll(visitor.getDeclarations());
		} catch (IOException e) {
			e.printStackTrace();
		}
		// Auxiliary fields
		List<VariableDeclarator> variableDeclarators = new ArrayList<VariableDeclarator>();
		VariableDeclarator declarator = new VariableDeclarator(
				new VariableDeclaratorId("_oldVersion"));
		variableDeclarators.add(declarator);
		FieldDeclaration fieldOldVersion = new FieldDeclaration(
				Modifier.PUBLIC, new ClassOrInterfaceType(clazz.getName()),
				variableDeclarators);
		declarations.add(fieldOldVersion);
		// Getter and Setter
		MethodDeclaration getter = null, setter = null;
		ClassOrInterfaceType classOrInterfaceType = new ClassOrInterfaceType(
				clazz.getName());
		ClassOrInterfaceType voidClassOrInterfaceType = new ClassOrInterfaceType(
				void.class.getName());
		ClassOrInterfaceType objectClassOrInterfaceType = new ClassOrInterfaceType(
				Object.class.getName());
		List<AnnotationExpr> annotations = new ArrayList<AnnotationExpr>();
		annotations.add(new NormalAnnotationExpr(new NameExpr(
				AuxiliaryMethod.class.getName()),
				new ArrayList<MemberValuePair>()));
		annotations.add(new NormalAnnotationExpr(new NameExpr(NoRedirect.class
				.getName()), new ArrayList<MemberValuePair>()));
		getter = new MethodDeclaration(Modifier.PUBLIC, classOrInterfaceType,
				"get_OldVersion");
		getter.setAnnotations(annotations);
		List<Parameter> parameter = new ArrayList<Parameter>();
		parameter.add(new Parameter(objectClassOrInterfaceType,
				new VariableDeclaratorId("newValue")));
		setter = new MethodDeclaration(Modifier.PUBLIC,
				voidClassOrInterfaceType, "set_OldVersion", parameter);
		setter.setAnnotations(annotations);
		try {
			getter.setBody(JavaParser.parseBlock("{ return _oldVersion; }"));
			setter.setBody(JavaParser.parseBlock("{ _oldVersion = ("
					+ clazz.getName() + ")newValue; }"));
		} catch (ParseException e) {
			e.printStackTrace();
		}
		declarations.add(getter);
		declarations.add(setter);
		return declarations;
	}

	/**
	 * Returns the new java file with the path of the 0 version. This path will
	 * be use to generate the real version paths. On the other hand, this method
	 * creates the folders needed to place this version files.
	 * 
	 * @param clazz
	 *            The class which version is created.
	 * @param newName
	 *            The name of the new version (ie. MyClass_NewVersion_23)
	 * @return Return the {@link File} where the source code will be written.
	 * @throws IOException
	 *             If any I/O error occurs creating the file this exception is
	 *             thrown.
	 */
	private File createFile(Class<?> clazz, String newName) {
		/*
		 * Creating the folders that represent the packages of the class. This
		 * is needed to differ between classes with the same name.
		 */
		File folders = new File(PathConstants.MODIFIED_SRC_PATH
				+ clazz.getPackage().getName().replace('.', '\\'));
		folders.mkdirs();
		// Creating the source code file
		File sourceFile = new File(folders.getPath() + "\\" + newName + ".java");
		return sourceFile;
	}

	/**
	 * Return the java file of the given class
	 * 
	 * @param clazz
	 *            This is the class to obtain the source code file.
	 * @return Return a {@link File} with the source code of the class.
	 * @throws ClassNotEditableException
	 *             If the file doesn't exist the class isn't editable.
	 * @throws StructuralIntercessionException
	 */
	private File loadJavaFile(Class<?> clazz) throws ClassNotEditableException,
			StructuralIntercessionException {
		// getting classpath
		String classpath = FileUtils.getProperty(SOURCE_PATH,
				PROPERTY_FILE_NAME);
		// pointing to source code file of the class
		classpath += clazz.getName().replace('.', '\\') + ".java";
		File javaFile = new File(classpath);
		if (!javaFile.exists()) // If there isn't the file the class cannot be
								// edited
			throw new ClassNotEditableException(
					"The class "
							+ clazz.getName()
							+ " cannot be modified because the source file is not accesible");
		return javaFile;
	}

	/**
	 * Obtains all ClassContent
	 * 
	 * @return Collection of all ClassContent
	 */
	public Collection<ClassContent> getAll() {
		return cache.values();
	}

}
