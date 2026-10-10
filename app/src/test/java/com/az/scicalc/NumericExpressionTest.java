package com.az.scicalc;
import org.junit.Test;
import static org.junit.Assert.*;
public class NumericExpressionTest {
    @Test public void precedenceAndFunctions(){assertEquals(-4,new NumericExpression("-2^2").value(0),0);assertEquals(512,new NumericExpression("2^3^2").value(0),0);assertEquals(5,new NumericExpression("2x+1").value(2),0);assertEquals(.5,new NumericExpression("sin(pi/6)").value(0),1e-12);assertEquals(1,new NumericExpression("1e-3*1000").value(0),1e-12);}
    @Test public void calculusAndDiscontinuity(){NumericExpression f=new NumericExpression("x^2-2");assertEquals(Math.sqrt(2),f.root(0,2),1e-9);assertEquals(6,f.derivative(3),1e-8);assertEquals(1.0/3,new NumericExpression("x^2").integral(0,1),1e-10);try{new NumericExpression("1/x").root(-1,1);fail("must reject pole");}catch(IllegalArgumentException expected){}}
}
