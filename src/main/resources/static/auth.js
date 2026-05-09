document.addEventListener("DOMContentLoaded", async () => {
    const app = window.ProjectApp;
    const page = document.body.dataset.page;

    const publicPages = new Set([
        "login",
        "register-customer",
        "register-specialist",
        "forgot-password",
        "reset-password"
    ]);

    bindPageHandlers();
    app.consumeFlash("toast");

    if (publicPages.has(page)) {
        const redirected = await app.redirectIfAuthenticated();
        if (redirected) {
            return;
        }
    }

    function bindPageHandlers() {
        if (page === "login") {
            document.getElementById("login-form").addEventListener("submit", onLogin);
        }

        if (page === "register-customer") {
            document.getElementById("customer-register-form").addEventListener("submit", (event) =>
                    onRegister(event, "/api/auth/register/customer", "Create Account")
            );
        }

        if (page === "register-specialist") {
            document.getElementById("specialist-register-form").addEventListener("submit", (event) =>
                    onRegister(event, "/api/auth/register/specialist", "Create Specialist Account", normalizeSpecialistPayload)
            );
        }

        if (page === "change-password") {
            document.getElementById("change-password-form").addEventListener("submit", onChangePassword);
        }

        if (page === "forgot-password") {
            document.getElementById("forgot-password-form").addEventListener("submit", onForgotPassword);
        }

        if (page === "reset-password") {
            document.getElementById("reset-password-form").addEventListener("submit", onResetPassword);
        }
    }

    if (page === "change-password") {
        const user = await app.requireAuth("/login.html");
        if (!user) {
            return;
        }
    }

    async function onLogin(event) {
        event.preventDefault();
        const form = event.currentTarget;
        const payload = app.formToObject(form);

        app.clearFormErrors(form);
        await app.withFormLoading(form, "Signing in...", async () => {
            const session = await app.api("/api/auth/login", {
                method: "POST",
                body: JSON.stringify(payload)
            });
            app.setToken(session.token);
            app.queueFlash("Sign-in successful.", "success");
            window.location.replace(app.workspacePathForRole(session.role));
        }).catch((error) => app.renderFormErrors(form, error, "Unable to sign in. Please check your account details."));
    }

    async function onRegister(event, endpoint, resetLabel, transform = (payload) => payload) {
        event.preventDefault();
        const form = event.currentTarget;
        const payload = transform(app.formToObject(form));

        app.clearFormErrors(form);
        await app.withFormLoading(form, "Registering...", async () => {
            await app.api(endpoint, {
                method: "POST",
                body: JSON.stringify(payload)
            });
            app.clearSession();
            app.queueFlash("Registration successful. Please sign in with your new account.", "success");
            window.location.replace("/login.html");
        }).catch((error) => app.renderFormErrors(form, error, "Unable to create the account. Please review the form."));
    }

    async function onChangePassword(event) {
        event.preventDefault();
        const form = event.currentTarget;
        const payload = app.formToObject(form);

        if (!validatePasswordConfirmation(form, payload.newPassword, payload.confirmPassword, "confirmPassword")) {
            return;
        }

        app.clearFormErrors(form);
        await app.withFormLoading(form, "Saving...", async () => {
            await app.api("/api/auth/change-password", {
                method: "POST",
                body: JSON.stringify({
                    currentPassword: payload.currentPassword,
                    newPassword: payload.newPassword
                })
            });
            app.clearSession();
            app.queueFlash("Password updated successfully. Please sign in again.", "success");
            window.location.replace("/login.html");
        }).catch((error) => app.renderFormErrors(form, error, "Unable to change the password. Please review the form."));
    }

    async function onForgotPassword(event) {
        event.preventDefault();
        const form = event.currentTarget;
        const panel = document.getElementById("reset-code-panel");
        const codeValue = document.getElementById("reset-code-value");
        const expiry = document.getElementById("reset-code-expiry");

        app.clearFormErrors(form);
        panel.classList.add("hidden");
        await app.withFormLoading(form, "Generating...", async () => {
            const response = await app.api("/api/auth/password-reset/request", {
                method: "POST",
                body: JSON.stringify(app.formToObject(form))
            });
            form.reset();
            if (response.resetCode) {
                codeValue.textContent = response.resetCode;
                expiry.textContent = `Expires at ${app.formatDateTime(response.expiresAt)}.`;
                panel.classList.remove("hidden");
            }
            app.showToast(response.message, "success", "toast");
        }).catch((error) => app.renderFormErrors(form, error, "Unable to generate a reset code. Please review the form."));
    }

    async function onResetPassword(event) {
        event.preventDefault();
        const form = event.currentTarget;
        const payload = app.formToObject(form);

        if (!validatePasswordConfirmation(form, payload.newPassword, payload.confirmPassword, "confirmPassword")) {
            return;
        }

        app.clearFormErrors(form);
        await app.withFormLoading(form, "Resetting...", async () => {
            await app.api("/api/auth/password-reset/confirm", {
                method: "POST",
                body: JSON.stringify({
                    resetCode: payload.resetCode,
                    newPassword: payload.newPassword
                })
            });
            app.clearSession();
            app.queueFlash("Password reset completed. Please sign in with the new password.", "success");
            window.location.replace("/login.html");
        }).catch((error) => app.renderFormErrors(form, error, "Unable to reset the password. Please review the form."));
    }

    function normalizeSpecialistPayload(payload) {
        return {
            ...payload,
            categoryName: (payload.categoryName || "").trim(),
            level: (payload.level || "").trim(),
            baseFee: Number(payload.baseFee)
        };
    }

    function validatePasswordConfirmation(form, password, confirmPassword, confirmFieldName) {
        app.clearFormErrors(form);

        if (password === confirmPassword) {
            return true;
        }

        app.setFieldError(form, confirmFieldName, "The password confirmation does not match.");
        app.setFormError(form, "Please correct the highlighted fields and try again.");
        return false;
    }
});
