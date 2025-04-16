package com.example.id_card_reader.mrz;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class MRZInfo {
    private final String countryCode;
    private final String documentNumber;
    private final String idNumber;
    private final String dateOfBirth;
    private final String expiryDate;
    private final String gender;
    private final String nationality;
    private final String fullName;
    private final char unknownDigit;

    public MRZInfo(String countryCode, String documentNumber, String idNumber, String dateOfBirth, String expiryDate, String gender, String nationality, String fullName, char unknownDigit) {
        this.countryCode = countryCode;
        this.documentNumber = documentNumber;
        this.idNumber = idNumber;
        this.dateOfBirth = dateOfBirth;
        this.expiryDate = expiryDate;
        this.gender = gender;
        this.nationality = nationality;
        this.fullName = fullName;
        this.unknownDigit = unknownDigit;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public String getDocumentNumber() {
        return documentNumber;
    }

    public String getIdNumber() {
        return idNumber;
    }

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public String getExpiryDate() {
        return expiryDate;
    }

    public String getGender() {
        return gender;
    }

    public String getNationality() {
        return nationality;
    }

    public String getFullName() {
        return fullName;
    }

    public char getUnknownDigit() {
        return unknownDigit;
    }

    @Override
    public @NotNull String toString() {
        String dobYear =   dateOfBirth.substring(0, 2);
        String dobMonth =  dateOfBirth.substring(2, 4);
        String dobDay =    dateOfBirth.substring(4, 6);

        String expiryYear =  expiryDate.substring(0, 2);
        String expiryMonth = expiryDate.substring(2, 4);
        String expiryDay =   expiryDate.substring(4, 6);

        return "{\n" +
                "    countryCode:" +  countryCode  + ",\n" +
                "    documentNumber:" +  documentNumber  + ",\n" +
                "    idNumber:" + idNumber + ",\n" +
                "    dateOfBirth:" +  dobYear + "/" + dobMonth + "/" + dobDay  + ",\n" +
                "    expiryDate:" +  expiryYear + "/" + expiryMonth + "/" + expiryDay  + ",\n" +
                "    gender:" + gender + ",\n" +
                "    nationality:" + nationality + ",\n" +
                "    fullName:" +  fullName  + "\n" +
                "}";
    }
}