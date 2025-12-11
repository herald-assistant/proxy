package com.acme.herald.domain.dto;

import java.util.Map;

public record SearchItem(String issue_key, Map<String,Object> fields) {}
