/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javafx.util.Pair
 */
package src.com.quarch.deviceXML;

import java.util.List;
import javafx.util.Pair;
import src.com.quarch.deviceXML.Channel;
import src.com.quarch.deviceXML.Channels;
import src.com.quarch.deviceXML.Param;
import src.com.quarch.deviceXML.UnitConverter;
import src.com.quarch.deviceXML.XMLFixtureChannels;
import src.com.quarch.deviceXML.XMLValidator;

public class XMLFixChlsValidator
extends XMLValidator {
    private XMLFixtureChannels xmlFixChls;

    public XMLFixChlsValidator(XMLFixtureChannels xmlFixChls) {
        this.xmlFixChls = xmlFixChls;
        this.validtyResult = new XMLValidator.ValidtyResult(this);
    }

    public Pair<Double, UnitConverter.Unit> maxChannelValue(String name, String type) {
        if (this.xmlFixChls == null) {
            return null;
        }
        Channels chls = this.xmlFixChls.channels;
        List<Channel> channels = chls.channels;
        boolean foundName = false;
        boolean foundType = false;
        String maxVal = null;
        String unit = null;
        for (Channel channel : channels) {
            List<Param> params = channel.getParam();
            for (Param param : params) {
                if (param.getName().equals("Name") && param.getValue().toLowerCase().equals(name.toLowerCase())) {
                    foundName = true;
                }
                if (param.getName().equals("Type") && param.getValue().toLowerCase().equals(type.toLowerCase())) {
                    foundType = true;
                }
                if (param.getName().equals("Unit")) {
                    unit = param.getValue();
                }
                if (!param.getName().equals("MaxVal")) continue;
                maxVal = param.getValue();
            }
            if (!foundName || !foundType || unit == null || maxVal == null) continue;
            break;
        }
        if (!foundName || !foundType || unit == null || maxVal == null) {
            return null;
        }
        double maxDouble = Double.parseDouble(maxVal);
        UnitConverter.Unit maxUnit = UnitConverter.Unit.makeUnit(unit.trim());
        Pair pair = new Pair((Object)maxDouble, (Object)maxUnit);
        return pair;
    }

    public boolean isValidValue(String name, String type, String value) {
        double valueDouble;
        Pair<Double, UnitConverter.Unit> max_unit = this.maxChannelValue(name, type);
        if (max_unit == null) {
            this.validtyResult.set(value, null, false, null, true);
            return false;
        }
        try {
            valueDouble = Double.parseDouble(value);
        }
        catch (NumberFormatException e) {
            this.validtyResult.set(value, ((UnitConverter.Unit)((Object)max_unit.getValue())).getUnit(), false, null, false);
            return false;
        }
        double min = 0.0;
        double max = (Double)max_unit.getKey();
        String unit = ((UnitConverter.Unit)((Object)max_unit.getValue())).getUnit();
        boolean retVal = true;
        if (valueDouble >= min && valueDouble <= max) {
            this.validtyResult.set(value, unit, true, value, false);
        } else if (valueDouble < min) {
            this.validtyResult.set(value, unit, false, Double.toString(min), false);
        } else if (valueDouble > max) {
            this.validtyResult.set(value, unit, false, Double.toString(max), false);
        } else {
            retVal = false;
        }
        return retVal;
    }
}

