package com.az.scicalc;
import java.util.*;

/** Weighted statistics and least-squares regression; frequency counts never expand the data. */
final class StatsData {
 final double[] x,y,w;final double n,sx,sy,sxx,syy,sxy;
 StatsData(double[] x,double[] y,double[] w){if(x.length!=y.length||x.length!=w.length||x.length==0)throw new IllegalArgumentException("请填写统计数据");this.x=x;this.y=y;this.w=w;double n=0,a=0,b=0,c=0,d=0,e=0;for(int i=0;i<x.length;i++){if(!Double.isFinite(x[i])||!Double.isFinite(y[i])||!Double.isFinite(w[i])||w[i]<=0||w[i]!=Math.rint(w[i]))throw new IllegalArgumentException("数据必须有限，频数须为正整数");n+=w[i];a+=w[i]*x[i];b+=w[i]*y[i];c+=w[i]*x[i]*x[i];d+=w[i]*y[i]*y[i];e+=w[i]*x[i]*y[i];}this.n=n;sx=a;sy=b;sxx=c;syy=d;sxy=e;}
 double mean(){return sx/n;}double populationVariance(){double m=mean(),s=0;for(int i=0;i<x.length;i++)s+=w[i]*(x[i]-m)*(x[i]-m);return s/n;}
 double sampleVariance(){if(n<=1)throw new IllegalArgumentException("样本方差需要至少两项数据");return populationVariance()*n/(n-1);}
 double min(){return Arrays.stream(x).min().getAsDouble();}double max(){return Arrays.stream(x).max().getAsDouble();}
 double median(){Integer[] order=new Integer[x.length];for(int i=0;i<x.length;i++)order[i]=i;Arrays.sort(order,Comparator.comparingDouble(i->x[i]));double left=Math.floor((n+1)/2),right=Math.floor((n+2)/2),sum=0,a=0,b=0;boolean found=false;for(int i:order){sum+=w[i];if(!found&&sum>=left){a=x[i];found=true;}if(sum>=right){b=x[i];break;}}return (a+b)/2;}
 double[] regression(String type){int dim=type.equals("quadratic")?3:2;double[][] matrix=new double[dim][dim+1];double tx=0,ty=0,txx=0,tyy=0,txy=0;for(int i=0;i<x.length;i++){double a=x[i],b=y[i];if(type.equals("log")||type.equals("power")){if(a<=0)throw new IllegalArgumentException("此回归需要 x > 0");a=Math.log(a);}if(type.equals("exponential")||type.equals("power")){if(b<=0)throw new IllegalArgumentException("此回归需要 y > 0");b=Math.log(b);}if(type.equals("inverse")){if(a==0)throw new IllegalArgumentException("逆数回归需要 x ≠ 0");a=1/a;}tx+=w[i]*a;ty+=w[i]*b;txx+=w[i]*a*a;tyy+=w[i]*b*b;txy+=w[i]*a*b;for(int r=0;r<dim;r++){for(int c=0;c<dim;c++)matrix[r][c]+=w[i]*Math.pow(a,r+c);matrix[r][dim]+=w[i]*b*Math.pow(a,r);}}
  for(int r=0;r<dim;r++){int best=r;for(int k=r+1;k<dim;k++)if(Math.abs(matrix[k][r])>Math.abs(matrix[best][r]))best=k;double[] swap=matrix[r];matrix[r]=matrix[best];matrix[best]=swap;if(Math.abs(matrix[r][r])<1e-12)throw new IllegalArgumentException("数据不足以确定回归系数");double divisor=matrix[r][r];for(int c=r;c<=dim;c++)matrix[r][c]/=divisor;for(int k=0;k<dim;k++)if(k!=r){double factor=matrix[k][r];for(int c=r;c<=dim;c++)matrix[k][c]-=factor*matrix[r][c];}}
  double[] result=new double[dim+1];for(int i=0;i<dim;i++)result[i]=matrix[i][dim];if(type.equals("exponential")||type.equals("power"))result[0]=Math.exp(result[0]);double denom=(n*txx-tx*tx)*(n*tyy-ty*ty);result[dim]=denom>0?(n*txy-tx*ty)/Math.sqrt(denom):Double.NaN;return result;
 }
}
