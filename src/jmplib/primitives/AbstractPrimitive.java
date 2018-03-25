package jmplib.primitives;

import java.util.HashSet;
import java.util.Set;

import jmplib.exceptions.StructuralIntercessionException;
import jmplib.sourcecode.ClassContent;
import jmplib.sourcecode.SourceCodeCache;
import jmplib.util.InheritanceTables;

/**
 * The superclass of all primitives. This class defines the behaviour of all
 * primitives and made the common operations to all of them.
 * 
 * @author Ignacio Lagartos
 *
 */
public abstract class AbstractPrimitive implements Primitive {

	protected Set<ClassContent> modifiedClasses = new HashSet<ClassContent>();

	protected ClassContent classContent = null;
	protected Class<?> clazz = null;

	public AbstractPrimitive(ClassContent classContent) {
		this.classContent = classContent;
		this.clazz = classContent.getClazz();
	}

	/**
	 * Increases the version number and updates the cached code to a new
	 * version. These changes are done for all of the classes in the inheritance
	 * tree of the modified class, so, all of the tree evolves to a new version.
	 */
	protected void updateVersion() {
		modifiedClasses.add(classContent);
		if (classContent.isUpdated()) {
			int newVersionNumber = classContent.getVersion();
			int oldVersionNumber = newVersionNumber - 1;

			// Update the className
			String content = classContent.getContent();
			content = changeVersion(content, clazz, oldVersionNumber,
					newVersionNumber);
			classContent.setContent(content);

			return;
		}
		ClassContent top = getTopSuperClass(classContent);
		modifiedClasses.add(top);

		// Update the version number
		int oldVersionNumber = top.getVersion();
		int newVersionNumber = oldVersionNumber + 1;
		top.setVersion(newVersionNumber);

		// Update the className
		String content = top.getContent();
		content = changeVersion(content, top.getClazz(), oldVersionNumber,
				newVersionNumber);
		top.setContent(content);

		// Update subclasses
		for (Class<?> subClass : InheritanceTables
				.getSubclasses(top.getClazz())) {
			try {
				updateVersion(subClass, top.getClazz().getSimpleName()
						+ "_NewVersion_" + newVersionNumber);
			} catch (StructuralIntercessionException e) {
				// Next class
			}
		}

		// Update the path
		String path = top.getPath();
		path = path.replaceAll("(_)(\\d+)(.java)", "_" + newVersionNumber
				+ ".java");
		top.setPath(path);
		// ClassContent updated
		top.setUpdated(true);
	}

	/**
	 * Updates the class and subclasses provided
	 * 
	 * @param clazz
	 *            The class to evolve
	 * @param superclassName
	 *            The new superclass
	 * @throws StructuralIntercessionException
	 */
	private void updateVersion(Class<?> clazz, String superclassName)
			throws StructuralIntercessionException {
		SourceCodeCache classEditor = SourceCodeCache.getInstance();
		ClassContent classContent = classEditor.getClassContent(clazz);
		if (classContent.isUpdated()) {
			// Update the superclass when the classContent is already updated
			String content = classContent.getContent();
			content = classContent.getContent().replaceFirst(
					"extends(\\s+)((\\w|\\.)*)(\\s*)",
					"extends " + superclassName + " ");
			classContent.setContent(content);
			return;
		}
		modifiedClasses.add(classContent);
		// Update the version number
		int oldVersionNumber = classContent.getVersion();
		int newVersionNumber = oldVersionNumber + 1;
		classContent.setVersion(newVersionNumber);
		String content = classContent.getContent();
		// Update the className
		content = changeVersion(content, clazz, oldVersionNumber,
				newVersionNumber);
		// Update the superclass
		content = content.replaceFirst("extends(\\s+)((\\w|\\.)*)(\\s*)",
				"extends " + superclassName + " ");
		classContent.setContent(content);
		// Update the path
		String path = classContent.getPath();
		path = path.replaceAll("(_)(\\d+)(.java)", "_" + newVersionNumber
				+ ".java");
		classContent.setPath(path);
		// Update subclasses
		for (Class<?> subClass : InheritanceTables.getSubclasses(clazz)) {
			updateVersion(subClass, clazz.getSimpleName() + "_NewVersion_"
					+ newVersionNumber);
		}
		// ClassContent updated
		classContent.setUpdated(true);
	}

