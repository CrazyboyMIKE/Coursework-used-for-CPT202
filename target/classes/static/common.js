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
            error.fieldErrors = data && typeof data === "object" && data.fieldErrors ? data.fieldErrors : {};
            throw error;
        }

        return data;
    }

    async function fetchCurrentUser() {
        return api("/api/users/me");
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

        toast.textContent = message;
        toast.className = `toast ${type || "success"}`;
        window.clearTimeout(showToast.timer);
        showToast.timer = window.setTimeout(() => {
            toast.className = "toast hidden";
        }, 2800);
    }

    function formToObject(form) {
        return Object.fromEntries(new FormData(form).entries());
    }

    function clearFormErrors(form) {
        if (!form) {
            return;
        }

        form.querySelectorAll(".input-error").forEach((field) => field.classList.remove("input-error"));
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
        }
    }

    function renderFormErrors(form, error) {
        clearFormErrors(form);
        const fieldErrors = error && error.fieldErrors ? error.fieldErrors : {};
        const entries = Object.entries(fieldErrors);

        entries.forEach(([fieldName, message]) => setFieldError(form, fieldName, message));

        if (entries.length > 0) {
            setFormError(form, "Please correct the highlighted fields and try again.");
            return true;
        }

        if (error && error.message) {
            setFormError(form, error.message);
        }
        return false;
    }

    function toApiDateTime(localValue) {
        return localValue ? `${localValue}:00` : "";
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

    function formatCurrency(value) {
        return new Intl.NumberFormat("en-US", {
            style: "currency",
            currency: "USD",
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
        getToken,
        queueFlash,
        redirectIfAuthenticated,
        renderFormErrors,
        requireAuth,
        setFieldError,
        setFormError,
        setToken,
        showToast,
        toApiDateTime,
        clearFormErrors,
        workspacePathForRole
    };
})();
