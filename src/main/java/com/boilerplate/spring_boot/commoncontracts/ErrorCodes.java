package com.boilerplate.spring_boot.commoncontracts;

public class ErrorCodes {


    public static final String GENERIC_ERROR_CODE = "1000";
    public static final String INVALID_REQUEST = "1001";
    public static final String INVALID_REQUEST_CODE = "1002";

    public enum CODE {
        BAD_REQUEST("BAD_REQUEST", "1002"),
        GENERIC_ERROR("DIGI_GENERIC_ERROR", "1000");
        private final String stringCode;

        private final  String numericCode;

        public String getStringCode() {
            return  stringCode;
        }

        public String getNumericCode() {
            return numericCode;
        }
        CODE(String stringCode, String numericCode) {
            this.stringCode = stringCode;
            this.numericCode = numericCode;
        }
    }
}
