/****************************************************************************
* Copyright (C) 2018 Eric Mor
*
* This file is part of SporeModder FX.
*
* SporeModder FX is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program.  If not, see <http://www.gnu.org/licenses/>.
****************************************************************************/

package sporemodder.updater;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A registry file is a list which assigns an integer hash value to a string, and vice versa.
 */
public class NameRegistry {
	// ~ names are in lowercase
	protected final Map<String, Integer> hashes = new LinkedHashMap<>();
	protected final Map<Integer, String> names = new LinkedHashMap<>();
	
	public void clear() {
		hashes.clear();
		names.clear();
	}
	
	/**
	 * Returns the name that is assigned to the given hash, or null if the hash is not assigned.
	 * @param hash The hash whose equivalent name will be returned.
	 * @return The equivalent name, or null.
	 */
	public String getName(int hash) {
		return names.get(hash);
	}
	
	/**
	 * Returns the hash that is assigned to the given name, or null if the name is not assigned. This
	 * does not calculate the hash, it only looks into the registry.
	 * @param name The name whose equivalent hash will be returned.
	 * @return The equivalent hash, or null.
	 */
	public Integer getHash(String name) {
		return hashes.get(name);
	}
	
	/**
	 * Adds a name-hash pair into this registry.
	 * @param name
	 * @param hash
	 */
	public void add(String name, int hash) {
		hashes.put(name, hash);
		names.put(hash, name);
	}
	
	/**
	 * Calculates the 32-bit FNV hash used by Spore for the given string.
	 * It is case-insensitive: the string is converted to lower-case before calculating the hash.
	 * @param string The string whose hash will be calculated.
	 * @return The equivalent hash.
	 */
	public static int fnvHash(String string) {
		char[] lower = string.toLowerCase().toCharArray();
        int rez = 0x811C9DC5;
        for (int i = 0; i < lower.length; i++) {
        	rez *= 0x1000193;
        	rez ^= lower[i];
        }
        return rez;
	}
	
	/**
	 * Returns the equivalent 32-bit signed integer parsed from the given string. The following formats are allowed:
	 *  <li><code>5309</code>: It is parsed as a decimal number, so 5309 is returned.
	 *  <li><code>0x6e62ba</code>: It is parsed as a hexadecimal number ignoring the <i>0x</i>, so 7234234 is returned.
	 *  <li><code>#6e62ba</code>: It is parsed as a hexadecimal number ignoring the <i>#</i>, so 7234234 is returned.
	 *  <li><code>b10011</code>: It is parsed as a binary number ignoring the <i>b</i>, so 19 is returned.
	 *  <li><code>$Creature</code>: The hash of '<i>Creature</i>' is returned, using the {@link #getFileHash(String)} method.
	 * @param str The string to decode into a number.
	 * @return The equivalent 32-bit signed integer (<code>int32</code>).
	 */
	public static int int32(String str) {
		int result = 0;
		
		if (str == null || str.length() == 0) {
			return 0;
		}
		
		if (str.startsWith("0x")) {
			result = Integer.parseUnsignedInt(str.substring(2), 16);
		}
		else if (str.startsWith("#")) {
			result = Integer.parseUnsignedInt(str.substring(1), 16);
		}
		else if (str.endsWith("b")) {
			result = Integer.parseUnsignedInt(str.substring(0, str.length() - 1), 2);
		}
		else {
			result = Integer.parseInt(str);
		}
		
		return result;
	}
	
	/**
	 * Processes a single line in the registry file, converting it to an entry in this class.
	 * @param str The line to be parsed.
	 */
	protected void parseEntry(String str) {
		// There are 1 or 2 strings: the name and, optionally, the hash.
		String[] strings = str.split("\t");
		String name = strings[0].trim();
		
		if (strings.length < 2) {
			int hash = fnvHash(name);
			names.put(hash, name);
		} 
		else {
			// Remove any trailing whitespaces
			String hashStr = strings[1].trim();
			int hash = int32(hashStr);
			
			if (name.endsWith("~")) {
				hashes.put(name.toLowerCase(), hash);
			}
			else {
				hashes.put(name, hash);
			}
			hashes.put(name, hash);
			names.put(hash, name);
		}
	}
	
	public void read(File file) throws IOException {
		try (BufferedReader in = new BufferedReader(new FileReader(file))) {
			read(in);
		}
	}
	
	public void read(BufferedReader in) throws IOException {
		String line;

		while ((line = in.readLine()) != null) {
			
			String str = line.split("//")[0].trim();
			
			if (str.length() == 0) continue;
			
			// Ignore
			if (str.startsWith("#")) {
				continue;
			}
			else {
				parseEntry(str);
			}
		}
	}
	
	public void write(BufferedWriter output, boolean forceHashes) throws IOException {
		for (Map.Entry<Integer, String> entry : names.entrySet()) {
			String name = entry.getValue();
			int hash = fnvHash(name);
			if (forceHashes || hash != entry.getKey()) {
				output.append(name + "\t0x" + Integer.toHexString(entry.getKey()));
			} else {
				output.append(name);
			}
			output.newLine();
		}
	}

	public boolean isEmpty() {
		return names.isEmpty() && hashes.isEmpty();
	}

	public Collection<String> getNames() {
		return names.values();
	}
}
