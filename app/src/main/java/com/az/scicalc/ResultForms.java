package com.az.scicalc;

import java.math.*;
import java.util.*;

/** Alternate representations are derived from the selected result, never from Ans. */
final class ResultForms {
 static LinkedHashMap<String,String> values(MathEngine engine,String value,int digits){
  LinkedHashMap<String,String> out=new LinkedHashMap<>();
  String rational=engine.evaluate(value,false);
  if(rational.matches("-?[0-9]+(?:\\.[0-9]+)?(?:[eE][+-]?[0-9]+)?")){
   BigDecimal number=new BigDecimal(rational).stripTrailingZeros();BigInteger n=number.unscaledValue(),d=BigInteger.TEN.pow(Math.max(0,number.scale()));if(number.scale()<0)n=n.multiply(BigInteger.TEN.pow(-number.scale()));BigInteger gcd=n.gcd(d);n=n.divide(gcd);d=d.divide(gcd);rational=d.equals(BigInteger.ONE)?n.toString():n+"/"+d;
  }
  String decimal=engine.evaluate(value,true,digits);
  boolean integer=rational.matches("-?[0-9]+");
  if(!integer)out.put("小数",decimal);
  if(rational.matches("-?[0-9]+/[0-9]+")){
   String[] parts=rational.split("/");String recurring=recurring(parts[0],parts[1]);
   if(!recurring.isEmpty())out.put("循环小数",recurring);
   out.put("分数",rational);
   BigInteger n=new BigInteger(parts[0]),d=new BigInteger(parts[1]);
   if(n.abs().compareTo(d)>0)out.put("带分数","["+ResultFormat.mixed(parts[0],parts[1])+"]");
  }else out.put("分数",rational);
  try{double n=engine.value(value);if(Double.isFinite(n)&&Math.abs(n)<=Integer.MAX_VALUE)out.put("度分秒",dms(n));}catch(Exception ignored){}
  if(integer&&new BigInteger(rational).abs().compareTo(BigInteger.ONE)>0){
   try{out.put("质因数分解",MathFunctions.factorDisplay(engine.evaluate("FactorInteger("+rational+")",false)));}catch(Exception ignored){}
  }
  return out;
 }
 static String recurring(String numerator,String denominator){
  BigInteger n=new BigInteger(numerator),d=new BigInteger(denominator);boolean negative=n.signum()*d.signum()<0;n=n.abs();d=d.abs();
  BigInteger[] qr=n.divideAndRemainder(d);BigInteger remainder=qr[1];StringBuilder decimals=new StringBuilder();Map<BigInteger,Integer> seen=new HashMap<>();
  while(remainder.signum()!=0&&!seen.containsKey(remainder)&&decimals.length()<256){seen.put(remainder,decimals.length());qr=remainder.multiply(BigInteger.TEN).divideAndRemainder(d);decimals.append(qr[0]);remainder=qr[1];}
  if(remainder.signum()==0||!seen.containsKey(remainder))return "";
  int start=seen.get(remainder);return "Recurring("+(negative?"-":"")+n.divide(d)+","+decimals.substring(0,start)+","+decimals.substring(start)+")";
 }
 static String dms(double value){
  BigDecimal seconds=BigDecimal.valueOf(Math.abs(value)).multiply(BigDecimal.valueOf(3600)).setScale(2,RoundingMode.HALF_UP);
  BigDecimal[] degrees=seconds.divideAndRemainder(BigDecimal.valueOf(3600)),minutes=degrees[1].divideAndRemainder(BigDecimal.valueOf(60));
  return (value<0?"−":"")+degrees[0].intValue()+"°"+minutes[0].intValue()+"′"+minutes[1].setScale(2,RoundingMode.HALF_UP).toPlainString()+"″";
 }
 static String clipboard(String value){java.util.regex.Matcher m=java.util.regex.Pattern.compile("Recurring\\((-?[0-9]+),([0-9]*),([0-9]+)\\)").matcher(value);if(!m.matches())return value;StringBuilder s=new StringBuilder(m.group(1)+"."+m.group(2));for(char c:m.group(3).toCharArray())s.append(c).append('\u0305');return s.toString();}
}
