package com.az.scicalc;
import java.util.*;

/** Native formula trees export without flattening fractions, grids, or editable templates. */
final class MathExport {
 static String latex(Formula f){return tex(f.root);}
 static String mathml(Formula f){return "<math xmlns=\"http://www.w3.org/1998/Math/MathML\">"+ml(f.root)+"</math>";}
 static String tex(Formula.Row r){StringBuilder s=new StringBuilder();for(Formula.Node n:r.nodes)s.append(tex(n));return s.toString();}
 static String t(String s){return s.replace("\\","\\backslash ").replace("{","\\{").replace("}","\\}").replace("_","\\_").replace("%","\\%").replace("×","\\times ").replace("·","\\cdot ").replace("÷","\\div ").replace("π","\\pi ").replace("−","-").replace("∞","\\infty ");}
 static String symbol(String s){switch(s){case "Pi":return "\\pi";case "E":return "e";case "I":return "i";case "Infinity":return "\\infty";case "->":return "\\to ";case "<=":return "\\le ";case ">=":return "\\ge ";case "!=":return "\\ne ";case "&&":return "\\land ";case "||":return "\\lor ";case "RandomReal()":return "\\operatorname{Ran\\#}";default:return t(s);}}
 static String tex(Formula.Node n){if(n.kind.equals("text"))return t(n.text);if(n.kind.equals("answer"))return tex(Formula.result(n.text));if(n.rows.isEmpty())return symbol(n.text);ArrayList<String> a=new ArrayList<>();for(Formula.Row r:n.rows)a.add(tex(r));String x=a.get(0),y=a.size()>1?a.get(1):"";
  switch(n.kind){
   case "frac":return "\\frac{"+x+"}{"+y+"}";case "pow":return "{"+x+"}^{"+y+"}";case "sqrt":return "\\sqrt{"+x+"}";case "root":return "\\sqrt["+y+"]{"+x+"}";case "nthroot":return "\\sqrt["+x+"]{"+y+"}";case "mixed":return x+"\\frac{"+y+"}{"+a.get(2)+"}";
   case "recurring":return x+"."+y+"\\overline{"+a.get(2)+"}";case "dms":return x+"^{\\circ}"+y+"^{\\prime}"+a.get(2)+"^{\\prime\\prime}";
   case "integral":case "sum":case "product":return (n.kind.equals("integral")?"\\int":n.kind.equals("sum")?"\\sum":"\\prod")+"_{"+(n.kind.equals("integral")?"":a.size()>3?a.get(3)+"=":"x=")+y+"}^{"+a.get(2)+"}"+x+(n.kind.equals("integral")?"\\,d"+(a.size()>3?a.get(3):"x"):"");
   case "indefinite":return "\\int "+x+"\\,d"+y;case "derivative":case "derivativeAt":return "\\frac{d}{d"+(a.size()>1?y:"x")+"}\\left("+x+"\\right)"+(n.kind.equals("derivativeAt")?"\\bigg|_{"+y+"="+a.get(2)+"}":"");case "limit":return "\\lim_{"+y+"\\to "+a.get(2)+(n.text.equals("-1")?"^+":n.text.equals("1")?"^-":"")+"}"+x;
   case "matrix":case "vector":{int cols=Integer.parseInt(n.text.split(",")[1]);StringBuilder s=new StringBuilder("\\begin{bmatrix}");for(int i=0;i<a.size();i++){if(i>0)s.append(i%cols==0?"\\\\":" & ");s.append(a.get(i));}return s+"\\end{bmatrix}";}
   case "binary":return x+"\\operatorname{"+t(n.text)+"}"+y;
   case "call":if(n.text.equals("LogBase"))return "\\log_{"+x+"}\\left("+y+"\\right)";return "\\operatorname{"+t(n.text)+"}\\left("+String.join(",",a)+"\\right)";
   default:if(n.text.equals("Abs"))return "\\left|"+x+"\\right|";String fn=n.text.equals("Log10")?"\\log":n.text.equals("Log")?"\\ln":n.text.equals("Sin")?"\\sin":n.text.equals("Cos")?"\\cos":n.text.equals("Tan")?"\\tan":n.text.isEmpty()?"":"\\operatorname{"+t(n.text)+"}";return fn+"\\left("+x+"\\right)";
  }
 }
 static String xml(String s){return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;");}
 static String tag(String tag,String s){return "<"+tag+">"+s+"</"+tag+">";}
 static String leaf(String s){s=s.equals("Pi")?"π":s.equals("E")?"e":s.equals("I")?"i":s.equals("Infinity")?"∞":s.equals("->")?"→":s.equals("<=")?"≤":s.equals(">=")?"≥":s.equals("!=")?"≠":s.equals("&&")?"∧":s.equals("||")?"∨":s;return tag(s.matches("[0-9.]+")?"mn":s.matches("[a-zA-Z][a-zA-Z0-9_]*")||s.equals("π")?"mi":"mo",xml(s));}
 static String ml(Formula.Row r){StringBuilder s=new StringBuilder();for(Formula.Node n:r.nodes)s.append(ml(n));if(r.empty())s.append("<mspace width=\"0.5em\"/>");return tag("mrow",s.toString());}
 static String ml(Formula.Node n){if(n.kind.equals("answer"))return ml(Formula.result(n.text));if(n.rows.isEmpty())return leaf(n.text);ArrayList<String> a=new ArrayList<>();for(Formula.Row r:n.rows)a.add(ml(r));String x=a.get(0),y=a.size()>1?a.get(1):"";
  switch(n.kind){
   case "frac":return tag("mfrac",x+y);case "pow":return tag("msup",x+y);case "sqrt":return tag("msqrt",x);case "root":return tag("mroot",x+y);case "nthroot":return tag("mroot",y+x);case "mixed":return tag("mrow",x+tag("mfrac",y+a.get(2)));case "recurring":return tag("mrow",x+leaf(".")+y+"<mover accent=\"true\">"+a.get(2)+leaf("¯")+"</mover>");case "dms":return tag("mrow",tag("msup",x+leaf("°"))+tag("msup",y+leaf("′"))+tag("msup",a.get(2)+leaf("″")));
   case "matrix":case "vector":{int cols=Integer.parseInt(n.text.split(",")[1]);StringBuilder s=new StringBuilder();for(int i=0;i<a.size();i++){if(i%cols==0)s.append("<mtr>");s.append(tag("mtd",a.get(i)));if(i%cols==cols-1)s.append("</mtr>");}return tag("mrow",leaf("[")+tag("mtable",s.toString())+leaf("]"));}
   case "integral":case "sum":case "product":return tag("mrow",tag("msubsup",leaf(n.kind.equals("integral")?"∫":n.kind.equals("sum")?"∑":"∏")+(n.kind.equals("integral")?y:tag("mrow",(a.size()>3?a.get(3):leaf("x"))+leaf("=")+y))+a.get(2))+x+(n.kind.equals("integral")?leaf("d")+(a.size()>3?a.get(3):leaf("x")):""));
   case "indefinite":return tag("mrow",leaf("∫")+x+leaf("d")+y);case "derivative":case "derivativeAt":return tag("mrow",tag("mfrac",leaf("d")+tag("mrow",leaf("d")+(a.size()>1?y:leaf("x"))))+leaf("(")+x+leaf(")")+(n.kind.equals("derivativeAt")?tag("msub",leaf("|")+tag("mrow",y+leaf("=")+a.get(2))):""));case "limit":return tag("mrow",tag("munder",leaf("lim")+tag("mrow",y+leaf("→")+a.get(2)+(n.text.isEmpty()?"":leaf(n.text.equals("-1")?"⁺":"⁻"))))+x);
   case "binary":return tag("mrow",x+leaf(n.text)+y);case "call":if(n.text.equals("LogBase"))return tag("mrow",tag("msub",leaf("log")+x)+leaf("(")+y+leaf(")"));StringBuilder s=new StringBuilder(leaf(n.text)+leaf("("));for(int i=0;i<a.size();i++){if(i>0)s.append(leaf(","));s.append(a.get(i));}return tag("mrow",s+leaf(")"));
   default:return tag("mrow",(n.text.isEmpty()?"":leaf(n.text.equals("Log10")?"log":n.text.equals("Log")?"ln":n.text))+leaf("(")+x+leaf(")"));
  }
 }
}
