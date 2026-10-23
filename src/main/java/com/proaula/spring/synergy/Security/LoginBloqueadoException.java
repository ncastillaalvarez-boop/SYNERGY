package com.proaula.spring.synergy.Security;

/** Se lanza cuando un correo superó el número de intentos fallidos (ver IntentosLoginService). */
public class LoginBloqueadoException extends RuntimeException {

    public LoginBloqueadoException(String mensaje) {
        super(mensaje);
    }
}
