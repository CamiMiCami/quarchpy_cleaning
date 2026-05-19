/*
 * Decompiled with CFR 0.152.
 */
package src.com.quarch.parser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import src.com.quarch.parser.DeviceParser;
import src.com.quarch.parser.DeviceParserConstants;
import src.com.quarch.parser.Token;

public class Tokens {
    public static void main(String[] args) throws FileNotFoundException {
        FileInputStream is = new FileInputStream(new File(args[0]));
        String[] tokenImage = DeviceParserConstants.tokenImage;
        for (Token token : Tokens.tokenize(new DeviceParser(is))) {
            String id = token.beginLine + ":" + tokenImage[token.kind];
            System.out.println(id + " => " + token.image);
        }
    }

    public static List<Token> tokenize(DeviceParser template) throws FileNotFoundException {
        ArrayList<Token> tokens = new ArrayList<Token>();
        Token token = template.getNextToken();
        while (token.kind != 0) {
            tokens.add(token);
            token = template.getNextToken();
        }
        return tokens;
    }
}

