package com.iglesiaintermedia.mobmuplat.nativepdgui;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.iglesiaintermedia.mobmuplat.MainActivity;

import android.content.res.AssetManager;
import android.net.Uri;
import android.util.Log;
import android.util.SparseArray;

public class MMPPdGuiUtils {

	/// Return two-item tuple
	/// [0] = processed lines to be written as the pd patch to execute. This
	/// [1] = proccessed lines to generate to the MMP GUI
	public static List<String[]>[]proccessAtomLines(List<String[]> lines) {
		List<String[]> patchLines = new ArrayList<String[]>();
		List<String[]> guiLines = new ArrayList<String[]>();
		
		int level = 0;
		int objIndex = 0;

		// Bookeeping for message box connections
	    // key = message box index, val = set of connection tuples (obj index, outlet index) that connect into that message box.
		SparseArray<Set<String[]>> messageBoxIndexToIncomingConnectionIndices = new SparseArray<Set<String[]>>();
		
		// Bookkeeping for grabbing send/rec names
	    // key = linetype string, val = tuple of send and rec indices.
		Map<String, Integer[]> objTypeToSendRecIndices = new HashMap<String, Integer[]>();
		objTypeToSendRecIndices.put("floatatom", new Integer[]{Integer.valueOf(10), Integer.valueOf(9)});
		objTypeToSendRecIndices.put("symbolatom", new Integer[]{Integer.valueOf(10), Integer.valueOf(9)});
		objTypeToSendRecIndices.put("bng", new Integer[]{Integer.valueOf(9), Integer.valueOf(10)});
		objTypeToSendRecIndices.put("tgl", new Integer[]{Integer.valueOf(7), Integer.valueOf(8)});
		objTypeToSendRecIndices.put("nbx", new Integer[]{Integer.valueOf(11), Integer.valueOf(12)});
		objTypeToSendRecIndices.put("hsl", new Integer[]{Integer.valueOf(11), Integer.valueOf(12)});
		objTypeToSendRecIndices.put("vsl", new Integer[]{Integer.valueOf(11), Integer.valueOf(12)});
		objTypeToSendRecIndices.put("hradio", new Integer[]{Integer.valueOf(9), Integer.valueOf(10)});
		objTypeToSendRecIndices.put("vradio", new Integer[]{Integer.valueOf(9), Integer.valueOf(10)});
		//@"vu" : @[@(-1), @(7)] not yet supported
		//canvas ("cnv") doesn't need one
		
		try {
		for (String[] line : lines) {
			//DEI check line length.
			
			String[] patchLine = line;
			String[] guiLine = line;
			
			String lineType = line[1]; // canvas/restore/obj/connect/floatatom/symbolatom
			if (lineType.equals("canvas")) {
				level++;
			} else if(lineType.equals("restore")) {
		        level--;
		    } 
			// find different types of UI element in the top level patch
			else if (level == 1) {
		    	String objType = line[1].equals("obj") ? line[4] : line[1];
		    	if (objTypeToSendRecIndices.containsKey(objType)) {
		    		// floatatom, symbolatom, bng, tgl, nbox, etc....
		    		Integer[] sendRecIndices = objTypeToSendRecIndices.get(objType);
		    		int sendIndex = sendRecIndices[0].intValue();
		    		int recIndex = sendRecIndices[1].intValue();
		    		patchLine = shimAtomLine(line, objIndex, sendIndex, recIndex);
		    		guiLine = guiAtomLine(line, objIndex, sendIndex, recIndex);
		    	} else if (objType.equals("msg")) {
			          // track connections, add an empty set 
		    		messageBoxIndexToIncomingConnectionIndices.put(objIndex, new HashSet<String[]>());
			        //
		    		guiLine = guiMsgAtomLine(line, objIndex);
		    	} else if (objType.equals("connect")) {
		    		// assume all obj boxes at level 1 are created by this point of gettign connections at level 1
		    		int connectionIndex = Integer.parseInt(line[4]); //failure will throw numberformatexception
		    		Set<String[]> connectionSet = messageBoxIndexToIncomingConnectionIndices.get(connectionIndex); 
		    		//if dict contains (msg box index) as key
			        if (connectionSet != null) {
			        	connectionSet.add(new String[]{line[2], line[3]});//add tuple of obj index string, outlet index string
			        }
		    	} else {
		    		// text, other "obj"
		    	}
		    }
			patchLines.add(patchLine);
			guiLines.add(guiLine); // todo, not necc if level > 1?
			if (line[0].equals("#X") && level == 1 && !line[1].equals("connect")) { //DEI just line index...
		        objIndex++;
			}
		}
		} catch (Exception e) {
			// catch out of bounds / number format errors, return as bad load.
			return null;
		}
		
		// Post-process the message boxes to connect to the message shims.
		for(int i = 0; i < messageBoxIndexToIncomingConnectionIndices.size(); i++) {
			// create shim
			int msgBoxIndex = messageBoxIndexToIncomingConnectionIndices.keyAt(i);
			patchLines.add(new String[]{ "#X", "obj", "0", "0", "MMPPdGuiFiles/MMPPdGuiMessageShim",
					String.format("%d-gui-send", msgBoxIndex),
					String.format("%d-gui-rec", msgBoxIndex) });
			
			// for objects going into message box, connect them to shim as well
			Set<String[]> connectionSet = messageBoxIndexToIncomingConnectionIndices.get(msgBoxIndex);
			for (String[] connectionTuple : connectionSet) {
				patchLines.add(new String[]{"#X", "connect",
		                           connectionTuple[0],
		                           connectionTuple[1],
		                           String.format("%d", objIndex),
		                           "0"} );
			}
			
		    // connect shim to message box
		    patchLines.add(new String[]{ "#X", "connect", String.format("%d", objIndex),
		                         "0",
		                         String.format("%d", msgBoxIndex),
		                         "0"} );
		    // inc
		    objIndex++;
		  }

		 List<String[]>[] result = (List<String[]>[])new ArrayList[2];
		 result[0] = patchLines;
		 result[1] = guiLines;
		return result;// new ArrayList<String[]>[]{patchLines, guiLines}; DEI here
	
	}

