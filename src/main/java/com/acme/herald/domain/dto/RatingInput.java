package com.acme.herald.domain.dto;

import java.util.List;

public record RatingInput(List<Category> categories) {
    public record Category(String name, double value) {}
}
