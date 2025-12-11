package com.acme.herald.web.error;

public class ConflictException extends RuntimeException {
    public ConflictException(String m) {
        super(m);
    }
}
