package com.az.scicalc;

import org.junit.Test;
import static org.junit.Assert.*;

public class AnswerParityTest {
    private final MathEngine engine=new MathEngine();
    @Test public void complexAnswerIsOneOperand(){
        Formula f=new Formula();f.answer("1+I");f.text("×2");assertEquals("2+I*2",engine.evaluate(f.source(1),false));
        f.clear();f.text("2÷");f.answer("1/2");assertEquals("4",engine.evaluate(f.source(1),false));
        assertEquals("4",engine.evaluate(f.calculationSource(1,true),false));
    }
    @Test public void negativeAnswerAndAdjacentDigitsKeepTheirMeaning(){
        Formula f=new Formula();f.answer("-3");f.power("2");assertEquals("9",engine.evaluate(f.source(1),false));
        f.clear();f.answer("2/3");f.text("2");assertEquals("4/3",engine.evaluate(f.source(1),false));
    }
    @Test public void answerIsFrozenAndDeletedAsOneItem(){
        Formula f=new Formula();f.answer("7/3");f.text("+1");assertEquals("10/3",engine.evaluate(f.source(1),false));
        f.delete();f.delete();f.delete();assertTrue(f.empty());
        f.answer("7/3");Formula restored=new Formula();restored.restore(f.save());assertEquals("7/3",restored.root.nodes.get(0).text);
        assertEquals("7/3",engine.evaluate(restored.source(1),false));
    }
    @Test public void previousAnswerNameAndValueSurvivePersistence(){
        Formula f=new Formula();f.answer("-8","PreAns");Formula restored=new Formula();restored.restore(f.save());
        assertEquals("PreAns",restored.root.nodes.get(0).answerName);assertEquals("-8",engine.evaluate(restored.source(1),false));
        restored.restore("[{\"k\":\"answer\",\"t\":\"5\",\"r\":[]}]");assertEquals("Ans",restored.root.nodes.get(0).answerName);
    }
}
