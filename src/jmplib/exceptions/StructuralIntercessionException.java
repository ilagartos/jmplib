package jmplib.exceptions;

/**
 * This exceptions is thrown when errors occurs while the library is performing
 * operations. The reason is usually stored in the cause field.
 * 
 * @author Ignacio Lagartos
 *
 */
public class StructuralIntercessionException extends Exception {

	private static final long serialVersionUID = 5590466639381456782L;

	public StructuralIntercessionException(String message) {
		super(message);
	}

	public StructuralIntercessionException(String message, Throwable cause) {
		super(message, cause);
	}

}
