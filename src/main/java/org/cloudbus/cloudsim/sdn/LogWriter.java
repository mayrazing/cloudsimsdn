/*
 * Title:        CloudSimSDN
 * Description:  SDN extension for CloudSim
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2017, The University of Melbourne, Australia
 */

package org.cloudbus.cloudsim.sdn;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

public class LogWriter {
	private PrintStream out = null;

	private static HashMap<String,LogWriter> map = new HashMap<String,LogWriter>();
	
	private LogWriter(String name) {
		out = openfile(name);
	}
	
	public static LogWriter getLogger(String name) {
		String exName = Configuration.workingDirectory + Configuration.experimentFolder + "/" + name;
		LogWriter writer = map.get(exName);
		if(writer != null)
			return writer;
		
		System.out.println("Creating logger..:" +exName);
		writer = new LogWriter(exName);
		map.put(exName, writer);
		return writer;
	}

	public void print(String s) {
		if(out == null)
			System.err.println("WorkloadResultWriter: "+s);
		else
			out.print(s);
	}
	
	public void printLine() {
		if(out == null)
			System.err.println("");
		else
			out.println();
	}
	
	public void printLine(String s) {
		out.println(s);
	}
		
	private PrintStream openfile(String name) {
		PrintStream out = null;
		try {
			out = new PrintStream(name);
		} catch (FileNotFoundException e){
			createFileDir(name);
			try {
				out = new PrintStream(name);
			} catch (IOException e1) {
				e1.printStackTrace();
				System.err.println("Error creating file: " + name);
			}
		}
        return out;
	}

	public static void createFileDir(String name) {
		// Set log file
		int indexSlash = name.lastIndexOf("/");
		if(indexSlash != -1) {
			String path_folder = name.substring(0, indexSlash + 1);
			Path path = Paths.get(path_folder);
			if(!Files.exists(path)) {
				try {
					Files.createDirectories(path);
				} catch (IOException e) {
					System.err.println("Error creating folder: " + path_folder);
				}
			}
		}
	}
	
	 public static String getExtension(String fullPath) {
	    int dot = fullPath.lastIndexOf(".");
	    return fullPath.substring(dot + 1);
	  }

	  public static String getBaseName(String fullPath) { // gets filename without extension
	    int dot = fullPath.lastIndexOf(".");
	    int sep = fullPath.lastIndexOf("/");
	    return fullPath.substring(sep + 1, dot);
	  }

	  public static String getPath(String fullPath) {
	    int sep = fullPath.lastIndexOf("/");
	    return fullPath.substring(0, sep);
	  }
}
