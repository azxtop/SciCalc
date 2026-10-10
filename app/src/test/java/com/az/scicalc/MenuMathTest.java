package com.az.scicalc;
import org.junit.Test;
import static org.junit.Assert.*;

public class MenuMathTest {
 @Test public void savedToolParametersCanBeEdited(){String template="Inverse("+ToolInput.marker(0)+")";MathEngine e=new MathEngine();assertEquals("{{1/2,0},{0,1/4}}",e.evaluate(ToolInput.apply(template,new String[]{"{{2,0},{0,4}}"}),false).replaceAll("\\s",""));String solve="Solve(("+ToolInput.marker(0)+")*x^2+("+ToolInput.marker(1)+")*x+("+ToolInput.marker(2)+")==0,x)";assertEquals("{{x->2},{x->3}}",e.evaluate(ToolInput.apply(solve,new String[]{"1","-5","6"}),false).replaceAll("\\s",""));}
 @Test public void invalidToolInputDoesNotBecomeAResult(){try{ToolInput.evaluate(new MathEngine(),"Inverse(2)");fail();}catch(IllegalArgumentException expected){}}
 @Test public void malformedMatrixIsRejected(){try{ToolInput.evaluate(new MathEngine(),"Inverse({{2*Null,0},{0,4}})");fail();}catch(IllegalArgumentException expected){}}
 @Test public void numericFormatting(){assertEquals("12.35",ResultFormat.format("12.3456","fixed",2));assertEquals("12.3457 × 10^3",ResultFormat.format("12345.6789","engineering",6));assertEquals("1/3",ResultFormat.format("1/3","fixed",6));}
 @Test public void decimalPrecisionSurvivesEngineSerialization(){MathEngine e=new MathEngine();assertEquals("0.8333333333",ResultFormat.format(e.evaluate("5/6",true,10),"normal",10));assertEquals(1.0/3,Double.parseDouble(e.evaluate("1/3",true,12)),1e-12);assertEquals(1e-15,Double.parseDouble(e.evaluate("1/10^15",true,15)),1e-29);}
 @Test public void scriptPrintStringsStateAndIsolation(){MathEngine e=new MathEngine();MathEngine.ScriptResult result=e.runScript("Print(\"hello e\");a=2;a^3");assertEquals("8",result.value);assertTrue(result.output,result.output.contains("hello e"));assertEquals("3",e.runScript("a+1").value);assertEquals("a",new MathEngine().runScript("a").value);assertEquals("",e.runScript("Print(123)").value);}
}
