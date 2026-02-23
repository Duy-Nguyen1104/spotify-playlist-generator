package com.example.playlistgenerator.exception;

public class SpotifyApiException extends RuntimeException {
    private final int statusCode;

    public SpotifyApiException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public SpotifyApiException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = 500;
    }

    public int getStatusCode() { return statusCode; }
}
