package com.harshul.demo.kyc.entity;

public enum DocumentType {
    AADHAAR("Aadhaar".toUpperCase()),
    PAN("pan".toUpperCase()),
    PASSPORT("passport".toUpperCase()),
    DRIVING_LICENSE("driving-license".toUpperCase()),
    VOTER_ID("voter-id".toUpperCase());

    private final String fileName;

    DocumentType(String fileName) {
        this.fileName = fileName;
    }

    public String fileName() {
        return fileName;
    }

}