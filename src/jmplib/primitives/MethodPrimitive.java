package jmplib.primitives;

import java.util.Arrays;
import java.util.stream.Collectors;

import jmplib.asm.util.ASMUtils;
import jmplib.sourcecode.ClassContent;

/**
 * Superclass for all method primitives
 * 
 * @author Ignacio Lagartos
 *
 */
public abstract class MethodPrimitive extends AbstractPrimitive {

	protected Class<?> returnClass = null;
	protected Class<?>[] parameterClasses = null, exceptionClasses = null;
	protected String[] exceptions = null;
	protected int modifiers = 0;

	public MethodPrimitive(ClassContent classContent, Class<?> returnClass,
			Class<?>[] parameterClasses, Class<?>[] exceptions, int modifiers) {
		super(classContent);
		this.modifiers = modifiers;
		this.returnClass = returnClass;
		this.parameterClasses = parameterClasses;
		this.exceptionClasses = exceptions;
	}

	public MethodPrimitive(ClassContent classContent, Class<?> returnClass,
			Class<?>[] parameterClasses, int modifiers) {
		super(classContent);
		this.modifiers = modifiers;
		this.returnClass = returnClass;
		this.parameterClasses = parameterClasses;
	}

	public MethodPrimitive(ClassContent classContent, Class<?> returnClass,
			Class<?>[] parameterClasses) {
		super(classContent);
		this.returnClass = returnClass;
		this.parameterClasses = parameterClasses;
	}

	/**
	 * Creates the bytecode descriptor of the method
	 * 
	 * @return Descriptor
	 */
	protected String getDescriptor() {
		String descriptor = "(";
		descriptor = descriptor
				.concat(Arrays.stream(parameterClasses)
						.map(ASMUtils::getDescriptor)
						.collect(Collectors.joining())).concat(")")
				.concat(ASMUtils.getDescriptor(returnClass));
		return descriptor;
	}
	/**
	 * Creates the bytecode descriptor of the invoker method
	 * 
	 * @return Descriptor
	 */
	protected String getInvokerDescriptor() {
		String descriptor = "(".concat(ASMUtils.getDescriptor(classContent
				.getClazz()));
		descriptor = descriptor
				.concat(Arrays.stream(parameterClasses)
						.map(ASMUtils::getDescriptor)
						.collect(Collectors.joining())).concat(")")
				.concat(ASMUtils.getDescriptor(returnClass));
		return descriptor;
	}

}
