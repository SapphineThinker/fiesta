package de.otto.edison.example.oauth;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.jwt.Jwt;
import org.springframework.security.jwt.JwtHelper;
import org.springframework.security.jwt.crypto.sign.RsaSigner;
import org.springframework.stereotype.Service;

import java.security.KeyPair;
import java.security.interfaces.RSAPrivateKey;
import java.time.ZonedDateTime;
import java.util.Base64;

@Service
public class OAuthService {

    private final KeyPair keyPair;

    @Autowired
    public OAuthService(KeyPair keyPair) {
        this.keyPair = keyPair;
    }

    public Jwt getExampleJWTToken() {
        final ZonedDateTime soon = ZonedDateTime.now().plusDays(365);
        final String jwtToken = "{\n" +
                "  \"aud\": [\n" +
                "    \"someAudience\"\n" +
                "  ],\n" +
                "  \"exp\": " + soon.toInstant().getEpochSecond() + ",\n" +
                "  \"user_name\": \"3d44bbc24614e28edd094bc54ef0497809717af5\",\n" +
                "  \"jti\": \"3cee521d-96a7-4d82-b726-7e02355f3a55\",\n" +
                "  \"client_id\": \"fe0661e5a99e4d43bd3496cc6c58025f\",\n" +
                "  \"scope\": [\n" +
                "    \"hello.read\"\n" +
                "  ]\n" +
                "}";
        final RsaSigner rsaSigner = new RsaSigner((RSAPrivateKey) keyPair.getPrivate());


        return JwtHelper.encode(jwtToken, rsaSigner);
    }

    public String getPublicKey() {
        return "-----BEGIN PUBLIC KEY-----\n" +
                new String(this.keyPair.getPublic().getEncoded()) +
                "\n-----END PUBLIC KEY-----";

    }
}
