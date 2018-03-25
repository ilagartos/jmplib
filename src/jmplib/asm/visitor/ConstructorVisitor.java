package jmplib.asm.visitor;

import java.lang.reflect.Modifier;

import jmplib.annotations.NoRedirect;
import jmplib.asm.util.ASMUtils;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;

/**
 * This visitor annotates the constructors of the class.
 * 
 * @author Ignacio Lagartos
 *
 */
public class ConstructorVisitor extends ClassVisitor implements Opcodes {

	private Type ownerType;
	private boolean abstractClass = false;
	private boolean isVersion;

	public ConstructorVisitor(int api, ClassVisitor visitor, boolean isVersion) {
		super(api, visitor);
		this.isVersion = isVersion;
	}

	public ConstructorVisitor(int api, boolean isVersion) {
		super(api);
		this.isVersion = isVersion;
	}

	@Override
	public void visit(int version, int access, String name, String signature,
			String superName, String[] interfaces) {
		ownerType = Type.getType(name);
		abstractClass = Modifier.isAbstract(access);
		super.visit(version, access, name, signature, superName, interfaces);
	}

	/**
	 * Adds {@link NoRedirect} annotation to each constructor
	 */
	@Override
	public MethodVisitor visitMethod(int access, String name, String desc,
			String signature, String[] exceptions) {
		if (name.equals("<clinit>")) {
			MethodVisitor mv = super.visitMethod(access, name, desc, signature,
					exceptions);
			mv.visitAnnotation(ASMUtils.getDescriptor(NoRedirect.class), true);
			mv.visitEnd();
			return mv;
		}
		if (name.equals("<init>")) {
			MethodVisitor mv = super.visitMethod(access, name, desc, signature,
					exceptions);
			mv.visitAnnotation(ASMUtils.getDescriptor(NoRedirect.class), true);
			if (abstractClass || isVersion) {
				mv.visitEnd();
				return mv;
			} else {
				AdviceAdapter adapter = new ConstructorBodyInstrumentor(ASM5,
						ownerType, access, name, desc, mv);
				return adapter;
			}
		}
		return super.visitMethod(access, name, desc, signature, exceptions);
	}
}
