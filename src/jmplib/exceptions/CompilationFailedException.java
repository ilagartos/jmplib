package jmplib.exceptions;

/**
 * This exception is thrown when the class to compile have errors in the code.
 * The specific errors can be seen through getCompilationError method.
 * 
 * @author Ignacio Lagartos
 * 
 */
public class CompilationFailedException extends Exception {

	private static final long serialVersionUID = 5998388867348030689L;

	private String compilationError;

	public CompilationFailedException(String message, String compilationError) {
		super(message);
		this.compilationError = compilationError;
	}

	/**
	 * Returns the compilation errors that occurs when the application try to
	 * compile a class.
	 * 
	 * @return An String with the errors.
	 */
	public String getCompilationError() {
		return compilationError;
	}

}
