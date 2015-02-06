package com.iglesiaintermedia.mobmuplat;

import java.util.List;

import org.puredata.core.PdBase;

import android.util.Log;
import android.util.SparseIntArray;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.InputDevice.MotionRange;

public class InputDeviceState {
	private static final String TAG = "InputDeviceState";
    private final InputDevice mDevice;
    private final int[] mAxes;
    private final float[] mAxisValues;
    private final SparseIntArray mKeys;
    private Object[] pdMsgArray;

    public InputDeviceState(InputDevice device) {
        mDevice = device;

        int numAxes = 0;
        final List<MotionRange> ranges = device.getMotionRanges();
        for (MotionRange range : ranges) {
            if ((range.getSource() & InputDevice.SOURCE_CLASS_JOYSTICK) != 0) {
                numAxes += 1;
            }
        }

        mAxes = new int[numAxes];
        mAxisValues = new float[numAxes];
        
        pdMsgArray = new Object[4];// address, index, /axiskey code as string, value
        pdMsgArray[0] = "/hid";
        pdMsgArray[1] = Integer.valueOf(0);
        
        int i = 0;
        for (MotionRange range : ranges) {
            if ((range.getSource() & InputDevice.SOURCE_CLASS_JOYSTICK) != 0) {
                mAxes[i++] = range.getAxis();
            }
        }

        mKeys = new SparseIntArray();
    }

    public InputDevice getDevice() {
        return mDevice;
    }

    public int getAxisCount() {
        return mAxes.length;
    }

    public int getAxis(int axisIndex) {
        return mAxes[axisIndex];
    }

    public float getAxisValue(int axisIndex) {
        return mAxisValues[axisIndex];
    }

    public int getKeyCount() {
        return mKeys.size();
    }

    public int getKeyCode(int keyIndex) {
        return mKeys.keyAt(keyIndex);
    }

    public boolean isKeyPressed(int keyIndex) {
        return mKeys.valueAt(keyIndex) != 0;
    }

    public boolean onKeyDown(KeyEvent event) {
        final int keyCode = event.getKeyCode();
        if (isGameKey(keyCode)) {
            if (event.getRepeatCount() == 0) {
                final String symbolicName = KeyEvent.keyCodeToString(keyCode);
                mKeys.put(keyCode, 1);
                //Log.i(TAG, mDevice.getName() + " - Key Down: " + symbolicName);
                pdMsgArray[2] = symbolicName;
                pdMsgArray[3] = Integer.valueOf(1);
                PdBase.sendList("fromSystem", pdMsgArray);
            }
            return true;
        }
        return false;
    }

    public boolean onKeyUp(KeyEvent event) {
        final int keyCode = event.getKeyCode();
        if (isGameKey(keyCode)) {
            int index = mKeys.indexOfKey(keyCode);
            if (index >= 0) {
                final String symbolicName = KeyEvent.keyCodeToString(keyCode);
                mKeys.put(keyCode, 0);
                //Log.i(TAG, mDevice.getName() + " - Key Up: " + symbolicName);
                pdMsgArray[2] = symbolicName;
                pdMsgArray[3] = Integer.valueOf(0);
                PdBase.sendList("fromSystem", pdMsgArray);
            }
            return true;
        }
        return false;
    }

    public boolean onJoystickMotion(MotionEvent event) {
        //StringBuilder message = new StringBuilder();
        //message.append(mDevice.getName()).append(" - Joystick Motion:\n");

        //final int historySize = event.getHistorySize();
        for (int i = 0; i < mAxes.length; i++) {
            final int axis = mAxes[i];
            final float value = event.getAxisValue(axis);
            mAxisValues[i] = value;
            
            pdMsgArray[2] = MotionEvent.axisToString(axis);
            pdMsgArray[3] = value;
            PdBase.sendList("fromSystem", pdMsgArray);
            
        }
        //Log.i(TAG, message.toString());
        return true;
    }

    // Check whether this is a key we care about.
    // In a real game, we would probably let the user configure which keys to use
    // instead of hardcoding the keys like this.
    private static boolean isGameKey(int keyCode) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_UP:
            case KeyEvent.KEYCODE_DPAD_DOWN:
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_DPAD_RIGHT:
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_SPACE:
                return true;
            default:
                return KeyEvent.isGamepadButton(keyCode);
        }
    }
  
}
