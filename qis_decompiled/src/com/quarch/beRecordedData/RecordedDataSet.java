/*
 * Decompiled with CFR 0.152.
 */
package src.com.quarch.beRecordedData;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import src.com.quarch.beCommandData.CmdStruct;
import src.com.quarch.deviceAbstractions.DecodedSerialNumber;
import src.com.quarch.deviceAbstractions.DeviceTypes;
import src.com.quarch.deviceCalibrationData.Calibration;
import src.com.quarch.deviceInterface.QuarchDeviceInfo;

public class RecordedDataSet {
    public IntBuffer[] data;
    public boolean[] Enabled;
    public String[] Name;
    public int Timebase;
    private Calibration ThisCalibration;
    private ChannelList ThisChannelList;
    private DecodedSerialNumber dsn;

    public int size() {
        return this.data[0].limit();
    }

    public boolean getData(QuarchDeviceInfo deviceInfo) {
        boolean[] Enabled = new boolean[4];
        int SamplePeriodUs = 0;
        this.dsn = new DecodedSerialNumber(deviceInfo.getDeviceAbstract().myDle);
        CmdStruct cmd = new CmdStruct();
        cmd.command = "RECord:DUMP 0 2000000ms BIN";
        deviceInfo.executeBulkDataCommand(cmd);
        ByteBuffer dump = cmd.bBuffer;
        if (dump == null) {
            return false;
        }
        int buffStartByte = 0;
        if (dump.limit() > 4) {
            Enabled[0] = this.getBitAt(dump.get(buffStartByte), 0);
            Enabled[1] = this.getBitAt(dump.get(buffStartByte), 1);
            Enabled[2] = this.getBitAt(dump.get(buffStartByte), 2);
            Enabled[3] = this.getBitAt(dump.get(buffStartByte), 3);
            SamplePeriodUs = new Byte(dump.get(++buffStartByte)).intValue();
            SamplePeriodUs += new Byte(dump.get(++buffStartByte)).intValue() << 8;
            SamplePeriodUs += new Byte(dump.get(++buffStartByte)).intValue() << 16;
            SamplePeriodUs += new Byte(dump.get(++buffStartByte)).intValue() << 24;
        }
        int enableCount = 0;
        for (boolean b : Enabled) {
            if (!b) continue;
            ++enableCount;
        }
        if (enableCount == 0) {
            return false;
        }
        if (this.dsn.getDeviceType() == DeviceTypes.ppm) {
            this.getCalibration(dump);
            dump.position(68);
        } else if (this.dsn.getDeviceType() == DeviceTypes.xlc) {
            dump.position(++buffStartByte);
        } else {
            dump.position(++buffStartByte);
            dump.position(++buffStartByte);
        }
        this.ThisChannelList = new ChannelList(this.dsn.getDeviceType() != DeviceTypes.ppm);
        this.Enabled = Enabled;
        this.Name = this.ThisChannelList.getNames();
        this.Timebase = SamplePeriodUs;
        Sample[] SampleTemp = new Sample[this.ThisChannelList.getSize()];
        IntBuffer[] outputBuffer = new IntBuffer[this.ThisChannelList.getSize()];
        for (int i = 0; i < outputBuffer.length; ++i) {
            outputBuffer[i] = IntBuffer.allocate(dump.remaining() / this.stripSize());
        }
        while (dump.remaining() >= this.stripSize()) {
            for (Channel c : this.ThisChannelList) {
                SampleTemp[c.id] = this.getSample(c, dump);
                outputBuffer[c.id].put(SampleTemp[c.id].value);
            }
        }
        this.data = outputBuffer;
        return true;
    }

    private boolean getBitAt(byte ThisByte, int ThisPosition) {
        return (ThisByte >> ThisPosition & 1) == 1;
    }

    private boolean getBitAt(char ThisWord, int ThisPosition) {
        return (ThisWord >> ThisPosition & 1) == 1;
    }

    private void getCalibration(ByteBuffer ThisBuffer) {
        this.ThisCalibration = new Calibration();
        if ((ThisBuffer.get(5) & 0xFF) == 255 && (ThisBuffer.get(6) & 0xFF) == 255) {
            if ((ThisBuffer.get(7) & 0xFF) == 1) {
                ThisBuffer.order(ByteOrder.LITTLE_ENDIAN);
                ThisBuffer.position(8);
                this.ThisCalibration.add12VCalPoint(ThisBuffer.getShort(), ThisBuffer.getFloat(), ThisBuffer.getFloat());
                this.ThisCalibration.add12VCalPoint(ThisBuffer.getShort(), ThisBuffer.getFloat(), ThisBuffer.getFloat());
                this.ThisCalibration.add12VCalPoint(ThisBuffer.getShort(), ThisBuffer.getFloat(), ThisBuffer.getFloat());
                this.ThisCalibration.add5VCalPoint(ThisBuffer.getShort(), ThisBuffer.getFloat(), ThisBuffer.getFloat());
                this.ThisCalibration.add5VCalPoint(ThisBuffer.getShort(), ThisBuffer.getFloat(), ThisBuffer.getFloat());
                this.ThisCalibration.add5VCalPoint(ThisBuffer.getShort(), ThisBuffer.getFloat(), ThisBuffer.getFloat());
            } else {
                this.ThisCalibration.add12VCalPoint(4096, 165.35829f, 1.917f);
                this.ThisCalibration.add5VCalPoint(4096, 165.35829f, 1.917f);
            }
        } else {
            System.err.println("error during calibration retrieval");
        }
    }

