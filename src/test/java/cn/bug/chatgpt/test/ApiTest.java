package cn.bug.chatgpt.test;
import cn.bug.chatgpt.domain.security.service.JwtUtil;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @description 单元测试
 */
public class ApiTest {

    @Test
    public void test_jwt() {
        JwtUtil util = new JwtUtil("xfg", SignatureAlgorithm.HS256);
        // 以tom作为秘钥，以HS256加密
        Map<String, Object> map = new HashMap<>();
        map.put("username", "xfg");
        map.put("password", "123");
        map.put("age", 100);

        String jwtToken = util.encode("xfg", 30000, map);

        util.decode(jwtToken).forEach((key, value) -> System.out.println(key + ": " + value));
    }

}

