package com.pbuczek.pf.security.dto;

import com.pbuczek.pf.security.UserType;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
public class UserDto {
    private UserType type;
    private String username;
    private String email;
}
