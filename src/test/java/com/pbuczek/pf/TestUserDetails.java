package com.pbuczek.pf;

import org.apache.commons.lang3.RandomStringUtils;

public interface TestUserDetails {
    String TEST_USERNAME = "testUser";
    String TEST_EMAIL = "testUser@test.com";
    String TEST_PASSWORD = "exPass@1" + RandomStringUtils.random(40);
}
