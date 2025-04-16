package com.example.id_card_reader.mrz;

public class MRZParser {

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
        String filler = line1.substring(27, 29);  // "<<"
        int idNumberCheckDigit = Character.getNumericValue(line1.charAt(29));

        // Line 2
        String dobYYMMDD = line2.substring(0, 6); // 6 digits
        int dobCheckDigit = Character.getNumericValue(line2.charAt(6));
        String gender = line2.substring(7, 8);     // "M" or "F"
        String expiryDateYYMMDD = line2.substring(8, 14); // 6 digits
        int expiryDateCheckDigit = Character.getNumericValue(line1.charAt(29));
        String nationality = line2.substring(15, 18);    // "VNM"
        String filler2 = line2.substring(18, 29); // "<..."
        char unknownDigit = line2.charAt(29);

        // Line 3
        String name = line3.trim();

        // Basic validation (length and known values)
        if (!countryCode.equals("IDVNM") || !nationality.equals("VNM") || !filler.equals("<<") ||
                !gender.equals("M") && !gender.equals("F")) {
            return null; // Invalid MRZ
        }

        //split name
        String fullName = parseVietnameseName(name);

        //check digits
        if (!validateCheckDigit(documentNumber, documentNumberCheckDigit) ||
                !validateCheckDigit(dobYYMMDD, dobCheckDigit) ||
                !validateCheckDigit(expiryDateYYMMDD, expiryDateCheckDigit) ||
                !validateCheckDigit(idNumber, idNumberCheckDigit)) {
            return null;
        }

        return new MRZInfo(countryCode, documentNumber, idNumber, dobYYMMDD, expiryDateYYMMDD, gender, nationality, fullName, unknownDigit);
    }

    private static String parseVietnameseName(String name) {
        String[] parts = name.split("<<");
        String familyName = parts[0].trim();
        StringBuilder fullNameBuilder = new StringBuilder(familyName);

        if (parts.length > 1) {
            String remainingName = parts[1].trim();
            String[] nameParts = remainingName.split("<");
            for (String part : nameParts) {
                if (!part.isEmpty()) {
                    fullNameBuilder.append(" ").append(part.trim());
                }
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

        for (int i = data.length() - 1; i >= 0; i--) {
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
}