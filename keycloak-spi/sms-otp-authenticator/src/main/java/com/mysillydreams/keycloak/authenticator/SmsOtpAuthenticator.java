package com.mysillydreams.keycloak.authenticator;

import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.*;

import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.SecureRandom;
import java.time.Instant;

public class SmsOtpAuthenticator implements Authenticator {

    private static final Logger logger = Logger.getLogger(SmsOtpAuthenticator.class);
    private static final String TPL_OTP_FORM = "sms-otp-form.ftl";
    private static final String TPL_MOBILE_FORM = "sms-mobile-form.ftl"; // Optional: if mobile needs to be collected/confirmed

    private static final String AUTH_NOTE_OTP_CODE = "smsOtpCode";
    private static final String AUTH_NOTE_OTP_EXPIRY = "smsOtpExpiry";
    private static final String AUTH_NOTE_MOBILE_NUMBER = "smsOtpMobileNumber";

    private final SecureRandom random = new SecureRandom();

    // Consider making HttpClient a shared instance or injected if this class is not a singleton
    private final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .build();

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        UserModel user = context.getUser();
        AuthenticatorConfigModel configModel = context.getAuthenticatorConfig();
        KeycloakSession session = context.getSession();

        String mobileNumber = user.getFirstAttribute("mobile_number"); // Assuming mobile_number is stored as a user attribute

        // Step 1: Get or confirm mobile number (simplified for now)
        // In a real scenario, if mobileNumber is null or needs verification,
        // you might show TPL_MOBILE_FORM first.
        // For this example, we assume mobile_number is present and verified.

        if (mobileNumber == null || mobileNumber.trim().isEmpty()) {
            logger.warnf("User %s does not have a mobile_number attribute set.", user.getUsername());
            // Option 1: Fail the flow
            // context.failure(AuthenticationFlowError.INVALID_USER_CREDENTIALS, Response.status(Response.Status.BAD_REQUEST).entity("Mobile number not found.").build());
            // Option 2: Redirect to a form to input mobile number (more complex)
            // For now, just display an error on the OTP form page or a generic error.
            Response challenge = context.form()
                .setError("smsOtpMobileNumberMissing")
                .createForm(TPL_OTP_FORM); // Or a dedicated error page
            context.challenge(challenge);
            return;
        }
        context.getAuthenticationSession().setAuthNote(AUTH_NOTE_MOBILE_NUMBER, mobileNumber);

        // Step 2: Generate and send OTP
        int otpLength = Integer.parseInt(configModel.getConfig().getOrDefault(SmsOtpAuthenticatorFactory.CONFIG_OTP_LENGTH, "6"));
        long otpTtlSeconds = Long.parseLong(configModel.getConfig().getOrDefault(SmsOtpAuthenticatorFactory.CONFIG_OTP_TTL_SECONDS, "300"));

        String otp = generateOtp(otpLength);
        Instant expiry = Instant.now().plusSeconds(otpTtlSeconds);

        context.getAuthenticationSession().setAuthNote(AUTH_NOTE_OTP_CODE, otp);
        context.getAuthenticationSession().setAuthNote(AUTH_NOTE_OTP_EXPIRY, String.valueOf(expiry.toEpochMilli()));

        boolean smsSent = sendOtpViaSmsService(context, mobileNumber, otp);

        if (!smsSent) {
            logger.errorf("Failed to send OTP SMS to user %s on number %s", user.getUsername(), mobileNumber);
            Response challenge = context.form()
                .setError("smsOtpSendFailed")
                .createForm(TPL_OTP_FORM);
            context.challenge(challenge); // Or context.failure() if it's a hard stop
            return;
        }

