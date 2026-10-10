package com.az.scicalc;

import java.util.*;
import java.util.regex.*;

/** Function signatures for the second keyboard; no formula-reference libraries. */
final class MathFunctions {
 static final class Entry {
  final String category,name,label;final String[] parameters;
  Entry(String c,String n,String l,String args){category=c;name=n;label=l;parameters=args.isEmpty()?new String[0]:args.split(",");}
  String signature(){return label+"\n"+name+"("+String.join(", ",parameters)+")";}
 }
 static final ArrayList<Entry> ALL=new ArrayList<>();
 private static void add(String c,String n,String l,String args){ALL.add(new Entry(c,n,l,args));}
 static {
  add("arith","Abs","绝对值","x");add("arith","Sign","符号","x");add("arith","Round","四舍五入","x");add("arith","Floor","向下取整","x");add("arith","Ceiling","向上取整","x");add("arith","IntegerPart","整数部分","x");add("arith","FractionalPart","小数部分","x");add("arith","GCD","最大公约数","a,b");add("arith","LCM","最小公倍数","a,b");add("arith","Mod","余数","a,b");add("arith","Quotient","整除","a,b");add("arith","LogBase","指定底数对数","底数,真数");add("arith","Hypot","直角三角形斜边","x,y");
  for(String n:new String[]{"Sin","Cos","Tan","Cot","Sec","Csc","ArcSin","ArcCos","ArcTan","ArcCot","ArcSec","ArcCsc","Sinh","Cosh","Tanh","Coth","Sech","Csch","ArcSinh","ArcCosh","ArcTanh","ArcCoth","ArcSech","ArcCsch","Exp","Log","Sqrt"})add("arith",n,n,"x");
  for(String n:new String[]{"Factor","Expand","FullSimplify","Together","Apart","Numerator","Denominator","Cancel"})add("algebra",n,n,"表达式");add("algebra","Coefficient","提取系数","表达式,变量,次数");add("algebra","PolynomialQuotient","多项式商","被除式,除式,变量");add("algebra","PolynomialRemainder","多项式余式","被除式,除式,变量");
  add("solver","Solve","符号求解","方程,变量");add("solver","NSolve","数值求解","方程,变量");add("solver","Reduce","方程与不等式","条件,变量");add("solver","FindRoot","数值求根","方程,{变量 初值}");
  add("calculus","D","求导","表达式,变量");add("calculus","Integrate","积分","表达式,变量或范围");add("calculus","NIntegrate","数值积分","表达式,{变量 下界 上界}");add("calculus","Limit","极限","表达式,变量→趋近值");add("calculus","Sum","求和","表达式,{变量 下界 上界}");add("calculus","Product","连乘","表达式,{变量 下界 上界}");add("calculus","Series","级数展开","表达式,{变量 展开点 阶数}");
  for(String n:new String[]{"Total","Length","Sort","Reverse","First","Last","Rest","Flatten","Union","Intersection","DeleteDuplicates"})add("list",n,n,"列表");add("list","Range","数列","起点,终点,步长");add("list","Part","读取元素","列表,位置");add("list","Append","追加元素","列表,元素");add("list","Prepend","前置元素","列表,元素");add("list","Table","生成列表","表达式,{变量 起点 终点}");
  for(String n:new String[]{"Det","Inverse","Transpose","MatrixRank","RowReduce","Eigenvalues","Eigenvectors","Tr","Diagonal"})add("linear algebra",n,n.equals("Tr")?"迹 Trace":n,"矩阵");add("linear algebra","Dot","点积 / 矩阵乘法","a,b");add("linear algebra","Cross","叉积","a,b");add("linear algebra","Norm","模长","向量");add("linear algebra","Normalize","单位向量","向量");add("linear algebra","IdentityMatrix","单位矩阵","阶数");add("linear algebra","CharacteristicPolynomial","特征多项式","矩阵,变量");add("linear algebra","LinearSolve","线性方程 Ax=b","A,b");
  add("combinatorics","Factorial","阶乘","n");add("combinatorics","Binomial","组合数","n,r");add("combinatorics","Permutations","排列列表","列表");add("combinatorics","Subsets","子集","列表");add("combinatorics","Fibonacci","斐波那契数","n");add("combinatorics","CatalanNumber","卡塔兰数","n");
  for(String n:new String[]{"Mean","Median","Variance","StandardDeviation","Min","Max"})add("stat dist",n,n,"数据列表");
  add("stat dist","normalpdf","正态分布密度","x,μ,σ");add("stat dist","normalcdf","正态分布区间概率","下界,上界,μ,σ");add("stat dist","invNorm","正态分布分位数","概率,μ,σ");add("stat dist","tpdf","学生 t 分布密度","x,自由度");add("stat dist","tcdf","学生 t 分布区间概率","下界,上界,自由度");add("stat dist","invT","学生 t 分布分位数","概率,自由度");add("stat dist","chi2pdf","卡方分布密度","x,自由度");add("stat dist","chi2cdf","卡方分布区间概率","下界,上界,自由度");add("stat dist","binompdf","二项分布概率","n,p,k");add("stat dist","binomcdf","二项分布累积概率","n,p,k");add("stat dist","poissonpdf","泊松分布概率","λ,k");add("stat dist","poissoncdf","泊松分布累积概率","λ,k");
  for(String n:new String[]{"Prime","PrimePi","NextPrime","Divisors","EulerPhi","MoebiusMu","IntegerDigits","IntegerLength","FactorInteger"})add("number",n,n,"整数");add("number","PowerMod","模幂","a,b,m");
  add("boolean","And","与","a,b");add("boolean","Or","或","a,b");add("boolean","Xor","异或","a,b");add("boolean","Not","非","a");add("boolean","Implies","蕴含","a,b");
  for(String n:new String[]{"IntegerQ","NumberQ","NumericQ","PrimeQ","EvenQ","OddQ","MatrixQ","VectorQ"})add("predicate",n,n,"表达式");
  for(String n:new String[]{"Gamma","Erf","Erfc","Zeta","BesselJ","BesselY"})add("special",n,n,n.startsWith("Bessel")?"阶数,x":"x");
  add("random","RandomReal","随机实数","");add("random","RanInt#","随机整数","最小值,最大值");add("random","RandomChoice","随机抽取","列表");add("random","RandomSample","随机排列","列表");
  add("domain","Element","属于","值,域");add("domain","NotElement","不属于","值,域");
  add("clustering","FindClusters","聚类","数据,簇数量");for(String n:new String[]{"EuclideanDistance","ManhattanDistance","CosineDistance","CanberraDistance","CorrelationDistance","HammingDistance","ChessboardDistance","BrayCurtisDistance","SquaredEuclideanDistance"})add("clustering",n,n,"a,b");
  add("others","If","条件选择","条件,真值,假值");add("others","Piecewise","分段函数","分段列表");add("others","ReplaceAll","替换","表达式,规则");
 }
 static List<Entry> category(String c,String query){ArrayList<Entry> result=new ArrayList<>();String q=query.toLowerCase(Locale.ROOT);for(Entry e:ALL)if((c.isEmpty()||e.category.equals(c))&&(e.name+e.label).toLowerCase(Locale.ROOT).contains(q))result.add(e);return result;}
 static String call(String name,String[] a){
  switch(name){
   case "Hypot":return "Sqrt(("+a[0]+")^2+("+a[1]+")^2)";
   case "normalpdf":return "PDF(NormalDistribution("+a[1]+","+a[2]+"),"+a[0]+")";
   case "normalcdf":return "CDF(NormalDistribution("+a[2]+","+a[3]+"),"+a[1]+")-CDF(NormalDistribution("+a[2]+","+a[3]+"),"+a[0]+")";
   case "invNorm":return "InverseCDF(NormalDistribution("+a[1]+","+a[2]+"),"+a[0]+")";
   case "tpdf":case "chi2pdf":return "PDF("+(name.equals("tpdf")?"StudentTDistribution":"ChiSquareDistribution")+"("+a[1]+"),"+a[0]+")";
   case "tcdf":case "chi2cdf":{String d=(name.equals("tcdf")?"StudentTDistribution":"ChiSquareDistribution")+"("+a[2]+")";return "CDF("+d+","+a[1]+")-CDF("+d+","+a[0]+")";}
   case "invT":return "InverseCDF(StudentTDistribution("+a[1]+"),"+a[0]+")";
   case "binompdf":case "binomcdf":return (name.equals("binompdf")?"PDF":"CDF")+"(BinomialDistribution("+a[0]+","+a[1]+"),"+a[2]+")";
   case "poissonpdf":case "poissoncdf":return (name.equals("poissonpdf")?"PDF":"CDF")+"(PoissonDistribution("+a[0]+"),"+a[1]+")";
  }
  return name+"("+String.join(",",a)+")";
 }
 static String factorDisplay(String source){Matcher m=Pattern.compile("\\{(-?\\d+),(\\d+)\\}").matcher(source.replaceAll("\\s",""));StringBuilder result=new StringBuilder();while(m.find()){if(result.length()>0)result.append('*');result.append(m.group(1));if(!m.group(2).equals("1"))result.append('^').append(m.group(2));}if(result.length()==0)throw new IllegalArgumentException("无法分解当前整数");return result.toString();}
 static String comparison(String source){return source.replaceAll("(?<![<>=!:])=(?!=)","==");}
 static String percent(String source,boolean relative){
  String s=source;int guard=0;
  while(s.contains("%")&&guard++<100){int end=s.indexOf('%'),start=end-1;if(start<0)throw new IllegalArgumentException("百分号前需要数值");
   if(s.charAt(start)==')'){int depth=1;start--;while(start>=0&&depth>0){if(s.charAt(start)==')')depth++;if(s.charAt(start)=='(')depth--;start--; }start++;}
   else {while(start>=0&&(Character.isDigit(s.charAt(start))||s.charAt(start)=='.'))start--;start++;}
   if(start==end)throw new IllegalArgumentException("百分号前需要数值或括号表达式");String value=s.substring(start,end);int op=start-1;
   if(relative&&op>=0&&(s.charAt(op)=='+'||s.charAt(op)=='-')){int base=op-1,depth=0;while(base>=0){char c=s.charAt(base);if(c==')'||c=='}')depth++;if(c=='('||c=='{'){if(depth==0)break;depth--;}if(c==','&&depth==0)break;base--; }base++;String left=s.substring(base,op);if(!left.isEmpty()){String replacement="(("+left+")"+s.charAt(op)+"("+left+")*("+value+")/100)";s=s.substring(0,base)+replacement+s.substring(end+1);continue;}}
   s=s.substring(0,start)+"(("+value+")/100)"+s.substring(end+1);
  }
  return s;
 }
}
