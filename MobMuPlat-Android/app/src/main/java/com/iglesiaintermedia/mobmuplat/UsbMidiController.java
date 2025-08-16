package com.iglesiaintermedia.mobmuplat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;

import org.puredata.android.midi.MidiToPdAdapter;
import org.puredata.core.PdBase;
import org.puredata.core.PdMidiReceiver;

import android.app.Activity;
import android.content.Context;
import android.media.midi.MidiDevice;
import android.media.midi.MidiDeviceInfo;
import android.media.midi.MidiInputPort;
import android.media.midi.MidiManager;
import android.media.midi.MidiOutputPort;
import android.media.midi.MidiReceiver;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.noisepages.nettoyeur.common.RawByteReceiver;
import com.noisepages.nettoyeur.midi.FromWireConverter;
import com.noisepages.nettoyeur.midi.ToWireConverter;

/// Not just USB midi, this class now interfaces with android midi in general.
public class UsbMidiController extends Observable{

    // All known devices; devices are automatically connected once discovered.
    public List<MidiDeviceInfo> deviceInfos;

    private final MidiManager mMidiManager;

    public UsbMidiController(Activity activity) {
        super();
       deviceInfos = new ArrayList<MidiDeviceInfo>();

        mMidiManager = (MidiManager) activity.getSystemService(Context.MIDI_SERVICE);
        if (mMidiManager != null) {
            mMidiManager.registerDeviceCallback(new MidiManager.DeviceCallback() {
                @Override
                public void onDeviceAdded(MidiDeviceInfo info) {
                    deviceInfos.add(info);
                    connectToMidiDevice(info); // Always connect
                    setChanged();
                    notifyObservers();
                }

                @Override
                public void onDeviceRemoved(MidiDeviceInfo info) {
                    // DEI anything more to disconect?
                    deviceInfos.remove(info);
                    setChanged();
                    notifyObservers();
                }
            }, new Handler(Looper.getMainLooper())); // End of connection callbacks

            // Get currently-known devices.
            MidiDeviceInfo[] currentMidiDeviceInfos = mMidiManager.getDevices();
            for (MidiDeviceInfo info : currentMidiDeviceInfos) {
                deviceInfos.add(info);
                connectToMidiDevice(info); // Always connect
            }
        }
    }

    private void connectToMidiDevice(MidiDeviceInfo info) {
        mMidiManager.openDevice(info, new MidiManager.OnDeviceOpenedListener() {
            @Override
            public void onDeviceOpened(MidiDevice device) {
                if (device != null) {

                    // Get the input and output port from the MidiDevice
                    // The port number is usually 0 for most devices
                    int portNumber = 0;
                    // Get output port from device to use as midi _input_ to Pd.
                    MidiOutputPort outputPort = device.openOutputPort(portNumber);
                    if (outputPort != null) {
                        MidiReceiver midiReceiver = new MMPMidiReceiver();
                        outputPort.connect(midiReceiver);
                    }
                    // Get input port from device to use as midi _output_ from Pd.
                    MidiInputPort inputPort = device.openInputPort(portNumber);
                    if (inputPort != null) {
                        MMPPdMidiReceiver pdMidiReceiver = new MMPPdMidiReceiver(inputPort);
                        // TODO PdBase only has one midi receiver at a time. This means the last connected device will be used.
                        PdBase.setMidiReceiver(pdMidiReceiver);
                    }

                    setChanged();
                    notifyObservers();
                }
            }
        }, new Handler(Looper.getMainLooper()));
    }
}

// MidiReceiver which receives raw MIDI from device; uses FromWireConverter+MidiToPdAdapter to send to Pd.
class MMPMidiReceiver extends MidiReceiver {

    MidiToPdAdapter midiToPdAdapter;
    FromWireConverter fromWireConverter;

    public MMPMidiReceiver() {
        midiToPdAdapter = new MidiToPdAdapter();
        fromWireConverter = new FromWireConverter(midiToPdAdapter);
    }

    @Override
    public void onSend(byte[] msg, int offset, int count, long timestamp) throws IOException {
        byte[] packedArray = new byte[count];
        System.arraycopy(msg, offset, packedArray, 0, count);
        fromWireConverter.onBytesReceived(count, packedArray);
    }
}

// PdMidiReceiver which receives Midi from PdBase and sends out to midi device
class MMPPdMidiReceiver implements PdMidiReceiver {
    MidiInputPort midiInputPort;
    ToWireConverter toWireConverter;

    public MMPPdMidiReceiver(MidiInputPort midiInputPort) {
        this.midiInputPort = midiInputPort;
        // Implement RawByteReceiver to feed midiInputPort.
        RawByteReceiver rawByteReceiver = new RawByteReceiver() {
            @Override
            public void onBytesReceived(int nBytes, byte[] buffer) {
                try {
                    midiInputPort.onSend(buffer, 0, nBytes, System.nanoTime());
                } catch (IOException e) {
                    Log.i("DEI", "send exception: "+e);
                }
            }

            @Override
            public boolean beginBlock() {
                return false;
            }

            @Override
            public void endBlock() {

            }
        };
        toWireConverter = new ToWireConverter(rawByteReceiver);
    }

    @Override
    public void receiveMidiByte(int port, int value) {
        toWireConverter.onRawByte((byte)value);
    }

    @Override
    public void receiveNoteOn(int channel, int pitch, int velocity) {
        toWireConverter.onNoteOn(channel, pitch, velocity);
    }

    @Override
    public void receiveControlChange(int channel, int controller, int value) {
        toWireConverter.onControlChange(channel, controller, value);
    }

    @Override
    public void receiveProgramChange(int channel, int value) {
        toWireConverter.onProgramChange(channel, value);
    }

    @Override
    public void receivePitchBend(int channel, int value) {
        toWireConverter.onPitchBend(channel, value);
    }

    @Override
    public void receiveAftertouch(int channel, int value) {
        toWireConverter.onAftertouch(channel, value);
    }

    @Override
    public void receivePolyAftertouch(int channel, int pitch, int value) {
        toWireConverter.onPolyAftertouch(channel, pitch, value);
    }

    @Override
    public boolean beginBlock() {
        return false;
    }

    @Override
    public void endBlock() {

    }
}