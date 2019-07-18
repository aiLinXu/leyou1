package com.leyou.common.auth;

import com.leyou.common.auth.entity.Payload;
import com.leyou.common.auth.entity.UserInfo;
import com.leyou.common.auth.utils.JwtUtils;
import com.leyou.common.auth.utils.RsaUtils;
import org.junit.Test;

import java.security.PrivateKey;
import java.security.PublicKey;

public class RsaUtilsTest {
    /**
     * 公钥地址
     */
    private String publicKeyFilename = "C:\\lesson\\heima68\\ssh\\id_rsa.pub";
    /**
     * 私钥地址
     */
    private String privateKeyFilename = "C:\\lesson\\heima68\\ssh\\id_rsa";


    @org.junit.Test
    public void generateKeyPair() throws Exception {
        // RSA算法运算的种子
        String secret = "HelloWorld";

        RsaUtils.generateKeyPair(publicKeyFilename, privateKeyFilename, secret, 0);
    }

    @Test
    public void readKey() throws Exception {
        PublicKey publicKey = RsaUtils.getPublicKey(publicKeyFilename);
        System.out.println("publicKey = " + publicKey);
        PrivateKey privateKey = RsaUtils.getPrivateKey(privateKeyFilename);
        System.out.println("privateKey = " + privateKey);
    }

    @Test
    public void testJwt() throws Exception {
        PrivateKey privateKey = RsaUtils.getPrivateKey(privateKeyFilename);
        PublicKey publicKey = RsaUtils.getPublicKey(publicKeyFilename);
        // 创建载荷中的用户信息
         UserInfo userInfo = new UserInfo(1L, "Jack", "guest");
        // 生成token
//        String jwt = JwtUtils.generateTokenExpireInMinutes(userInfo, privateKey, 5);
//        String jwt = "eyJhbGciOiJSUzI1NiJ9.eyJleHAiOjE1NjI0Njg1NTQsImp0aSI6IjhjYjlmZjU2LTI4MmUtNGQxMy1hMzM3LTMwYWNkZWU5ZTI1ZiIsInVzZXIiOiJ7XCJpZFwiOjEsXCJ1c2VybmFtZVwiOlwiSmFja01hXCIsXCJyb2xlXCI6XCJhZG1pblwifSJ9.bhc2A6VM6xM2c2BS3AdMlYIE3Sl0jwHTsNjraV9oG5drEvKgJOJMPlhibA5OwG0j5LyIjiimZlE-cDOugqsVGN1a6aSCHBR50vAIEQBj932yzCz3Xu0i1beNJmBuRXkCOTZ9ymjJA34RX_tDtu1isNXFACRLftPPfEpq21YSQy9cLIp8Apit7khUiIiKmeJRlwYLsRG5rihPtKgKu__qWQMj8qUEqpriUdO-aJXuhUOE6zXBvOjHjPzRUpGtsx_YTaxF5J78rE1YhX4vD8S__f_YAegNj1fm-bUk2iqwMCRzBxNxyn1uRoKmMeTkHUMJ-o2jbac41DMmXCyXqoSx8A";
        String jwt = JwtUtils.generateTokenExpireInSeconds(userInfo, privateKey, 5);
        System.out.println("jwt = " + jwt);

        Thread.sleep(6000L);

        // 解析token
        Payload<UserInfo> payload = JwtUtils.getInfoFromToken(jwt, publicKey, UserInfo.class);

        System.out.println("payload = " + payload);
    }

    /*private <T> Payload<T> parseToken(String token, PublicKey publicKey, Class<T> returnType) {
        Jws<Claims> jws = Jwts.parser()         // (1) 得到解析器
                .setSigningKey(publicKey)         // (2) 添加解析的密钥
                .parseClaimsJws(token); // (3) 开始解析
        Claims body = jws.getBody();
        Payload<T> payload = new Payload<>();
        payload.setId(body.getId());
        payload.setExpiration(body.getExpiration());
        payload.setUserInfo(JsonUtils.toBean(body.get("user", String.class), returnType));
        return payload;
    }

    public String generateToken(Object userInfo, PrivateKey key, int expireMinutes) {
        return Jwts.builder() // (1) 构建工程
                .setExpiration(new Date(System.currentTimeMillis() + expireMinutes * 60 * 1000))  // (2) 设置载荷 Exp
                .setId(UUID.randomUUID().toString())  // JTI
                .claim("user", JsonUtils.toString(userInfo)) // 自定义载荷
                .signWith(key, SignatureAlgorithm.RS256) // 签名，指定密钥
                .compact();
    }*/
}