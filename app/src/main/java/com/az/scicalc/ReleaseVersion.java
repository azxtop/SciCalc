package com.az.scicalc;

import java.math.BigInteger;
import java.util.regex.*;

/** Numeric release comparison, independent of dates or GitHub's display name. */
final class ReleaseVersion {
    private static BigInteger[] parts(String text){Matcher m=Pattern.compile("^v?(\\d+)\\.(\\d+)(?:\\.(\\d+))?(?:\\+[0-9A-Za-z.-]+)?$").matcher(text.trim());if(!m.matches())throw new IllegalArgumentException("无法识别发布版本号");return new BigInteger[]{new BigInteger(m.group(1)),new BigInteger(m.group(2)),new BigInteger(m.group(3)==null?"0":m.group(3))};}
    static int compare(String left,String right){BigInteger[] a=parts(left),b=parts(right);for(int i=0;i<3;i++){int n=a[i].compareTo(b[i]);if(n!=0)return n;}return 0;}
    static boolean officialUrl(String url){try{java.net.URI uri=new java.net.URI(url);return "https".equals(uri.getScheme())&&"github.com".equals(uri.getHost())&&uri.getUserInfo()==null&&uri.getPort()==-1&&uri.getPath().startsWith("/azxtop/SciCalc/releases/");}catch(Exception e){return false;}}
}
