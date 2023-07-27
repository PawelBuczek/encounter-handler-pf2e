package com.pbuczek;

import org.junit.platform.suite.api.IncludeTags;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

@SelectPackages("com.pbuczek.pf.it")

@IncludeTags("IntegrationTest")
@Suite
@SuiteDisplayName("IntegrationTests")
public class IntegrationTests {

}