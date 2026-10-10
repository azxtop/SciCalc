package com.az.scicalc;
import org.junit.Test;
import static org.junit.Assert.*;
public class ReleaseVersionTest {
    @Test public void numericVersionsDoNotUseStringOrdering(){assertTrue(ReleaseVersion.compare("v1.0.10","1.0.9")>0);assertTrue(ReleaseVersion.compare("v1.1.0","1.0.99")>0);assertTrue(ReleaseVersion.compare("v0.5.5","1.0.0")<0);assertEquals(0,ReleaseVersion.compare("v1.0.0","1.0.0"));assertEquals(0,ReleaseVersion.compare("1.0","1.0.0+test"));}
    @Test(expected=IllegalArgumentException.class) public void prereleaseTagIsNotTreatedAsStable(){ReleaseVersion.compare("v1.0.1-beta","1.0.0");}
    @Test public void downloadPageMustBelongToTheOfficialRepository(){assertTrue(ReleaseVersion.officialUrl("https://github.com/azxtop/SciCalc/releases/tag/v1.0.1"));assertFalse(ReleaseVersion.officialUrl("https://github.com.evil.example/azxtop/SciCalc/releases/tag/v1.0.1"));assertFalse(ReleaseVersion.officialUrl("https://github.com/another/project/releases/tag/v1.0.1"));assertFalse(ReleaseVersion.officialUrl("http://github.com/azxtop/SciCalc/releases/tag/v1.0.1"));assertFalse(ReleaseVersion.officialUrl("https://evil@github.com/azxtop/SciCalc/releases/tag/v1.0.1"));}
}
