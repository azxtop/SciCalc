package com.az.scicalc;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

/** Structural formula model: each template contains independently editable rows. */
public final class Formula {
    public static final class Node {
        public final String kind;
        public String text;
        public String answerName="Ans";
        public boolean closed=true;
        public int dmsMarked=3;
        public final List<Row> rows = new ArrayList<>();
        public Row parent;
        Node(String kind, String text) { this.kind=kind; this.text=text; }
        Node field() { Row r=new Row(); r.owner=this; rows.add(r); return this; }
        String source(boolean degrees) {return source(degrees?0:1);}
        String source(int angle) {return source(angle,false);}
        String source(int angle,boolean priority) {
            if(kind.equals("text")) return text.replace("×","*").replace("·","*").replace("÷","/").replace("−","-").replace("π","Pi").replace("∞","Infinity").replace("√","Sqrt");
            if(kind.equals("variable"))return text.equals("e")?"scvE":text;
            if(kind.equals("symbol"))return text;
            if(kind.equals("answer"))return "("+text+")";
            if(kind.equals("token"))return text;
            String a=rows.get(0).source(angle,priority),factor=angle==0?"*Pi/180":angle==2?"*Pi/200":"";
            String b=rows.size()>1?rows.get(1).source(angle,priority):"";
            switch(kind) {
                case "frac": return "("+a+")/("+b+")";
                case "pow": return "("+a+")^("+b+")";
                case "scientific":return "("+a+")*10^("+b+")";
                case "sqrt": return "Sqrt("+a+")";
                case "root": return "Surd("+a+","+b+")";
                case "nthroot":return "Surd("+b+","+a+")";
                case "mixed":return "("+a+")"+(a.startsWith("-")?"-":"+")+"("+b+")/("+rows.get(2).source(angle,priority)+")";
                case "recurring": {String non=rows.get(1).empty()?"0":b,rep=rows.get(2).source(angle,priority);int m=rows.get(1).empty()?0:non.length();return "("+a+")"+(a.startsWith("-")?"-":"+")+"("+non+")/10^"+m+(a.startsWith("-")?"-":"+")+"("+rep+")/(10^"+m+"*(10^"+rep.length()+"-1))";}
                case "dms":return "("+a+")"+(a.startsWith("-")?"-":"+")+"("+b+")/60"+(a.startsWith("-")?"-":"+")+"("+rows.get(2).source(angle,priority)+")/3600";
                case "integral": {String v=rows.size()>3?rows.get(3).source(angle,priority):"x";return rows.get(1).empty() && rows.get(2).empty() ? "Integrate("+a+","+v+")" : (text.isEmpty()?"Integrate":"NIntegrate")+"("+a+",{"+v+","+b+","+rows.get(2).source(angle,priority)+"}"+(text.isEmpty()?"":",Method->"+text)+")";}
                case "indefinite":return "Integrate("+a+","+b+")";
                case "derivative":return "D("+a+","+(rows.size()>1?b:"x")+")";
                case "derivativeAt":return "D("+a+","+b+")/."+b+"->("+rows.get(2).source(angle,priority)+")";
                case "sum":case "product": {String v=rows.size()>3?rows.get(3).source(angle,priority):"x";return (kind.equals("sum")?"Sum":"Product")+"("+a+",{"+v+","+b+","+rows.get(2).source(angle,priority)+"})";}
                case "limit":return "Limit("+a+","+b+"->("+rows.get(2).source(angle,priority)+")"+(text.isEmpty()?"":",Direction->"+text)+")";
                case "binary":
                    switch(text){case "dot":return "Dot("+a+","+b+")";case "xor":return "Xor("+a+","+b+")";case "nPr":return "Factorial("+a+")/Factorial(("+a+")-("+b+"))";case "nCr":return "Binomial("+a+","+b+")";case "mod":return "Mod("+a+","+b+")";case "÷R":return "{Quotient("+a+","+b+"),Mod("+a+","+b+")}";case "∠":return "("+a+")*(Cos(("+b+")"+factor+")+I*Sin(("+b+")"+factor+"))";}
                    return "("+a+")"+text+"("+b+")";
                case "matrix":case "vector":{String[] dim=text.split(",");int cols=Integer.parseInt(dim[1]);StringBuilder out=new StringBuilder("{");for(int i=0;i<rows.size();i++){if(i>0)out.append(',');if(kind.equals("matrix")&&i%cols==0)out.append('{');out.append(rows.get(i).source(angle,priority));if(kind.equals("matrix")&&i%cols==cols-1)out.append('}');}return out+"}";}
                case "call":
                    if(text.equals("Arg"))return "Arg("+a+")"+(angle==0?"*180/Pi":angle==2?"*200/Pi":"");
                    if(text.equals("Conjg"))return "Conjugate("+a+")";
                    if(text.equals("LogBase"))return "Log("+a+","+b+")";
                    if(text.equals("RanInt#"))return "RandomInteger({"+a+","+b+"})";
                    if(text.equals("Pol"))return "{Sqrt(("+a+")^2+("+b+")^2),ArcTan("+a+","+b+")"+(angle==0?"*180/Pi":angle==2?"*200/Pi":"")+"}";
                    if(text.equals("Rec"))return "{("+a+")*Cos(("+b+")"+factor+"),("+a+")*Sin(("+b+")"+factor+")}";
                    String[] args=new String[rows.size()];for(int i=0;i<args.length;i++)args[i]=rows.get(i).source(angle,priority);return MathFunctions.call(text,args);
                case "func":
                    if(text.equals("Arg"))return "Arg("+a+")"+(angle==0?"*180/Pi":angle==2?"*200/Pi":"");
                    if(text.equals("Conjg"))return "Conjugate("+a+")";
                    if(text.equals("Log10"))return "Log(10,"+a+")";
                    if(angle!=1 && (text.equals("Sin") || text.equals("Cos") || text.equals("Tan")||text.equals("Cot")||text.equals("Sec")||text.equals("Csc"))) return text+"(("+a+")"+factor+")";
                    if(angle!=1 && (text.equals("ArcSin") || text.equals("ArcCos") || text.equals("ArcTan")||text.equals("ArcCot")||text.equals("ArcSec")||text.equals("ArcCsc"))) return "("+text+"("+a+")"+(angle==0?"*180/Pi":"*200/Pi")+")";
                    return text+"("+a+")";
                default: return a;
            }
        }
        String arguments(int angle){StringBuilder s=new StringBuilder();for(Row row:rows){if(s.length()>0)s.append(',');s.append(row.source(angle));}return s.toString();}
        boolean complete() {
            if(kind.equals("integral")&&text.isEmpty()&&rows.get(1).empty()&&rows.get(2).empty())return !rows.get(0).empty()&&rows.get(0).complete()&&(rows.size()<4||!rows.get(3).empty());
            for(int i=0;i<rows.size();i++){Row r=rows.get(i);if(kind.equals("recurring")&&i==1&&r.empty())continue;if(r.empty()||!r.complete())return false;}
            if(kind.equals("recurring"))return rows.get(0).source(1).matches("[0-9]+")&&rows.get(1).source(1).matches("[0-9]*")&&rows.get(2).source(1).matches("[0-9]+");
            return true;
        }
        JSONObject json() throws Exception { JSONObject j=new JSONObject(); j.put("k",kind); j.put("t",text);if(kind.equals("answer")&&!answerName.equals("Ans"))j.put("answerName",answerName);if(!closed)j.put("closed",false);if(kind.equals("dms")){j.put("dmsMarked",dmsMarked);JSONArray hidden=new JSONArray();for(Row row:rows)hidden.put(row.defaultValue);j.put("dmsHidden",hidden);} JSONArray a=new JSONArray(); for(Row r:rows)a.put(r.json());j.put("r",a);return j; }
        static Node from(JSONObject j) throws Exception { Node n=new Node(j.getString("k"),j.getString("t")); n.answerName=j.optString("answerName","Ans");n.closed=j.optBoolean("closed",true);n.dmsMarked=j.optInt("dmsMarked",3);JSONArray a=j.getJSONArray("r");for(int i=0;i<a.length();i++){Row r=Row.from(a.getJSONArray(i));r.owner=n;n.rows.add(r);}if(n.kind.equals("dms")&&j.has("dmsHidden")){JSONArray hidden=j.getJSONArray("dmsHidden");for(int i=0;i<n.rows.size();i++)n.rows.get(i).defaultValue=hidden.optBoolean(i,false);}return n; }
    }
    public static final class Row {
        public final List<Node> nodes=new ArrayList<>();
        public Node owner;
        boolean defaultValue=false;
        public boolean empty(){return nodes.isEmpty();}
        void add(Node n){n.parent=this;nodes.add(n);}
        String source(boolean degrees){return source(degrees?0:1);}
        String source(int angle){return source(angle,false);}
        String source(int angle,boolean priority){
            StringBuilder b=new StringBuilder();Node previous=null;
            for(int i=0;i<nodes.size();i++){
                Node n=nodes.get(i);if(previous!=null&&adjacent(previous,n))b.append('*');
                if(priority&&n.kind.equals("text")&&n.text.matches("[÷/]")){
                    b.append("/(");int end=i+1,depth=0;Node last=null;
                    while(end<nodes.size()){
                        Node next=nodes.get(end);String token=next.kind.equals("text")?next.text:"";
                        if(last==null&&token.matches("[+−-]")){b.append(next.source(angle,priority));end++;continue;}
                        boolean number=last!=null&&last.kind.equals("text")&&next.kind.equals("text")&&last.text.matches("[0-9.]")&&next.text.matches("[0-9.]");
                        if(depth==0&&last!=null&&!adjacent(last,next)&&!number)break;
                        if(last!=null&&adjacent(last,next))b.append('*');
                        b.append(next.source(angle,priority));if(token.equals("("))depth++;if(token.equals(")"))depth--;last=next;end++;
                    }
                    b.append(')');i=end-1;previous=last;continue;
                }
                b.append(n.source(angle,priority));previous=n;
            }return b.toString();
        }
        private boolean adjacent(Node left,Node right){boolean l=!left.kind.equals("text")&&!left.kind.equals("token")||left.text.matches("[0-9.)π]");boolean r=!right.kind.equals("text")&&!right.kind.equals("token")||right.text.matches("[0-9(π]");if(left.kind.equals("text")&&right.kind.equals("text")&&left.text.matches("[0-9.]")&&right.text.matches("[0-9.]") )return false;return l&&r;}
        boolean complete(){for(Node n:nodes)if(!n.complete())return false;return true;}
        JSONArray json() throws Exception {JSONArray a=new JSONArray();for(Node n:nodes)a.put(n.json());return a;}
        static Row from(JSONArray a) throws Exception {Row r=new Row();for(int i=0;i<a.length();i++)r.add(Node.from(a.getJSONObject(i)));return r;}
    }
    public Row root=new Row(), cursor=root;
    public int index=0;
    public String source(boolean degrees){return root.source(degrees);}
    public String source(int angle){return root.source(angle);}
    public String calculationSource(int angle,boolean priority){return root.source(angle,priority);}
    public boolean complete(){return root.complete();}
    public boolean empty(){return root.empty();}
    public void clear(){root=new Row();cursor=root;index=0;}
    public void text(String s){for(int i=0;i<s.length();i++)insert(new Node("text",s.substring(i,i+1)));}
    private void insert(Node n){if(cursor.defaultValue){cursor.nodes.clear();index=0;cursor.defaultValue=false;}n.parent=cursor;cursor.nodes.add(index++,n);}
    public void variable(String name){insert(new Node("variable",name));}
    public void symbol(String name){insert(new Node("symbol",name));}
    public void answer(String value){answer(value,"Ans");}
    public void answer(String value,String name){Node node=new Node("answer",value);node.answerName=name;insert(node);}
    public void token(String source){insert(new Node("token",source));}
    public void call(String name,int fields){if(fields==0){symbol(name+"()");return;}Node n=new Node("call",name);for(int i=0;i<fields;i++)n.field();insert(n);cursor=n.rows.get(0);index=0;}
    public void grid(int r,int c,boolean vector){Node n=new Node(vector?"vector":"matrix",r+","+c);for(int i=0;i<r*c;i++)n.field();insert(n);cursor=n.rows.get(0);index=0;}
    public void template(String kind,String name){
        Node n=new Node(kind,name).field();if(kind.equals("func")&&!name.equals("Abs"))n.closed=false;boolean captured=false;
        if(kind.equals("frac")||kind.equals("pow")||kind.equals("scientific")||kind.equals("binary")||kind.equals("mixed")||kind.equals("nthroot")||kind.equals("dms")){
            if(kind.equals("frac")||kind.equals("pow")||kind.equals("scientific")||kind.equals("binary"))n.field();
            if(index>0){
                int start=index-1;
                Node prev=cursor.nodes.get(start);
                if(prev.kind.equals("text") && prev.text.matches("[0-9.]"))while(start>0 && cursor.nodes.get(start-1).kind.equals("text") && cursor.nodes.get(start-1).text.matches("[0-9.]"))start--;
                if(!prev.kind.equals("text") || prev.text.matches("[0-9.a-zA-Zπ]")){
                    List<Node> before=new ArrayList<>(cursor.nodes.subList(start,index));cursor.nodes.subList(start,index).clear();index=start;for(Node b:before)n.rows.get(0).add(b);captured=true;
                }
            }
        }
        if(kind.equals("integral")||kind.equals("sum")||kind.equals("product")){n.field().field().field();addText(n.rows.get(3),"x");}
        if(kind.equals("indefinite")||kind.equals("derivative")){n.field();addText(n.rows.get(1),"x");}
        if(kind.equals("derivativeAt")||kind.equals("limit")){n.field().field();addText(n.rows.get(1),"x");}
        if(kind.equals("mixed")){n.text="bracketed";n.field().field();}
        if(kind.equals("recurring")){n.field().field();addText(n.rows.get(0),"0");}
        if(kind.equals("dms")){n.dmsMarked=1;n.field().field();for(Row row:n.rows)if(row.empty())addText(row,"0");}
        if(kind.equals("nthroot")){n.field();}
        if(kind.equals("root")){n.field();addText(n.rows.get(1),name.isEmpty()?"3":name);}
        for(int i=0;i<n.rows.size();i++){Row row=n.rows.get(i);if(!row.empty()&&!(i==0&&captured)&&!(kind.equals("frac")||kind.equals("pow")||kind.equals("binary")))row.defaultValue=true;}
        boolean baseFilled=!n.rows.get(0).empty();insert(n);cursor=n.rows.get(kind.equals("recurring")?2:kind.equals("dms")?1:baseFilled?1:0);index=cursor.nodes.size();
    }
    /** Close the surrounding function rather than inserting a second literal parenthesis. */
    public boolean closeFunction(){
        int depth=0;for(int i=0;i<index;i++){Node n=cursor.nodes.get(i);if(n.kind.equals("text")){if(n.text.equals("("))depth++;else if(n.text.equals(")"))depth--;}}if(depth>0)return false;
        Row row=cursor;while(row.owner!=null){Node node=row.owner;if(node.kind.equals("func")&&!node.text.equals("Abs")||node.kind.equals("call")){node.closed=true;cursor=node.parent;index=cursor.nodes.indexOf(node)+1;return true;}row=node.parent;}
        return false;
    }
    public boolean nextArgument(){if(cursor.owner==null||!cursor.owner.kind.equals("call")||index!=cursor.nodes.size())return false;Node n=cursor.owner;int field=n.rows.indexOf(cursor);if(field+1>=n.rows.size())return false;right();return true;}
    public void decimalPoint(){
        if(cursor.defaultValue){text("0");text(".");return;}
        int start=index;while(start>0&&cursor.nodes.get(start-1).kind.equals("text")&&cursor.nodes.get(start-1).text.matches("[0-9.]"))start--;
        for(int i=start;i<index;i++)if(cursor.nodes.get(i).text.equals("."))return;
        if(start==index)text("0");text(".");
    }
    public boolean nextDms(){if(cursor.owner==null||!cursor.owner.kind.equals("dms"))return false;Node node=cursor.owner;int field=node.rows.indexOf(cursor);node.dmsMarked=Math.max(node.dmsMarked,field+1);cursor.defaultValue=false;index=cursor.nodes.size();right();return true;}
    public void power(String exponent){template("pow","");Node n=cursor.owner;n.rows.get(1).nodes.clear();addText(n.rows.get(1),exponent);if(!n.rows.get(0).empty()){cursor=n.parent;index=cursor.nodes.indexOf(n)+1;}}
    public void right(){cursor.defaultValue=false;
        if(index<cursor.nodes.size()){Node n=cursor.nodes.get(index);if(!n.rows.isEmpty()){cursor=n.rows.get(0);index=0;}else index++;}
        else exit(true);
    }
    public void left(){cursor.defaultValue=false;
        if(index>0){Node n=cursor.nodes.get(index-1);if(!n.rows.isEmpty()){cursor=n.rows.get(n.rows.size()-1);index=cursor.nodes.size();}else index--;}
        else exit(false);
    }
    private void exit(boolean next){
        Node o=cursor.owner;if(o==null)return;int r=o.rows.indexOf(cursor);
        if(next && r+1<o.rows.size()){cursor=o.rows.get(r+1);index=0;}
        else if(!next&&r>0){cursor=o.rows.get(r-1);index=cursor.nodes.size();}
        else {if(next&&o.kind.equals("func"))o.closed=true;cursor=o.parent;index=cursor.nodes.indexOf(o)+(next?1:0);}
    }
    public void vertical(boolean down){Node o=cursor.owner;if(o!=null&&o.rows.size()>1){int r=o.rows.indexOf(cursor),step=o.kind.equals("matrix")?Integer.parseInt(o.text.split(",")[1]):1;int target=Math.max(0,Math.min(o.rows.size()-1,r+(down?step:-step)));cursor=o.rows.get(target);index=Math.min(index,cursor.nodes.size());}else if(down){while(cursor.owner!=null)exit(true);index=cursor.nodes.size();}else {cursor=root;index=0;}}
    public void delete(){cursor.defaultValue=false;if(index>0)cursor.nodes.remove(--index);else if(cursor.owner!=null){Node o=cursor.owner;int field=o.rows.indexOf(cursor);if(field>0){cursor=o.rows.get(field-1);index=cursor.nodes.size();}else{cursor=o.parent;index=cursor.nodes.indexOf(o);cursor.nodes.remove(index);}}}
    public String save(){try{return root.json().toString();}catch(Exception e){return "[]";}}
    public JSONObject saveCursor(){
        JSONObject state=new JSONObject();ArrayList<Integer> path=new ArrayList<>();Row row=cursor;
        while(row!=root&&row.owner!=null){Node owner=row.owner;path.add(0,owner.rows.indexOf(row));path.add(0,owner.parent.nodes.indexOf(owner));row=owner.parent;}
        try{state.put("path",new JSONArray(path)).put("index",index).put("rootDefault",root.defaultValue).put("cursorDefault",cursor.defaultValue);}catch(Exception ignored){}
        return state;
    }
    public void restoreCursor(JSONObject state){
        if(state==null)return;
        try{Row row=root;JSONArray path=state.optJSONArray("path");if(path!=null)for(int i=0;i+1<path.length();i+=2)row=row.nodes.get(path.getInt(i)).rows.get(path.getInt(i+1));cursor=row;index=Math.max(0,Math.min(row.nodes.size(),state.optInt("index",row.nodes.size())));root.defaultValue=state.optBoolean("rootDefault");cursor.defaultValue=state.optBoolean("cursorDefault");}catch(Exception ignored){cursor=root;index=root.nodes.size();}
    }
    public void restore(String s){try{root=Row.from(new JSONArray(s));cursor=root;index=root.nodes.size();}catch(Exception e){clear();}}
    public void load(String s){clear();text(s);}
    public void insertSaved(String s){try{Row row=Row.from(new JSONArray(s));for(Node n:row.nodes)insert(n);}catch(Exception e){throw new IllegalArgumentException("无法读取公式",e);}}
    public void insertExpression(String s){s=s.replace('×','*').replace('÷','/').replace('−','-').replace("π","Pi");try{ResultParser parser=new ResultParser(s);Row row=parser.sum();parser.skip();if(parser.i!=s.length())throw new IllegalArgumentException();for(Node n:row.nodes)insert(n);}catch(Exception e){text(s);}}

