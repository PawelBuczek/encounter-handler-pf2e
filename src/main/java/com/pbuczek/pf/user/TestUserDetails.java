package com.pbuczek.pf.user;

import org.apache.commons.lang3.RandomStringUtils;

public interface TestUserDetails {
    // every user created for testing purposes should contain below username phrases in their username and email
    String TEST_USERNAME = "T_E_S_T_user-9.8.7.6.5.4.3.2.1";
    String TEST_EMAIL = "T_E_S_T_user-9.8.7.6.5.4.3.2.1";

    String TEST_PASSWORD = "exPass@1" + RandomStringUtils.random(40);
}
