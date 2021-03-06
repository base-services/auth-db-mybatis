package net.tokensmith.authorization.register.exception;


import net.tokensmith.authorization.register.RegisterError;

public class RegisterException extends Exception {
    private RegisterError registerError;

    public RegisterException(String message, RegisterError registerError) {
        super(message);
        this.registerError = registerError;
    }

    public RegisterException(String message, RegisterError registerError, Throwable cause) {
        super(message, cause);
        this.registerError = registerError;
    }

    public RegisterError getRegisterError() {
        return registerError;
    }

    public void setRegisterError(RegisterError registerError) {
        this.registerError = registerError;
    }
}
