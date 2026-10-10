package com.az.scicalc;
import java.util.*;import java.util.regex.*;
/** Stored values do not replace local integration, differentiation, or summation variables. */
final class ScopedValues {
 static String apply(String source,Map<String,String> values){return scan(source,values,Collections.emptySet(),0);}
 static String scan(String source,Map<String,String> values,Set<String> bound,int depth){if(depth>64)throw new IllegalArgumentException("公式嵌套超过 64 层");StringBuilder out=new StringBuilder();for(int i=0;i<source.length();){char ch=source.charAt(i);if(Character.isLetter(ch)||ch=='_'){int end=i+1;while(end<source.length()&&(Character.isLetterOrDigit(source.charAt(end))||source.charAt(end)=='_'))end++;String name=source.substring(i,end);int open=end;while(open<source.length()&&Character.isWhitespace(source.charAt(open)))open++;if(open<source.length()&&source.charAt(open)=='('){int level=1,close=open+1;
   for(;close<source.length();close++){char c=source.charAt(close);if(c=='(')level++;else if(c==')'&&--level==0)break;}
   if(close==source.length()){out.append(source.substring(i));break;}
   String inside=source.substring(open+1,close);ArrayList<String> args=arguments(inside);Set<String> local=new HashSet<>(bound);if(Arrays.asList("D","Integrate","NIntegrate","Sum","Product","Table","Limit").contains(name)&&args.size()>1){Matcher m=Pattern.compile("^\\s*\\{?\\s*([a-zA-Z][a-zA-Z0-9_]*)").matcher(args.get(1));if(m.find())local.add(m.group(1));}out.append(name).append('(').append(scan(inside,values,local,depth+1)).append(')');i=close+1;
  }else {String value=values.get(name);out.append(value!=null&&!bound.contains(name)&&!source.substring(end).trim().startsWith("->")&&!source.substring(end).trim().startsWith(":=")?"("+value+")":name);i=end;}}
  else {out.append(ch);i++;}}return out.toString();}
 static ArrayList<String> arguments(String source){ArrayList<String> result=new ArrayList<>();int depth=0,start=0;for(int i=0;i<source.length();i++){char c=source.charAt(i);if(c=='('||c=='{'||c=='[')depth++;if(c==')'||c=='}'||c==']')depth--;if(c==','&&depth==0){result.add(source.substring(start,i));start=i+1;}}result.add(source.substring(start));return result;}
}