        // Step 3: Display OTP entry form
        Response challenge = context.form()
                .setAttribute("username", user.getUsername()) // For display on the form
                .createForm(TPL_OTP_FORM);
        context.challenge(challenge);
    }

    private String generateOtp(int length) {
        StringBuilder otp = new StringBuilder();
        for (int i = 0; i < length; i++) {
            otp.append(random.nextInt(10));
        }
        return otp.toString();
    }

    private boolean sendOtpViaSmsService(AuthenticationFlowContext context, String mobileNumber, String otp) {
        AuthenticatorConfigModel configModel = context.getAuthenticatorConfig();
        String smsServiceUrl = configModel.getConfig().get(SmsOtpAuthenticatorFactory.CONFIG_SMS_SERVICE_URL);
        UserModel user = context.getUser();

        if (smsServiceUrl == null || smsServiceUrl.trim().isEmpty()) {
            logger.error("SMS Service URL is not configured.");
            return false;
        }

        String message = "Your OTP for " + context.getRealm().getDisplayName() + " is: " + otp;
        // In a real SMS service, you'd likely send JSON
        String requestBody = String.format("{\"mobileNumber\": \"%s\", \"message\": \"%s\"}", mobileNumber, message);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(smsServiceUrl)) // Assuming smsServiceUrl is the full endpoint path
                    .header("Content-Type", "application/json") // Or whatever your sms-service expects
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            logger.infof("Sending OTP request to SMS service for user %s at URL %s", user.getUsername(), smsServiceUrl);
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                logger.infof("Successfully sent OTP to user %s via SMS service. Response: %s", user.getUsername(), response.body());
                return true;
            } else {
                logger.errorf("Failed to send OTP via SMS service for user %s. Status: %d, Response: %s",
                        user.getUsername(), response.statusCode(), response.body());
                return false;
            }
        } catch (Exception e) {
            logger.errorf(e, "Exception while sending OTP via SMS service for user %s.", user.getUsername());
            return false;
        }
    }


    @Override
    public void action(AuthenticationFlowContext context) {
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
        String enteredOtp = formData.getFirst("otp_code");

        AuthenticationSessionModel authSession = context.getAuthenticationSession();
        String storedOtp = authSession.getAuthNote(AUTH_NOTE_OTP_CODE);
        String otpExpiryMillisStr = authSession.getAuthNote(AUTH_NOTE_OTP_EXPIRY);

        if (storedOtp == null || otpExpiryMillisStr == null) {
            logger.warn("OTP not found in session or expired. Possible tampering or session issue.");
            Response challenge = context.form()
                    .setError("smsOtpNoOtpInSession")
                    .createForm(TPL_OTP_FORM);
            context.failureChallenge(AuthenticationFlowError.EXPIRED_CODE, challenge);
            return;
        }

        Instant otpExpiry = Instant.ofEpochMilli(Long.parseLong(otpExpiryMillisStr));

        if (Instant.now().isAfter(otpExpiry)) {
            logger.warnf("OTP for user %s has expired.", context.getUser().getUsername());
            Response challenge = context.form()
                    .setError("smsOtpExpired")
                    .createForm(TPL_OTP_FORM);
            context.failureChallenge(AuthenticationFlowError.EXPIRED_CODE, challenge);
            return;
        }

        if (storedOtp.equals(enteredOtp)) {
            logger.infof("User %s successfully authenticated with SMS OTP.", context.getUser().getUsername());
            // Clear auth notes after successful validation
            authSession.removeAuthNote(AUTH_NOTE_OTP_CODE);
            authSession.removeAuthNote(AUTH_NOTE_OTP_EXPIRY);
            authSession.removeAuthNote(AUTH_NOTE_MOBILE_NUMBER);
            context.success();
        } else {
            logger.warnf("Invalid OTP entered by user %s.", context.getUser().getUsername());
            Response challenge = context.form()
                    .setError("smsOtpInvalid")
                    .setUser(context.getUser()) // Keep user context for the form
                    .createForm(TPL_OTP_FORM);
            context.failureChallenge(AuthenticationFlowError.INVALID_CREDENTIALS, challenge);
        }
    }

    @Override
    public boolean requiresUser() {
        // This authenticator acts on an already identified user (to get their mobile number)
        return true;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        // Check if the user has a mobile number configured.
        // This could also check if the user has explicitly enabled SMS OTP.
        // For now, just checks for mobile_number attribute.
        return user.getFirstAttribute("mobile_number") != null;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
        // If 'configuredFor' returns false, this could add a required action
        // for the user to set up their mobile number.
        // Example: user.addRequiredAction("CONFIGURE_SMS_OTP_ACTION_ALIAS");
        // This requires a corresponding RequiredActionProvider.
    }

    @Override
    public void close() {
        // Called when Keycloak server shuts down.
        // Clean up any resources if necessary.
    }
}
