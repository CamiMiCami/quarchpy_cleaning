/*
 * Decompiled with CFR 0.152.
 */
package src.com.quarch.deviceMOM;

import java.nio.ByteBuffer;
import java.util.List;
import src.com.quarch.deviceMOM.DataGenerator;

public class ToTextHelper {
    private boolean firstLine;
    private List<DataGenerator> dataGeneratorList;
    private String currentLine;
    public static String defaultTextSeparater = " ";

    public ToTextHelper(List<DataGenerator> dataGeneratorList) {
        this.dataGeneratorList = dataGeneratorList;
    }

    public boolean getLLine() {
        return false;
    }

    public static void toStringBuffer(DataGenerator dataGenerator, ByteBuffer data, StringBuilder sb) {
        dataGenerator.toStringBuilder(data, sb, defaultTextSeparater);
    }
}

