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
        const button = form.querySelector("button");
        const payload = app.formToObject(form);

        app.clearFormErrors(form);
        button.disabled = true;
        button.textContent = "Signing in...";

        try {
            const session = await app.api("/api/auth/login", {
                method: "POST",
                body: JSON.stringify(payload)
            });
            app.setToken(session.token);
            app.queueFlash("Sign-in successful.", "success");
            window.location.replace(app.workspacePathForRole(session.role));
        } catch (error) {
            app.renderFormErrors(form, error);
        } finally {
            button.disabled = false;
            button.textContent = "Sign In";
        }
    }

    async function onRegister(event, endpoint, resetLabel, transform = (payload) => payload) {
        event.preventDefault();
        const form = event.currentTarget;
        const button = form.querySelector("button");
        const payload = transform(app.formToObject(form));

        app.clearFormErrors(form);
        button.disabled = true;
        button.textContent = "Registering...";

        try {
            await app.api(endpoint, {
                method: "POST",
                body: JSON.stringify(payload)
            });
            app.clearSession();
            app.queueFlash("Registration successful. Please sign in with your new account.", "success");
            window.location.replace("/login.html");
        } catch (error) {
            app.renderFormErrors(form, error);
        } finally {
            button.disabled = false;
            button.textContent = resetLabel;
        }
    }

    async function onChangePassword(event) {
        event.preventDefault();
        const form = event.currentTarget;
        const button = form.querySelector("button");
        const payload = app.formToObject(form);

        if (!validatePasswordConfirmation(form, payload.newPassword, payload.confirmPassword, "confirmPassword")) {
            return;
        }

        app.clearFormErrors(form);
        button.disabled = true;
        button.textContent = "Saving...";

        try {
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
        } catch (error) {
            app.renderFormErrors(form, error);
        } finally {
            button.disabled = false;
            button.textContent = "Save Password";
        }
    }

    async function onForgotPassword(event) {
        event.preventDefault();
        const form = event.currentTarget;
        const button = form.querySelector("button");
        const panel = document.getElementById("reset-code-panel");
        const codeValue = document.getElementById("reset-code-value");
        const expiry = document.getElementById("reset-code-expiry");

        app.clearFormErrors(form);
        panel.classList.add("hidden");
        button.disabled = true;
        button.textContent = "Generating...";

        try {
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
        } catch (error) {
            app.renderFormErrors(form, error);
        } finally {
            button.disabled = false;
            button.textContent = "Generate Reset Code";
        }
    }

    async function onResetPassword(event) {
        event.preventDefault();
        const form = event.currentTarget;
        const button = form.querySelector("button");
        const payload = app.formToObject(form);

        if (!validatePasswordConfirmation(form, payload.newPassword, payload.confirmPassword, "confirmPassword")) {
            return;
        }

        app.clearFormErrors(form);
        button.disabled = true;
        button.textContent = "Resetting...";

        try {
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
        } catch (error) {
            app.renderFormErrors(form, error);
        } finally {
            button.disabled = false;
            button.textContent = "Reset Password";
        }
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
