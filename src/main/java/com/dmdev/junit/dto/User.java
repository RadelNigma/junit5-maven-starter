package com.dmdev.junit.dto;

import lombok.Value;

@Value(staticConstructor = "of")
public class User {
    private Integer id;
    private String userName;
    private String password;
}
