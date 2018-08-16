package com.iglesiaintermedia.mobmuplat.nativepdgui;

import java.text.DecimalFormat;
import java.util.Arrays;

import org.puredata.core.PdBase;
import org.puredata.core.PdListener;
import org.puredata.core.utils.PdDispatcher;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.Log;
import android.widget.RelativeLayout;

public class Widget extends RelativeLayout implements PdListener {
    static PdDispatcher dispatcher = null;
    static Typeface defaultTypeFace;

    protected float scale;

    public RectF originalRect;
    protected RectF postLayoutInnerRect = new RectF();
    Paint paint;
    private float value = 0;
    boolean sendValueOnInit = false;
    String sendname = null;
    private String receiveName = null;
    String labelString = null;
    float[] labelpos = new float[2]; //TODO int?
    int labelfont=0;
    int labelsize=14; //push to iem widget umbrella
    Typeface font = Typeface.create("Courier", Typeface.BOLD);
    float[] textoffset = new float[2];

    protected int lineWidth;

    int bgcolor=0xFFFFFFFF, fgcolor=0xFF000000, labelcolor=0xFF000000;

    private static int IEM_GUI_MAX_COLOR = 30;
    private static int iemgui_color_hex[] = {
            16579836, 10526880, 4210752, 16572640, 16572608,
            16579784, 14220504, 14220540, 14476540, 16308476,
            14737632, 8158332, 2105376, 16525352, 16559172,
            15263784, 1370132, 2684148, 3952892, 16003312,
            12369084, 6316128, 0, 9177096, 5779456,
            7874580, 2641940, 17488, 5256, 5767248
    };

    public static void setDispatcher(PdDispatcher dispatcher) {
        Widget.dispatcher = dispatcher;
    }
    public static void setDefaultTypeface(Typeface typeface) {
        Widget.defaultTypeFace = typeface;
    }

    public Widget(Context context, float scale) {
        super(context);
        this.scale = scale;
        this.setBackgroundColor(Color.TRANSPARENT);//without this, no widgets are viewable. no idea why!
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setTypeface(defaultTypeFace);
        // compute a line width
        lineWidth = scale > 2 ? 2 : 1;
        paint.setStrokeWidth(lineWidth);
    }

    public void reshape() {
        setLayoutParams(new RelativeLayout.LayoutParams((int) (originalRect.width() * scale), (int)(originalRect.height() * scale)));
        setX(originalRect.left * scale);
        setY(originalRect.top * scale);
    }

