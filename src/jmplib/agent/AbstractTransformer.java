package jmplib.agent;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

/**
 * Superclass of all transformers of the library. This class defines the
 * behaviour of all transformers.
 * 
 * @author Ignacio Lagartos
 *
 */
public abstract class AbstractTransformer implements ClassFileTransformer {

	/**
	 * Checks if the class is instrumentable and transforms it. If it is not
	 * instrumentable, it returns null
	 */
	@Override
	public byte[] transform(ClassLoader loader, String className,
			Class<?> classBeingRedefined, ProtectionDomain protectionDomain,
			byte[] classfileBuffer) throws IllegalClassFormatException {
		if (instrumentableClass(className, classBeingRedefined)) {
			byte[] bytes = transform(className, classBeingRedefined,
					classfileBuffer);
			return bytes;
		}
		return null;
	}

	/**
	 * Transforms the bytes of the class
	 * 
	 * @param className
	 *            The internal class name
	 * @param classBeingRedefined
	 *            The Class<?> object or nullF
	 * @param classfileBuffer
	 *            The bytes of the class
	 * @return
	 */
	protected abstract byte[] transform(String className,
			Class<?> classBeingRedefined, byte[] classfileBuffer);

	/**
	 * Checks if the class have to be transformed
	 * 
	 * @param className
	 *            The internal class name
	 * @param classBeingRedefined
	 *            The Class<?> object
	 * @return
	 */
	protected abstract boolean instrumentableClass(String className,
			Class<?> classBeingRedefined);


}
