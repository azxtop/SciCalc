package com.az.scicalc;
import java.math.*;
import java.util.Locale;

final class ResultFormat {
 static String orderPolynomial(String value){
  String source=value.replace(" ","");if(source.isEmpty()||source.matches(".*[{}=/].*"))return value;
  java.util.ArrayList<String> terms=new java.util.ArrayList<>();int depth=0,start=0;for(int i=0;i<source.length();i++){char c=source.charAt(i);if(c=='(')depth++;if(c==')')depth--;if(i>start&&depth==0&&(c=='+'||c=='-')&&source.charAt(i-1)!='^'){terms.add(source.substring(start,i));start=i;}}terms.add(source.substring(start));
  String variable="";java.util.ArrayList<Integer> degrees=new java.util.ArrayList<>();java.util.regex.Pattern pattern=java.util.regex.Pattern.compile("[a-zA-Z]+(?:\\^\\(?([0-9]+)\\)?)?");
  for(String term:terms){java.util.regex.Matcher m=pattern.matcher(term);int degree=0,count=0;while(m.find()){String name=m.group().split("\\^")[0];if(name.length()!=1||!variable.isEmpty()&&!variable.equals(name))return value;variable=name;degree=m.group(1)==null?1:Integer.parseInt(m.group(1));count++;}if(count>1)return value;degrees.add(degree);}
  if(variable.isEmpty())return value;java.util.ArrayList<Integer> indices=new java.util.ArrayList<>();for(int i=0;i<terms.size();i++)indices.add(i);indices.sort((a,b)->Integer.compare(degrees.get(b),degrees.get(a)));StringBuilder result=new StringBuilder();for(int i:indices){String term=terms.get(i);if(result.length()>0&&!term.startsWith("+")&&!term.startsWith("-"))result.append('+');result.append(term);}return result.toString().replaceFirst("^\\+","");
 }
 static String scientific(String value,boolean eStyle){java.util.regex.Matcher m=java.util.regex.Pattern.compile("^([+-]?[0-9.]+)[eE]([+-]?[0-9]+)$").matcher(value.trim());if(!m.matches())return value;String mantissa=new BigDecimal(m.group(1)).stripTrailingZeros().toPlainString();int exponent=Integer.parseInt(m.group(2));return eStyle?mantissa+"E"+exponent:mantissa+"*10^("+exponent+")";}
 static String mixed(String numerator,String denominator){BigInteger n=new BigInteger(numerator),d=new BigInteger(denominator);BigInteger[] qr=n.divideAndRemainder(d);if(qr[1].signum()==0)return qr[0].toString();if(qr[0].signum()==0)return n+"/"+d;return qr[0]+" "+qr[1].abs()+"/"+d;}
 static String roman(int n){if(n<1||n>3999)throw new IllegalArgumentException("罗马数字范围为 1~3999");int[] values={1000,900,500,400,100,90,50,40,10,9,5,4,1};String[] symbols={"M","CM","D","CD","C","XC","L","XL","X","IX","V","IV","I"};StringBuilder s=new StringBuilder();for(int i=0;i<values.length;i++)while(n>=values[i]){s.append(symbols[i]);n-=values[i];}return s.toString();}
 static String exactDisplay(String value,String style,int digits,boolean eStyle){
  if(!value.matches("[+-]?(?:[0-9]+\\.[0-9]*|[0-9]*\\.[0-9]+)(?:[eE][+-]?[0-9]+)?|[+-]?[0-9]+[eE][+-]?[0-9]+"))return value;
  return scientific(format(value,style,digits),eStyle);
 }
 static String format(String value,String style,int digits){
  try{BigDecimal n=new BigDecimal(value);int d=Math.max(2,Math.min(15,digits));
   if(style.equals("fixed"))return n.setScale(d,RoundingMode.HALF_UP).toPlainString();
   if(style.equals("scientific"))return String.format(Locale.US,"%."+(d-1)+"e",n);
   if(style.equals("engineering")||style.equals("si")){n=n.round(new MathContext(d));if(n.signum()==0)return "0";int exponent=Math.floorDiv(n.precision()-n.scale()-1,3)*3;String mantissa=n.movePointLeft(exponent).stripTrailingZeros().toPlainString();if(style.equals("si")&&exponent>=-24&&exponent<=24){String[] units={"y","z","a","f","p","n","µ","m","","k","M","G","T","P","E","Z","Y"};return mantissa+" "+units[(exponent+24)/3];}return mantissa+" × 10^"+exponent;}
   return n.round(new MathContext(d)).stripTrailingZeros().toPlainString();
  }catch(NumberFormatException ignored){return value;}
 }
}
