package net.tokensmith.authorization.http.presenter;

import java.util.Optional;

public class ForgotPasswordPresenter extends AssetPresenter {
    private Optional<String> errorMessage;
    private String email;
    private String encodedCsrfToken;

    public ForgotPasswordPresenter(String globalCssPath, String email, String encodedCsrfToken) {
        super(globalCssPath);
        this.email = email;
        this.encodedCsrfToken = encodedCsrfToken;
    }

    public Optional<String> getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(Optional<String> errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getEncodedCsrfToken() {
        return encodedCsrfToken;
    }

    public void setEncodedCsrfToken(String encodedCsrfToken) {
        this.encodedCsrfToken = encodedCsrfToken;
    }
}
