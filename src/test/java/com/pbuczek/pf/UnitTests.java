package com.pbuczek.pf;

import org.junit.platform.suite.api.*;

@SelectPackages("com.pbuczek.pf")

@IncludeTags("UnitTest")
@IncludeClassNamePatterns({"(?i)^.*TEST$"})
@Suite
@SuiteDisplayName("UnitTests")
public class UnitTests {

}