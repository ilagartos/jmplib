package jmplib.primitives;

import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import jmplib.agent.UpdaterAgent;
import jmplib.classversions.VersionTables;
import jmplib.compiler.ClassCompiler;
import jmplib.compiler.PolyglotAdapter;
import jmplib.exceptions.CompilationFailedException;
import jmplib.exceptions.StructuralIntercessionException;
import jmplib.sourcecode.ClassContent;
import jmplib.sourcecode.ClassContentSerializer;
import jmplib.sourcecode.SourceCodeCache;
import jmplib.util.ClassPathUtil;
import jmplib.util.JavaSourceFromString;

public class PrimitiveExecutor {

	private Queue<Primitive> primitives = null;
	private Deque<Primitive> executedPrimitives = new ArrayDeque<Primitive>();
	private Set<ClassContent> classContents = new HashSet<ClassContent>();
	private boolean safeChange = true;

	public PrimitiveExecutor(Primitive primitive) {
		if (primitive == null) {
			throw new RuntimeException("The primitive cannot be null");
		}
		primitives = new LinkedList<Primitive>();
		primitives.add(primitive);
	}

	public PrimitiveExecutor(Queue<Primitive> primitives) {
		if (primitives.isEmpty()) {
			throw new RuntimeException("The primitive list cannot be empty");
		}
		this.primitives = primitives;
	}

	/**
	 * Executes all primitves in order. If an error happens all primitives are
	 * undone in inverse order. The new verions are compiled and the last
	 * versions are redireted to the new version.
	 * 
	 * @throws StructuralIntercessionException
	 */
	public synchronized void executePrimitives()
			throws StructuralIntercessionException {
		try {
			// Execute each primitive
			while (!primitives.isEmpty()) {
				Primitive primitive = primitives.poll();
				// Store the affected ClassContents
				classContents.addAll(primitive.execute());
				// Store the primitive in the stack of executed
				executedPrimitives.push(primitive);
				safeChange &= primitive.isSafe(); 
				// setClassContentsUpdated();
			}
			makeChangesEffective();
		} catch (StructuralIntercessionException e) {
			// If the primitive fails, undo the changes of all primitive
			// executed previously
			undoChanges();
			throw e;
		}
	}

	/**
	 * Serializes the classes in the file system for Polyglot instrumentation.
	 * When Polyglot finishes, the Java Compiler compile all the files and all
	 * classes are loaded, instrumented and old version classes are redirected
	 * to the new ones.
	 * 
	 * @throws StructuralIntercessionException
	 */
	private void makeChangesEffective() throws StructuralIntercessionException {
		try {
			// Serialize the ClassContents to files
			File[] files = null;
			if (safeChange)
				files = ClassContentSerializer.serialize(classContents);
			else
				files = ClassContentSerializer.serialize(SourceCodeCache
						.getInstance().getAll());
			// Instrument with Polyglot
			// files = PolyglotAdapter.instrument(files);
			JavaSourceFromString[] instrumented = PolyglotAdapter
					.instrument(files);
			if (!safeChange)
				instrumented = filterInstrumented(instrumented);
			// Compile the new java files
			ClassCompiler.getInstance().compile(
					ClassPathUtil.getApplicationClassPath(), instrumented);
			// Update the VersionTable with the new Classes
			updateVersionTable();
			// Update original class references
			updateReferences();
		} catch (IOException e) {
			throw new StructuralIntercessionException(e.getMessage(), e);
		} catch (CompilationFailedException e) {
			throw new StructuralIntercessionException(e.getCompilationError(),
					e);
		}
	}

	/**
	 * Filter the instrumented java files to compile only the new versions
	 * 
	 * @param instrumented
	 *            The instrumented files
	 * @return Filtered files
	 */
	private JavaSourceFromString[] filterInstrumented(
			JavaSourceFromString[] instrumented) {
		List<JavaSourceFromString> filtered = new ArrayList<JavaSourceFromString>();
		for (JavaSourceFromString file : instrumented) {
			for (ClassContent classContent : classContents) {
				if (classContent.getPath().hashCode() == file.getIdentifier()) {
					filtered.add(file);
				}
			}
		}
		return filtered.toArray(new JavaSourceFromString[0]);
	}

	/**
	 * Retransforms all version and original class to point the new version.
	 */
	private void updateReferences() {
		List<Class<?>> classes = new ArrayList<Class<?>>();
		for (ClassContent classContent : classContents) {
			Class<?> clazz = classContent.getClazz();
			classes.add(classContent.getClazz());
			classes.addAll(VersionTables.getVersions(classContent.getClazz()));
			classes.remove(VersionTables.getNewVersion(classContent.getClazz()));
			try {
				clazz.getField("_currentClassVersion").setInt(null,
						classContent.getVersion());
			} catch (IllegalArgumentException | IllegalAccessException
					| NoSuchFieldException | SecurityException e) {
				throw new RuntimeException(
						"Errors setting class version attribute", e);
			}
		}
		UpdaterAgent.updateClass(classes.toArray(new Class<?>[0]));
	}

	/**
	 * Updates version tables to add the new versions of each modified class
	 * 
	 * @throws StructuralIntercessionException
	 */
	private void updateVersionTable() throws StructuralIntercessionException {
		for (ClassContent classContent : classContents) {
			// Obtain the new class
			Class<?> newClazz;
			try {
				newClazz = Class.forName(classContent.getClazz().getPackage()
						.getName()
						+ "."
						+ classContent.getClazz().getSimpleName()
						+ "_NewVersion_" + classContent.getVersion());
			} catch (ClassNotFoundException e) {
				throw new StructuralIntercessionException(
						"The new version cannot be found", e);
			}
			// Set the new class version
			VersionTables.addNewVersion(classContent.getClazz(), newClazz);
			classContent.setUpdated(false);
		}
	}

	/**
	 * All executed primitives are undone by inverse order
	 * 
	 * @throws StructuralIntercessionException
	 */
	private void undoChanges() throws StructuralIntercessionException {
		while (!executedPrimitives.isEmpty()) {
			Primitive primitive = executedPrimitives.pop();
			primitive.undo();
		}
	}

}
