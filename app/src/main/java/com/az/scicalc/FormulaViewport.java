package com.az.scicalc;

/** A row keeps its own horizontal position; redraws do not follow the caret. */
final class FormulaViewport {
    float contentWidth, width, offset;
    long scrollbarUntil;
    float max(){return Math.max(0,contentWidth-width);}
    void resize(float content,float visible){contentWidth=Math.max(0,content);width=Math.max(1,visible);moveTo(offset);}
    void moveTo(float value){offset=Float.isFinite(value)?Math.max(0,Math.min(max(),value)):0;}
    void drag(float distance){moveTo(offset-distance);}
    void reveal(float caret,float margin){
        float inset=Math.min(Math.max(0,margin),width/3);
        if(caret<offset+inset)moveTo(caret-inset);
        else if(caret>offset+width-inset)moveTo(caret-width+inset);
    }
}
