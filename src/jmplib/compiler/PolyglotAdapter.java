package jmplib.compiler;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import jmplib.exceptions.CompilationFailedException;
import jmplib.util.ClassPathUtil;
import jmplib.util.FileUtils;
import jmplib.util.JavaSourceFromString;
import jmplib.util.PathConstants;
import polyglot.main.Main.TerminationException;
import polyglot.util.ErrorInfo;
import polyglot.util.SilentErrorQueue;

/**
 * This class fits the functionality of Polyglot inside the library.
 * 
 * @author Ignacio Lagartos
 *
 */
public class PolyglotAdapter {

	private static boolean DEBUG = true;
	private static List<File> classPath = ClassPathUtil
			.getApplicationClassPath();

	/**
	 * Instrument with Polyglot the files provided
	 * 
	 * @param files
	 *            The source files of the new classes
	 * @return The files instrumented
	 * @throws CompilationFailedException
	 *             If any errors in the files
	 */
	public static JavaSourceFromString[] instrument(File... files)
			throws CompilationFailedException {
		String argPath = "";
		for (File file : classPath) {
			argPath += file.getAbsolutePath() + ';';
		}
		argPath = argPath.substring(0, argPath.length() - 1);
		// Make arguments for polyglot
		String[] args = new String[files.length + 6];
		args[0] = "-c";
		args[1] = "-extclass";
		args[2] = "polyglot.ext.jl7.JL7ExtensionInfo";
		args[3] = "-simpleoutput";
		args[4] = "-classpath";
		args[5] = argPath;
		for (int i = 0; i < files.length; i++) {
			args[i + 6] = files[i].getAbsolutePath();
		}
		SilentErrorQueue errorQueue = new SilentErrorQueue(100, "errors");
		JavaSourceFromString[] sources = null;
		try {
			sources = new polyglot.main.Main().start(args, errorQueue).toArray(
					new JavaSourceFromString[0]);

		} catch (TerminationException e) {
			String error = getError(errorQueue);
			throw new CompilationFailedException(
					"The compilation of the classes failed.\n" + error, error);
		}
		for (int i = 0; i < sources.length; i++) {
			String name = files[i].getName().replaceAll("\\.java", "");
			sources[i] = new JavaSourceFromString(name, sources[i].getCode(),
					files[i].getAbsolutePath().hashCode());
			if (DEBUG) {
				try {
					FileWriter writer = new FileWriter(files[i]);
					writer.write(sources[i].getCode());
					writer.close();
				} catch (IOException e) {
				}
			}
		}
		return sources;
	}

	/**
	 * Build the compilation errors
	 * 
	 * @param errorQueue
	 *            Errors from Polyglot
	 * @return Compilation error message
	 */
	private static String getError(SilentErrorQueue errorQueue) {
		String error = "";
		for (ErrorInfo errorInfo : errorQueue) {
			error += parseErrorInfo(errorInfo);
		}
		return error;
	}

	/**
	 * Extracts data from Polyglot error
	 * 
	 * @param errorInfo
	 *            Polyglot error
	 * @return line with the error data
	 */
	private static String parseErrorInfo(ErrorInfo errorInfo) {
		String message = errorInfo.getMessage();
		int line = errorInfo.getPosition().line();
		int startColumn = errorInfo.getPosition().column();
		int endColumn = errorInfo.getPosition().endColumn();
		String file = errorInfo.getPosition().file();
		String errorFormat = "\n- %s (%s: line %s, columns %s-%s)\n\t\t%s";
		String error = String.format(errorFormat, message,
				file.substring(file.lastIndexOf("\\") + 1), line, startColumn,
				endColumn, file);
		return error;
	}

	static {
		File polyglotFolder = new File(PathConstants.POLYGLOT_PATH);
		if (polyglotFolder.exists())
			FileUtils.deleteFile(polyglotFolder);
	}

}
