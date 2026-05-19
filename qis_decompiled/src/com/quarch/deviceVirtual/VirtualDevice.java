/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  Quarch.QChangeHandler
 *  commsInterface.CommsInterfaceBase
 *  commsInterface.CommsInterfaceType
 *  device.DeviceBase
 *  device.DeviceChangeEventData
 *  device.DeviceRef
 */
package src.com.quarch.deviceVirtual;

import Quarch.QChangeHandler;
import commsInterface.CommsInterfaceBase;
import commsInterface.CommsInterfaceType;
import device.DeviceBase;
import device.DeviceChangeEventData;
import device.DeviceRef;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import src.com.quarch.beCommandData.CmdStruct;
import src.com.quarch.beWrapper.DeviceSpecifier;
import src.com.quarch.deviceInterface.DeviceListEntry;
import src.com.quarch.deviceInterface.QuarchDeviceInfo;
import src.com.quarch.deviceMOM.StreamHeader;
import src.com.quarch.deviceVirtual.VirtualDeviceInterface;
import src.com.quarch.deviceVirtual.VirtualDeviceLink;
import src.com.quarch.devices.BaseStreamDevicePPM;
import src.com.quarch.devices.IFVirtualisableDevice;

public class VirtualDevice
extends BaseStreamDevicePPM {
    final List<IFVirtualisableDevice> componentDevices = new ArrayList<IFVirtualisableDevice>();
    final List<VirtualDeviceLink> sourceDevices;
    public QuarchDeviceInfo qdi;
    public DeviceListEntry dle;
    AtomicInteger channelOffset = new AtomicInteger(0);
    AtomicInteger totalGroups = new AtomicInteger(0);
    private final CountDownLatch allheadersReadSignal;
    private final String origCmdText;
    int sourceDeviceIndex = 0;

    private QuarchDeviceInfo createQuarchDeviceInfo(String name) {
        VirtualDeviceInterface retVal = new VirtualDeviceInterface(this);
        retVal.sourceDevices = this.sourceDevices;
        retVal.setSerialNumberStr(name);
        retVal.idnName = name;
        retVal.setDeviceHasStream(true);
        return retVal;
    }

    public VirtualDevice(String name, String description, List<VirtualDeviceLink> sourceDevices, String origCmdText) {
        this.sourceDevices = sourceDevices;
        this.origCmdText = origCmdText.trim();
        this.qdi = this.createQuarchDeviceInfo(name);
        this.dle = new DeviceListEntry(name, this.qdi);
        this.setNeedFurtherIndetification(false);
        this.dle.deviceInfo.setDeviceAbstract(this);
        CommsInterfaceBase commsInterfaceBase = new CommsInterfaceBase(CommsInterfaceType.VIRTUAL, "Virtual");
        VirtualDeviceBase vdb = new VirtualDeviceBase(commsInterfaceBase);
        VirtualDeviceRef devRef = new VirtualDeviceRef(null, null, null, vdb);
        devRef.forceIdnName(description);
        this.setHasStream(true);
        this.dle.deviceInfo.setqDevice(devRef);
        this.dle.deviceInfo.getqDevice().setCurrentCommsInterface(CommsInterfaceType.VIRTUAL);
        this.dle.deviceInfo.setIntefaceName(DeviceSpecifier.VIRTUALinterfaceNameStr);
        this.allheadersReadSignal = new CountDownLatch(sourceDevices.size());
        this.setSharedVirtualData(sourceDevices);
    }

    private void setSharedVirtualData(List<VirtualDeviceLink> sourceDevices) {
        int deviceIndex = 0;
        for (VirtualDeviceLink sd : sourceDevices) {
            sd.dle.deviceInfo.getDeviceAbstract().setVirtualOwner(this, sd.prefix, sd.groupOffset, this.totalGroups, this.allheadersReadSignal, deviceIndex++);
        }
    }

    public void clearSharedVirtualData() {
        for (VirtualDeviceLink sd : this.sourceDevices) {
            sd.dle.deviceInfo.getDeviceAbstract().setVirtualOwner(null, "", null, null, null, 0);
        }
    }

    public void headerReceivedFor(int deviceIndex, int groupCount) {
        this.sourceDevices.get((int)deviceIndex).groupCount = groupCount;
    }

    public void allHeadersWaitDone(int deviceIndex) {
        int groupOffset = 0;
        int totalGroups = 0;
        for (VirtualDeviceLink sd : this.sourceDevices) {
            sd.groupOffset.set(groupOffset);
            groupOffset += sd.groupCount;
            totalGroups += sd.groupCount;
        }
        this.totalGroups.set(totalGroups);
    }

    @Override
    public void enableStreamReceive(DeviceListEntry dle) {
        this.channelOffset.set(0);
        for (VirtualDeviceLink sd : this.sourceDevices) {
            if (!sd.dle.deviceInfo.checkStreamSupport()) continue;
            sd.dle.deviceInfo.enableStreamReceive(sd.dle);
        }
    }

    @Override
    public boolean internalCommand(CmdStruct cmd, Object caller) {
        int maxReadStripes = 3072;
        String sCmd = cmd.command.replaceAll("(\\r|\\n)", "");
        if (sCmd.toLowerCase().startsWith("stream text")) {
            this.cmdStreamText(cmd, maxReadStripes);
            cmd.action = 333;
        } else if (sCmd.toLowerCase().startsWith("stream bin")) {
            this.cmdStreamBin(cmd, maxReadStripes);
        } else if (this.streamChannelCmd(sCmd, cmd)) {
            this.cmdPostAmble(cmd);
        } else {
            ArrayList responses = new ArrayList();
            for (VirtualDeviceLink sd : this.sourceDevices) {
                cmd.response.clear();
                sd.dle.deviceInfo.getDeviceAbstract().internalCommand(cmd, this);
                ArrayList saveResponse = new ArrayList(cmd.response);
                responses.add(saveResponse);
            }
            cmd.response.addAll((Collection)responses.get(0));
        }
        return cmd.action == 333;
    }

    private boolean streamChannelCmd(String sCmd, CmdStruct cmd) {
        return false;
    }

    private void cmdPostAmble(CmdStruct cmd) {
        cmd.response.add(">");
        cmd.action = 333;
    }

    private void cmdStreamBin(CmdStruct cmd, int maxReadStripes) {
        VirtualDeviceLink sd = this.sourceDevices.get(this.sourceDeviceIndex);
        cmd.response.clear();
        sd.dle.deviceInfo.getDeviceAbstract().internalCommand(cmd, this);
        ++this.sourceDeviceIndex;
        if (this.sourceDeviceIndex >= this.sourceDevices.size()) {
            this.sourceDeviceIndex = 0;
        }
    }

    private void cmdStreamText(CmdStruct cmd, int maxReadStripes) {
        if (cmd.command.substring(12).trim().startsWith("header")) {
            this.buildStreamTextHeader(cmd);
        }
    }

    private void buildStreamTextHeader(CmdStruct cmd) {
        List retVal = cmd.response;
        retVal.clear();
        StreamHeader.setHeaderPreamble(retVal);
        long fastestSampleTimenS = Long.MAX_VALUE;
        StreamHeader headerFastestDeviceRef = null;
        for (VirtualDeviceLink sd : this.sourceDevices) {
            long sampleTimenS = sd.dle.deviceInfo.getDeviceAbstract().getStreamHeaderRef().getFastestSampleTimenS();
            if (sampleTimenS >= fastestSampleTimenS) continue;
            fastestSampleTimenS = sampleTimenS;
            headerFastestDeviceRef = sd.dle.deviceInfo.getDeviceAbstract().getStreamHeaderRef();
        }
        StreamHeader.setV3MultiRatePreamble(headerFastestDeviceRef, retVal);
        StreamHeader.setGroupsPreamble(retVal);
        for (VirtualDeviceLink sd : this.sourceDevices) {
            sd.dle.deviceInfo.getDeviceAbstract().getStreamHeaderRef().setMultiRateGroups(retVal);
        }
        StreamHeader.setCloseGroupsPreamble(retVal);
        for (VirtualDeviceLink sd : this.sourceDevices) {
            sd.dle.deviceInfo.getDeviceAbstract().getStreamHeaderRef().setChannels(retVal);
        }
        StreamHeader.setCloseHeaderPreamble(retVal);
        cmd.response.add(">");
    }

    public String getOrigCmdText() {
        return this.origCmdText;
    }

    public class VirtualDeviceRef
    extends DeviceRef {
        public VirtualDeviceRef(ArrayBlockingQueue<DeviceChangeEventData> deviceChangeQueue, QChangeHandler<DeviceChangeEventData> changeHandler, CommsInterfaceBase descriptor) {
            super(deviceChangeQueue, changeHandler, descriptor);
        }

        public VirtualDeviceRef(ArrayBlockingQueue<DeviceChangeEventData> deviceChangeQueue, QChangeHandler<DeviceChangeEventData> changeHandler, CommsInterfaceBase commsInterface, DeviceBase device) {
            super(deviceChangeQueue, changeHandler, commsInterface, device);
        }
    }

    public class VirtualDeviceBase
    extends DeviceBase {
        public VirtualDeviceBase(CommsInterfaceBase commsInterfaceBase) {
            super(commsInterfaceBase);
        }
    }
}

