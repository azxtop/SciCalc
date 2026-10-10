package com.az.scicalc;
import org.junit.Test;
import static org.junit.Assert.*;
public class FormulaTest {
    @Test public void divisionPriorityKeepsSignsAndParentheses(){Formula f=new Formula();f.text("1÷−2+3");assertEquals("1/(-2)+3",f.calculationSource(1,true));f.clear();f.text("1÷(2+3)");f.symbol("Pi");assertEquals("1/((2+3)*Pi)",f.calculationSource(1,true));}
    @Test public void implicitDivisionPriorityPreservesWholeNumbersAndNestedFields(){Formula f=new Formula();f.text("1÷20");f.symbol("Pi");assertEquals("1/(20*Pi)",f.calculationSource(1,true));assertEquals("1/20*Pi",f.calculationSource(1,false));f.clear();f.template("sqrt","");f.text("1÷2");f.symbol("Pi");assertEquals("Sqrt(1/(2*Pi))",f.calculationSource(1,true));}
    @Test public void fractionWrapAndNavigation(){Formula f=new Formula();f.text("12");f.template("frac","");assertFalse(f.complete());f.text("3");f.right();assertTrue(f.complete());assertEquals("(12)/(3)",f.source(false));f.text("+");f.template("sqrt","");f.text("2");f.right();assertEquals("(12)/(3)+Sqrt(2)",f.source(false));f.left();f.delete();assertFalse(f.complete());}
    @Test public void nestedTemplatesAndAngles(){Formula f=new Formula();f.template("func","Sin");f.text("30");assertEquals("Sin((30)*Pi/180)",f.source(true));f.clear();f.template("integral","");f.text("x");f.power("2");assertTrue(f.complete());assertEquals("Integrate((x)^(2),x)",f.source(false));f.right();f.text("0");f.right();f.text("1");assertEquals("Integrate((x)^(2),{x,0,1})",f.source(false));}
    @Test public void emptySquareTemplate(){Formula f=new Formula();f.power("2");assertFalse(f.complete());f.text("3");assertTrue(f.complete());assertEquals("(3)^(2)",f.source(false));}
}
