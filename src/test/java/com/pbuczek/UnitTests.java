package com.pbuczek;

import org.junit.platform.suite.api.*;

@SelectPackages("com.pbuczek.pf")
@ExcludePackages("com.pbuczek.pf.it")

@IncludeTags("UnitTest")
@Suite
@SuiteDisplayName("UnitTests")
public class UnitTests {

}