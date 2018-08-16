package com.iglesiaintermedia.mobmuplat.nativepdgui;

import android.content.Context;

public abstract class IEMWidget extends Widget {

    public IEMWidget(Context context, float scale) {
        super(context, scale);
    }

    protected boolean receiveEditMessage(String message, Object... args) {
        if (message.equals("color") && args.length > 2 && args[0] instanceof Float &&
                args[1] instanceof Float && args[2] instanceof Float) {
            // background color, front color, label color
            bgcolor = getColor(((Float) (args[0])).intValue());
            fgcolor = getColor(((Float) (args[1])).intValue());
            labelcolor = getColor(((Float) (args[2])).intValue());
            invalidate();
            return true;
        } else if (message.equals("size") && args.length > 1 && args[0] instanceof Float &&
                args[1] instanceof Float) {
            //width, heightorigin
	    return false;
        }
        /*
        if([message isEqualToString:@"color"] && [arguments count] > 2 &&
                ([arguments isNumberAt:0] && [arguments isNumberAt:1] && [arguments isNumberAt:2])) {
            // background, front-color, label-color
            self.fillColor = [IEMWidget colorFromIEMColor:[[arguments objectAtIndex:0] intValue]];
            self.controlColor = [IEMWidget colorFromIEMColor:[[arguments objectAtIndex:1] intValue]];
            self.label.textColor = [IEMWidget colorFromIEMColor:[[arguments objectAtIndex:2] intValue]];
		[self reshape];
		[self setNeedsDisplay];
            return YES;
        }
	else if([message isEqualToString:@"size"] && [arguments count] > 1 &&
                ([arguments isNumberAt:0] && [arguments isNumberAt:1])) {
            // width, height
            self.originalFrame = CGRectMake(
                    self.originalFrame.origin.x, self.originalFrame.origin.y,
                    CLAMP([[arguments objectAtIndex:0] floatValue], IEM_GUI_MINSIZE, IEM_GUI_MAXSIZE),
            CLAMP([[arguments objectAtIndex:1] floatValue], IEM_GUI_MINSIZE, IEM_GUI_MAXSIZE));
		[self reshape];
		[self setNeedsDisplay];
            return YES;
        }
	else if([message isEqualToString:@"pos"] && [arguments count] > 1 &&
                ([arguments isNumberAt:0] && [arguments isNumberAt:1])) {
            // absolute pos
            self.originalFrame = CGRectMake(
                    [[arguments objectAtIndex:0] floatValue], [[arguments objectAtIndex:1] floatValue],
            CGRectGetWidth(self.originalFrame), CGRectGetHeight(self.originalFrame));
		[self reshape];
		[self setNeedsDisplay];
            return YES;
        }
	else if([message isEqualToString:@"delta"] && [arguments count] > 1 &&
                ([arguments isNumberAt:0] && [arguments isNumberAt:1])) {
            // relative pos
            self.originalFrame = CGRectMake(
                    self.originalFrame.origin.x + [[arguments objectAtIndex:0] floatValue],
            self.originalFrame.origin.y + [[arguments objectAtIndex:1] floatValue],
            CGRectGetWidth(self.originalFrame), CGRectGetHeight(self.originalFrame));
		[self reshape];
		[self setNeedsDisplay];
            return YES;
        }
	else if([message isEqualToString:@"label"] && [arguments count] > 0 && [arguments isStringAt:0]) {
            self.label.text = [arguments objectAtIndex:0];
		[self reshape];
		[self setNeedsDisplay];
            return YES;
        }
	else if([message isEqualToString:@"label_pos"] && [arguments count] > 1 &&
                ([arguments isNumberAt:0] && [arguments isNumberAt:1])) {
            // x, y
            self.originalLabelPos = CGPointMake([[arguments objectAtIndex:0] floatValue],
											[[arguments objectAtIndex:1] floatValue]);
		[self reshape];
		[self setNeedsDisplay];
            return YES;
        }
	else if([message isEqualToString:@"label_font"] && [arguments count] > 1 &&
                ([arguments isNumberAt:0] && [arguments isNumberAt:1])) {
            self.labelFontStyle = [[arguments objectAtIndex:0] intValue];
            self.labelFontSize = [[arguments objectAtIndex:1] floatValue];
		[self reshape];
		[self setNeedsDisplay];
            return YES;
        }
	else if([message isEqualToString:@"send"] && [arguments count] > 0 && [arguments isStringAt:0]) {
            self.sendName = [arguments objectAtIndex:0];
            return YES;
        }
	else if([message isEqualToString:@"receive"] && [arguments count] > 0 && [arguments isStringAt:0]) {
            self.receiveName = [arguments objectAtIndex:0];
            return YES;
        }
	else if([message isEqualToString:@"init"] && [arguments count] > 0 && [arguments isNumberAt:0]) {
            self.inits = [[arguments objectAtIndex:0] boolValue];
            return YES;
        }
        return NO;
    }*/
        return false;
    }

}