    private int stripSize() {
        int stripSize = 0;
        for (Channel c : this.ThisChannelList) {
            if (!this.Enabled[c.id]) continue;
            stripSize += c.bytes;
        }
        return stripSize;
    }

    private Sample getSample(Channel ThisChannel, ByteBuffer ThisBuffer) {
        Sample ThisSample = new Sample();
        if (this.Enabled[ThisChannel.id]) {
            char ThisWord = (char)(ThisBuffer.get() & 0xFF);
            ThisWord = (char)(ThisWord + (char)((ThisBuffer.get() & 0xFF) << 8));
            ThisSample.valid = this.getBitAt(ThisWord, 15);
            ThisSample.trigger = this.getBitAt(ThisWord, 14);
            switch (ThisChannel.name) {
                case "V5Voltage": {
                    ThisSample.value = ThisWord & 0x3FFF;
                    if (this.dsn.getDeviceType() != DeviceTypes.ppm) break;
                    ThisSample.value = (int)((double)ThisSample.value * 3.538305666);
                    break;
                }
                case "V5Current": {
                    ThisSample.value = ThisWord & 0xFFF;
                    if (this.dsn.getDeviceType() == DeviceTypes.ppm) {
                        ThisSample.value = this.ThisCalibration.get5VCurrentCalResult(ThisSample.value) * 1000;
                        break;
                    }
                    ThisSample.value <<= 12;
                    ThisWord = (char)(ThisBuffer.get() & 0xFF);
                    ThisWord = (char)(ThisWord + (char)((ThisBuffer.get() & 0xFF) << 8));
                    ThisSample.value += ThisWord & 0xFFF;
                    break;
                }
                case "V12Voltage": {
                    ThisSample.value = ThisWord & 0x3FFF;
                    if (this.dsn.getDeviceType() != DeviceTypes.ppm) break;
                    ThisSample.value = (int)((double)ThisSample.value * 3.538305666);
                    break;
                }
                case "V12Current": {
                    ThisSample.value = ThisWord & 0xFFF;
                    if (this.dsn.getDeviceType() == DeviceTypes.ppm) {
                        ThisSample.value = this.ThisCalibration.get12VCurrentCalResult(ThisSample.value) * 1000;
                        break;
                    }
                    ThisSample.value = (ThisWord & 0xFFF) << 12;
                    ThisWord = (char)(ThisBuffer.get() & 0xFF);
                    ThisWord = (char)(ThisWord + (char)((ThisBuffer.get() & 0xFF) << 8));
                    ThisSample.value += ThisWord & 0xFFF;
                }
            }
        } else {
            ThisSample.value = 0;
            ThisSample.valid = false;
            ThisSample.trigger = false;
        }
        return ThisSample;
    }

    private class Sample {
        public int value;
        public boolean trigger;
        public boolean valid;

        private Sample() {
        }
    }

    private class ChannelList
    implements Iterable<Channel> {
        private final ArrayList<Channel> ChannelList = new ArrayList();

        ChannelList(boolean isXLC) {
            if (isXLC) {
                this.ChannelList.add(new Channel(0, "V5Voltage", 2));
                this.ChannelList.add(new Channel(1, "V5Current", 4));
                this.ChannelList.add(new Channel(2, "V12Voltage", 2));
                this.ChannelList.add(new Channel(3, "V12Current", 4));
            } else {
                this.ChannelList.add(new Channel(0, "V5Voltage", 2));
                this.ChannelList.add(new Channel(1, "V5Current", 2));
                this.ChannelList.add(new Channel(2, "V12Voltage", 2));
                this.ChannelList.add(new Channel(3, "V12Current", 2));
            }
        }

        int getSize() {
            return this.ChannelList.size();
        }

        String[] getNames() {
            String[] result = new String[this.ChannelList.size()];
            for (Channel c : this.ChannelList) {
                result[c.id] = c.name;
            }
            return result;
        }

        @Override
        public Iterator<Channel> iterator() {
            return this.ChannelList.iterator();
        }
    }

    private class Channel {
        int id;
        String name;
        int bytes;

        Channel(int thisId, String thisName, int theseBytes) {
            this.id = thisId;
            this.name = thisName;
            this.bytes = theseBytes;
        }
    }
}

