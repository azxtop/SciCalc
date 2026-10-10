package com.az.scicalc;

import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import org.json.*;

/** Reads public release metadata on demand. Opening downloads is an explicit user action. */
final class UpdateChecker {
    static JSONObject latest() throws IOException,JSONException {
        HttpURLConnection c=(HttpURLConnection)new URL("https://api.github.com/repos/azxtop/SciCalc/releases/latest").openConnection();
        c.setConnectTimeout(10000);c.setReadTimeout(10000);c.setRequestProperty("Accept","application/vnd.github+json");c.setRequestProperty("User-Agent","SciCalc/"+BuildConfig.VERSION_NAME);c.setRequestProperty("X-GitHub-Api-Version","2022-11-28");
        try{int code=c.getResponseCode();if(code==404)throw new IOException("仓库暂时没有正式发布版本");if(code==403||code==429)throw new IOException("GitHub 暂时限制请求，请稍后重试");if(code!=200)throw new IOException("服务器返回 "+code);ByteArrayOutputStream buffer=new ByteArrayOutputStream();try(InputStream in=c.getInputStream()){byte[] bytes=new byte[8192];int n;while((n=in.read(bytes))!=-1){if(buffer.size()+n>1024*1024)throw new IOException("更新信息过大");buffer.write(bytes,0,n);}}JSONObject release=new JSONObject(buffer.toString(StandardCharsets.UTF_8.name()));if(release.optBoolean("draft")||release.optBoolean("prerelease"))throw new IOException("没有可用的正式版本");if(!ReleaseVersion.officialUrl(release.optString("html_url")))throw new IOException("更新链接无效");ReleaseVersion.compare(release.getString("tag_name"),BuildConfig.VERSION_NAME);return release;}finally{c.disconnect();}
    }
}
