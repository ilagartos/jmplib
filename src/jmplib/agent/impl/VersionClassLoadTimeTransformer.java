package jmplib.agent.impl;

import jmplib.agent.AbstractTransformer;
import jmplib.agent.UpdaterAgent;
import jmplib.asm.visitor.ConstructorVisitor;
import jmplib.asm.visitor.TransferStateVisitor;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

/**
 * This transformer instruments version classes adding annotations to its
 * constructors.
 * 
 * @author Ignacio Lagartos
 *
 */
public class VersionClassLoadTimeTransformer extends AbstractTransformer {

	/**
	 * Annotates the constructors of the class.
	 */
	@Override
	protected byte[] transform(String className, Class<?> classBeingRedefined,
			byte[] classfileBuffer) {
		ClassReader reader = new ClassReader(classfileBuffer);
		ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
		ConstructorVisitor annotation = new ConstructorVisitor(
				Opcodes.ASM5, writer, true);
		TransferStateVisitor transfer = new TransferStateVisitor(Opcodes.ASM5,
				annotation);
		reader.accept(transfer, 0);
		UpdaterAgent.instrumentables.put(className.hashCode(), className);
		return writer.toByteArray();
	}

	/**
	 * It is aplicable when it is the first load of the class and the class is a
	 * version class.
	 */
	@Override
	protected boolean instrumentableClass(String className,
			Class<?> classBeingRedefined) {
		if (classBeingRedefined != null)
			return false;
		if (!className.contains("_NewVersion_"))
			return false;
		return true;
	}

}