	/**
	 * Reverts all changes done by {@code updateVersion}
	 */
	protected void undoUpdateVersion() {
		modifiedClasses.add(classContent);
		if (!classContent.isUpdated()) {
			return;
		}
		ClassContent top = getTopSuperClass(classContent);
		// Update the version number
		int newVersionNumber = top.getVersion();
		int oldVersionNumber = newVersionNumber - 1;
		top.setVersion(oldVersionNumber);
		String content = top.getContent();
		// Update the className
		content = changeVersion(content, top.getClazz(), newVersionNumber,
				oldVersionNumber);
		// if (VersionTables.hasNewVersion(clazz.getSuperclass())) {
		// Class<?> newSuperclass = VersionTables.getNewVersion(clazz
		// .getSuperclass());
		// content.replaceFirst("extends(\\s+)((\\w|\\.)*)(\\s*)", "extends "
		// + newSuperclass.getName() + " ");
		// }
		// Update the path
		top.setContent(content);
		String path = top.getPath();
		path = path.replaceAll("(_)(\\d+)(.java)", "_" + oldVersionNumber
				+ ".java");
		top.setPath(path);
		// Update subclasses
		for (Class<?> subClass : InheritanceTables
				.getSubclasses(top.getClazz())) {
			try {
				undoUpdateVersion(subClass, top.getClazz().getSimpleName()
						+ "_NewVersion_" + oldVersionNumber);
			} catch (StructuralIntercessionException e) {
				// next class
			}
		}
		// ClassContent updated
		top.setUpdated(false);
	}

	/**
	 * Reverts the changes done by {@code updateVersion}
	 * 
	 * @param clazz
	 *            The class to revert
	 * @param superclassName
	 *            The old superclass name
	 * @throws StructuralIntercessionException
	 */
	private void undoUpdateVersion(Class<?> clazz, String superclassName)
			throws StructuralIntercessionException {
		SourceCodeCache classEditor = SourceCodeCache.getInstance();
		ClassContent classContent = classEditor.getClassContent(clazz);
		if (!classContent.isUpdated()) {
			// Update the superclass when the classContent is already updated
			String content = classContent.getContent();
			content = classContent.getContent().replaceFirst(
					"extends(\\s+)((\\w|\\.)*)(\\s*)",
					"extends " + superclassName + " ");
			classContent.setContent(content);
			return;
		}
		modifiedClasses.add(classContent);
		// Update the version number
		int newVersionNumber = classContent.getVersion();
		int oldVersionNumber = newVersionNumber - 1;
		classContent.setVersion(oldVersionNumber);
		String content = classContent.getContent();
		// Update the className
		content = changeVersion(content, clazz, newVersionNumber,
				oldVersionNumber);
		// Update the superclass
		content = content.replaceFirst("extends(\\s+)((\\w|\\.)*)(\\s*)",
				"extends " + superclassName + " ");
		classContent.setContent(content);
		// Update the path
		String path = classContent.getPath();
		path = path.replaceAll("(_)(\\d+)(.java)", "_" + oldVersionNumber
				+ ".java");
		classContent.setPath(path);
		// Update subclasses
		for (Class<?> subClass : InheritanceTables.getSubclasses(clazz)) {
			undoUpdateVersion(subClass, clazz.getSimpleName() + "_NewVersion_"
					+ oldVersionNumber);
		}
		// ClassContent updated
		classContent.setUpdated(false);
	}

	/**
	 * Changes the version of the class in the source code.
	 * 
	 * @param content
	 *            The source code
	 * @param clazz
	 *            The original class
	 * @param from
	 *            Actual version number of the source code
	 * @param to
	 *            New version number
	 * @return Source code updated
	 */
	private String changeVersion(String content, Class<?> clazz, int from,
			int to) {
		// Update the className
		content = content.replaceAll(clazz.getSimpleName() + "_NewVersion_"
				+ from, clazz.getSimpleName() + "_NewVersion_" + to);
		return content;
	}

	/**
	 * Obtains the final superclass
	 * 
	 * @param classContent
	 *            The {@link ClassContent} of the class
	 * @return The {@link ClassContent} of the top superclass
	 */
	private ClassContent getTopSuperClass(ClassContent classContent) {
		if (classContent.getClazz().getSuperclass() == null)
			return classContent;
		ClassContent superclassContent = null;
		try {
			superclassContent = SourceCodeCache.getInstance().getClassContent(
					classContent.getClazz().getSuperclass());
			return getTopSuperClass(superclassContent);
		} catch (StructuralIntercessionException e) {
			return classContent;
		}
	}

	/**
	 * Reverts all changes
	 */
	@Override
	public void undo() throws StructuralIntercessionException {
		undoUpdateVersion();
		undoPrimitive();
	}

	/**
	 * Do changes in the class and update the version number
	 */
	@Override
	public Set<ClassContent> execute() throws StructuralIntercessionException {
		executePrimitive();
		updateVersion();
		return modifiedClasses;
	}

	/**
	 * Method that each primitive have to override to implement its own
	 * funtionality.
	 * 
	 * @throws StructuralIntercessionException
	 *             If there are any error
	 */
	protected abstract void executePrimitive()
			throws StructuralIntercessionException;

	/**
	 * Method that each primitive have to override to revert its own changes
	 * over the ClassContent.
	 * 
	 * @throws StructuralIntercessionException
	 *             If there are any error
	 */
	protected abstract void undoPrimitive()
			throws StructuralIntercessionException;

	/**
	 * Returns if the primitive is safe or not. If it isn't safe, the
	 * PrimitiveExecutor hace to send all clases to recompile for type error
	 * detection
	 */
	@Override
	public boolean isSafe() {
		return true;
	}

}
