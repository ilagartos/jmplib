package jmplib.asm.visitor;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;

import jmplib.asm.util.ASMUtils;
import jmplib.classversions.VersionClass;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

/**
 * This visitor adds _newVersion field to the class.
 * 
 * @author Ignacio Lagartos
 *
 */
public class VersionInterfaceVisitor extends ClassVisitor implements Opcodes {

	

	public VersionInterfaceVisitor(int api, ClassVisitor visitor) {
		super(api, visitor);
	}

	public VersionInterfaceVisitor(int api) {
		super(api);
	}

	@Override
	public void visit(int arg0, int arg1, String arg2, String arg3,
			String arg4, String[] arg5) {
		if (Modifier.isInterface(arg1)) {
			super.visit(arg0, arg1, arg2, arg3, arg4, arg5);
		}
		List<String> interfaces = Arrays.asList(arg5);
		interfaces.add(ASMUtils.getInternalName(VersionClass.class));
		super.visit(arg0, arg1, arg2, arg3, arg4, interfaces.toArray(new String[0]));
	}

}
