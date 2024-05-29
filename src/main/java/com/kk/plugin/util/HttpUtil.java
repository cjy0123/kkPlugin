package com.kk.plugin.util;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class HttpUtil {

	/**
	 * 从网络Url中下载文件
	 */
	public static void downLoadFromUrl(String urlStr, int timeout, String fileName, String savePath) throws IOException {
		URL url = new URL(getUtf8Url(urlStr));
		HttpURLConnection conn = null;
		try {
			conn = (HttpURLConnection) url.openConnection();
			// 设置超时间
			conn.setConnectTimeout(timeout);//连接超时时间
			conn.setReadTimeout(timeout);//读数据超时时间
			conn.setUseCaches(false);
			// 文件保存位置
			File saveDir = new File(savePath);
			if (!saveDir.exists()) {
				if (!saveDir.mkdirs()) {
					throw new RuntimeException("创建下载目录失败！");
				}
			}
			File file = new File(saveDir + File.separator + fileName);
			try (InputStream in = conn.getInputStream();
					FileOutputStream out = new FileOutputStream(file)) {
				byte[] buffer = new byte[1024];
				int len;
				while ((len = in.read(buffer)) != -1) {
					out.write(buffer, 0, len);
				}
			}
		} finally {
			try {
				if (conn != null) {
					conn.disconnect();
				}
			} catch (Throwable e){
				e.printStackTrace();
			}
		}
	}

	/**
	 * 发送get请求获取返回文本数据
	 */
	public static String doGet(String urlStr, int timeout) throws IOException {
		URL url = new URL(getUtf8Url(urlStr));
		HttpURLConnection conn = null;
		try {
			conn = (HttpURLConnection) url.openConnection();
			// 设置超时间
			conn.setConnectTimeout(timeout);//连接超时时间
			conn.setReadTimeout(5000);//读数据超时时间
			conn.setUseCaches(false);
			try (InputStream in = conn.getInputStream();
				 ByteArrayOutputStream out = new ByteArrayOutputStream()) {
				byte[] buffer = new byte[1024];
				int len;
				while ((len = in.read(buffer)) != -1) {
					out.write(buffer, 0, len);
				}
				return out.toString(StandardCharsets.UTF_8);
			}
		} finally {
			try {
				if (conn != null) {
					conn.disconnect();
				}
			} catch (Throwable e){
				e.printStackTrace();
			}
		}
	}

	/**
	 * 中文或其他多字节字符转化为url编码
	 */
	private static String getUtf8Url(String url) {
		StringBuilder utf8Url = new StringBuilder();
		for (int i = 0; i < url.length(); i++) {
			char c = url.charAt(i);
			String s = String.valueOf(c);
			if (s.getBytes().length > 1) {//多字节表示的字符
				utf8Url.append(URLEncoder.encode(s, StandardCharsets.UTF_8));
			}else{
				utf8Url.append(c);
			}
		}
		return utf8Url.toString();
	}
}