    public void setup(){
        //no - op.
    }

    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (changed == true) {
            postLayoutInnerRect.set(0,0,right-left,bottom-top);
        }
    }

    /* Set the label (checking for special null values) */
    public String sanitizeLabel(String labelString) {
        if (labelString.equals("-") || labelString.equals("empty")) {
            return null;
        } else {
            return labelString;
        }
    }

    public static int getColor(int iemcolor) {
        //Log.e("ORIGINAL COLOR", "" + iemcolor);
        int color = 0;

        if(iemcolor < 0)
        {
            iemcolor = -1 - iemcolor;
            color = ((iemcolor & 0x3f000) << 6 )
                    + ((iemcolor & 0xfc0) << 4 )
                    + ((iemcolor & 0x3f) << 2 )
                    + 0xFF000000;
            //(iemcolor & 0xffffff) + 0xFF000000;
        }
        else
        {
            color = (iemgui_color_hex[iemcolor%IEM_GUI_MAX_COLOR] & 0xFFFFFF) | 0xFF000000;
        }

        //Log.e("COLOR", "" + color);
        return color;
    }

    public void drawLabel(Canvas canvas) {
        if (labelString != null) {
            paint.setStrokeWidth(0); //draws text filled in instead of line
            paint.setColor(labelcolor);
            paint.setTextSize(labelsize * scale);

            Rect rect = new Rect();
            paint.getTextBounds(labelString, 0, labelString.length(), rect);
            float labelHeight = rect.height();
            // on pd, label y pos is measured at vertical center, so push label down by labelheight/2.
            canvas.drawText(labelString, /*dRect.left +*/ labelpos[0] * scale, labelpos[1] * scale + labelHeight/2 , paint);
        }
    }

    public void replaceDollarZero(int dollarZero) {
        sendname = replaceDollarZeroString(sendname, dollarZero);
        setReceiveName(replaceDollarZeroString(receiveName, dollarZero));
        labelString = replaceDollarZeroString(labelString, dollarZero);
    }

    private static String replaceDollarZeroString(String string, int dollarZeroValue) {
        if(string == null) return null;
        String newString = string.replace("\\$0", ""+dollarZeroValue);
        newString = newString.replace("#0", ""+dollarZeroValue);
        return newString;
    }

    public void setValue(float v) {
        value = v;
        invalidate();
    }

    public float getValue() {
        return value;
    }

    public void setReceiveName(String name) {
        if (name != null && name.length() > 0) {
            if (receiveName != null) {
                dispatcher.removeListener(receiveName, this);
            }
            receiveName = name;
            dispatcher.addListener(name, this);
        }
    }

    public void sendFloat(float f) {
        if (sendname != null && !sendname.equals("") && !sendname.equals("empty")) {
            PdBase.sendFloat(sendname, f);
        }
    }

    public void sendBang() {
        PdBase.sendBang(sendname);
    }


    @Override
    public void receiveBang(String source) {
        receiveBangFromSource(source);
    }

    @Override
    public void receiveFloat(String source, float val) {
        receiveFloatFromSource(val, source);
    }

    @Override
    public void receiveList(String source, Object... args) {
        //Log.i("WIDGET", "receive list on "+this.receiveName);
        if(args.length > 0) {
            // pass float through, setting the value
            if(args[0] instanceof Float) {
                receiveFloatFromSource(((Float)args[0]).floatValue(), source);
            }
            else if(args[0] instanceof String) {
                // if we receive a set message
                if(args[0].equals("set")) {
                    // assume args[1] is String
                    receiveEditMessage((String)args[1], Arrays.copyOfRange(args, 1, args.length));
               /*
                    // set value but don't pass through
                    if(args.length > 1) {
                        if(args[1] instanceof Float) {
                            receiveSetFloat(((Float)args[1]).floatValue());
                        }
                        else if(args[1] instanceof String) {
                            receiveSetSymbol((String)args[1]);
                        }
                    }*/
                } else if(args[0].equals("bang")) { // got a bang!
                    receiveBangFromSource(source);
                } else { // pass symbol through., setting the value
                    receiveSymbolFromSource((String)args[0], source);
                }
            }
        }
        else {
            //DDLogWarn(@"%@: dropped list", self.type);
        }
    }

    @Override
    public void receiveMessage(String source, String message, Object... args) {
        //Log.i("WIDGET", "receive message on "+this.receiveName);
        if(message.equals("set") && args.length == 0) {
            receiveSetBang(); //special, this means a bang is sent to the GUI object for display, not output
        } else if (message.equals("set") && args.length == 1) {
            if (args[0] instanceof Float) {
                receiveSetFloat(((Float)args[0]).floatValue());
            } else if (args[0] instanceof String) {
                receiveSetSymbol((String)args[0]);
            }
        } else if (message.equals("bang")) {
            receiveBangFromSource(source);
        } else {
            receiveEditMessage(message, args);
            // edit, drop
        }

    }

    @Override
    public void receiveSymbol(String source, String val) {
        receiveSymbolFromSource(val, source);
    }

    // End of PdListener overrides, below are overrides for widget subclasses.

    protected void receiveSetBang() { //NEW put it everywhere that needs it: toggle, num/num2, slider, etc

    }

    protected void receiveSetFloat(float val) {

    }

    protected void receiveSetSymbol(String val) {

    }

    // return whether callee handled it or not.
    protected boolean receiveEditMessage(String message, Object... args) {
        return false;
    }

    protected void receiveBangFromSource(String source) {

    }

    protected void receiveFloatFromSource(float val, String source) {

    }

    protected void receiveSymbolFromSource(String symbol, String source) {

    }

    // Used by number box and number box 2
    // adapted from PdParty's Number.m, which was
    // adapted from void my_numbox_ftoa(t_my_numbox *x) in g_numbox.c
    protected String stringForFloat(float val, int width, DecimalFormat fmt) {
        boolean isExp = false;
        int i, idecimal;
        String string = fmt.format(val);
        // if it is in exponential mode
        if(string.length() >= 5) {
            i = (int)string.length() - 4;
            if((string.charAt(i) == 'e') || (string.charAt(i) == 'E')) {
                isExp = true;
            }
        }

        // if to reduce
        if(string.length() > width) {
            if(isExp) {
                if(width <= 5) {
                    //[string setString:(f < 0.0 ? @"-" : @"+")];
                    string = val < 0.0 ? "-" : "+";
                    return string; //DEI edit for bug fix
                }
                i = width - 4;
                for(idecimal=0; idecimal < i; idecimal++) {
                    if(string.charAt(idecimal) == '.') {
                        break;
                    }
                }
                if(idecimal > (width - 4)){
                    string = val < 0.0 ? "-" : "+";
                    return string;//DEI edit for bug fix
                }
                else {
                    int new_exp_index = width-4, old_exp_index = (int)string.length()-4;
                    StringBuilder sb = new StringBuilder(string.substring(0,new_exp_index - 1));
                    for(i = 0; i < 4; i++, new_exp_index++, old_exp_index++) {
                        //[string setCharacter:[string characterAtIndex:old_exp_index] atIndex:new_exp_index];
                        sb.append(string.charAt(old_exp_index));
                    }
                    string = sb.toString();
                }
            }
            else { //not exp
                for(idecimal = 0; idecimal < string.length(); idecimal++) {
                    if(string.charAt(idecimal) == '.') {
                        break;
                    }
                }
            }
            if(idecimal > width) {
                string = val < 0.0 ? "-" : "+";
            }

        }
        // On non-exp, can still have long number. Truncate to width.
        if (string.length() > width){
            string = string.substring(0, width);
        }
        return string;
    }
}
