package jmplib.util;

import java.util.Map;

import jmplib.Evaluator;

/**
 * Interface needed to support global variables in the generated classes created
 * by eval methods in the {@link Evaluator} class.
 * 
 * @author Ignacio Lagartos
 *
 */
public interface EnvironmentSetUp {

	/**
	 * This method set the value of the global variables in the generated eval
	 * classes. It is important to use it before invoke the functional interface
	 * method, if not, some variables could have null values and do not work
	 * properly
	 * 
	 * @param environment
	 *            map that define the values of the global variables
	 */
	public void setEnvironment(Map<String, Object> environment);

}
