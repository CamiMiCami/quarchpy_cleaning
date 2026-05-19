/*
 * Decompiled with CFR 0.152.
 */
package src.com.quarch.deviceAbstractions;

import src.com.quarch.deviceAbstractions.DeviceDetails;
import src.com.quarch.deviceAbstractions.DeviceTypes;
import src.com.quarch.deviceInterface.DeviceListEntry;

public class DecodedSerialNumber {
    private boolean isQTL = false;
    private boolean isValid = false;
    public String devId;
    public String devRevision;
    public String devSNumber;
    private DeviceTypes deviceType;

    public DecodedSerialNumber(DeviceListEntry dle) {
        String str;
        if (dle.serialNo.toLowerCase().startsWith("qtl")) {
            this.isQTL = true;
            str = dle.serialNo.substring(3);
        } else {
            str = dle.serialNo;
        }
        int dashPos = str.indexOf(45);
        if (dashPos >= 0) {
            try {
                this.devId = str.substring(0, dashPos);
                str = str.substring(dashPos + 1);
                dashPos = str.indexOf(45);
                this.devRevision = str.substring(0, dashPos);
                this.devSNumber = str.substring(dashPos + 1);
                this.setValid(true);
                DeviceTypes deviceType = dle.deviceInfo.getDeviceType();
                if (deviceType == null) {
                    this.setDeviceType(DeviceDetails.sNoToDeviceType("qtl" + this.devId));
                } else {
                    this.setDeviceType(deviceType);
                }
            }
            catch (IndexOutOfBoundsException e) {
                this.setValid(false);
            }
        }
    }

    public boolean isValid() {
        return this.isValid;
    }

    private void setValid(boolean isValid) {
        this.isValid = isValid;
    }

    public DeviceTypes getDeviceType() {
        return this.deviceType;
    }

    private void setDeviceType(DeviceTypes deviceType) {
        this.deviceType = deviceType;
    }
}

