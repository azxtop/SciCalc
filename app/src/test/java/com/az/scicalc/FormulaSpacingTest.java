package com.az.scicalc;

import java.lang.reflect.Method;
import org.junit.Test;
import static org.junit.Assert.*;

public class FormulaSpacingTest {
    private float[] spaces(Formula.Renderer renderer,Formula formula) throws Exception {
        Method method=Formula.Renderer.class.getDeclaredMethod("boundaries",Formula.Row.class,float.class);
        method.setAccessible(true);
        return (float[])method.invoke(renderer,formula.root,60f);
    }
    @Test public void thousandsAndDecimalGroupsKeepTheirOriginalSpacing() throws Exception {
        Formula formula=new Formula();formula.text("1234567.1234567");
        float[] values=spaces(new Formula.Renderer(),formula);
        assertEquals(15,values.length);
        for(int i=0;i<values.length;i++)assertEquals("Boundary "+i,i==0?0:i==1||i==4||i==11||i==14?18.6f:3f,values[i],.001f);
    }
    @Test public void OperatorsSeparateNumericGroupsAndKeepAsymmetricSpacing() throws Exception {
        Formula formula=new Formula();formula.text("1234+5678");
        assertArrayEquals(new float[]{0,18.6f,3,3,14.7f,6.3f,18.6f,3,3},spaces(new Formula.Renderer(),formula),.001f);
    }
    @Test public void GroupingOptionsAreIndependent() throws Exception {
        Formula formula=new Formula();formula.text("1234.1234");Formula.Renderer renderer=new Formula.Renderer();
        renderer.groupThousands=false;
        assertArrayEquals(new float[]{0,3,3,3,3,3,3,3,18.6f},spaces(renderer,formula),.001f);
        renderer.groupDecimals=false;
        assertArrayEquals(new float[]{0,3,3,3,3,3,3,3,3},spaces(renderer,formula),.001f);
    }
}
