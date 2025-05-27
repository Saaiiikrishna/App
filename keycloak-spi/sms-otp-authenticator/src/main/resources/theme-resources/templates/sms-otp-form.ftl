<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
    <#if section = "header">
        ${msg("smsOtpTitleText", realm.name)}
    <#elseif section = "form">
        <form id="kc-sms-otp-login-form" class="${properties.kcFormClass!}" action="${url.loginAction}" method="post">
            <div class="${properties.kcFormGroupClass!}">
                <div class="${properties.kcLabelWrapperClass!}">
                    <label for="otp_code" class="${properties.kcLabelClass!}">${msg("smsOtpLabelText")}</label>
                </div>
                <div class="${properties.kcInputWrapperClass!}">
                    <input id="otp_code" name="otp_code" type="text" class="${properties.kcInputClass!}" autofocus pattern="\d*" inputmode="numeric" autocomplete="one-time-code" />
                </div>
            </div>

            <#-- Display error messages -->
            <#if message?has_content>
                <div class="alert alert-${message.type}">
                    <#if message.type = 'error' || message.type = 'warning'> <#-- Check if message.type is error or warning -->
                         <span class="${properties.kcFeedbackErrorIcon!}"></span>
                    <#else>
                         <span class="${properties.kcFeedbackSuccessIcon!}"></span> <#-- Default to success icon if not error or warning -->
                    </#!if>
                    <span class="kc-feedback-text">${message.summary}</span>
                </div>
            </#if>
            
            <div class="${properties.kcFormGroupClass!}">
                <div id="kc-form-options" class="${properties.kcFormOptionsClass!}">
                    <div class="${properties.kcFormOptionsWrapperClass!}">
                    </div>
                </div>

                <div id="kc-form-buttons" class="${properties.kcFormButtonsClass!}">
                    <input class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}" 
                           name="login" id="kc-login" type="submit" value="${msg("doLogIn")}"/>
                </div>
            </div>
        </form>
    </#if>
</@layout.registrationLayout>
