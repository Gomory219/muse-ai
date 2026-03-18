package cn.edu.sxu.museai.utils;

import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.json.JSONUtil;

public class CacheKeyUtils {

    public static String generateCacheKey(Object o) {
        String jsonStr = JSONUtil.toJsonStr(o);
        return DigestUtil.md5Hex(jsonStr);
    }

}
