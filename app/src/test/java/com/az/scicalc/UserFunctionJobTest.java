package com.az.scicalc;

import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

public class UserFunctionJobTest {
    private JSONObject trig()throws Exception{return new JSONObject().put("Turn",new JSONObject().put("params","x").put("body","Sin(x)").put("trig",true));}
    @Test public void degreeJobKeepsItsAngleWhenOtherJobsUseRadians()throws Exception{
        JSONObject functions=trig();MathEngine engine=new MathEngine();
        String degrees=Workbench.expandFunctions("Turn(90)",functions,0);
        String radians=Workbench.expandFunctions("Turn(Pi/2)",functions,1);
        assertEquals("1",engine.evaluate(degrees,false));
        assertEquals("1",engine.evaluate(radians,false));
        assertEquals("1",engine.evaluate(Workbench.expandFunctions("Turn(90)",functions,0),false));
    }
    @Test public void functionDefinitionSnapshotSurvivesLaterEditing()throws Exception{
        JSONObject current=trig(),snapshot=new JSONObject(current.toString());
        current.getJSONObject("Turn").put("body","Cos(x)");MathEngine engine=new MathEngine();
        assertEquals("1",engine.evaluate(Workbench.expandFunctions("Turn(90)",snapshot,0),false));
        assertEquals("0",engine.evaluate(Workbench.expandFunctions("Turn(90)",current,0),false));
    }
    @Test public void substitutionHandlesNestedArgumentsAndDistinctParameters()throws Exception{
        JSONObject functions=new JSONObject().put("Blend",new JSONObject().put("params","x,y").put("body","x^2+y"));
        assertEquals("13",new MathEngine().evaluate(Workbench.expandFunctions("Blend(1+2,4)",functions,1),false));
    }
}
