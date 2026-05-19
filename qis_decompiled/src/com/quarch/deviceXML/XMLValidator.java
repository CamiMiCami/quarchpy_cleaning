/*
 * Decompiled with CFR 0.152.
 */
package src.com.quarch.deviceXML;

public abstract class XMLValidator {
    protected ValidtyResult validtyResult = new ValidtyResult();

    public ValidtyResult getValidtyResult() {
        return this.validtyResult;
    }

    public class ValidtyResult {
        private String value = null;
        private String unit = null;
        private boolean isValidValue = false;
        private String validValue = null;
        private boolean isValidityError = false;

        public String getValue() {
            return this.value;
        }

        public String getUnit() {
            return this.unit;
        }

        public boolean isValidValue() {
            return this.isValidValue;
        }

        public String getValidValue() {
            return this.validValue;
        }

        public boolean isValidityError() {
            return this.isValidityError;
        }

        public void set(String value, String unit, boolean isValidValue, String validValue, boolean isValidityError) {
            this.value = value;
            this.unit = unit;
            this.isValidValue = isValidValue;
            this.validValue = validValue;
            this.isValidityError = isValidityError;
        }
    }
}

