package com.pbuczek.pf;

import org.apache.commons.lang3.RandomStringUtils;

import java.time.LocalDate;

public interface TestUserDetails {
    String TEST_USERNAME_ADMIN_1 = "_Test_" + LocalDate.now() + RandomStringUtils.random(24, true, true);
    String TEST_EMAIL_ADMIN_1 = TEST_USERNAME_ADMIN_1 + "@test.com";
    String TEST_USERNAME_STANDARD_1 = "_Test_" + LocalDate.now() + RandomStringUtils.random(24, true, true);
    String TEST_EMAIL_STANDARD_1 = TEST_USERNAME_STANDARD_1 + "@test.com";
    String TEST_USERNAME_STANDARD_2 = "_Test_" + LocalDate.now() + RandomStringUtils.random(24, true, true);
    String TEST_EMAIL_STANDARD_2 = TEST_USERNAME_STANDARD_2 + "@test.com";
    String TEST_PASSWORD = "aB@1" + RandomStringUtils.random(50);
}
