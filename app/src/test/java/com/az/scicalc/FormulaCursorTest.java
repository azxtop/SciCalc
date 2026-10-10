package com.az.scicalc;
import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class FormulaCursorTest {
    @Test public void nestedFractionCursorSurvivesSaveAndRestore(){Formula source=new Formula();source.text("12");source.template("frac","");source.template("sqrt","");source.text("34");source.left();Formula restored=new Formula();restored.restore(source.save());restored.restoreCursor(source.saveCursor());restored.text("9");assertEquals("(12)/(Sqrt(394))",restored.source(1));assertEquals("(12)/(Sqrt(34))",source.source(1));}
    @Test public void malformedCursorFallsBackToEndWithoutDroppingExpression()throws Exception{Formula f=new Formula();f.text("123");f.restoreCursor(new JSONObject().put("path",new JSONArray().put(9).put(4)).put("index",90));f.text("4");assertEquals("1234",f.source(1));}
    @Test public void placeholderReplacementStateSurvivesRestart(){Formula source=new Formula();source.text("20");source.root.defaultValue=true;Formula restored=new Formula();restored.restore(source.save());restored.restoreCursor(source.saveCursor());restored.text("3");assertEquals("3",restored.source(1));}
}
