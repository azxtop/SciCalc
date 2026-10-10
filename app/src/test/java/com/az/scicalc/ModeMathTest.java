package com.az.scicalc;
import org.junit.Test;
import static org.junit.Assert.*;
public class ModeMathTest {
 @Test public void integerFunctionsAndBitWidth(){assertEquals(1024,BaseExpression.evaluate("2**10",10));assertEquals(9,BaseExpression.evaluate("Sqrt(81)",10));assertEquals(6,BaseExpression.evaluate("GCD(18,24)",10));assertEquals(129,BaseExpression.evaluate("RoR(3,1)",10,8));assertEquals(-2,BaseExpression.evaluate("3 nand 1",10));assertEquals(1,BaseExpression.evaluate("5 xnor 3",10)&7);assertEquals(Long.MAX_VALUE,BaseExpression.evaluate("-1>>>1",10));}
 @Test public void presentationPreservesPolynomialTerms(){assertEquals("x^3+2*x^2-x+6",ResultFormat.orderPolynomial("6-x+2*x^2+x^3"));assertEquals("Sin(x)+x^2",ResultFormat.orderPolynomial("Sin(x)+x^2"));assertEquals("1/x+x^2",ResultFormat.orderPolynomial("1/x+x^2"));assertEquals("1.23*10^(-7)",ResultFormat.scientific("1.230000e-07",false));assertEquals("1.23E-7",ResultFormat.scientific("1.230000e-07",true));}
}
