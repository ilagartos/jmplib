package jmplib.agent.impl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import jmplib.agent.AbstractTransformer;
import jmplib.agent.UpdaterAgent;
import jmplib.util.PathConstants;

import org.objectweb.asm.Opcodes;

/**
 * This transformer writes the modified bytecode in the modified classpath.
 * 
 * @author Ignacio Lagartos
 *
 */
public class ChangeWriterTransformer extends AbstractTransformer implements
		Opcodes {

	/**
	 * It is aplicable when it is the first load of the class and the class is
	 * inside the instrumentables collection inside the UpdaterAgent class.
	 */
	protected boolean instrumentableClass(String className,
			Class<?> classBeingRedefined) {
		if (classBeingRedefined == null
				&& UpdaterAgent.instrumentables.containsKey(className
						.hashCode()))
			return true;
		else
			return false;
	}

	/**
	 * Writes the modified bytecode in the modified classpath.
	 */
	@Override
	public byte[] transform(String className, Class<?> classBeingRedefined,
			byte[] classfileBuffer) {
		File file = new File(PathConstants.MODIFIED_CLASS_PATH + className
				+ ".class");
		updateClassFile(file, classfileBuffer);
		return classfileBuffer;
	}

	/**
	 * It saves the new bytes of the class in the new class path to allow the
	 * new compiled classes to reference the new members like _newVersion
	 * attribute.
	 * 
	 * @param file
	 *            The <type>File</type> .class of the corresponding class in the
	 *            new bin folder.
	 * @param bytes
	 *            The new bytes of the class that must be updated.
	 */
	private void updateClassFile(File file, byte[] bytes) {
		try {
			// Creating the folders needed
			file.getParentFile().mkdirs();
			// Creating the file if not exists
			file.createNewFile();
			// Saving the bytes
			FileOutputStream fos = new FileOutputStream(file);
			fos.write(bytes);
			fos.close();
		} catch (FileNotFoundException e) {
			throw new RuntimeException("The class generated class "
					+ "path hasn't all the class files", e);
		} catch (IOException e) {
			throw new RuntimeException(
					"An error happenend while updating the bytes", e);
		}
	}
}
