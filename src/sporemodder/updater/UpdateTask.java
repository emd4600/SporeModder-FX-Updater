package sporemodder.updater;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.concurrent.Task;

/**
 * Concurrent task that executes all the updating subtasks:
 * - `addFile`: Extracting mandatory files (they are always extracted).
 * - `addOptionalFile`: Extracting optional files (they are only extracted if they didn't exist in the destination folder).
 * - `modifyRegistry`: Adding entries into a name registry.
 * - `forcedModifyRegistry`: Replacing entries of a name registry.
 */
public class UpdateTask extends Task<Void> {
	
	private class FileEntry {
		String internalName;
		String outputName;
		boolean optional;
	}
	
	private final File destFolder;
	private final List<FileEntry> fileMap = new ArrayList<>();
	
	private final Map<String, String> registriesMap = new HashMap<>();
	// The entries here are always replaced
	private final Map<String, String> forcedRegistriesMap = new HashMap<>();
	
	/**
	 * Initializes an updater task, where files will be extracted and modified relative
	 * to the given `destFolder`, which should be the base SporeModder FX folder.
	 * @param destFolder
	 */
	public UpdateTask(File destFolder) {
		super();
		this.destFolder = destFolder;
	}
	
	/**
	 * Extracts the file `path` into the same path relative to the SMFX folder,
	 * replacing the file if it already existed.
	 * @param path Path to the source file, relative to the `resources` package, 
	 * and also path to the destination file, relative to SMFX base folder.
	 */
	public void addFile(String path) {
		addFile(path, path);
	}
	
	/**
	 * Extracts the file `internalName` into `outputPath`, replacing the file if it already existed.
	 * @param internalPath Path to the source file, relative to the `resources` package.
	 * @param outputPath Path to the destination file, relative to SMFX base folder.
	 */
	public void addFile(String internalPath, String outputPath) {
		FileEntry entry = new FileEntry();
		entry.internalName = internalPath;
		entry.outputName = outputPath;
		entry.optional = false;
		fileMap.add(entry);
	}
	
	/**
	 * Extracts the file `internalName` into `outputPath`, only if `outputPath` does not exist yet.
	 * If the output file already exists, nothing happens.
	 * @param internalName Path to the source file, relative to the `resources` package.
	 * @param outputPath Path to the destination file, relative to SMFX base folder.
	 */
	public void addOptionalFile(String internalName, String outputPath) {
		FileEntry entry = new FileEntry();
		entry.internalName = internalName;
		entry.outputName = outputPath;
		entry.optional = true;
		fileMap.add(entry);
	}
	
	/**
	 * Adds all the entries of the file `internalName` to the registry file `outputPath`.
	 * All entries are added to the end of the file, no other modifications are done.
	 * @param internalPath Path to the source file, relative to the `resources` package.
	 * @param outputPath Path to the destination file, relative to SMFX base folder.
	 */
	public void modifyRegistry(String internalPath, String outputPath) {
		registriesMap.put(outputPath, internalPath);
	}
	
	/**
	 * Adds all entries of the file `internalName` to the registry file `outputPath`,
	 * REPLACING existing entries if there are collisions. That is: if any new entry already had 
	 * its hash or its name in the file, the old value will be deleted and replaced with the new one.
	 * @param internalPath Path to the source file, relative to the `resources` package.
	 * @param outputPath Path to the destination file, relative to SMFX base folder.
	 */
	public void forcedModifyRegistry(String internalPath, String outputPath) {
		forcedRegistriesMap.put(outputPath, internalPath);
	}
	
	/**
	 * Returns an input stream for a file stored in the `resources` package.
	 * @param internalPath Path to the source file, relative to the `resources` package.
	 * @return
	 */
	private InputStream getInternalStream(String internalPath) {
		return getClass().getResourceAsStream("/sporemodder/updater/resources/" + internalPath);
	}

	@SuppressWarnings("unlikely-arg-type")
	@Override
	protected Void call() throws Exception {
		int count = fileMap.size() + registriesMap.size();
		int done = 0;
		
		// Extract files
		for (FileEntry entry : fileMap) {
			File destFile = new File(destFolder, entry.outputName);
			if (entry.optional && destFile.exists()) continue;
			
			destFile.mkdirs();
			Files.copy(getInternalStream(entry.internalName), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
			
			this.updateProgress(done, count);
			++done;
		}
		
		for (Map.Entry<String, String> entry : registriesMap.entrySet()) {
			File regFile = new File(destFolder, entry.getValue());
			if (!regFile.exists()) continue;
			
			NameRegistry registry = new NameRegistry();
			registry.read(regFile);
			
			NameRegistry inputRegistry = new NameRegistry();
			try (InputStream input = getInternalStream(entry.getKey())) {
				inputRegistry.read(new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8)));
			}
			
			boolean addedBlankLine = false;
			
			try (BufferedWriter output = new BufferedWriter(new FileWriter(regFile, true))) {
				for (Map.Entry<Integer, String> hash : inputRegistry.names.entrySet()) 
				{
					if (registry.getName(hash.getKey()) == null) {
						if (!addedBlankLine) {
							output.newLine();
							addedBlankLine = true;
						}
						output.newLine();
						output.append(hash.getValue());
						
						if (hash.getValue().endsWith("~") || NameRegistry.fnvHash(hash.getValue()) != hash.getKey()) {
							output.append("\t0x");
							output.append(Integer.toHexString(hash.getKey()));
						}
					}
				}
				
				// If we added it it means there were new names
				if (addedBlankLine) {
					output.newLine();
				}
			}
		}
		
		for (Map.Entry<String, String> entry : forcedRegistriesMap.entrySet()) {
			File regFile = new File(destFolder, entry.getValue());
			if (!regFile.exists()) continue;
			
			NameRegistry registry = new NameRegistry();
			registry.read(regFile);
			
			NameRegistry inputRegistry = new NameRegistry();
			try (InputStream input = getInternalStream(entry.getKey())) {
				inputRegistry.read(new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8)));
			}
			
			for (Map.Entry<Integer, String> hash : inputRegistry.names.entrySet()) 
			{
				registry.hashes.remove(hash.getKey());
				registry.names.remove(registry.getName(hash.getKey()));
				registry.add(hash.getValue(), hash.getKey());
			}
			
			try (BufferedWriter output = new BufferedWriter(new FileWriter(regFile))) {
				registry.write(output, true);
			}
		}
		
		return null;
	}

}
