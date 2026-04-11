package com.project.nagarSetu.error.exception;

public class VerificationCodeExpiredException extends RuntimeException{
    public VerificationCodeExpiredException(String message) {
        super(message);
    }
}
