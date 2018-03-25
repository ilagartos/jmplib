package jmplib.agent.impl;

import static jmplib.util.PathConstants.ORIGINAL_SRC_PATH;
import static org.objectweb.asm.Opcodes.ASM5;

import java.io.File;

import jmplib.agent.AbstractTransformer;
import jmplib.agent.UpdaterAgent;
import jmplib.asm.visitor.ConstructorVisitor;
import jmplib.asm.visitor.InstanceFieldAccessMethodVisitor;
import jmplib.asm.visitor.NewVersionVisitor;
import jmplib.asm.visitor.StaticFieldAccessMethodVisitor;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

/**
 * This transformer modifies original classes to add new fields and methods to
 * support the functionality of the library .
 * 
 * @author Nacho
 *
 */
public class OriginalClassLoadTimeTransformer extends AbstractTransformer {

	/**
	 * Adds new fields and methods to the class to support the functionality of
	 * the library. Additionally, adds the class to the instrumentables
	 * collection inside the UpdaterAgent class.
	 */
	@Override
	protected byte[] transform(String className, Class<?> classBeingRedefined,
			byte[] classfileBuffer) {
		ClassReader reader = new ClassReader(classfileBuffer);
		ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
		NewVersionVisitor newVersion = new NewVersionVisitor(ASM5, writer);
		ConstructorVisitor constructorAnnotation = new ConstructorVisitor(
				ASM5, newVersion, false);
		StaticFieldAccessMethodVisitor accessMethod = new StaticFieldAccessMethodVisitor(
				ASM5, constructorAnnotation);
		InstanceFieldAccessMethodVisitor instanceAccesMethod = new InstanceFieldAccessMethodVisitor(
				ASM5, accessMethod);
		reader.accept(instanceAccesMethod, 0);
		UpdaterAgent.instrumentables.put(className.hashCode(), className);
		return writer.toByteArray();
	}

	/**
	 * It is aplicable when it is the first load of the class, the class is not
	 * a version class and there is a source file of the class inside the src
	 * specified folder.
	 */
	@Override
	protected boolean instrumentableClass(String className,
			Class<?> classBeingRedefined) {
		if (classBeingRedefined != null)
			return false;
		if (className.contains("_NewVersion_"))
			return false;
		if (!(new File(ORIGINAL_SRC_PATH + className + ".java").exists()))
			return false;
		return true;
	}

}
