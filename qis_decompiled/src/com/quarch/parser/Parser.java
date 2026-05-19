/*
 * Decompiled with CFR 0.152.
 */
package src.com.quarch.parser;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import src.com.quarch.parser.DeviceParser;
import src.com.quarch.parser.ParseException;

public class Parser {
    public static void main(String[] args) throws ParseException, FileNotFoundException {
        FileInputStream is = new FileInputStream(new File(args[0]));
        DeviceParser parser = new DeviceParser(new BufferedInputStream(is));
        for (String name : parser.parseFile()) {
            System.out.println("Hello " + name + "!");
        }
    }
}

