package com.az.scicalc;

import org.matheclipse.core.eval.ExprEvaluator;
import org.matheclipse.core.interfaces.IExpr;
import java.util.concurrent.TimeUnit;

/** A single instance is owned by one background worker, never by the UI thread. */
public final class MathEngine {
    private ExprEvaluator evaluator;
    public static final class ScriptResult {public final String value,output;ScriptResult(String v,String o){value=v;output=o;}}
    public void initialize(){if(evaluator==null){evaluator=new ExprEvaluator(true,(short)100);evaluator.getEvalEngine().setIterationLimit(20000);evaluator.getEvalEngine().setRecursionLimit(512);}}
    public static String normalize(String s){
        s=s.replace("π","Pi").replace("×","*").replace("÷","/").replace("−","-").replace("∞","Infinity");
        String[][] names={{"sin","Sin"},{"cos","Cos"},{"tan","Tan"},{"asin","ArcSin"},{"acos","ArcCos"},{"atan","ArcTan"},{"ln","Log"},{"sqrt","Sqrt"},{"abs","Abs"},{"exp","Exp"}};
        for(String[] n:names)s=s.replaceAll("\\b"+n[0]+"(?=\\s*\\()",n[1]);
        return s.replaceAll("\\bpi\\b","Pi").replaceAll("\\bi\\b","I").replaceAll("\\be\\b","E");
    }
    public String evaluate(String s,boolean approximate){
        return evaluate(s,approximate,12);
    }
    public String evaluate(String s,boolean approximate,int digits){
        String input=normalize(s);if(approximate)input="N(("+input+"),"+Math.max(2,Math.min(15,digits))+")";
        return script(input);
    }
    public String script(String input){
        initialize();
        IExpr out=evaluator.evaluateWithTimeout(input,8,TimeUnit.SECONDS,true,null);
        if(out==null)throw new IllegalArgumentException("表达式没有产生结果");
        String result=out instanceof org.matheclipse.core.interfaces.INum n&&n.precision()<=16?Double.toString(n.getRealPart()):out.toString();
        if(result.equals("$Aborted"))throw new IllegalArgumentException("计算超过 8 秒，请简化表达式");
        if(result.equals("Null")||result.equals("$Failed"))throw new IllegalArgumentException("无法计算，请检查表达式");
        return result;
    }
    public ScriptResult runScript(String input){
        initialize();org.matheclipse.core.eval.EvalEngine state=evaluator.getEvalEngine();
        java.io.PrintStream oldOut=state.getOutPrintStream(),oldError=state.getErrorPrintStream();
        java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream(){@Override public synchronized void write(byte[] b,int off,int len){super.write(b,off,Math.min(len,Math.max(0,32768-size())));}@Override public synchronized void write(int b){if(size()<32768)super.write(b);}};
        java.io.PrintStream output=new java.io.PrintStream(bytes,true,java.nio.charset.StandardCharsets.UTF_8);
        state.setOutPrintStream(output);state.setErrorPrintStream(output);
        try{IExpr value=evaluator.evaluateWithTimeout(input,8,TimeUnit.SECONDS,true,null);if(value==null||value.toString().equals("$Aborted")||value.toString().equals("$Failed"))throw new IllegalArgumentException("脚本未完成，请检查表达式或缩短运行时间");String result=value.toString();output.flush();String printed=bytes.toString(java.nio.charset.StandardCharsets.UTF_8);if(bytes.size()>=32768)printed+="\n（输出已截断）";return new ScriptResult(result.equals("Null")?"":result,printed);}
        finally{state.setOutPrintStream(oldOut);state.setErrorPrintStream(oldError);output.close();}
    }
    public double value(String s){initialize();return evaluator.evalf(normalize(s));}
    public double at(String expression,String variable,double value){return value(expression.replaceAll("\\b"+variable+"\\b","("+Double.toString(value)+")"));}
}
