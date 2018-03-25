package jmplib.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation marks a method as a default method. A default method is a
 * redefinition of a superclass method and contains a call to the superclass
 * implementation.
 * 
 * @author Nacho
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
public @interface DefaultMethod {

}
