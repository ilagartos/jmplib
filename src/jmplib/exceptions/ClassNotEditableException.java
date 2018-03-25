package jmplib.exceptions;

/**
 * This exception is thrown when a not editable class is used with the JMPlib
 * API.
 * 
 * @author Ignacio Lagartos
 * 
 */
public class ClassNotEditableException extends Exception {

	private static final long serialVersionUID = 5359832750401736477L;

	public ClassNotEditableException(String message) {
		super(message);
	}

	public ClassNotEditableException(String message, Throwable throwable) {
		super(message, throwable);
	}

}
