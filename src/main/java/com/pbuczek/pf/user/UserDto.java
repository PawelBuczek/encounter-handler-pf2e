package com.pbuczek.pf.user;

import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
public class UserDto {
    private String username;
    private String email;
    private String password;
}
