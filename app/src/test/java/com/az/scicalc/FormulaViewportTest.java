package com.az.scicalc;

import org.junit.Test;
import static org.junit.Assert.*;

public class FormulaViewportTest {
    @Test public void longRowCanReachBothEndsWithoutOverscroll(){
        FormulaViewport row=new FormulaViewport();row.resize(3000,1000);row.drag(-700);assertEquals(700,row.offset,0);
        row.drag(-5000);assertEquals(2000,row.offset,0);row.drag(5000);assertEquals(0,row.offset,0);
    }
    @Test public void redrawAndResizeKeepManualPositionUntilClamped(){
        FormulaViewport row=new FormulaViewport();row.resize(3000,1000);row.moveTo(800);row.resize(3000,1000);assertEquals(800,row.offset,0);
        row.resize(3000,2500);assertEquals(500,row.offset,0);row.resize(500,1000);assertEquals(0,row.offset,0);
    }
    @Test public void cursorRevealOnlyMovesWhenNecessary(){
        FormulaViewport row=new FormulaViewport();row.resize(3000,1000);row.moveTo(800);row.reveal(1200,60);assertEquals(800,row.offset,0);
        row.reveal(2900,60);assertEquals(1960,row.offset,0);row.reveal(100,60);assertEquals(40,row.offset,0);
    }
    @Test public void independentRowsAndInvalidSavedOffsetsStaySafe(){
        FormulaViewport input=new FormulaViewport(),output=new FormulaViewport();input.resize(3000,1000);output.resize(4000,900);
        input.drag(-600);assertEquals(0,output.offset,0);output.moveTo(Float.NaN);assertEquals(0,output.offset,0);
        output.moveTo(Float.POSITIVE_INFINITY);assertEquals(0,output.offset,0);
    }
}
