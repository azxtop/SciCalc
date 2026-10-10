package com.az.scicalc;
import org.junit.Test;
import static org.junit.Assert.*;
public class KeyEntryRegressionTest {
 private final MathEngine engine=new MathEngine();
 @Test public void closingNestedFunctionDoesNotProduceAnExtraParenthesis(){Formula f=new Formula();f.template("func","Log");f.template("func","Sin");f.text("90");assertTrue(f.closeFunction());assertEquals("Log(Sin((90)*Pi/180))",f.source(0));assertTrue(f.closeFunction());assertEquals("0",engine.evaluate(f.source(0),false));assertFalse(f.closeFunction());}
 @Test public void decimalPointAddsLeadingZeroAndIgnoresDuplicateDecimal(){Formula f=new Formula();f.decimalPoint();f.text("5");f.decimalPoint();assertEquals("0.5",f.source(1));f.text("+");f.decimalPoint();f.text("25");assertEquals(0.75,engine.value(f.source(1)),1e-12);}
 @Test public void sequentialDmsHasCorrectValueAndSurvivesSaving(){Formula f=new Formula();f.text("30");f.template("dms","");f.text("30");assertTrue(f.nextDms());f.text("18");assertTrue(f.nextDms());assertEquals(30.505,engine.value(f.source(1)),1e-12);Formula restored=new Formula();restored.restore(f.save());assertEquals(f.source(1),restored.source(1));assertEquals(3,restored.root.nodes.get(0).dmsMarked);}
 @Test public void emptyDmsStartsWithZeroDegreesAndKeepsMinutesHidden(){Formula f=new Formula();f.template("dms","");Formula restored=new Formula();restored.restore(f.save());assertEquals(1,restored.root.nodes.get(0).dmsMarked);assertTrue(restored.root.nodes.get(0).rows.get(1).defaultValue);assertEquals(0,engine.value(f.source(1)),1e-12);}
 @Test public void openFunctionStateIsSavedButComputedResultIsStillCorrect(){Formula f=new Formula();f.template("func","Cos");f.text("60");Formula restored=new Formula();restored.restore(f.save());assertFalse(restored.root.nodes.get(0).closed);assertEquals(0.5,engine.value(restored.source(0)),1e-12);}
 @Test public void decimalPointReplacesDefaultDmsZero(){Formula f=new Formula();f.template("dms","");f.decimalPoint();f.text("5");f.nextDms();assertEquals(0.5/60,engine.value(f.source(1)),1e-12);}
 @Test public void nestedLiteralGroupClosesBeforeItsFunction(){Formula f=new Formula();f.template("func","Sin");f.text("(30+60");assertFalse(f.closeFunction());f.text(")");assertTrue(f.closeFunction());assertEquals(1,engine.value(f.source(0)),1e-12);}
 @Test public void commasMoveThroughStructuredArgumentsAndClosingExitsThem(){Formula f=new Formula();f.call("Pol",2);f.text("3");assertTrue(f.nextArgument());f.text("4");assertTrue(f.closeFunction());f.text("+1");assertSame(f.root,f.cursor);assertTrue(f.source(1).endsWith("}+1"));}
 @Test public void exponentEntryComputesSignedPowersOfTen(){Formula f=new Formula();f.text("1.5");f.template("scientific","");f.text("−3");assertEquals(0.0015,engine.value(f.source(1)),1e-12);Formula restored=new Formula();restored.restore(f.save());assertEquals(f.source(1),restored.source(1));}
 @Test public void gcdAcceptsCommaSeparatedArgumentsInOneOpenFunction(){Formula f=new Formula();f.template("func","GCD");f.text("12,18");assertTrue(f.closeFunction());assertEquals("6",engine.evaluate(f.source(1),false));}
}
