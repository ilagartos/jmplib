package jmplib.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.EnumSet;
import java.util.Properties;

import jmplib.exceptions.StructuralIntercessionException;

/**
 * Helper class to operate with {@link File}
 * 
 * @author Nacho
 *
 */
public class FileUtils {

	/**
	 * Deletes all files provided
	 * 
	 * @param files
	 *            Array of files
	 */
	public static void deleteFile(File... files) {
		for (File file : files) {
			deleteFile(file);
		}
	}

	/**
	 * Delete a File. If it is a directory then, deletes all subitems recusively
	 * 
	 * @param file
	 *            The file to delete
	 */
	public static void deleteFile(File file) {
		if (file.isDirectory()) {
			File[] files = file.listFiles();
			for (File subFile : files) {
				deleteFile(subFile);
			}
			file.delete();
		} else {
			file.delete();
		}
	}

	/**
	 * Copy one directory into another
	 * 
	 * @param source
	 *            The directory to be copied
	 * @param destination
	 *            The destination of the copy
	 * @throws StructuralIntercessionException
	 */
	public static void copyDirectory(File source, File destination)
			throws StructuralIntercessionException {
		final Path origin = source.toPath();
		final Path target = destination.toPath();
		try {
			Files.walkFileTree(source.toPath(),
					EnumSet.of(FileVisitOption.FOLLOW_LINKS),
					Integer.MAX_VALUE, new SimpleFileVisitor<Path>() {

						@Override
						public FileVisitResult preVisitDirectory(Path dir,
								BasicFileAttributes attrs) throws IOException {
							Path targetdir = target.resolve(origin
									.relativize(dir));
							try {
								Files.copy(dir, targetdir);
							} catch (FileAlreadyExistsException e) {
								if (!Files.isDirectory(targetdir))
									throw e;
							}
							return super.preVisitDirectory(dir, attrs);
						}

						@Override
						public FileVisitResult visitFile(Path file,
								BasicFileAttributes attrs) throws IOException {
							Files.copy(file,
									target.resolve(origin.relativize(file)));
							return super.visitFile(file, attrs);
						}
					});
		} catch (IOException e) {
			throw new StructuralIntercessionException(
					"An error occurs when trying to copy the directories", e);
		}
	}

	/**
	 * Obtains a property of on properties file
	 * 
	 * @param property
	 *            The propierty name
	 * @param fileName
	 *            The file name
	 * @return The value of the property
	 * @throws StructuralIntercessionException
	 */
	public static String getProperty(String property, String fileName)
			throws StructuralIntercessionException {
		Properties properties = new Properties();
		String propertyValue = null;
		try {
			properties.load(new FileInputStream(fileName));
			propertyValue = properties.getProperty(property);
		} catch (Exception e) {
			throw new StructuralIntercessionException(
					"The properties file cannot be obtained", e);
		}
		return propertyValue;
	}

}
