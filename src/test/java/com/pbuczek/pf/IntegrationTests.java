package com.pbuczek.pf;

import org.junit.platform.suite.api.*;

@SelectPackages("com.pbuczek.pf")

@IncludeTags("IntegrationTest")
@IncludeClassNamePatterns({"(?i)^.*IT$"})
@Suite
@SuiteDisplayName("IntegrationTests")
public class IntegrationTests {

}