	private static String[] shimAtomLine(String[] atomLine, int objIndex, int sendNameIndex, int recNameIndex) {
		//DEI check length
		List<String> result = new ArrayList<String>(Arrays.asList(atomLine).subList(0,4)); //copy first 4
		result.set(1,"obj"); //change floatatom,etc to obj.
		
		  // vu meter doesn't have a "send" - not yet supported
		  /*if ([atomLine[4] isEqualToString:@"vu"]) {
		    [result addObject:@"MMPPdGuiFiles/MMPPdGuiNoSetShim"];
		    [result addObject: [NSString stringWithFormat:@"%lu-gui-send", (unsigned long)index]];//send name
		    [result addObject: [NSString stringWithFormat:@"%lu-gui-rec", index]];
		    [result addObject: [self shimSanitizeAtom:@"empty"]];
		    [result addObject: [self shimSanitizeAtom:atomLine[recNameIndex]]];
		    return result;
		  }*/

		// Bang gets special blocking shim, all others respect "set" and get default shim
		if (atomLine[4].equals("bng")) {
			result.add("MMPPdGuiFiles/MMPPdGuiNoSetShim");
		} else {
			result.add("MMPPdGuiFiles/MMPPdGuiShim");
		}
		result.add(String.format("%d-gui-send", objIndex));//send name
		result.add(String.format("%d-gui-rec", objIndex));
		result.add(shimSanitizeAtom(atomLine[sendNameIndex]));
		result.add(shimSanitizeAtom(atomLine[recNameIndex])); //Check range
		return result.toArray(new String[result.size()]); //conversion to typed array
	}
	
	private static String shimSanitizeAtom(String atom) {
		if (atom.charAt(atom.length()-1) == ',') { //strip off trailing comma (from floatatom line)
			return atom.substring(0,atom.length()-2); 
		} else {
			return atom;
		}
	}

	private static String[] guiAtomLine(String[] atomLine, int guiIndex, int sendNameIndex, int recNameIndex) {
		//DEI check length
		String[] result = atomLine.clone();
		result[sendNameIndex] = String.format("%d-gui-send", guiIndex);//send name
		result[recNameIndex] = String.format("%d-gui-rec", guiIndex);
		return result;
	}
	
	private static String[] guiMsgAtomLine(String[] atomLine, int guiIndex) {
		//DEI check length
		List<String> result = new ArrayList<String>(Arrays.asList(atomLine));
		// TODO Sanitize any \$1
		// append send/rec names to line
		result.add(String.format("%d-gui-send", guiIndex));//send name
		result.add(String.format("%d-gui-rec", guiIndex));
		return result.toArray(new String[result.size()]); //conversion to typed array
	}

	/// Call before opening a pd gui. Ensure that the Documents folder with the shim files exists.
	public static void maybeCreatePdGuiFolderAndFiles(AssetManager assetManager, boolean shouldForce) {
		String pdGuiDirectoryName = "MMPPdGuiFiles";
		File pdGuiDirectoryFile = new File(MainActivity.getDocumentsFolderPath(), pdGuiDirectoryName);
		// If folder doesn't exist, create
		if (!pdGuiDirectoryFile.exists()) {
			pdGuiDirectoryFile.mkdir();
		}
		//
		String[] pdGuiPatchFilenames = new String[]{"MMPPdGuiShim.pd", "MMPPdGuiNoSetShim.pd", "MMPPdGuiMessageShim.pd"};
		for (String filename : pdGuiPatchFilenames) {
			String assetFilename = "PdGui"+File.separator+filename;
			File patchDestFile = new File(pdGuiDirectoryFile, filename);
			try {
				InputStream in = assetManager.open(assetFilename);
				OutputStream out = new FileOutputStream(patchDestFile);
				byte[] data = new byte[in.available()];
				in.read(data);
				out.write(data);
				in.close();
				out.close();
				//Log.i(TAG, "Copied file: "+assetFilename);
			} catch (FileNotFoundException e) {
				Log.i("MMPPdGuiUtils", "Unable to copy file: "+e.getMessage());
			} catch (IOException e) {
				Log.i("MMPPdGuiUtils", "Unable to copy file: "+e.getMessage());
			} 
		}
	}	
}
