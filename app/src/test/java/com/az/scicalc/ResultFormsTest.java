package com.az.scicalc;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;

public class ResultFormsTest {
 final MathEngine engine=new MathEngine();
 @Test public void irrationalResultsRemainSymbolic(){assertEquals("Sqrt(2)",ResultForms.values(engine,"Sqrt(2)",10).get("分数"));}
 @Test public void rationalResultKeepsExactAndRecurringRepresentations(){Map<String,String> v=ResultForms.values(engine,"7/3",10);assertEquals("7/3",v.get("分数"));assertEquals("[2 1/3]",v.get("带分数"));assertEquals("Recurring(2,,3)",v.get("循环小数"));assertEquals("2°20′0.00″",v.get("度分秒"));assertEquals("7/3",engine.evaluate(Formula.result(v.get("循环小数")).source(1),false));}
 @Test public void finiteDecimalsDoNotGetARepeatingEntry(){Map<String,String> v=ResultForms.values(engine,"1.5",10);assertEquals("3/2",v.get("分数"));assertEquals("[1 1/2]",v.get("带分数"));assertFalse(v.containsKey("循环小数"));assertEquals("1°30′0.00″",v.get("度分秒"));}
 @Test public void integerMenuContainsFactorization(){Map<String,String> v=ResultForms.values(engine,"5226",10);assertEquals("2*3*13*67",v.get("质因数分解"));assertFalse(v.containsKey("小数"));assertFalse(v.containsKey("带分数"));}
 @Test public void signedRepeatingDecimalsAndSecondCarry(){String recurring=ResultForms.recurring("-7","3");assertEquals("-7/3",engine.evaluate(Formula.result(recurring).source(1),false));assertEquals("-1/6",engine.evaluate(Formula.result(ResultForms.recurring("-1","6")).source(1),false));assertEquals("1°0′0.00″",ResultForms.dms(59.999999/60));assertEquals("−30°30′0.00″",ResultForms.dms(-30.5));assertEquals("",ResultForms.recurring("1","8"));}
 @Test public void machineRealResultsRespectDisplayPrecisionButExactValuesStayExact(){assertEquals("30",ResultFormat.exactDisplay("30.000000000000004","normal",10,false));assertEquals("0.0015",ResultFormat.exactDisplay("0.0015000000000000002","normal",10,false));assertEquals("12345678901234567890",ResultFormat.exactDisplay("12345678901234567890","normal",10,false));assertEquals("1/3",ResultFormat.exactDisplay("1/3","normal",10,false));assertEquals("Sqrt(2)",ResultFormat.exactDisplay("Sqrt(2)","normal",10,false));}
}
