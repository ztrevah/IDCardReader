package com.example.id_card_reader.mrz;

import android.util.Log;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MRZParser {
    public static final String VIETNAMESE_MRZ_REGEX
            = "[A-Z0-9]{27}<<\\d" + "[\\r\\n]+"
            + "[A-Z0-9]{18}<{11}\\d" + "[\\r\\n]+"
            + "[A-Z<]{30}";

    /**
     * Parses a Vietnamese ID card MRZ string, which consists of three lines, each 30 characters long.
     *
     * @param mrz The MRZ string to parse.
     * @return A {@link MRZInfo} object containing the extracted data, or null if the MRZ is invalid.
     */
    public static MRZInfo parseVietnameseMRZ(String mrz) {
        if (mrz == null || mrz.length() != 90) {
            return null; // Invalid MRZ format
        }

        String line1 = mrz.substring(0, 30);
        String line2 = mrz.substring(30, 60);
        String line3 = mrz.substring(60, 90);


        // Line 1
        String countryCode = line1.substring(0, 5); // "IDVNM"
        String documentNumber = line1.substring(5, 14); // 9 digits
        int documentNumberCheckDigit = Character.getNumericValue(line1.charAt(14));
        String idNumber = line1.substring(15, 27); // 12 digits
        // String filler = line1.substring(27, 29);  // "<<"
        int idNumberCheckDigit = Character.getNumericValue(line1.charAt(29));



        // Line 2
        String dobYYMMDD = line2.substring(0, 6); // 6 digits
        int dobCheckDigit = Character.getNumericValue(line2.charAt(6));
        String gender = line2.substring(7, 8);     // "M" or "F"
        String expiryDateYYMMDD = line2.substring(8, 14); // 6 digits
        int expiryDateCheckDigit = Character.getNumericValue(line2.charAt(14));
        String nationality = line2.substring(15, 18);    // "VNM"
        // String filler2 = line2.substring(18, 29); // "<..."
        char unknownDigit = line2.charAt(29);

        // Line 3
        String name = line3.trim();

        // Basic validation (length and known values)
        if (!countryCode.equals("IDVNM") || !nationality.equals("VNM") ||
                (!gender.equals("M") && !gender.equals("F"))) {
            return null; // Invalid MRZ
        }

        //split name
        String fullName = parseVietnameseName(name);
        if(fullName == null) return null;

        //check digits
        if (!validateCheckDigit(documentNumber, documentNumberCheckDigit) ||
                !validateCheckDigit(dobYYMMDD, dobCheckDigit) ||
                !validateCheckDigit(expiryDateYYMMDD, expiryDateCheckDigit) ||
                !validateCheckDigit(idNumber, idNumberCheckDigit)) {
            return null;
        }



        return new MRZInfo(countryCode, documentNumber, idNumber, dobYYMMDD, expiryDateYYMMDD, gender, nationality, fullName, unknownDigit);
    }

    public static String parseVietnameseName(String name) {
        int pos = name.indexOf("<<");
        if(pos == -1) return null;

        String familyName = name.substring(0,pos);
        if(familyName.contains("<")) return null;
        StringBuilder fullNameBuilder = new StringBuilder(familyName);
        int lpos = pos + 2, rpos;
        for(int i=pos+2;i<name.length();i++) {
            if(name.charAt(i) == '<') {
                if(name.charAt(i-1) == '<') break;

                rpos = i - 1;
                fullNameBuilder.append(" ").append(name.substring(lpos, rpos + 1));
                lpos = i + 1;
            }
            else if(i == name.length() - 1) {
                fullNameBuilder.append(" ").append(name.substring(lpos));
            }
        }

        return fullNameBuilder.toString().trim();
    }

    /**
     * Calculates the check digit using the 7-3-1 algorithm.
     *
     * @param data The data string for which to calculate the check digit.
     * @return The calculated check digit.
     */
    private static int calculateCheckDigit(String data) {
        int sum = 0;
        int[] weights = {7, 3, 1};
        int weightIndex = 0;

        for (int i = 0; i < data.length(); i++) {
            int digit = Character.getNumericValue(data.charAt(i));
            sum += digit * weights[weightIndex];
            weightIndex = (weightIndex + 1) % 3;
        }

        // Corrected calculation:
        return  sum % 10;
    }

    /**
     * Validates the check digit.
     *
     * @param data The data string.
     * @param checkDigit The check digit to validate.
     * @return True if the check digit is valid, false otherwise.
     */
    private static boolean validateCheckDigit(String data, int checkDigit) {
        return calculateCheckDigit(data) == checkDigit;
    }

    public static String extractVietnameseMrzCode(String rawInput) {
        Pattern pattern = Pattern.compile(VIETNAMESE_MRZ_REGEX);
        Matcher matcher = pattern.matcher(rawInput);

        if (matcher.find()) {
            String mrzCode = matcher.group();
            mrzCode = mrzCode.replaceAll("[\\r\\n]+", "");

            return mrzCode;
        }

        return null;
    }
}