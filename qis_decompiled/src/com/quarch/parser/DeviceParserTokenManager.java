/*
 * Decompiled with CFR 0.152.
 */
package src.com.quarch.parser;

import java.io.IOException;
import java.io.PrintStream;
import src.com.quarch.parser.DeviceParserConstants;
import src.com.quarch.parser.SimpleCharStream;
import src.com.quarch.parser.Token;
import src.com.quarch.parser.TokenMgrError;

public class DeviceParserTokenManager
implements DeviceParserConstants {
    public PrintStream debugStream = System.out;
    static final long[] jjbitVec0 = new long[]{0L, 0L, -1L, -1L};
    static final int[] jjnextStates = new int[]{29, 30, 34, 35, 36, 31, 29, 30, 31, 34, 35, 36, 86, 87, 89, 90, 85, 86, 87, 88, 89, 90, 40, 60, 61, 82, 83, 110, 116, 32, 33, 37, 38};
    public static final String[] jjstrLiteralImages = new String[]{"", null, null, null, null, "+", "-", "*", "/", "(", ")", ",", "$", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null};
    public static final String[] lexStateNames = new String[]{"DEFAULT"};
    static final long[] jjtoToken = new long[]{436207585L};
    static final long[] jjtoSkip = new long[]{30L};
    protected SimpleCharStream input_stream;
    private final int[] jjrounds = new int[117];
    private final int[] jjstateSet = new int[234];
    protected char curChar;
    int curLexState = 0;
    int defaultLexState = 0;
    int jjnewStateCnt;
    int jjround;
    int jjmatchedPos;
    int jjmatchedKind;

    public void setDebugStream(PrintStream ds) {
        this.debugStream = ds;
    }

    private int jjStopAtPos(int pos, int kind) {
        this.jjmatchedKind = kind;
        this.jjmatchedPos = pos;
        return pos + 1;
    }

    private int jjMoveStringLiteralDfa0_0() {
        switch (this.curChar) {
            case '\t': {
                this.jjmatchedKind = 2;
                return this.jjMoveNfa_0(17, 0);
            }
            case '\n': {
                this.jjmatchedKind = 4;
                return this.jjMoveNfa_0(17, 0);
            }
            case '\r': {
                this.jjmatchedKind = 3;
                return this.jjMoveNfa_0(17, 0);
            }
            case ' ': {
                this.jjmatchedKind = 1;
                return this.jjMoveNfa_0(17, 0);
            }
            case '$': {
                this.jjmatchedKind = 12;
                return this.jjMoveNfa_0(17, 0);
            }
            case '(': {
                this.jjmatchedKind = 9;
                return this.jjMoveNfa_0(17, 0);
            }
            case ')': {
                this.jjmatchedKind = 10;
                return this.jjMoveNfa_0(17, 0);
            }
            case '*': {
                this.jjmatchedKind = 7;
                return this.jjMoveNfa_0(17, 0);
            }
            case '+': {
                this.jjmatchedKind = 5;
                return this.jjMoveNfa_0(17, 0);
            }
            case ',': {
                this.jjmatchedKind = 11;
                return this.jjMoveNfa_0(17, 0);
            }
            case '-': {
                this.jjmatchedKind = 6;
                return this.jjMoveNfa_0(17, 0);
            }
            case '/': {
                this.jjmatchedKind = 8;
                return this.jjMoveNfa_0(17, 0);
            }
            case 'C': 
            case 'c': {
                return this.jjMoveStringLiteralDfa1_0(65536L);
            }
        }
        return this.jjMoveNfa_0(17, 0);
    }

    private int jjMoveStringLiteralDfa1_0(long active0) {
        try {
            this.curChar = this.input_stream.readChar();
        }
        catch (IOException e) {
            return this.jjMoveNfa_0(17, 0);
        }
        switch (this.curChar) {
            case 'R': 
            case 'r': {
                return this.jjMoveStringLiteralDfa2_0(active0, 65536L);
            }
        }
        return this.jjMoveNfa_0(17, 1);
    }

    private int jjMoveStringLiteralDfa2_0(long old0, long active0) {
        if ((active0 &= old0) == 0L) {
            return this.jjMoveNfa_0(17, 1);
        }
        try {
            this.curChar = this.input_stream.readChar();
        }
        catch (IOException e) {
            return this.jjMoveNfa_0(17, 1);
        }
        switch (this.curChar) {
            case 'E': 
            case 'e': {
                return this.jjMoveStringLiteralDfa3_0(active0, 65536L);
            }
        }
        return this.jjMoveNfa_0(17, 2);
    }

    private int jjMoveStringLiteralDfa3_0(long old0, long active0) {
        if ((active0 &= old0) == 0L) {
            return this.jjMoveNfa_0(17, 2);
        }
        try {
            this.curChar = this.input_stream.readChar();
        }
        catch (IOException e) {
            return this.jjMoveNfa_0(17, 2);
        }
        switch (this.curChar) {
            case 'A': 
            case 'a': {
                return this.jjMoveStringLiteralDfa4_0(active0, 65536L);
            }
        }
        return this.jjMoveNfa_0(17, 3);
    }

    private int jjMoveStringLiteralDfa4_0(long old0, long active0) {
        if ((active0 &= old0) == 0L) {
            return this.jjMoveNfa_0(17, 3);
        }
        try {
            this.curChar = this.input_stream.readChar();
        }
        catch (IOException e) {
            return this.jjMoveNfa_0(17, 3);
        }
        switch (this.curChar) {
            case 'T': 
            case 't': {
                return this.jjMoveStringLiteralDfa5_0(active0, 65536L);
            }
        }
        return this.jjMoveNfa_0(17, 4);
    }

    private int jjMoveStringLiteralDfa5_0(long old0, long active0) {
        if ((active0 &= old0) == 0L) {
            return this.jjMoveNfa_0(17, 4);
        }
        try {
            this.curChar = this.input_stream.readChar();
        }
        catch (IOException e) {
            return this.jjMoveNfa_0(17, 4);
        }
        switch (this.curChar) {
            case 'E': 
            case 'e': {
                return this.jjMoveStringLiteralDfa6_0(active0, 65536L);
            }
        }
        return this.jjMoveNfa_0(17, 5);
    }

    private int jjMoveStringLiteralDfa6_0(long old0, long active0) {
        if ((active0 &= old0) == 0L) {
            return this.jjMoveNfa_0(17, 5);
        }
        try {
            this.curChar = this.input_stream.readChar();
        }
        catch (IOException e) {
            return this.jjMoveNfa_0(17, 5);
        }
        switch (this.curChar) {
            case ' ': {
                return this.jjMoveStringLiteralDfa7_0(active0, 65536L);
            }
        }
        return this.jjMoveNfa_0(17, 6);
    }

    private int jjMoveStringLiteralDfa7_0(long old0, long active0) {
        if ((active0 &= old0) == 0L) {
            return this.jjMoveNfa_0(17, 6);
        }
        try {
            this.curChar = this.input_stream.readChar();
        }
        catch (IOException e) {
            return this.jjMoveNfa_0(17, 6);
        }
        switch (this.curChar) {
            case 'D': 
            case 'd': {
                return this.jjMoveStringLiteralDfa8_0(active0, 65536L);
            }
        }
        return this.jjMoveNfa_0(17, 7);
    }

    private int jjMoveStringLiteralDfa8_0(long old0, long active0) {
        if ((active0 &= old0) == 0L) {
            return this.jjMoveNfa_0(17, 7);
        }
        try {
            this.curChar = this.input_stream.readChar();
        }
        catch (IOException e) {
            return this.jjMoveNfa_0(17, 7);
        }
        switch (this.curChar) {
            case 'E': 
            case 'e': {
                return this.jjMoveStringLiteralDfa9_0(active0, 65536L);
            }
        }
        return this.jjMoveNfa_0(17, 8);
    }

    private int jjMoveStringLiteralDfa9_0(long old0, long active0) {
        if ((active0 &= old0) == 0L) {
            return this.jjMoveNfa_0(17, 8);
        }
        try {
            this.curChar = this.input_stream.readChar();
        }
        catch (IOException e) {
            return this.jjMoveNfa_0(17, 8);
        }
        switch (this.curChar) {
            case 'V': 
            case 'v': {
                return this.jjMoveStringLiteralDfa10_0(active0, 65536L);
            }
        }
        return this.jjMoveNfa_0(17, 9);
    }

    private int jjMoveStringLiteralDfa10_0(long old0, long active0) {
        if ((active0 &= old0) == 0L) {
            return this.jjMoveNfa_0(17, 9);
        }
        try {
            this.curChar = this.input_stream.readChar();
        }
        catch (IOException e) {
            return this.jjMoveNfa_0(17, 9);
        }
        switch (this.curChar) {
            case 'I': 
            case 'i': {
                return this.jjMoveStringLiteralDfa11_0(active0, 65536L);
            }
        }
        return this.jjMoveNfa_0(17, 10);
    }

    private int jjMoveStringLiteralDfa11_0(long old0, long active0) {
        if ((active0 &= old0) == 0L) {
            return this.jjMoveNfa_0(17, 10);
        }
        try {
            this.curChar = this.input_stream.readChar();
        }
        catch (IOException e) {
            return this.jjMoveNfa_0(17, 10);
        }
        switch (this.curChar) {
            case 'C': 
            case 'c': {
                return this.jjMoveStringLiteralDfa12_0(active0, 65536L);
            }
        }
        return this.jjMoveNfa_0(17, 11);
    }

    private int jjMoveStringLiteralDfa12_0(long old0, long active0) {
        if ((active0 &= old0) == 0L) {
            return this.jjMoveNfa_0(17, 11);
        }
        try {
            this.curChar = this.input_stream.readChar();
        }
        catch (IOException e) {
            return this.jjMoveNfa_0(17, 11);
        }
        switch (this.curChar) {
            case 'E': 
            case 'e': {
                if ((active0 & 0x10000L) == 0L) break;
                this.jjmatchedKind = 16;
                this.jjmatchedPos = 12;
                break;
            }
        }
        return this.jjMoveNfa_0(17, 12);
    }

    private int jjMoveNfa_0(int startState, int curPos) {
        int strKind = this.jjmatchedKind;
        int strPos = this.jjmatchedPos;
        int seenUpto = curPos + 1;
        this.input_stream.backup(seenUpto);
        try {
            this.curChar = this.input_stream.readChar();
        }
        catch (IOException e) {
            throw new Error("Internal Error");
        }
        curPos = 0;
        int startsAt = 0;
        this.jjnewStateCnt = 117;
        int i = 1;
        this.jjstateSet[0] = startState;
        int kind = Integer.MAX_VALUE;
        while (true) {
            if (++this.jjround == Integer.MAX_VALUE) {
                this.ReInitRounds();
            }
            if (this.curChar < '@') {
                long l = 1L << this.curChar;
                block137: do {
                    switch (this.jjstateSet[--i]) {
                        case 17: {
                            if (this.curChar != '\"') break;
                            this.jjCheckNAddStates(0, 5);
                            break;
                        }
                        case 1: {
                            if ((0x3FF000000000000L & l) == 0L) break;
                            this.jjstateSet[this.jjnewStateCnt++] = 2;
                            break;
                        }
                        case 2: {
                            if ((0x3FF000000000000L & l) == 0L) break;
                            this.jjstateSet[this.jjnewStateCnt++] = 3;
                            break;
                        }
                        case 3: {
                            if ((0x3FF000000000000L & l) == 0L) break;
                            this.jjstateSet[this.jjnewStateCnt++] = 4;
                            break;
                        }
                        case 4: {
                            if ((0x3FF000000000000L & l) == 0L) break;
                            this.jjstateSet[this.jjnewStateCnt++] = 5;
                            break;
                        }
                        case 5: {
                            if (this.curChar != '-') break;
                            this.jjstateSet[this.jjnewStateCnt++] = 6;
                            break;
                        }
                        case 6: {
                            if ((0x3FF000000000000L & l) == 0L) break;
                            this.jjstateSet[this.jjnewStateCnt++] = 7;
                            break;
                        }
                        case 7: {
                            if ((0x3FF000000000000L & l) == 0L) break;
                            this.jjstateSet[this.jjnewStateCnt++] = 8;
                            break;
                        }
                        case 8: {
                            if (this.curChar != '-') break;
                            this.jjstateSet[this.jjnewStateCnt++] = 9;
                            break;
                        }
                        case 9: {
                            if ((0x3FF000000000000L & l) == 0L) break;
                            this.jjstateSet[this.jjnewStateCnt++] = 10;
                            break;
                        }
                        case 10: {
                            if ((0x3FF000000000000L & l) == 0L) break;
                            this.jjstateSet[this.jjnewStateCnt++] = 11;
                            break;
                        }
                        case 11: {
                            if ((0x3FF000000000000L & l) == 0L) continue block137;
                            if (kind > 15) {
                                kind = 15;
                            }
                            this.jjstateSet[this.jjnewStateCnt++] = 12;
                            break;
                        }
                        case 12: {
                            if (this.curChar != '-') break;
                            this.jjstateSet[this.jjnewStateCnt++] = 13;
                            break;
                        }
                        case 13: {
                            if ((0x3FF000000000000L & l) == 0L) break;
                            this.jjstateSet[this.jjnewStateCnt++] = 14;
                            break;
                        }
                        case 14: {
                            if ((0x3FF000000000000L & l) == 0L) break;
                            this.jjstateSet[this.jjnewStateCnt++] = 15;
                            break;
                        }
                        case 15: {
                            if ((0x3FF000000000000L & l) == 0L || kind <= 15) continue block137;
                            kind = 15;
                            break;
                        }
                        case 19: {
                            if (this.curChar != '(' || kind <= 18) continue block137;
                            kind = 18;
                            break;
                        }
                        case 29: {
                            if ((0xFFFFFFFBFFFFFFFFL & l) == 0L) break;
                            this.jjCheckNAddStates(6, 8);
                            break;
                        }
                        case 30: {
                            if (this.curChar != '\"' || kind <= 13) continue block137;
                            kind = 13;
                            break;
                        }
                        case 32: {
                            if (this.curChar != '\"') break;
                            this.jjCheckNAddStates(6, 8);
                            break;
                        }
                        case 34: {
                            if ((0xFFFFFFFBFFFFFFFFL & l) == 0L) break;
                            this.jjCheckNAddStates(9, 11);
                            break;
                        }
                        case 35: {
                            if (this.curChar != '\"' || kind <= 19) continue block137;
                            kind = 19;
                            break;
                        }
                        case 37: {
                            if (this.curChar != '\"') break;
                            this.jjCheckNAddStates(9, 11);
                            break;
                        }
                        case 41: {
                            if (this.curChar != ':') break;
                            this.jjstateSet[this.jjnewStateCnt++] = 59;
                            break;
                        }
                        case 43: {
                            if ((0x3FF000000000000L & l) == 0L) break;
                            this.jjstateSet[this.jjnewStateCnt++] = 44;
                            break;
                        }
                        case 44: {
                            if ((0x3FF000000000000L & l) == 0L) break;
                            this.jjstateSet[this.jjnewStateCnt++] = 45;
                            break;
                        }
                        case 45: {
                            if ((0x3FF000000000000L & l) == 0L) break;
                            this.jjstateSet[this.jjnewStateCnt++] = 46;
                            break;
                        }
                        case 46: {
                            if ((0x3FF000000000000L & l) == 0L) break;
                            this.jjstateSet[this.jjnewStateCnt++] = 47;
                            break;
                        }
                        case 47: {
                            if (this.curChar != '-') break;
                            this.jjstateSet[this.jjnewStateCnt++] = 48;
                            break;
                        }
                        case 48: {
                            if ((0x3FF000000000000L & l) == 0L) break;
                            this.jjstateSet[this.jjnewStateCnt++] = 49;
                            break;
                        }
                        case 49: {
                            if ((0x3FF000000000000L & l) == 0L) break;
                            this.jjstateSet[this.jjnewStateCnt++] = 50;
                            break;
                        }
                        case 50: {
                            if (this.curChar != '-') break;
                            this.jjstateSet[this.jjnewStateCnt++] = 51;
                            break;
                        }
                        case 51: {
                            if ((0x3FF000000000000L & l) == 0L) break;
                            this.jjstateSet[this.jjnewStateCnt++] = 52;
                            break;
                        }
                        case 52: {
                            if ((0x3FF000000000000L & l) == 0L) break;
                            this.jjstateSet[this.jjnewStateCnt++] = 53;
                            break;
                        }
                        case 53: {
                            if ((0x3FF000000000000L & l) == 0L) continue block137;
                            if (kind > 14) {
                                kind = 14;
                            }
                            this.jjstateSet[this.jjnewStateCnt++] = 54;
                            break;
                        }
                        case 54: {
                            if (this.curChar != '-') break;
                            this.jjstateSet[this.jjnewStateCnt++] = 55;
                            break;
                        }
                        case 55: {
                            if ((0x3FF000000000000L & l) == 0L) break;
                            this.jjstateSet[this.jjnewStateCnt++] = 56;
                            break;
                        }
                        case 56: {
                            if ((0x3FF000000000000L & l) == 0L) break;
                            this.jjstateSet[this.jjnewStateCnt++] = 57;
                            break;
                        }
                        case 57: {
                            if ((0x3FF000000000000L & l) == 0L || kind <= 14) continue block137;
                            kind = 14;
                            break;
                        }
                        case 60: {
                            if (this.curChar != ':') break;
                            this.jjstateSet[this.jjnewStateCnt++] = 41;
                            break;
                        }
                        case 62: {
                            if (this.curChar != ':') break;
                            this.jjstateSet[this.jjnewStateCnt++] = 81;
                            break;
                        }
                        case 64: {
                            if ((0x3FF000000000000L & l) == 0L) break;
                            this.jjstateSet[this.jjnewStateCnt++] = 65;
                            break;
                        }
                        case 65: {
                            if ((0x3FF000000000000L & l) == 0L) break;
                            this.jjstateSet[this.jjnewStateCnt++] = 66;
                            break;
                        }
                        case 66: {
                            if ((0x3FF000000000000L & l) == 0L) break;
                            this.jjstateSet[this.jjnewStateCnt++] = 67;
                            break;
                        }
                        case 67: {
                            if ((0x3FF000000000000L & l) == 0L) break;
                            this.jjstateSet[this.jjnewStateCnt++] = 68;
                            break;
                        }
                        case 68: {
                            if (this.curChar != '-') break;
                            this.jjstateSet[this.jjnewStateCnt++] = 69;
                            break;
                        }
                        case 69: {
                            if ((0x3FF000000000000L & l) == 0L) break;
                            this.jjstateSet[this.jjnewStateCnt++] = 70;
                            break;
                        }
                        case 70: {
                            if ((0x3FF000000000000L & l) == 0L) break;
                            this.jjstateSet[this.jjnewStateCnt++] = 71;
                            break;
                        }
                        case 71: {
                            if (this.curChar != '-') break;
                            this.jjstateSet[this.jjnewStateCnt++] = 72;
                            break;
                        }
                        case 72: {
                            if ((0x3FF000000000000L & l) == 0L) break;
                            this.jjstateSet[this.jjnewStateCnt++] = 73;
                            break;
                        }
                        case 73: {
                            if ((0x3FF000000000000L & l) == 0L) break;
                            this.jjstateSet[this.jjnewStateCnt++] = 74;
                            break;
                        }
                        case 74: {
                            if ((0x3FF000000000000L & l) == 0L) break;
                            this.jjCheckNAddTwoStates(75, 79);
                            break;
                        }
                        case 75: {
                            if (this.curChar != '-') break;
                            this.jjstateSet[this.jjnewStateCnt++] = 76;
                            break;
                        }
                        case 76: {
                            if ((0x3FF000000000000L & l) == 0L) break;
                            this.jjstateSet[this.jjnewStateCnt++] = 77;
                            break;
                        }
                        case 77: {
                            if ((0x3FF000000000000L & l) == 0L) break;
                            this.jjstateSet[this.jjnewStateCnt++] = 78;
                            break;
                        }
                        case 78: {
                            if ((0x3FF000000000000L & l) == 0L) break;
                            this.jjCheckNAdd(79);
                            break;
                        }
                        case 79: {
                            if (this.curChar != ',' || kind <= 22) continue block137;
                            kind = 22;
                            break;
                        }
                        case 82: {
                            if (this.curChar != ':') break;
                            this.jjstateSet[this.jjnewStateCnt++] = 62;
                            break;
                        }
                        case 85: {
                            if ((0x3FF000000000000L & l) == 0L) continue block137;
                            if (kind > 17) {
                                kind = 17;
                            }
                            this.jjstateSet[this.jjnewStateCnt++] = 85;
                            break;
                        }
                        case 86: {
                            if ((0x3FF000000000000L & l) == 0L) break;
                            this.jjAddStates(12, 13);
                            break;
                        }
                        case 87: {
                            if (this.curChar != ')' || kind <= 23) continue block137;
                            kind = 23;
                            break;
                        }
                        case 88: {
                            if ((0x3FF000000000000L & l) == 0L) continue block137;
                            if (kind > 24) {
                                kind = 24;
                            }
                            this.jjstateSet[this.jjnewStateCnt++] = 88;
                            break;
                        }
                        case 89: {
                            if ((0x3FF000000000000L & l) == 0L) break;
                            this.jjAddStates(14, 15);
                            break;
                        }
                        case 90: {
                            if (this.curChar != '(' || kind <= 28) continue block137;
                            kind = 28;
                            break;
                        }
                        case 93: {
                            if (this.curChar != '(') break;
                            this.jjstateSet[this.jjnewStateCnt++] = 106;
                            break;
                        }
                        case 95: {
                            if (this.curChar != '(') continue block137;
                            if (kind > 20) {
                                kind = 20;
                            }
                            this.jjstateSet[this.jjnewStateCnt++] = 101;
                            break;
                        }
                        case 112: {
                            if (this.curChar != '(' || kind <= 21) continue block137;
                            kind = 21;
                            break;
                        }
                    }
                } while (i != startsAt);
            } else if (this.curChar < '\u0080') {
                long l = 1L << (this.curChar & 0x3F);
                block138: do {
                    switch (this.jjstateSet[--i]) {
                        case 17: {
                            if ((0x7FFFFFE87FFFFFEL & l) != 0L) {
                                if (kind > 17) {
                                    kind = 17;
                                }
                                this.jjCheckNAddStates(16, 21);
                            }
                            if ((0x7FFFFFE07FFFFFEL & l) != 0L) {
                                if (kind > 27) {
                                    kind = 27;
                                }
                                this.jjCheckNAddStates(22, 26);
                            }
                            if ((0x1000000010L & l) != 0L) {
                                this.jjAddStates(27, 28);
                                break;
                            }
                            if ((0x400000004000L & l) != 0L) {
                                this.jjstateSet[this.jjnewStateCnt++] = 26;
                                break;
                            }
                            if ((0x2000000020000L & l) == 0L) break;
                            this.jjstateSet[this.jjnewStateCnt++] = 16;
                            break;
                        }
                        case 0: {
                            if ((0x100000001000L & l) == 0L) break;
                            this.jjstateSet[this.jjnewStateCnt++] = 1;
                            break;
                        }
                        case 16: {
                            if ((0x10000000100000L & l) == 0L) break;
                            this.jjstateSet[this.jjnewStateCnt++] = 0;
                            break;
                        }
                        case 18: {
                            if ((0x2000000020L & l) == 0L) break;
                            this.jjstateSet[this.jjnewStateCnt++] = 19;
                            break;
                        }
                        case 20: {
                            if ((0x800000008L & l) == 0L) break;
                            this.jjstateSet[this.jjnewStateCnt++] = 18;
                            break;
                        }
                        case 21: {
                            if ((0x20000000200L & l) == 0L) break;
                            this.jjstateSet[this.jjnewStateCnt++] = 20;
                            break;
                        }
                        case 22: {
                            if ((0x40000000400000L & l) == 0L) break;
                            this.jjstateSet[this.jjnewStateCnt++] = 21;
                            break;
                        }
                        case 23: {
                            if ((0x2000000020L & l) == 0L) break;
                            this.jjstateSet[this.jjnewStateCnt++] = 22;
                            break;
                        }
                        case 24: {
                            if ((0x1000000010L & l) == 0L) break;
                            this.jjstateSet[this.jjnewStateCnt++] = 23;
                            break;
                        }
                        case 25: {
                            if ((0x80000000800000L & l) == 0L) break;
                            this.jjstateSet[this.jjnewStateCnt++] = 24;
                            break;
                        }
                        case 26: {
                            if ((0x2000000020L & l) == 0L) break;
                            this.jjstateSet[this.jjnewStateCnt++] = 25;
                            break;
                        }
                        case 27: {
                            if ((0x400000004000L & l) == 0L) break;
                            this.jjstateSet[this.jjnewStateCnt++] = 26;
                            break;
                        }
                        case 29: {
                            if ((0xFFFFFFFFEFFFFFFFL & l) == 0L) break;
                            this.jjCheckNAddStates(6, 8);
                            break;
                        }
                        case 31: {
                            if (this.curChar != '\\') break;
                            this.jjAddStates(29, 30);
                            break;
                        }
                        case 33: {
                            if (this.curChar != '\\') break;
                            this.jjCheckNAddStates(6, 8);
                            break;
                        }
                        case 34: {
                            if ((0xFFFFFFFFEFFFFFFFL & l) == 0L) break;
                            this.jjCheckNAddStates(9, 11);
                            break;
                        }
                        case 36: {
                            if (this.curChar != '\\') break;
                            this.jjAddStates(31, 32);
                            break;
                        }
                        case 38: {
                            if (this.curChar != '\\') break;
                            this.jjCheckNAddStates(9, 11);
                            break;
                        }
                        case 39: {
                            if ((0x7FFFFFE07FFFFFEL & l) == 0L) continue block138;
                            if (kind > 27) {
                                kind = 27;
                            }
                            this.jjCheckNAddStates(22, 26);
                            break;
                        }
                        case 40: {
                            if ((0x7FFFFFE07FFFFFEL & l) == 0L) break;
                            this.jjCheckNAddTwoStates(40, 60);
                            break;
                        }
                        case 42: {
                            if ((0x100000001000L & l) == 0L) break;
                            this.jjstateSet[this.jjnewStateCnt++] = 43;
                            break;
                        }
                        case 58: {
                            if ((0x10000000100000L & l) == 0L) break;
                            this.jjstateSet[this.jjnewStateCnt++] = 42;
                            break;
                        }
                        case 59: {
                            if ((0x2000000020000L & l) == 0L) break;
                            this.jjstateSet[this.jjnewStateCnt++] = 58;
                            break;
                        }
                        case 61: {
                            if ((0x7FFFFFE07FFFFFEL & l) == 0L) break;
                            this.jjCheckNAddTwoStates(61, 82);
                            break;
                        }
                        case 63: {
                            if ((0x100000001000L & l) == 0L) break;
                            this.jjstateSet[this.jjnewStateCnt++] = 64;
                            break;
                        }
                        case 80: {
                            if ((0x10000000100000L & l) == 0L) break;
                            this.jjstateSet[this.jjnewStateCnt++] = 63;
                            break;
                        }
                        case 81: {
                            if ((0x2000000020000L & l) == 0L) break;
                            this.jjstateSet[this.jjnewStateCnt++] = 80;
                            break;
                        }
                        case 83: {
                            if ((0x7FFFFFE07FFFFFEL & l) == 0L) continue block138;
                            if (kind > 27) {
                                kind = 27;
                            }
                            this.jjCheckNAdd(83);
                            break;
                        }
                        case 84: {
                            if ((0x7FFFFFE87FFFFFEL & l) == 0L) continue block138;
                            if (kind > 17) {
                                kind = 17;
                            }
                            this.jjCheckNAddStates(16, 21);
                            break;
                        }
                        case 85: {
                            if ((0x7FFFFFE87FFFFFEL & l) == 0L) continue block138;
                            if (kind > 17) {
                                kind = 17;
                            }
                            this.jjCheckNAdd(85);
                            break;
                        }
                        case 86: {
                            if ((0x7FFFFFE87FFFFFEL & l) == 0L) break;
                            this.jjCheckNAddTwoStates(86, 87);
                            break;
                        }
                        case 88: {
                            if ((0x7FFFFFE87FFFFFEL & l) == 0L) continue block138;
                            if (kind > 24) {
                                kind = 24;
                            }
                            this.jjCheckNAdd(88);
                            break;
                        }
                        case 89: {
                            if ((0x7FFFFFE87FFFFFEL & l) == 0L) break;
                            this.jjCheckNAddTwoStates(89, 90);
                            break;
                        }
                        case 91: {
                            if ((0x1000000010L & l) == 0L) break;
                            this.jjAddStates(27, 28);
                            break;
                        }
                        case 92: {
                            if ((0x2000000020L & l) == 0L) break;
                            this.jjstateSet[this.jjnewStateCnt++] = 93;
                            break;
                        }
                        case 94: 
                        case 96: {
                            if ((0x2000000020L & l) == 0L) break;
                            this.jjCheckNAdd(95);
                            break;
                        }
                        case 97: {
                            if ((0x800000008L & l) == 0L) break;
                            this.jjstateSet[this.jjnewStateCnt++] = 96;
                            break;
                        }
                        case 98: {
                            if ((0x20000000200L & l) == 0L) break;
                            this.jjstateSet[this.jjnewStateCnt++] = 97;
                            break;
                        }
                        case 99: {
                            if ((0x40000000400000L & l) == 0L) break;
                            this.jjstateSet[this.jjnewStateCnt++] = 98;
                            break;
                        }
                        case 100: {
                            if ((0x2000000020L & l) == 0L) break;
                            this.jjstateSet[this.jjnewStateCnt++] = 99;
                            break;
                        }
                        case 101: {
                            if ((0x1000000010L & l) == 0L) break;
                            this.jjstateSet[this.jjnewStateCnt++] = 100;
                            break;
                        }
                        case 102: {
                            if ((0x800000008L & l) == 0L) break;
                            this.jjstateSet[this.jjnewStateCnt++] = 94;
                            break;
                        }
                        case 103: {
                            if ((0x20000000200L & l) == 0L) break;
                            this.jjstateSet[this.jjnewStateCnt++] = 102;
                            break;
                        }
                        case 104: {
                            if ((0x40000000400000L & l) == 0L) break;
                            this.jjstateSet[this.jjnewStateCnt++] = 103;
                            break;
                        }
                        case 105: {
                            if ((0x2000000020L & l) == 0L) break;
                            this.jjstateSet[this.jjnewStateCnt++] = 104;
                            break;
                        }
                        case 106: {
                            if ((0x1000000010L & l) == 0L) break;
                            this.jjstateSet[this.jjnewStateCnt++] = 105;
                            break;
                        }
                        case 107: {
                            if ((0x800000008L & l) == 0L) break;
                            this.jjstateSet[this.jjnewStateCnt++] = 92;
                            break;
                        }
                        case 108: {
                            if ((0x20000000200L & l) == 0L) break;
                            this.jjstateSet[this.jjnewStateCnt++] = 107;
                            break;
                        }
                        case 109: {
                            if ((0x40000000400000L & l) == 0L) break;
                            this.jjstateSet[this.jjnewStateCnt++] = 108;
                            break;
                        }
                        case 110: {
                            if ((0x2000000020L & l) == 0L) break;
                            this.jjstateSet[this.jjnewStateCnt++] = 109;
                            break;
                        }
                        case 111: {
                            if ((0x2000000020L & l) == 0L) break;
                            this.jjstateSet[this.jjnewStateCnt++] = 112;
                            break;
                        }
                        case 113: {
                            if ((0x800000008L & l) == 0L) break;
                            this.jjstateSet[this.jjnewStateCnt++] = 111;
                            break;
                        }
                        case 114: {
                            if ((0x20000000200L & l) == 0L) break;
                            this.jjstateSet[this.jjnewStateCnt++] = 113;
                            break;
                        }
                        case 115: {
                            if ((0x40000000400000L & l) == 0L) break;
                            this.jjstateSet[this.jjnewStateCnt++] = 114;
                            break;
                        }
                        case 116: {
                            if ((0x2000000020L & l) == 0L) break;
                            this.jjstateSet[this.jjnewStateCnt++] = 115;
                            break;
                        }
                    }
                } while (i != startsAt);
            } else {
                int i2 = (this.curChar & 0xFF) >> 6;
                long l2 = 1L << (this.curChar & 0x3F);
                do {
                    switch (this.jjstateSet[--i]) {
                        case 29: {
                            if ((jjbitVec0[i2] & l2) == 0L) break;
                            this.jjAddStates(6, 8);
                            break;
                        }
                        case 34: {
                            if ((jjbitVec0[i2] & l2) == 0L) break;
                            this.jjAddStates(9, 11);
                            break;
                        }
                    }
                } while (i != startsAt);
            }
            if (kind != Integer.MAX_VALUE) {
                this.jjmatchedKind = kind;
                this.jjmatchedPos = curPos;
                kind = Integer.MAX_VALUE;
            }
            ++curPos;
            i = this.jjnewStateCnt;
            this.jjnewStateCnt = startsAt;
            if (i == (startsAt = 117 - this.jjnewStateCnt)) break;
            try {
                this.curChar = this.input_stream.readChar();
            }
            catch (IOException e) {
                // empty catch block
                break;
            }
        }
        if (this.jjmatchedPos > strPos) {
            return curPos;
        }
        int toRet = Math.max(curPos, seenUpto);
        if (curPos < toRet) {
            i = toRet - Math.min(curPos, seenUpto);
            while (i-- > 0) {
                try {
                    this.curChar = this.input_stream.readChar();
                }
                catch (IOException e) {
                    throw new Error("Internal Error : Please send a bug report.");
                }
            }
        }
        if (this.jjmatchedPos < strPos) {
            this.jjmatchedKind = strKind;
            this.jjmatchedPos = strPos;
        } else if (this.jjmatchedPos == strPos && this.jjmatchedKind > strKind) {
            this.jjmatchedKind = strKind;
        }
        return toRet;
    }

    public DeviceParserTokenManager(SimpleCharStream stream) {
        this.input_stream = stream;
    }

    public DeviceParserTokenManager(SimpleCharStream stream, int lexState) {
        this(stream);
        this.SwitchTo(lexState);
    }

    public void ReInit(SimpleCharStream stream) {
        this.jjnewStateCnt = 0;
        this.jjmatchedPos = 0;
        this.curLexState = this.defaultLexState;
        this.input_stream = stream;
        this.ReInitRounds();
    }

    private void ReInitRounds() {
        this.jjround = -2147483647;
        int i = 117;
        while (i-- > 0) {
            this.jjrounds[i] = Integer.MIN_VALUE;
        }
    }

    public void ReInit(SimpleCharStream stream, int lexState) {
        this.ReInit(stream);
        this.SwitchTo(lexState);
    }

    public void SwitchTo(int lexState) {
        if (lexState >= 1 || lexState < 0) {
            throw new TokenMgrError("Error: Ignoring invalid lexical state : " + lexState + ". State unchanged.", 2);
        }
        this.curLexState = lexState;
    }

    protected Token jjFillToken() {
        String im = jjstrLiteralImages[this.jjmatchedKind];
        String curTokenImage = im == null ? this.input_stream.GetImage() : im;
        int beginLine = this.input_stream.getBeginLine();
        int beginColumn = this.input_stream.getBeginColumn();
        int endLine = this.input_stream.getEndLine();
        int endColumn = this.input_stream.getEndColumn();
        Token t = Token.newToken(this.jjmatchedKind, curTokenImage);
        t.beginLine = beginLine;
        t.endLine = endLine;
        t.beginColumn = beginColumn;
        t.endColumn = endColumn;
        return t;
    }

    public Token getNextToken() {
        int curPos;
        block7: {
            curPos = 0;
            do {
                try {
                    this.curChar = this.input_stream.BeginToken();
                }
                catch (IOException e) {
                    this.jjmatchedKind = 0;
                    Token matchedToken = this.jjFillToken();
                    return matchedToken;
                }
                this.jjmatchedKind = Integer.MAX_VALUE;
                this.jjmatchedPos = 0;
                curPos = this.jjMoveStringLiteralDfa0_0();
                if (this.jjmatchedKind == Integer.MAX_VALUE) break block7;
                if (this.jjmatchedPos + 1 >= curPos) continue;
                this.input_stream.backup(curPos - this.jjmatchedPos - 1);
            } while ((jjtoToken[this.jjmatchedKind >> 6] & 1L << (this.jjmatchedKind & 0x3F)) == 0L);
            Token matchedToken = this.jjFillToken();
            return matchedToken;
        }
        int error_line = this.input_stream.getEndLine();
        int error_column = this.input_stream.getEndColumn();
        String error_after = null;
        boolean EOFSeen = false;
        try {
            this.input_stream.readChar();
            this.input_stream.backup(1);
        }
        catch (IOException e1) {
            EOFSeen = true;
            String string = error_after = curPos <= 1 ? "" : this.input_stream.GetImage();
            if (this.curChar == '\n' || this.curChar == '\r') {
                ++error_line;
                error_column = 0;
            }
            ++error_column;
        }
        if (!EOFSeen) {
            this.input_stream.backup(1);
            error_after = curPos <= 1 ? "" : this.input_stream.GetImage();
        }
        throw new TokenMgrError(EOFSeen, this.curLexState, error_line, error_column, error_after, this.curChar, 0);
    }

    private void jjCheckNAdd(int state) {
        if (this.jjrounds[state] != this.jjround) {
            this.jjstateSet[this.jjnewStateCnt++] = state;
            this.jjrounds[state] = this.jjround;
        }
    }

    private void jjAddStates(int start, int end) {
        do {
            this.jjstateSet[this.jjnewStateCnt++] = jjnextStates[start];
        } while (start++ != end);
    }

    private void jjCheckNAddTwoStates(int state1, int state2) {
        this.jjCheckNAdd(state1);
        this.jjCheckNAdd(state2);
    }

    private void jjCheckNAddStates(int start, int end) {
        do {
            this.jjCheckNAdd(jjnextStates[start]);
        } while (start++ != end);
    }
}

