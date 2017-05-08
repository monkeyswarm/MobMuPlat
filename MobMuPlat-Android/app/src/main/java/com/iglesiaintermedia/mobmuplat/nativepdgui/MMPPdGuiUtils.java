package com.iglesiaintermedia.mobmuplat.nativepdgui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
        // key = obj box index, val = set of connection tuples (obj index, outlet index) that connect into that obj box.
        SparseArray<Set<String[]>> objectIndexToIncomingConnectionIndices = new SparseArray<Set<String[]>>();

        // Bookkeeping to generate extra receive objects for grabbing receive-name messages
        // key = obj index, val = receive name
        SparseArray<String> objectIndexToPatchReceiveName = new SparseArray<String>();

        // Track necessary send+receive additions to float/symbol atoms that have been de-named.
        // key = objec index, val = send or receive name that must be manually added, via objects.
        SparseArray<String> unshimmableObjectIndexToPatchReceiveName = new SparseArray<String>();
        SparseArray<String> unshimmableObjectIndexToPatchSendName = new SparseArray<String>();

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
                    // skip empty objects
                    /*if (line[1].equals("obj") && line.length < 5) {
                        continue; //Dangerous since this may throw off objIndex count in comparison to puredata
                    }*/
                    String objType = line[1].equals("obj") ? line[4] : line[1];
                    if (objTypeToSendRecIndices.containsKey(objType)) {
                        // floatatom, symbolatom, bng, tgl, nbox, etc....
                        Integer[] sendRecIndices = objTypeToSendRecIndices.get(objType);
                        int sendIndex = sendRecIndices[0].intValue();
                        int recIndex = sendRecIndices[1].intValue();

                        guiLine = guiAtomLine(line, objIndex, sendIndex, recIndex);

                        objectIndexToIncomingConnectionIndices.put(objIndex, new HashSet<String[]>());

                        // grab patch receive handle and store
                        String recHandle = line[recIndex];
                        if (recHandle!=null && !recHandle.equals("-") && !recHandle.equals("empty")) {
                            objectIndexToPatchReceiveName.put(objIndex, recHandle);
                        }

                        // special case for the "unshimables". De-name floatatom, symbolatom. Add additional connections in post-processing.
                        if (objType.equals("floatatom") || objType.equals("symbolatom")) {
                            // store objIndex and send/rec names
                            String sendHandle = line[sendIndex];
                            if (sendHandle!=null && !sendHandle.equals("-") ) {
                                unshimmableObjectIndexToPatchSendName.put(objIndex, sendHandle);
                            }
                            if (recHandle!=null && !recHandle.equals("-")) {
                                unshimmableObjectIndexToPatchReceiveName.put(objIndex, recHandle);
                            }

                            //de-name, substitute in '-',
                            String[] patchLineCopy = patchLine.clone();
                            patchLineCopy[sendIndex] = "-";
                            patchLineCopy[recIndex] = "-";
                            patchLine = patchLineCopy;
                        }


                    } else if (objType.equals("msg")) {
                        // track connections, add an empty set
                        objectIndexToIncomingConnectionIndices.put(objIndex, new HashSet<String[]>());
                        //
                        guiLine = guiMsgAtomLine(line, objIndex);
                    } else if (objType.equals("connect")) {
                        // assume all obj boxes at level 1 are created by this point of gettign connections at level 1
                        int connectionIndex = Integer.parseInt(line[4]); //failure will throw numberformatexception
                        Set<String[]> connectionSet = objectIndexToIncomingConnectionIndices.get(connectionIndex);
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
                if (line[0].equals("#X") &&
                        level == 1 &&
                        !line[1].equals("connect") &&
                        !line[1].equals("declare")) { //DEI do positive counting (line is obj), not negative (line is not connect/declare)
                    objIndex++;
                }
            }
        } catch (Exception e) {
            // catch out of bounds / number format errors, return as bad load.
            return null;
        }

        // Post-process the message boxes to connect to the message shims.
        for(int i = 0; i < objectIndexToIncomingConnectionIndices.size(); i++) {
            int objBoxToShimIndex = objectIndexToIncomingConnectionIndices.keyAt(i);

            //get handles in patch
            String receiveHandle = objectIndexToPatchReceiveName.get(objBoxToShimIndex);
            // extra insertions in the case of float/symbol send/rec names
            String unshimmableReceiveHandle = unshimmableObjectIndexToPatchReceiveName.get(objBoxToShimIndex);
            String unshimmableSendHandle = unshimmableObjectIndexToPatchSendName.get(objBoxToShimIndex);

            String shimSendHandle = String.format("%d-gui-rec", objBoxToShimIndex);
            String shimRecHandle = String.format("%d-gui-send", objBoxToShimIndex);

            String[] newPatchLine1 = new String[]{"#X", "obj", "0", "0", "s",shimSendHandle}; //maybe not necc if no outgoing?
            String[] newPatchLine2 = new String[]{"#X", "obj", "0", "0", "r",shimRecHandle}; //maybe not necc if no incoming?

            patchLines.add(newPatchLine1);
            patchLines.add(newPatchLine2);
            int newPatchObjLinesCount = 2; //TODO better numbering for this, e.g. assign integers at each step.

            // for all objects going into obj box, connect them to SEND shim
            Set<String[]> connectionSet = objectIndexToIncomingConnectionIndices.get(objBoxToShimIndex);
            for (String[] connectionTuple : connectionSet) {
                patchLines.add(new String[]{"#X", "connect", connectionTuple[0], connectionTuple[1],
                        String.format("%d", objIndex), "0" });
            }

            // connect RECEIVE shim to OBJ box
            patchLines.add(new String[]{"#X", "connect", String.format("%d", objIndex+1), "0",
                    String.format("%d", objBoxToShimIndex), "0"});


            //if there's a receive handle, send to that SEND shim too
            if (receiveHandle!=null) {
                String[] newPatchLine3 = new String[]{"#X", "obj", "0", "0", "r", receiveHandle }; //objIndex+2
                patchLines.add(newPatchLine3);
                patchLines.add(new String[]{"#X", "connect",
                        String.format("%d",objIndex+newPatchObjLinesCount), "0",
                        String.format("%d",objIndex), "0"});
                newPatchObjLinesCount++;
            }

            // if unshimmable send/rec handle.
            // connect recehve handle to original object
            if (unshimmableReceiveHandle!=null) {
                String[] newPatchLine4 = new String[]{"#X", "obj", "0", "0", "r", unshimmableReceiveHandle};
                patchLines.add(newPatchLine4);
                patchLines.add(new String[]{"#X","connect",
                        String.format("%d",objIndex+newPatchObjLinesCount),"0",
                        String.format("%d",objBoxToShimIndex),"0"});
                newPatchObjLinesCount++;
            }
            if (unshimmableSendHandle!=null) {
                String[] newPatchLine4 = new String[]{"#X", "obj", "0", "0", "s", unshimmableSendHandle};
                patchLines.add(newPatchLine4);
                patchLines.add(new String[]{"#X","connect",
                        String.format("%d",objBoxToShimIndex), "0",
                        String.format("%d",objIndex+newPatchObjLinesCount),"0" });
                newPatchObjLinesCount++;
            }

            // inc
            objIndex+=newPatchObjLinesCount;
        }

        List<String[]>[] result = (List<String[]>[])new ArrayList[2];
        result[0] = patchLines;
        result[1] = guiLines;
        return result;// new ArrayList<String[]>[]{patchLines, guiLines};

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
}
