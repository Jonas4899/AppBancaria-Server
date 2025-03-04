package com.appBancaria.servicio;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class JWTUtil {
    // Clave secreta para firmar el token JWT
    private static final String SECRET_KEY = "clave_secreta_app_bancaria";
    // Tiempo de expiración del token (1 hora en milisegundos)
    private static final long EXPIRATION_TIME = 3600000;
    
    /**
     * Genera un token JWT para el usuario autenticado
     * @param userId ID del usuario en la base de datos
     * @return Token JWT generado y sessionId
     */
    public static Map<String, String> generarToken(int userId) {
        Map<String, String> resultado = new HashMap<>();
        
        try {
            // Generar un sessionId único
            String sessionId = UUID.randomUUID().toString();
            
            // Calcular fechas de emisión y expiración
            Date now = new Date();
            Date expiryDate = new Date(now.getTime() + EXPIRATION_TIME);
            
            // Crear el token JWT
            String token = JWT.create()
                    .withClaim("userId", userId)
                    .withClaim("sessionId", sessionId)
                    .withIssuedAt(now)
                    .withExpiresAt(expiryDate)
                    .sign(Algorithm.HMAC256(SECRET_KEY));
            
            resultado.put("token", token);
            resultado.put("sessionId", sessionId);
            
            return resultado;
        } catch (JWTCreationException exception) {
            throw new RuntimeException("Error al generar token JWT", exception);
        }
    }
    
    /**
     * Valida un token JWT y devuelve la información contenida
     * @param token Token JWT a validar
     * @return Mapa con la información del token, null si el token es inválido
     */
    public static Map<String, Object> validarToken(String token) {
        try {
            JWTVerifier verifier = JWT.require(Algorithm.HMAC256(SECRET_KEY)).build();
            DecodedJWT jwt = verifier.verify(token);
            
            Map<String, Object> claims = new HashMap<>();
            claims.put("userId", jwt.getClaim("userId").asInt());
            claims.put("sessionId", jwt.getClaim("sessionId").asString());
            claims.put("iat", jwt.getIssuedAt());
            claims.put("exp", jwt.getExpiresAt());
            
            return claims;
        } catch (JWTVerificationException exception) {
            return null;
        }
    }
}