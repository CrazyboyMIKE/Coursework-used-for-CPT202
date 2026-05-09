(function () {
    const TOKEN_KEY = "bookingToken";
    const FLASH_KEY = "bookingFlash";

    function getToken() {
        return localStorage.getItem(TOKEN_KEY) || "";
    }

    function setToken(token) {
        localStorage.setItem(TOKEN_KEY, token);
    }

    function clearSession() {
        localStorage.removeItem(TOKEN_KEY);
    }

    async function api(url, options = {}) {
        const headers = { ...(options.headers || {}) };
        const token = getToken();

        if (token) {
            headers["X-Auth-Token"] = token;
        }

        if (options.body && !headers["Content-Type"]) {
            headers["Content-Type"] = "application/json";
        }

        const response = await fetch(url, { ...options, headers });

        if (response.status === 204) {
            return null;
        }

        const text = await response.text();
        let data = null;

        if (text) {
            try {
                data = JSON.parse(text);
            } catch (error) {
                data = text;
            }
        }

        if (!response.ok) {
            const message = data && typeof data === "object" ? data.message : null;
            const error = new Error(message || "Request failed");
            error.status = response.status;
            error.code = data && typeof data === "object" && data.code ? data.code : "";
            error.fieldErrors = data && typeof data === "object" && data.fieldErrors ? data.fieldErrors : {};
            error.payload = data && typeof data === "object" ? data : null;
            throw error;
        }

        return data;
    }

    async function fetchCurrentUser() {
        return api("/api/users/me");
    }

    async function logout() {
        try {
            await api("/api/auth/logout", { method: "POST" });
        } catch (error) {
            console.warn(error.message);
        } finally {
            clearSession();
            queueFlash("Signed out successfully.", "success");
            window.location.replace("/login.html");
        }
    }

    function workspacePathForRole(role) {
        return role === "ADMIN" ? "/admin.html" : "/dashboard.html";
    }

    async function redirectIfAuthenticated(target) {
        if (!getToken()) {
            return false;
        }

        try {
            const user = await fetchCurrentUser();
            window.location.replace(target || workspacePathForRole(user.role));
            return true;
        } catch (error) {
            clearSession();
            return false;
        }
    }

    async function requireAuth(target) {
        if (!getToken()) {
            queueFlash("Please sign in first.", "error");
            window.location.replace(target);
            return null;
        }

        try {
            return await fetchCurrentUser();
        } catch (error) {
            clearSession();
            if (error.status === 401 || error.status === 403) {
                queueFlash("Your session has expired. Please sign in again.", "error");
            } else {
                queueFlash(error.message || "Unable to load your account. Please sign in again.", "error");
            }
            window.location.replace(target);
            return null;
        }
    }

    function queueFlash(message, type) {
        sessionStorage.setItem(FLASH_KEY, JSON.stringify({ message, type }));
    }

    function consumeFlash(containerId) {
        const raw = sessionStorage.getItem(FLASH_KEY);
        if (!raw) {
            return;
        }

        sessionStorage.removeItem(FLASH_KEY);

        try {
            const flash = JSON.parse(raw);
            showToast(flash.message, flash.type || "success", containerId);
        } catch (error) {
            showToast(raw, "success", containerId);
        }
    }

    function showToast(message, type, containerId) {
        const toast = document.getElementById(containerId || "toast");
        if (!toast) {
            return;
        }

        toast.setAttribute("role", "status");
        toast.setAttribute("aria-live", "polite");
        toast.textContent = message;
        toast.className = `toast ${type || "success"}`;
        window.clearTimeout(showToast.timer);
        showToast.timer = window.setTimeout(() => {
            toast.className = "toast hidden";
        }, 2800);
    }

    function setButtonLoading(button, loading, loadingText) {
        if (!button) {
            return;
        }

        if (loading) {
            if (!button.dataset.defaultLabel) {
                button.dataset.defaultLabel = button.textContent;
            }
            button.textContent = loadingText || "Working...";
            button.disabled = true;
            button.setAttribute("aria-busy", "true");
            return;
        }

        button.textContent = button.dataset.defaultLabel || button.textContent;
        button.disabled = false;
        button.removeAttribute("aria-busy");
    }

    async function withButtonLoading(button, loadingText, task) {
        if (button && button.disabled) {
            return null;
        }

        setButtonLoading(button, true, loadingText);
        try {
            return await task();
        } finally {
            setButtonLoading(button, false);
        }
    }

    async function withFormLoading(form, loadingText, task) {
        const button = form ? form.querySelector('button[type="submit"]') : null;
        return withButtonLoading(button, loadingText, task);
    }

    function showFormSuccess(form, message) {
        if (!form) {
            showToast(message, "success", "toast");
            return;
        }

        let notice = form.querySelector("[data-form-success]");
        if (!notice) {
            notice = document.createElement("div");
            notice.className = "form-success";
            notice.setAttribute("data-form-success", "");
            notice.setAttribute("role", "status");
            const heading = form.querySelector("h3, h2");
            if (heading && heading.nextSibling) {
                form.insertBefore(notice, heading.nextSibling);
            } else {
                form.prepend(notice);
            }
        }

        notice.textContent = message;
        notice.classList.add("visible");
        window.clearTimeout(showFormSuccess.timer);
        showFormSuccess.timer = window.setTimeout(() => {
            notice.classList.remove("visible");
        }, 4200);
        showToast(message, "success", "toast");
    }

    function confirmAction(options = {}) {
        return new Promise((resolve) => {
            const previous = document.querySelector(".modal-backdrop");
            if (previous) {
                previous.remove();
            }

            const backdrop = document.createElement("div");
            backdrop.className = "modal-backdrop";
            backdrop.innerHTML = `
                <section class="modal-card" role="dialog" aria-modal="true" aria-labelledby="confirm-title">
                    <div class="modal-head">
                        <div>
                            <p class="eyebrow">${escapeHtml(options.eyebrow || "Confirm Action")}</p>
                            <h2 id="confirm-title">${escapeHtml(options.title || "Confirm this action")}</h2>
                        </div>
                    </div>
                    <p class="modal-copy">${escapeHtml(options.message || "Please confirm before continuing.")}</p>
                    ${options.reasonLabel ? `
                        <label class="modal-reason">
                            <span>${escapeHtml(options.reasonLabel)}</span>
                            <textarea rows="3" maxlength="255" placeholder="${escapeHtml(options.reasonPlaceholder || "")}"></textarea>
                        </label>
                    ` : ""}
                    <div class="modal-actions">
                        <button class="${options.danger ? "danger-button" : ""}" type="button" data-confirm-action="confirm">${escapeHtml(options.confirmLabel || "Confirm")}</button>
                        <button class="ghost-button" type="button" data-confirm-action="cancel">${escapeHtml(options.cancelLabel || "Cancel")}</button>
                    </div>
                </section>
            `;

            function close(result) {
                document.removeEventListener("keydown", onKeyDown);
                backdrop.remove();
                resolve(result);
            }

            function onKeyDown(event) {
                if (event.key === "Escape") {
                    close({ confirmed: false, reason: "" });
                }
            }

            backdrop.addEventListener("click", (event) => {
                if (event.target === backdrop) {
                    close({ confirmed: false, reason: "" });
                    return;
                }

                const button = event.target.closest("[data-confirm-action]");
                if (!button) {
                    return;
                }

                if (button.dataset.confirmAction === "cancel") {
                    close({ confirmed: false, reason: "" });
                    return;
                }

                const input = backdrop.querySelector("textarea");
                close({ confirmed: true, reason: input ? input.value.trim() : "" });
            });

            document.body.appendChild(backdrop);
            document.addEventListener("keydown", onKeyDown);
            const focusTarget = backdrop.querySelector("textarea") || backdrop.querySelector("[data-confirm-action='confirm']");
            focusTarget?.focus();
        });
    }

    function formToObject(form) {
        return Object.fromEntries(new FormData(form).entries());
    }

    function clearFormErrors(form) {
        if (!form) {
            return;
        }

        form.querySelectorAll(".input-error").forEach((field) => {
            field.classList.remove("input-error");
            field.removeAttribute("aria-invalid");
        });
        form.querySelectorAll("[data-error-for]").forEach((field) => {
            field.textContent = "";
            field.classList.remove("visible");
        });

        const summary = form.querySelector("[data-form-error]");
        if (summary) {
            summary.textContent = "";
            summary.classList.add("hidden");
        }
    }

    function setFieldError(form, fieldName, message) {
        const input = form.querySelector(`[name="${fieldName}"]`);
        if (input) {
            input.classList.add("input-error");
            input.setAttribute("aria-invalid", "true");
        }

        const target = form.querySelector(`[data-error-for="${fieldName}"]`);
        if (target) {
            target.textContent = message;
            target.classList.add("visible");
        }
    }

    function setFormError(form, message) {
        const summary = form.querySelector("[data-form-error]");
        if (summary) {
            summary.textContent = message;
            summary.classList.remove("hidden");
            summary.setAttribute("role", "alert");
        }
    }

    function friendlyErrorMessage(error, fallback) {
        if (error && error.message) {
            return error.message;
        }

        if (error && error.status === 401) {
            return "Please sign in again before continuing.";
        }

        if (error && error.status === 403) {
            return "You do not have permission to complete this action.";
        }

        if (error && error.status >= 500) {
            return "The server could not complete the request. Please try again later.";
        }

        return fallback || "Unable to complete the request. Please review the details and try again.";
    }

    function renderFormErrors(form, error, fallback) {
        clearFormErrors(form);
        const fieldErrors = error && error.fieldErrors ? error.fieldErrors : {};
        const entries = Object.entries(fieldErrors);

        entries.forEach(([fieldName, message]) => setFieldError(form, fieldName, message));

        if (entries.length > 0) {
            setFormError(form, "Please correct the highlighted fields and try again.");
            return true;
        }

        setFormError(form, friendlyErrorMessage(error, fallback));
        return false;
    }

    function toApiDateTime(localValue) {
        return localValue ? `${localValue}:00` : "";
    }

    function buildQueryUrl(baseUrl, params = {}) {
        const query = new URLSearchParams();
        Object.entries(params).forEach(([key, value]) => {
            if (value !== undefined && value !== null && value !== "") {
                query.set(key, value);
            }
        });
        return query.toString() ? `${baseUrl}?${query.toString()}` : baseUrl;
    }

    function escapeHtml(value) {
        return String(value)
                .replaceAll("&", "&amp;")
                .replaceAll("<", "&lt;")
                .replaceAll(">", "&gt;")
                .replaceAll("\"", "&quot;")
                .replaceAll("'", "&#39;");
    }

    function formatDateTime(value) {
        return new Intl.DateTimeFormat("en-US", {
            year: "numeric",
            month: "2-digit",
            day: "2-digit",
            hour: "2-digit",
            minute: "2-digit"
        }).format(new Date(value));
    }

    function formatCurrency(value, currency = "USD") {
        return new Intl.NumberFormat("en-US", {
            style: "currency",
            currency: currency || "USD",
            minimumFractionDigits: 2
        }).format(Number(value || 0));
    }

    window.ProjectApp = {
        api,
        clearSession,
        consumeFlash,
        escapeHtml,
        fetchCurrentUser,
        formToObject,
        formatCurrency,
        formatDateTime,
        friendlyErrorMessage,
        getToken,
        logout,
        queueFlash,
        redirectIfAuthenticated,
        renderFormErrors,
        requireAuth,
        confirmAction,
        setFieldError,
        setFormError,
        setButtonLoading,
        setToken,
        showFormSuccess,
        showToast,
        withButtonLoading,
        withFormLoading,
        buildQueryUrl,
        toApiDateTime,
        clearFormErrors,
        workspacePathForRole
    };
})();
