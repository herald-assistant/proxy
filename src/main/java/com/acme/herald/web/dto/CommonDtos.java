package com.acme.herald.web.dto;

public class CommonDtos {

    public record CommentReq(String text) {
    }

    public record LikeReq(boolean up) {
    }
}
