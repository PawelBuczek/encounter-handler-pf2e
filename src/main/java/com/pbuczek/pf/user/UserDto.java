package com.pbuczek.pf.user;

import com.pbuczek.pf.user.UserType;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
public class UserDto {
    private UserType type;
    private String username;
    private String email;
    private String password;
}
