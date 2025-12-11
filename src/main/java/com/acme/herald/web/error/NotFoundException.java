package com.acme.herald.web.error;

public class NotFoundException extends RuntimeException {
    public NotFoundException(String m) {
        super(m);
    }
}
