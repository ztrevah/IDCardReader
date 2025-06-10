package com.example.id_card_reader.models.dto;

public class ErrorResponseDto {
    public class ErrorData {
        private String message;
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }

    private ErrorData error;
    public ErrorData getError() { return error; }
    public void setError(ErrorData error) { this.error = error; }
}
