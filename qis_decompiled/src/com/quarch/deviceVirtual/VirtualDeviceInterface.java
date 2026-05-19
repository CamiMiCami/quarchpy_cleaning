/*
 * Decompiled with CFR 0.152.
 */
package src.com.quarch.deviceVirtual;

import java.util.ArrayList;
import java.util.List;
import src.com.quarch.beCommandData.CmdStruct;
import src.com.quarch.beWrapper.DeviceSpecifier;
import src.com.quarch.deviceAbstractions.DeviceDetails;
import src.com.quarch.deviceInterface.DeviceListEntry;
import src.com.quarch.deviceInterface.QuarchDeviceInfo;
import src.com.quarch.deviceVirtual.VirtualDevice;
import src.com.quarch.deviceVirtual.VirtualDeviceLink;

public class VirtualDeviceInterface
extends QuarchDeviceInfo {
    public List<VirtualDeviceLink> sourceDevices;
    private final VirtualDevice parent;

    public VirtualDeviceInterface(VirtualDevice virtualDevice) {
        this.parent = virtualDevice;
        this.setIntefaceName(DeviceSpecifier.VIRTUALinterfaceNameStr);
    }

    @Override
    public void executeCommand(CmdStruct cmd) {
        ArrayList responses = new ArrayList();
        for (VirtualDeviceLink virtualDeviceLink : this.sourceDevices) {
            virtualDeviceLink.dle.deviceInfo.executeCommand(cmd);
            ArrayList saveResponse = new ArrayList(cmd.response);
            responses.add(saveResponse);
        }
        cmd.response.clear();
        for (List list : responses) {
            cmd.response.addAll(list);
        }
    }

    @Override
    public boolean openForStream() {
        VirtualDeviceLink alreadyStreaming = null;
        for (VirtualDeviceLink sd : this.sourceDevices) {
            if (!sd.dle.deviceInfo.checkStreamSupport() || !sd.dle.deviceInfo.isStreaming()) continue;
            alreadyStreaming = sd;
        }
        if (alreadyStreaming != null) {
            return false;
        }
        for (VirtualDeviceLink sd : this.sourceDevices) {
            sd.dle.deviceInfo.openForStream();
        }
        return true;
    }

    @Override
    public boolean checkStreamSupport() {
        boolean hasStream = false;
        for (VirtualDeviceLink sd : this.sourceDevices) {
            hasStream |= DeviceDetails.sNoHasStream(sd.dle.deviceInfo.getSerialNumberStr(), sd.dle.deviceInfo.getIntefaceName());
        }
        return hasStream;
    }

    @Override
    public void enableStreamReceive(DeviceListEntry dle) {
        this.parent.enableStreamReceive(null);
    }
}