    public static class Box {final float w,a,d;Box(float w,float a,float d){this.w=w;this.a=a;this.d=d;}}
    public static final class Spot {final Row row;final int index;final float x,y,a,d;Spot(Row r,int i,float x,float y){this(r,i,x,y,0,0);}Spot(Row r,int i,float x,float y,float a,float d){row=r;index=i;this.x=x;this.y=y;this.a=a;this.d=d;}}
    public static final class Renderer {
        private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        public final List<Spot> spots=new ArrayList<>();
        public int color=0xff142725, accent=0xff9465eb;
        public Formula editor;
        public boolean showCursor=true, stretchRoots=true,closeParen=false;
        public boolean groupThousands=true,groupDecimals=true,decimalComma=false,fractionMultiply=true,descendingPowers=false;
        public int ansStyle=2;
        public int answerBackground=0xffd4e4e3;
        public float fractionScale=1f, strokeRatio=.05f;
        private boolean compactFractions;
        private Renderer answerRenderer;
        private Renderer answerRenderer(){
            if(answerRenderer==null)answerRenderer=new Renderer();
            Renderer r=answerRenderer;r.typeface=typeface;r.color=color;r.groupThousands=groupThousands;r.groupDecimals=groupDecimals;r.decimalComma=decimalComma;r.fractionMultiply=fractionMultiply;r.closeParen=closeParen;r.compactFractions=true;r.strokeRatio=.08f;
            return r;
        }
        private Box answerBox(Node n,float z){Row row=result(n.text);Box value=answerRenderer().measure(row,z*.95f);boolean tall=value.a+value.d>z*1.4f;return new Box(Math.max(value.w+z*.298f,ansStyle==1?textWidth(n.answerName,z*.32f)+z*.16f:0),value.a+z*(tall?.07f:.18f),value.d+z*(ansStyle==1?.45f:tall?.08f:.14f));}
        public android.graphics.Typeface typeface=UiFonts.REGULAR;
        private float textWidth(String s,float size){p.setTypeface(typeface);p.setTextSize(size);return p.measureText(s);}
        private String display(Node n){if(n.kind.equals("symbol"))return n.text.equals("E")?"e":n.text.equals("I")?"i":n.text.equals("Pi")?"π":n.text.equals("RandomReal()")?"Ran#":n.text;return n.text.replace("&&","and").replace("||","or").replace("!=","≠").replace("<=","≤").replace(">=","≥").replace("->","→").replace(":=",":=");}
        private String hint(Row r){if(r.owner==null||!r.owner.kind.equals("call"))return "";if(r.owner.text.equals("RanInt#"))return r.owner.rows.indexOf(r)==0?"min":"max";return r.owner.text.matches("Det|MatrixRank|Inverse|Eigenvalues|Eigenvectors|Transpose|Tr|RowReduce")?"matrix":"";}
        private String functionName(String n){return n.equals("Log10")?"Log":n.equals("Log")?"Ln":n.equals("ArcSin")?"Sin⁻¹":n.equals("ArcCos")?"Cos⁻¹":n.equals("ArcTan")?"Tan⁻¹":n.equals("ArcCot")?"Cot⁻¹":n;}
        private Node fraction(Node n){Node f=new Node("frac","");f.rows.add(n.rows.get(1));f.rows.add(n.rows.get(2));return f;}
        private float[] columns(Node n,float z){int count=Integer.parseInt(n.text.split(",")[1]);float[] widths=new float[count];for(int i=0;i<n.rows.size();i++)widths[i%count]=Math.max(widths[i%count],measure(n.rows.get(i),z).w+z*.45f);return widths;}
        private float rowHeight(Node n,float z){float h=z*1.35f;for(Row row:n.rows){Box b=measure(row,z);h=Math.max(h,b.a+b.d+z*.3f);}return h;}
        private static boolean oneOf(String text,String chars){return text.length()==1&&chars.indexOf(text.charAt(0))>=0;}
        private static boolean digit(Node n){return n.kind.equals("text")&&oneOf(n.text,"0123456789");}
        private static boolean numberPart(Node n){return n.kind.equals("text")&&oneOf(n.text,"0123456789.");}
        private float gap(Node n,float z){return n.kind.equals("text")&&oneOf(n.text,"+−×÷=·<>")||n.kind.equals("token")?z*.14f:n.kind.equals("text")?z*.025f:z*.04f;}
        private boolean multiplyBefore(Row r,int i){if(!fractionMultiply||i==0||!r.nodes.get(i).kind.equals("frac"))return false;Node left=r.nodes.get(i-1);return !left.kind.equals("text")&&!left.kind.equals("token")||oneOf(left.text,"0123456789.)π");}
        // Inspect each numeric run once. The old per-boundary scan walked the
        // entire number repeatedly, including during every ripple frame.
        private float[] boundaries(Row row,float z){
            int count=row.nodes.size();float[] spaces=new float[count];
            for(int i=1;i<count;i++){
                Node left=row.nodes.get(i-1),right=row.nodes.get(i);
                float space=gap(left,z)+gap(right,z);
                if(multiplyBefore(row,i))space+=textWidth("×",z)+z*.2f;
                if(left.kind.equals("text")&&oneOf(left.text,"+−×÷=·<>"))space-=z*.06f;
                if(right.kind.equals("text")&&oneOf(right.text,"+−×÷=·<>"))space+=z*.08f;
                spaces[i]=space;
            }
            for(int start=0;start<count;){
                if(!numberPart(row.nodes.get(start))){start++;continue;}
                int end=start,point=-1;
                while(end<count&&numberPart(row.nodes.get(end))){if(point<0&&row.nodes.get(end).text.equals("."))point=end;end++;}
                if(point<0)point=end;
                for(int i=start+1;i<end;i++)if(digit(row.nodes.get(i-1))&&digit(row.nodes.get(i))&&
                    (i<point&&groupThousands&&(point-i)%3==0||i>point&&groupDecimals&&(i-point-1)%3==0))spaces[i]+=z*.26f;
                start=end;
            }
            return spaces;
        }
        public Box measure(Row r,float z){float w=0,a=z*.79f,d=z*.23f;if(r.empty()&&!hint(r).isEmpty())return new Box(textWidth(hint(r),z)+z*.12f,z*.9f,z*.22f);if(r.empty())return r.owner!=null&&(r.owner.kind.equals("func")&&!r.owner.text.equals("Abs")||r.owner.kind.equals("binary")||r.owner.kind.equals("scientific"))?new Box(0,z*.79f,z*.23f):new Box(z*.84f,z*.98f,z*.16f);float[] spaces=boundaries(r,z);for(int i=0;i<r.nodes.size();i++){Node n=r.nodes.get(i);Box b=measure(n,z);w+=b.w+spaces[i];a=Math.max(a,b.a);d=Math.max(d,b.d);}return new Box(w,a,d);}
        private Box measure(Node n,float z){
            if(n.kind.equals("text"))return new Box(textWidth(n.text,z),z*.79f,z*.23f);
            if(n.kind.equals("symbol")||n.kind.equals("variable")||n.kind.equals("token"))return new Box(textWidth(display(n),z),z*.79f,z*.23f);
            if(n.kind.equals("answer")){if(ansStyle==0)return new Box(textWidth(n.answerName,z),z*.79f,z*.23f);return answerBox(n,z);}
            Box b=measure(n.rows.get(0),z);
            switch(n.kind){
                case "frac": {Box t=measure(n.rows.get(0),z*fractionScale),u=measure(n.rows.get(1),z*fractionScale);return new Box(Math.max(t.w,u.w)+z*(compactFractions?.25f:.30f),t.a+z*(compactFractions?.65f:.70f),u.d+z*(compactFractions?.82f:.92f));}
                case "pow": {Box e=measure(n.rows.get(1),z*.64f);return new Box(b.w+e.w+z*.05f,Math.max(b.a,z*.52f+e.a),b.d);}
                case "sqrt":return new Box(b.w+z*(n.rows.get(0).empty()?1.01f:.89f),b.a+z*(n.rows.get(0).empty()?.02f:.13f),Math.max(b.d,z*.3f));
                case "root":return new Box(b.w+z*1.04f,b.a+z*.3f,b.d);
                case "nthroot":{Box rad=measure(n.rows.get(1),z);return new Box(rad.w+Math.max(z*.84f,b.w*.5f+z*.55f),rad.a+z*.3f,rad.d);}
                case "mixed":{Box f=measure(fraction(n),z);return new Box(b.w+z*.12f+f.w+(n.text.equals("bracketed")?textWidth("[",z)+textWidth("]",z):0),Math.max(b.a,f.a),Math.max(b.d,f.d));}
                case "recurring":{Box middle=measure(n.rows.get(1),z),repeat=measure(n.rows.get(2),z);return new Box(b.w+z*.25f+(n.rows.get(1).empty()?0:middle.w)+repeat.w,Math.max(b.a,repeat.a)+z*.13f,Math.max(b.d,repeat.d));}
                case "dms":{float w=0,a=z*.79f,d=z*.23f;for(int i=0;i<3;i++){Row r=n.rows.get(i);if(i>=n.dmsMarked&&r.defaultValue)continue;Box cell=measure(r,z);w+=cell.w+(i<n.dmsMarked?z*.25f:0);a=Math.max(a,cell.a);d=Math.max(d,cell.d);}return new Box(w,a,d);}
                case "scientific":case "binary":{Box right=measure(n.rows.get(1),z);return new Box(b.w+textWidth(n.kind.equals("scientific")?"E":n.text.equals("dot")?"•":n.text.equals("nPr")?"P":n.text.equals("nCr")?"C":n.text,z*.9f)+right.w+z*.16f,Math.max(b.a,right.a),Math.max(b.d,right.d));}
                case "integral":return new Box(z*.9f+b.w+textWidth(" d",z*.84f)+measure(n.rows.get(3),z*.84f).w,Math.max(b.a,z*1.8f),Math.max(b.d,z*1.05f));
                case "sum":case "product":{Box l=measure(n.rows.get(1),z*.8f),u=measure(n.rows.get(2),z*.8f);float v=measure(n.rows.get(3),z*.8f).w,cap=Math.max(z*1.65f,Math.max(l.w+v+textWidth("=",z*.8f),u.w));return new Box(cap+z*.5f+b.w+textWidth("[",z)+textWidth("]",z),Math.max(b.a,z*2.3f),Math.max(b.d,z*1.4f));}
                case "indefinite":return new Box(z*.85f+b.w+textWidth("d",z)+measure(n.rows.get(1),z).w,Math.max(b.a,z*1.2f),Math.max(b.d,z*.4f));
                case "derivative":case "derivativeAt":{float var=measure(n.rows.get(1),z*.7f).w,extra=n.kind.equals("derivativeAt")?measure(n.rows.get(1),z*.6f).w+measure(n.rows.get(2),z*.6f).w+textWidth("=",z*.6f)+z*.16f:0;return new Box(z*.65f+var+textWidth("[",z)+b.w+textWidth("]",z)+extra,Math.max(b.a,z),Math.max(b.d,z*.55f));}
                case "limit":{float under=measure(n.rows.get(1),z*.52f).w+measure(n.rows.get(2),z*.52f).w+z*.65f;return new Box(Math.max(z*1.3f,under)+b.w+z*.25f,Math.max(b.a,z*.8f),Math.max(b.d,z*.8f));}
                case "matrix":case "vector":{float w=z*.35f;for(float v:columns(n,z))w+=v;int cols=Integer.parseInt(n.text.split(",")[1]);float h=n.rows.size()/cols*rowHeight(n,z);return new Box(w,h/2+z*.3f,h/2-z*.3f);}
                case "call":{
                    if(n.text.equals("LogBase")){Box arg=measure(n.rows.get(1),z);return new Box(textWidth("Log",z)+b.w*.6f+arg.w+textWidth("[",z)+textWidth("]",z),Math.max(b.a*.6f,arg.a),Math.max(b.d*.6f+z*.3f,arg.d));}
                    float w=textWidth(n.text,z)+textWidth("(",z)+textWidth(")",z),a=b.a,d=b.d;for(int i=0;i<n.rows.size();i++){Box arg=measure(n.rows.get(i),z);w+=arg.w+(i>0?textWidth(", ",z):0);a=Math.max(a,arg.a);d=Math.max(d,arg.d);}return new Box(w,a,d);
                }
                default:if(n.kind.equals("func")&&n.text.equals("Abs"))return new Box(b.w+z*.36f,b.a,b.d);return new Box(textWidth(functionName(n.text),z)+textWidth("(",z)+(n.kind.equals("func")&&!n.closed&&!closeParen?0:textWidth(")",z))+b.w,b.a,b.d);
            }
        }
        public void draw(Canvas c,Row r,float x,float baseline,float z){
            Box firstAnswer=!r.empty()&&r.nodes.get(0).kind.equals("answer")&&ansStyle!=0?measure(r.nodes.get(0),z):null;
            spots.add(new Spot(r,0,x+(r.empty()&&hint(r).isEmpty()?measure(r,z).w:0),baseline,firstAnswer==null?0:firstAnswer.a,firstAnswer==null?0:firstAnswer.d));
            if(r.empty()&&!hint(r).isEmpty()){Box box=measure(r,z);int old=color;color=0xff56635f;tx(c,hint(r),x+z*.06f,baseline,z);p.setColor(color);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(z*.025f);c.drawRect(x,baseline-box.a,x+box.w,baseline+box.d,p);p.setStyle(Paint.Style.FILL);color=old;return;}
            if(r.empty()&&!(r.owner!=null&&(r.owner.kind.equals("func")&&!r.owner.text.equals("Abs")||r.owner.kind.equals("binary")||r.owner.kind.equals("scientific")))){p.setColor(color);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(z*.05f);c.drawRect(x+z*.04f,baseline-z*.86f,x+z*.68f,baseline+z*.14f,p);p.setStyle(Paint.Style.FILL);}
            float[] spaces=boundaries(r,z);for(int i=0;i<r.nodes.size();i++){Node n=r.nodes.get(i);float space=spaces[i];if(multiplyBefore(r,i))tx(c,"×",x+(space-textWidth("×",z))/2,baseline,z);x+=space;drawNode(c,n,x,baseline,z);Box box=measure(n,z);x+=box.w;spots.add(new Spot(r,i+1,x,baseline,n.kind.equals("answer")&&ansStyle!=0?box.a:0,n.kind.equals("answer")&&ansStyle!=0?box.d:0));}
        }
        public void drawCursor(Canvas c,float z){if(editor==null||!showCursor)return;for(Spot s:spots)if(s.row==editor.cursor&&s.index==editor.index){p.setColor(accent);p.setStrokeWidth(Math.max(2,z*.045f));c.drawLine(s.x+2,s.y-(s.a>0?s.a:z*.85f),s.x+2,s.y+(s.a>0?s.d:z*.18f),p);break;}}
        private void tx(Canvas c,String t,float x,float y,float z){p.setTypeface(typeface);p.setColor(color);p.setTextSize(z);p.setStyle(Paint.Style.FILL);c.drawText(t,x,y,p);}
        private void line(Canvas c,float x,float y,float xx,float yy,float z){p.setColor(color);p.setStrokeWidth(Math.max(1,z*strokeRatio));c.drawLine(x,y,xx,yy,p);}
        private void drawNode(Canvas c,Node n,float x,float y,float z){
            if(n.kind.equals("text")){tx(c,decimalComma&&n.text.equals(".")?",":n.text,x,y,z);return;}
            if(n.kind.equals("symbol")||n.kind.equals("variable")||n.kind.equals("token")){tx(c,display(n),x,y,z);return;}
            if(n.kind.equals("answer")){
                if(ansStyle==0){tx(c,n.answerName,x,y,z);return;}
                Box box=measure(n,z);float radius=z*.16f;p.setStyle(Paint.Style.FILL);p.setColor(answerBackground);c.drawRoundRect(x,y-box.a,x+box.w,y+box.d,radius,radius,p);
                float inset=z*.14f;p.setColor(0xffbfcecd);c.drawRoundRect(x+inset,y-box.a+inset,x+box.w-inset,y+box.d-inset,radius*.8f,radius*.8f,p);
                p.setColor(color);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(z*.075f);c.drawRoundRect(x,y-box.a,x+box.w,y+box.d,radius,radius,p);p.setStyle(Paint.Style.FILL);
                Renderer contents=answerRenderer();Row row=result(n.text);Box value=contents.measure(row,z*.95f);contents.spots.clear();contents.draw(c,row,x+(box.w-value.w)/2,y+(value.a+value.d>z*1.4f?z*.10f:0),z*.95f);
                if(ansStyle==1)tx(c,n.answerName,x+box.w/2-textWidth(n.answerName,z*.32f)/2,y+box.d-z*.08f,z*.32f);return;
            }
            Row r=n.rows.get(0);Box b=measure(r,z),all=measure(n,z);
            switch(n.kind){
                case "frac":{float s=z*fractionScale;Box t=measure(r,s),u=measure(n.rows.get(1),s);float width=all.w-z*.14f;x+=z*(compactFractions?.07f:.14f);draw(c,r,x+(width-t.w)/2,y-z*(compactFractions?.65f:.70f),s);line(c,x+z*.02f,y-z*(compactFractions?.31f:.35f),x+width-z*.02f,y-z*(compactFractions?.31f:.35f),z);draw(c,n.rows.get(1),x+(width-u.w)/2,y+z*(compactFractions?.67f:.72f),s);break;}
                case "pow":draw(c,r,x,y,z);draw(c,n.rows.get(1),x+b.w+z*.05f,y-z*.52f,z*.64f);break;
                case "sqrt":radical(c,x,y,z,b,all.w,r.empty());draw(c,r,x+z*(r.empty()?.84f:.72f),y,z);break;
                case "root":draw(c,n.rows.get(1),x,y-z*.62f,z*.45f);radical(c,x+z*.2f,y,z,b,all.w-z*.2f,r.empty());draw(c,r,x+z*.98f,y,z);break;
                case "nthroot":{Box rad=measure(n.rows.get(1),z);float lead=Math.max(z*.84f,b.w*.5f+z*.55f);draw(c,r,x,y-z*.62f,z*.5f);radical(c,x+lead-z*.78f,y,z,rad,all.w-lead+z*.78f,n.rows.get(1).empty());draw(c,n.rows.get(1),x+lead,y,z);break;}
                case "mixed":{boolean bracket=n.text.equals("bracketed");if(bracket){tx(c,"[",x,y,z);x+=textWidth("[",z);}draw(c,r,x,y,z);drawNode(c,fraction(n),x+b.w+z*.12f,y,z);if(bracket)tx(c,"]",x+b.w+z*.12f+measure(fraction(n),z).w,y,z);break;}
                case "recurring":{draw(c,r,x,y,z);float at=x+b.w;tx(c,".",at,y,z);at+=z*.25f;if(!n.rows.get(1).empty()){draw(c,n.rows.get(1),at,y,z);at+=measure(n.rows.get(1),z).w;}draw(c,n.rows.get(2),at,y,z);Box rep=measure(n.rows.get(2),z);line(c,at,y-rep.a-z*.08f,at+rep.w,y-rep.a-z*.08f,z);break;}
                case "dms":{float at=x;String[] units={"°","′","″"};for(int i=0;i<3;i++){Row cell=n.rows.get(i);if(i>=n.dmsMarked&&cell.defaultValue){spots.add(new Spot(cell,0,at,y));continue;}draw(c,cell,at,y,z);at+=measure(cell,z).w;if(i<n.dmsMarked){tx(c,units[i],at,y,z*.65f);at+=z*.25f;}}break;}
                case "scientific":case "binary":{draw(c,r,x,y,z);String op=n.kind.equals("scientific")?"E":n.text.equals("dot")?"•":n.text.equals("nPr")?"P":n.text.equals("nCr")?"C":n.text;float at=x+b.w+z*.08f;tx(c,op,at,y,z*.9f);at+=textWidth(op,z*.9f)+z*.08f;draw(c,n.rows.get(1),at,y,z);break;}
                case "integral":{Box lower=measure(n.rows.get(1),z*.52f),upper=measure(n.rows.get(2),z*.52f);tx(c,"∫",x,y+z*.25f,z*1.8f);draw(c,n.rows.get(2),x+z*.52f-upper.w/2,y-z*1.37f,z*.52f);draw(c,n.rows.get(1),x+z*.52f-lower.w/2,y+z*.96f,z*.52f);draw(c,r,x+z*.9f,y,z);tx(c," d",x+z*.9f+b.w,y,z*.84f);draw(c,n.rows.get(3),x+z*.9f+b.w+textWidth(" d",z*.84f),y,z*.84f);break;}
                case "sum":case "product":{Box lower=measure(n.rows.get(1),z*.8f),upper=measure(n.rows.get(2),z*.8f);float v=measure(n.rows.get(3),z*.8f).w,eq=textWidth("=",z*.8f),cap=Math.max(z*1.65f,Math.max(lower.w+v+eq,upper.w)),lead=cap+z*.5f;MathGlyphs.draw(c,p,n.kind.equals("product"),x+(cap-z*1.65f)/2,y-z*1.2f,z*1.65f,z*1.6f,color);draw(c,n.rows.get(2),x+(cap-upper.w)/2,y-z*1.45f,z*.8f);float under=x+(cap-lower.w-v-eq)/2;draw(c,n.rows.get(3),under,y+z*1.22f,z*.8f);tx(c,"=",under+v,y+z*1.22f,z*.8f);draw(c,n.rows.get(1),under+v+eq,y+z*1.22f,z*.8f);tx(c,"[",x+lead,y,z);draw(c,r,x+lead+textWidth("[",z),y,z);tx(c,"]",x+lead+textWidth("[",z)+b.w,y,z);break;}
                case "indefinite":tx(c,"∫",x,y+z*.12f,z*1.5f);draw(c,r,x+z*.85f,y,z);tx(c,"d",x+z*.85f+b.w,y,z);draw(c,n.rows.get(1),x+z*.85f+b.w+textWidth("d",z),y,z);break;
                case "derivative":case "derivativeAt":{float v=n.rows.size()>1?measure(n.rows.get(1),z*.7f).w:z*.45f,lead=z*.65f+v;tx(c,"d",x+lead*.35f,y-z*.4f,z*.7f);line(c,x,y-z*.2f,x+lead,y-z*.2f,z);tx(c,"d",x,y+z*.4f,z*.7f);if(n.rows.size()>1)draw(c,n.rows.get(1),x+z*.4f,y+z*.4f,z*.7f);else tx(c,"x",x+z*.4f,y+z*.4f,z*.7f);tx(c,"[",x+lead,y,z);draw(c,r,x+lead+textWidth("[",z),y,z);float at=x+lead+textWidth("[",z)+b.w;tx(c,"]",at,y,z);if(n.kind.equals("derivativeAt")){at+=textWidth("]",z);draw(c,n.rows.get(1),at+z*.08f,y+z*.45f,z*.6f);float vv=measure(n.rows.get(1),z*.6f).w;tx(c,"=",at+vv+z*.08f,y+z*.45f,z*.6f);draw(c,n.rows.get(2),at+vv+z*.08f+textWidth("=",z*.6f),y+z*.45f,z*.6f);}break;}
                case "limit":{Box var=measure(n.rows.get(1),z*.52f),point=measure(n.rows.get(2),z*.52f);float lead=Math.max(z*1.3f,var.w+point.w+z*.65f);tx(c,"lim",x,y,z);draw(c,n.rows.get(1),x,y+z*.55f,z*.52f);tx(c,"→",x+var.w,y+z*.55f,z*.52f);draw(c,n.rows.get(2),x+var.w+z*.5f,y+z*.55f,z*.52f);if(!n.text.isEmpty())tx(c,n.text.equals("-1")?"⁺":"⁻",x+lead-z*.2f,y+z*.3f,z*.4f);draw(c,r,x+lead+z*.25f,y,z);break;}
                case "matrix":case "vector":{float[] widths=columns(n,z);float h=rowHeight(n,z),at=x+z*.2f,top=y-all.a;int cols=widths.length;line(c,x+z*.12f,top,x+z*.02f,top,z);line(c,x+z*.02f,top,x+z*.02f,y+all.d,z);line(c,x+z*.02f,y+all.d,x+z*.12f,y+all.d,z);for(int i=0;i<n.rows.size();i++){int col=i%cols;if(col==0)at=x+z*.2f;Box cell=measure(n.rows.get(i),z);draw(c,n.rows.get(i),at+(widths[col]-cell.w)/2,top+(i/cols+.5f)*h+(cell.a-cell.d)/2,z);at+=widths[col];}line(c,x+all.w-z*.12f,top,x+all.w,top,z);line(c,x+all.w,top,x+all.w,y+all.d,z);line(c,x+all.w,y+all.d,x+all.w-z*.12f,y+all.d,z);break;}
                case "call":{if(n.text.equals("LogBase")){float t=textWidth("Log",z);tx(c,"Log",x,y,z);draw(c,r,x+t,y+z*.3f,z*.6f);float at=x+t+b.w*.6f;tx(c,"[",at,y,z);draw(c,n.rows.get(1),at+textWidth("[",z),y,z);tx(c,"]",at+textWidth("[",z)+measure(n.rows.get(1),z).w,y,z);break;}float t=textWidth(n.text,z),at=x+t+textWidth("(",z);tx(c,n.text,x,y,z);tx(c,"(",x+t,y,z);for(int i=0;i<n.rows.size();i++){if(i>0){tx(c,",",at,y,z);at+=textWidth(", ",z);}draw(c,n.rows.get(i),at,y,z);at+=measure(n.rows.get(i),z).w;}tx(c,")",at,y,z);break;}
                case "func":if(n.text.equals("Abs")){line(c,x+z*.04f,y-b.a,x+z*.04f,y+b.d,z);draw(c,r,x+z*.18f,y,z);line(c,x+all.w-z*.04f,y-b.a,x+all.w-z*.04f,y+b.d,z);break;}
                default:float t=textWidth(functionName(n.text),z);int old=color;if(n.text.equals("Det"))color=0xffc3221e;tx(c,functionName(n.text),x,y,z);color=old;tx(c,"(",x+t,y,z);draw(c,r,x+t+textWidth("(",z),y,z);if(!n.kind.equals("func")||n.closed||closeParen)tx(c,")",x+t+textWidth("(",z)+b.w,y,z);
            }
        }
        private void radical(Canvas c,float x,float y,float z,Box b,float width,boolean empty){
            float top=y-b.a-z*(empty?.10f:.24f),bottom=y+(empty?z*.30f:Math.max(z*.13f,b.d-z*.15f));
            p.setColor(color);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(Math.max(1,z*strokeRatio));p.setStrokeJoin(Paint.Join.MITER);p.setStrokeCap(Paint.Cap.SQUARE);
            float start=y-z*(b.a>z?.1f:.35f);Path path=new Path();path.moveTo(x+z*.03f,start);path.lineTo(x+z*.19f,start);path.lineTo(x+z*.36f,bottom);path.lineTo(x+z*.66f,top);path.lineTo(x+width,top);path.lineTo(x+width,top+z*.12f);c.drawPath(path,p);p.setStyle(Paint.Style.FILL);
        }
        public void select(float x,float y){Spot best=null;double dist=Double.MAX_VALUE;for(Spot s:spots){double d=Math.pow(s.x-x,2)+Math.pow((s.y-y)*1.5,2);if(d<dist){dist=d;best=s;}}if(best!=null&&editor!=null){editor.cursor=best.row;editor.index=best.index;}}
    }
    public static Row result(String value){
        value=value.replace("×","*").replaceAll("\\s+"," ").trim();
        if(value.matches("-?[0-9]+ [0-9]+/[0-9]+")){String[] parts=value.split("[ /]");Node n=new Node("mixed","").field().field().field();for(int i=0;i<3;i++)addText(n.rows.get(i),parts[i]);Row row=new Row();row.add(n);return row;}
        if(value.startsWith("[")&&value.endsWith("]")&&value.substring(1,value.length()-1).matches("-?[0-9]+ [0-9]+/[0-9]+")){Row row=new Row();addText(row,"[");for(Node n:result(value.substring(1,value.length()-1)).nodes)row.add(n);addText(row,"]");return row;}
        java.util.regex.Matcher recurring=java.util.regex.Pattern.compile("Recurring\\((-?[0-9]+),([0-9]*),([0-9]+)\\)").matcher(value);
        if(recurring.matches()){Node n=new Node("recurring","").field().field().field();for(int i=0;i<3;i++)addText(n.rows.get(i),recurring.group(i+1));Row row=new Row();row.add(n);return row;}
        try{ResultParser parser=new ResultParser(value);Row parsed=parser.sum();parser.skip();if(parser.i==value.length())return parsed;}catch(Exception ignored){}
        Row r=new Row();
        if(value.matches("-?[0-9]+/[0-9]+")){String[] t=value.split("/");Node f=new Node("frac","").field().field();addText(f.rows.get(0),t[0]);addText(f.rows.get(1),t[1]);r.add(f);}
        else if(value.matches("Sqrt\\([0-9]+\\)")){Node n=new Node("sqrt","").field();addText(n.rows.get(0),value.substring(5,value.length()-1));r.add(n);}
        else addText(r,value.replace("Sqrt","√").replace("Pi","π").replace("Infinity","∞").replace("*","·"));
        return r;
    }
    /** Converts conventional engine output to a natural display; preserves unsupported output verbatim. */
    private static final class ResultParser {
        final String s;int i,depth;ResultParser(String s){this.s=s;}
        void skip(){while(i<s.length()&&Character.isWhitespace(s.charAt(i)))i++;}
        boolean take(char c){skip();if(i<s.length()&&s.charAt(i)==c){i++;return true;}return false;}
        Row sum(){Row a=addition();while(true){skip();String op=null;for(String candidate:new String[]{"->","==","!=","<=",">=","&&","||","<",">","="})if(s.startsWith(candidate,i)){op=candidate;break;}if(op==null)return a;i+=op.length();a.add(new Node("token",op));append(a,addition());}}
        Row addition(){Row a=product();while(true){if(take('+')){addText(a,"+");append(a,product());}else {skip();if(i<s.length()&&s.charAt(i)=='-'&&!s.startsWith("->",i)){i++;addText(a,"−");append(a,product());}else return a;}}}
        Row product(){Row a=power();while(true){if(take('*')){addText(a,"×");append(a,power());}else if(take('/')){Node n=new Node("frac","").field().field();append(n.rows.get(0),a);append(n.rows.get(1),power());a=new Row();a.add(n);}else return a;}}
        Row power(){Row a=atom();if(take('^')){Node n=new Node("pow","").field().field();append(n.rows.get(0),a);append(n.rows.get(1),power());a=new Row();a.add(n);}return a;}
        Row atom(){if(++depth>64)throw new IllegalArgumentException();skip();Row a=new Row();if(take('-')){addText(a,"−");append(a,atom());}else if(take('(')){Node n=new Node("func","").field();append(n.rows.get(0),sum());if(!take(')'))throw new IllegalArgumentException();a.add(n);}else if(take('{')){ArrayList<Row> rows=new ArrayList<>();if(!take('}')){do{rows.add(sum());}while(take(','));if(!take('}'))throw new IllegalArgumentException();}boolean matrix=!rows.isEmpty();int cols=-1;for(Row row:rows){if(row.nodes.size()!=1||!row.nodes.get(0).kind.equals("vector")){matrix=false;break;}int width=row.nodes.get(0).rows.size();if(cols>=0&&width!=cols){matrix=false;break;}cols=width;}if(rows.isEmpty())a.add(new Node("symbol","{}"));else{Node n=new Node(matrix?"matrix":"vector",matrix?rows.size()+","+cols:"1,"+rows.size());if(matrix){for(Row row:rows)for(Row cell:row.nodes.get(0).rows){cell.owner=n;n.rows.add(cell);}}else for(Row row:rows){row.owner=n;n.rows.add(row);}a.add(n);}}else if(i<s.length()&&(Character.isDigit(s.charAt(i))||s.charAt(i)=='.')){int start=i;while(i<s.length()&&(Character.isDigit(s.charAt(i))||s.charAt(i)=='.'))i++;if(i<s.length()&&(s.charAt(i)=='E'||s.charAt(i)=='e')&&i+1<s.length()&&(Character.isDigit(s.charAt(i+1))||s.charAt(i+1)=='-'||s.charAt(i+1)=='+')){i++;if(i<s.length()&&(s.charAt(i)=='+'||s.charAt(i)=='-'))i++;while(i<s.length()&&Character.isDigit(s.charAt(i)))i++;a.add(new Node("symbol",s.substring(start,i)));}else addText(a,s.substring(start,i));}else if(i<s.length()&&Character.isLetter(s.charAt(i))){int start=i;while(i<s.length()&&(Character.isLetterOrDigit(s.charAt(i))||s.charAt(i)=='_'))i++;String name=s.substring(start,i);if(take('(')){ArrayList<Row> rows=new ArrayList<>();if(!take(')')){do{rows.add(sum());}while(take(','));if(!take(')'))throw new IllegalArgumentException();}if(rows.isEmpty())a.add(new Node("symbol",name+"()"));else{Node n=new Node(name.equals("Sqrt")&&rows.size()==1?"sqrt":rows.size()==1?"func":"call",name);for(Row row:rows){row.owner=n;n.rows.add(row);}a.add(n);}}else a.add(new Node(name.length()==1&&name.matches("[a-z]")?"variable":"symbol",name.equals("scvE")?"e":name));}else throw new IllegalArgumentException();depth--;return a;}
        private void append(Row to,Row from){for(Node n:new ArrayList<>(from.nodes))to.add(n);}
    }
    static void addText(Row r,String s){for(int i=0;i<s.length();i++)r.add(new Node("text",s.substring(i,i+1)));}
}
