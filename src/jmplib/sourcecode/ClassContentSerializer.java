package jmplib.sourcecode;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Serializes {@link ClassContent} to {@link File}
 * 
 * @author Ignacio Lagartos
 *
 */
public class ClassContentSerializer {

	/**
	 * Serializes a collection of {@link ClassContent} to {@link File} array
	 * 
	 * @param classContents
	 *            Collection of {@link ClassContent}
	 * @return {@link File} array
	 * @throws IOException
	 */
	public static File[] serialize(Collection<ClassContent> classContents)
			throws IOException {
		List<File> files = new ArrayList<File>();
		for (ClassContent classContent : classContents) {
			files.add(serialize(classContent));
		}
		return files.toArray(new File[0]);
	}

	/**
	 * Serializes a {@link ClassContent} to {@link File}
	 * 
	 * @param classContents
	 *            {@link ClassContent} to serialize
	 * @return {@link File} with the serialized data
	 * @throws IOException
	 */
	public static File serialize(ClassContent classContent) throws IOException {
		File file = new File(classContent.getPath());
		file.createNewFile();
		FileWriter writer = new FileWriter(file, false);
		writer.write(classContent.getContent());
		writer.close();
		return file;
	}

}
