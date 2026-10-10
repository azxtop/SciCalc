package com.az.scicalc;

import java.util.Locale;

/** Small compiled real-valued expression tree for plotting; no script evaluation. */
public final class NumericExpression {
    interface Term {double eval(double x,double y,double t);}
    private final Term term;
    public NumericExpression(String source){Parser p=new Parser(source);term=p.sum();p.space();if(p.pos!=p.s.length())throw p.error("无法识别的符号");}
    public double value(double x){return term.eval(x,0,x);}
    public double value(double x,double y,double t){return term.eval(x,y,t);}
    private static final class Parser {
        final String s;int pos;
        Parser(String s){this.s=s.replace("π","pi").replace("×","*").replace("÷","/").replace("−","-").replace("√","sqrt");if(s.length()>2048)throw new IllegalArgumentException("表达式过长");}
        void space(){while(pos<s.length()&&Character.isWhitespace(s.charAt(pos)))pos++;}
        boolean eat(char c){space();if(pos<s.length()&&s.charAt(pos)==c){pos++;return true;}return false;}
        IllegalArgumentException error(String message){return new IllegalArgumentException(message+"（位置 "+(pos+1)+"）");}
        Term sum(){Term a=product();while(true){if(eat('+')){Term l=a,r=product();a=(x,y,t)->l.eval(x,y,t)+r.eval(x,y,t);}else if(eat('-')){Term l=a,r=product();a=(x,y,t)->l.eval(x,y,t)-r.eval(x,y,t);}else return a;}}
        Term product(){Term a=unary();while(true){space();if(eat('*')){Term l=a,r=unary();a=(x,y,t)->l.eval(x,y,t)*r.eval(x,y,t);}else if(eat('/')){Term l=a,r=unary();a=(x,y,t)->l.eval(x,y,t)/r.eval(x,y,t);}else if(eat('%')){Term l=a,r=unary();a=(x,y,t)->l.eval(x,y,t)%r.eval(x,y,t);}else if(pos<s.length()&&(s.charAt(pos)=='('||Character.isLetter(s.charAt(pos)))){Term l=a,r=unary();a=(x,y,t)->l.eval(x,y,t)*r.eval(x,y,t);}else return a;}}
        Term unary(){if(eat('+'))return unary();if(eat('-')){Term a=unary();return(x,y,t)->-a.eval(x,y,t);}return power();}
        Term power(){Term a=primary();if(eat('^')){Term l=a,r=unary();a=(x,y,t)->Math.pow(l.eval(x,y,t),r.eval(x,y,t));}while(eat('!')){Term l=a;a=(x,y,t)->factorial(l.eval(x,y,t));}return a;}
        Term primary(){space();if(eat('(')){Term a=sum();if(!eat(')'))throw error("缺少右括号");return a;}if(pos>=s.length())throw error("表达式未完成");char c=s.charAt(pos);
            if(Character.isDigit(c)||c=='.'){int start=pos;while(pos<s.length()&&(Character.isDigit(s.charAt(pos))||s.charAt(pos)=='.'))pos++;if(pos<s.length()&&(s.charAt(pos)=='e'||s.charAt(pos)=='E')&&pos+1<s.length()&&(Character.isDigit(s.charAt(pos+1))||s.charAt(pos+1)=='+'||s.charAt(pos+1)=='-')){pos++;if(s.charAt(pos)=='+'||s.charAt(pos)=='-')pos++;while(pos<s.length()&&Character.isDigit(s.charAt(pos)))pos++;}double n;try{n=Double.parseDouble(s.substring(start,pos));}catch(Exception e){throw error("数字格式错误");}return(x,y,t)->n;}
            if(Character.isLetter(c)){int start=pos;while(pos<s.length()&&(Character.isLetterOrDigit(s.charAt(pos))||s.charAt(pos)=='_'))pos++;String name=s.substring(start,pos).toLowerCase(Locale.ROOT);
                if(name.equals("x"))return(x,y,t)->x;if(name.equals("y"))return(x,y,t)->y;if(name.equals("t"))return(x,y,t)->t;if(name.equals("pi"))return(x,y,t)->Math.PI;if(name.equals("e"))return(x,y,t)->Math.E;
                if(!eat('('))throw error("未知变量 "+name);Term a=sum(),b=null;if(eat(','))b=sum();if(!eat(')'))throw error("缺少右括号");final Term arg=a,arg2=b;
                switch(name){
                 case "cot":return(x,y,t)->1/Math.tan(arg.eval(x,y,t));case "sec":return(x,y,t)->1/Math.cos(arg.eval(x,y,t));case "csc":return(x,y,t)->1/Math.sin(arg.eval(x,y,t));case "arccot":return(x,y,t)->Math.atan(1/arg.eval(x,y,t));case "arcsec":return(x,y,t)->Math.acos(1/arg.eval(x,y,t));case "arccsc":return(x,y,t)->Math.asin(1/arg.eval(x,y,t));case "coth":return(x,y,t)->1/Math.tanh(arg.eval(x,y,t));case "sech":return(x,y,t)->1/Math.cosh(arg.eval(x,y,t));case "csch":return(x,y,t)->1/Math.sinh(arg.eval(x,y,t));case "surd":if(arg2==null)throw error("n 次根需要两个参数");return(x,y,t)->{double a0=arg.eval(x,y,t),n=arg2.eval(x,y,t);return a0<0&&n==Math.rint(n)&&Math.abs(n%2)==1?-Math.pow(-a0,1/n):Math.pow(a0,1/n);};
                 case "sin":return(x,y,t)->Math.sin(arg.eval(x,y,t));case "cos":return(x,y,t)->Math.cos(arg.eval(x,y,t));case "tan":return(x,y,t)->Math.tan(arg.eval(x,y,t));case "asin":case "arcsin":return(x,y,t)->Math.asin(arg.eval(x,y,t));case "acos":case "arccos":return(x,y,t)->Math.acos(arg.eval(x,y,t));case "atan":case "arctan":return(x,y,t)->Math.atan(arg.eval(x,y,t));case "sinh":return(x,y,t)->Math.sinh(arg.eval(x,y,t));case "cosh":return(x,y,t)->Math.cosh(arg.eval(x,y,t));case "tanh":return(x,y,t)->Math.tanh(arg.eval(x,y,t));case "sqrt":return(x,y,t)->Math.sqrt(arg.eval(x,y,t));case "abs":return(x,y,t)->Math.abs(arg.eval(x,y,t));case "ln":return(x,y,t)->Math.log(arg.eval(x,y,t));case "log":return(x,y,t)->arg2==null?Math.log(arg.eval(x,y,t)):Math.log(arg2.eval(x,y,t))/Math.log(arg.eval(x,y,t));case "log10":return(x,y,t)->Math.log10(arg.eval(x,y,t));case "exp":return(x,y,t)->Math.exp(arg.eval(x,y,t));case "floor":return(x,y,t)->Math.floor(arg.eval(x,y,t));case "ceil":case "ceiling":return(x,y,t)->Math.ceil(arg.eval(x,y,t));case "round":return(x,y,t)->Math.rint(arg.eval(x,y,t));case "sign":return(x,y,t)->Math.signum(arg.eval(x,y,t));case "min":case "max":case "pow":if(arg2==null)throw error("此函数需要两个参数");return(x,y,t)->name.equals("min")?Math.min(arg.eval(x,y,t),arg2.eval(x,y,t)):name.equals("max")?Math.max(arg.eval(x,y,t),arg2.eval(x,y,t)):Math.pow(arg.eval(x,y,t),arg2.eval(x,y,t));default:throw error("绘图暂不支持函数 "+name);}
            }throw error("无法识别的表达式");
        }
    }
    private static double factorial(double a){if(a<0||a>170||a!=Math.rint(a))return Double.NaN;double n=1;for(int i=2;i<=a;i++)n*=i;return n;}
    public double derivative(double x){double h=Math.max(1,Math.abs(x))*1e-5;return(value(x+h)-value(x-h))/(2*h);}
    public double integral(double a,double b){int n=1024;double h=(b-a)/n,s=value(a)+value(b);for(int i=1;i<n;i++)s+=(i%2==0?2:4)*value(a+i*h);return s*h/3;}
    public double root(double a,double b){double fa=value(a),fb=value(b);if(!Double.isFinite(fa)||!Double.isFinite(fb)||fa*fb>0)throw new IllegalArgumentException("区间两端须异号且有限");if(fa==0)return a;if(fb==0)return b;for(int i=0;i<100;i++){double m=(a+b)/2,fm=value(m);if(!Double.isFinite(fm))throw new IllegalArgumentException("区间存在不连续点");if(Math.abs(fm)<1e-10)return m;if(fa*fm<0){b=m;fb=fm;}else{a=m;fa=fm;}}double r=(a+b)/2;if(Math.abs(value(r))>1e-6)throw new IllegalArgumentException("该区间没有连续零点");return r;}
}
