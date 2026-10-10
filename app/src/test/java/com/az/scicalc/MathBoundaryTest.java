package com.az.scicalc;

import org.junit.Test;
import static org.junit.Assert.*;

public class MathBoundaryTest {
    @Test public void invalidFrequenciesAndNonFiniteDataAreRejected(){
        for(double frequency:new double[]{0,-1,.5,Double.POSITIVE_INFINITY}){
            try{new StatsData(new double[]{1},new double[]{2},new double[]{frequency});fail("frequency accepted");}catch(IllegalArgumentException expected){}
        }
        try{new StatsData(new double[]{Double.NaN},new double[]{2},new double[]{1});fail();}catch(IllegalArgumentException expected){}
    }
    @Test public void insufficientAndOutOfDomainRegressionsFailClearly(){
        StatsData sameX=new StatsData(new double[]{1,1},new double[]{2,3},new double[]{1,1});
        try{sameX.regression("linear");fail();}catch(IllegalArgumentException expected){}
        StatsData zeroX=new StatsData(new double[]{0,1},new double[]{2,3},new double[]{1,1});
        for(String type:new String[]{"log","power","inverse"})try{zeroX.regression(type);fail(type);}catch(IllegalArgumentException expected){}
    }
    @Test public void numericValuesHandleElementaryAndSymbolicInput(){
        MathEngine engine=new MathEngine();assertEquals(.5,engine.value("Sin(Pi/6)"),1e-12);
        assertEquals(5.0/6,engine.value("1/2+1/3"),1e-12);assertEquals(5,engine.value("Det({{1,2},{3,11}})"),1e-12);
        assertTrue(Double.isNaN(engine.value("Sqrt(-1)")));assertTrue(Double.isNaN(engine.value("1/0")));
    }
}
