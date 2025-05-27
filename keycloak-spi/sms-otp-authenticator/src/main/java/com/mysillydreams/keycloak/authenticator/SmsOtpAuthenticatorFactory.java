package com.mysillydreams.keycloak.authenticator;

import org.keycloak.Config;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.Arrays;
import java.util.List;

public class SmsOtpAuthenticatorFactory implements AuthenticatorFactory {

    public static final String PROVIDER_ID = "sms-otp-authenticator";
    private static final SmsOtpAuthenticator SINGLETON = new SmsOtpAuthenticator();

    public static final String CONFIG_SMS_SERVICE_URL = "sms.service.url";
    public static final String CONFIG_OTP_LENGTH = "otp.length";
    public static final String CONFIG_OTP_TTL_SECONDS = "otp.ttl.seconds";


    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public Authenticator create(KeycloakSession session) {
        // Pass the session to the authenticator if it needs it,
        // though for this simple case, SINGLETON might not store session state directly.
        // If SmsOtpAuthenticator needs per-request session data, it should be instantiated here:
        // return new SmsOtpAuthenticator(session);
        return SINGLETON;
    }

    private static final AuthenticationExecutionModel.Requirement[] REQUIREMENT_CHOICES = {
            AuthenticationExecutionModel.Requirement.REQUIRED,
            AuthenticationExecutionModel.Requirement.ALTERNATIVE,
            AuthenticationExecutionModel.Requirement.DISABLED
    };

    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return REQUIREMENT_CHOICES;
    }

    @Override
    public boolean isUserSetupAllowed() {
        // true if users can configure this authenticator themselves (e.g., register mobile number)
        // For SMS OTP, initial setup might be admin-driven or part of a separate flow.
        // Let's assume true for now, can be refined.
        return true;
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return Arrays.asList(
                new ProviderConfigProperty(
                        CONFIG_SMS_SERVICE_URL,
                        "SMS Service URL",
                        "The base URL of the SMS microservice (e.g., http://sms-service:8080/send-otp).",
                        ProviderConfigProperty.STRING_TYPE,
                        "http://localhost:8082/sms/send" // Default value, adjust port as needed for sms-service
                ),
                new ProviderConfigProperty(
                        CONFIG_OTP_LENGTH,
                        "OTP Length",
                        "The number of digits for the OTP code.",
                        ProviderConfigProperty.STRING_TYPE,
                        "6"
                ),
                new ProviderConfigProperty(
                        CONFIG_OTP_TTL_SECONDS,
                        "OTP Time-to-Live (seconds)",
                        "The validity duration of the OTP in seconds.",
                        ProviderConfigProperty.STRING_TYPE,
                        "300" // 5 minutes
                )
        );
    }

    @Override
    public String getHelpText() {
        return "Provides OTP authentication via an external SMS service. Users enter a code sent to their mobile device.";
    }

    @Override
    public String getDisplayType() {
        return "SMS OTP Authenticator";
    }

    @Override
    public String getReferenceCategory() {
        return "otp"; // Or "multi-factor" or a custom category
    }

    @Override
    public void init(Config.Scope config) {
        // Called when Keycloak server starts.
        // Can be used to initialize resources based on global server config if needed.
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // Called after all provider factories have been initialized.
    }

    @Override
    public void close() {
        // Called when Keycloak server shuts down.
    }
}
