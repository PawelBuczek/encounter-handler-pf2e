package com.pbuczek.pf;

import org.junit.platform.suite.api.IncludeTags;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

@SelectPackages("com.pbuczek.pf")

@IncludeTags("UnitTest")
@Suite
@SuiteDisplayName("UnitTests")
public class UnitTests {

}