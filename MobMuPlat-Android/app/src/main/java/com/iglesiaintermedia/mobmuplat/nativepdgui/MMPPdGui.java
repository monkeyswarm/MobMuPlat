package com.iglesiaintermedia.mobmuplat.nativepdgui;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.view.View;

public class MMPPdGui {
    public final ArrayList<Widget> widgets;
    private boolean inLevel2CanvasShowingArray;

    public MMPPdGui() {
        super();
        widgets = new ArrayList<Widget>();
    }

    public void buildUI(Context context, List<String[]> atomlines, float scale) {
        //ArrayList<String> canvases = new ArrayList<String>();
        if (atomlines.size() == 0 || atomlines.get(0).length < 6) {
            return; //alert
        }

        String[] firstLine = atomlines.get(0);
        int fontSize = Integer.parseInt(firstLine[6]); // font size loaded from patch
        // pixel size of original pd patch
        int patchWidth = Integer.parseInt(firstLine[4]);
        int patchHeight = Integer.parseInt(firstLine[5]);

        int level = 0;

        for (int lineIndex = 0; lineIndex<atomlines.size();lineIndex++) {
            String[] line = atomlines.get(lineIndex);

            if (line.length >= 4) {
                // find canvas begin and end lines
                if (line[1].equals("canvas")) {
                    level += 1;
                } else if (line[1].equals("restore")) {
                    //canvases.remove(0);
                    level -= 1;
                    if (level == 1 && !inLevel2CanvasShowingArray) {
                        // render object [pd name]
                        widgets.add(new ObjectBox(context, line, scale, fontSize));
                    }
                    inLevel2CanvasShowingArray = false;
                } else if (line[1].equals("array") && level == 2) {
                    inLevel2CanvasShowingArray = true;
                    //expect
                    // #X array <arrayname> 100 float 3; (this line) last element is bit mask of save/no save and draw type (poly,points,bez)
                    // #A <values, if save contents is on>
                    // #X coords 0 1 100 -1 300 140 1 0 0;
                    // #X restore 8 17 graph;
                    boolean willHaveSaveData = (Integer.parseInt(line[5]) & 0x1) == 1;
                    // check line count
                    int expectedSubsequentLineCount = willHaveSaveData ? 3 : 2;
                    if (atomlines.size() <= lineIndex + expectedSubsequentLineCount) {
                        continue;
                    }
                    String[] arrayValueLine = willHaveSaveData ? atomlines.get(lineIndex + 1) : null;
                    String[] arrayCoordsLine = willHaveSaveData ?
                            atomlines.get(lineIndex + 2) :
                            atomlines.get(lineIndex + 1);
                    String[] arrayRestoreLine = willHaveSaveData ?
                            atomlines.get(lineIndex + 3) :
                            atomlines.get(lineIndex + 2);
                    ArrayWidget arrayWidget = new ArrayWidget(context, line, arrayValueLine, arrayCoordsLine, arrayRestoreLine, scale, fontSize);
                    widgets.add(arrayWidget);
                } else if (level == 1) { // find different types of UI element in the top level patch
                    if (line.length >= 2) {
                        // builtin pd things
                        if (line[1].equals("text")) {
                            widgets.add(new Comment(context, line, scale, fontSize));
                        } else if (line[1].equals("floatatom")) {
                            widgets.add(new NumberBox(context, line, scale, fontSize));
                        } else if (line[1].equals("symbolatom")) {
                            widgets.add(new Symbol(context, line, scale, fontSize));
                        } else if (line[1].equals("msg")) {
                            widgets.add(new MessageBox(context, line, scale, fontSize));
                        } else if (line.length >= 5 && line[1].equals("obj")) {
                            // pd objects
                            if (line[4].equals("vsl")) {
                                widgets.add(new Slider(context, line, scale, false));
                            } else if (line[4].equals("hsl")) {
                                widgets.add(new Slider(context, line, scale, true));
                            } else if (line[4].equals("vradio")) {
                                widgets.add(new Radio(context, line, scale, false));
                            } else if (line[4].equals("hradio")) {
                                widgets.add(new Radio(context, line, scale, true));
                            } else if (line[4].equals("tgl")) {
                                widgets.add(new Toggle(context, line, scale));
                            } else if (line[4].equals("bng")) {
                                widgets.add(new Bang(context, line, scale));
                            } else if (line[4].equals("nbx")) {
                                widgets.add(new NumberBox2(context, line, scale, fontSize));
                            } else if (line[4].equals("cnv")) {
                                widgets.add(new CanvasRect(context, line, scale));
                            } else {
                                //other "obj"
                                widgets.add(new ObjectBox(context, line, scale, fontSize));
                            }
                        } //connect
                    }
                }
            }
        }
        
    }
}
