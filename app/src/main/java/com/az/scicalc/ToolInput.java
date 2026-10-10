package com.az.scicalc;

/** Persisted parameter template for reopening a calculation without showing engine calls. */
final class ToolInput {
 static String marker(int i){return "__SCICALC_PARAMETER_"+i+"__";}
 static String apply(String template,String[] values){String result=template;for(int i=0;i<values.length;i++)result=result.replace(marker(i),values[i]);return result;}
 static String evaluate(MathEngine engine,String source){String result=engine.evaluate(source,false);if(result.contains("Null")||result.matches("(?s)^(Inverse|Solve|Reduce|FindRoot|LinearSolve|Det|Dot|Cross|Transpose|MatrixRank|RowReduce|Eigenvalues|Eigenvectors|Norm|Normalize)\\(.*"))throw new IllegalArgumentException("无法完成计算，请检查参数、矩阵维度或表达式");return result;}
 static String readable(String title,String[] labels,String[] values){StringBuilder b=new StringBuilder(title);for(int i=0;i<values.length;i++){String label=labels[i].replace("（每行用花括号）","");String value=values[i];if(value.startsWith("{{")&&value.endsWith("}}"))value=value.replaceAll("\\},\\s*\\{", " ]\n[ ").replace("{{","[ ").replace("}}"," ]");b.append("\n\n").append(label).append("：\n").append(value);}return b.toString();}
}
