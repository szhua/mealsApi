package com.sancanji.mealsapi.util;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class WeChatUtil {

    @Value("${wechat.appid}")
    private String appid;

    @Value("${wechat.secret}")
    private String secret;

    /**
     * 通过code获取微信OpenID和SessionKey
     */
    public JSONObject code2Session(String code) {
        String url = String.format(
                "https://api.weixin.qq.com/sns/jscode2session?appid=%s&secret=%s&js_code=%s&grant_type=authorization_code",
                appid, secret, code
        );
        String result = HttpUtil.get(url);
        return JSONUtil.parseObj(result);
    }

    /**
     * 获取AccessToken
     */
    public String getAccessToken() {
        String url = String.format(
                "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid=%s&secret=%s",
                appid, secret
        );
        String result = HttpUtil.get(url);
        JSONObject json = JSONUtil.parseObj(result);
        return json.getStr("access_token");
    }

    /**
     * 获取手机号
     */
    public JSONObject getPhoneNumber(String code) {
        String accessToken = getAccessToken();
        String url = "https://api.weixin.qq.com/wxa/business/getuserphonenumber?access_token=" + accessToken;
        String body = JSONUtil.createObj().set("code", code).toString();
        String result = HttpUtil.post(url, body);
        return JSONUtil.parseObj(result);
    }
}