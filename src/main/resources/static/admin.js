document.addEventListener("DOMContentLoaded", async () => {
    const app = window.ProjectApp;
    const currentUser = await app.requireAuth("/login.html");

    if (!currentUser) {
        return;
    }

    if (currentUser.role !== "ADMIN") {
        app.queueFlash("Administrator access is required for the admin portal.", "error");
        window.location.replace(app.workspacePathForRole(currentUser.role));
        return;
    }

    const state = {
        currentUser,
        users: [],
        specialists: [],
        categories: [],
        bookings: [],
        summary: null,
        selectedUserId: null,
        selectedSpecialistId: null,
        selectedCategoryId: null,
        selectedBookingStatus: "ALL"
    };

    const elements = {
        logoutButton: document.getElementById("logout-button"),
        refreshButton: document.getElementById("refresh-admin-data"),
        headerFullName: document.getElementById("header-full-name"),
        headerRole: document.getElementById("header-role"),
        metricUsers: document.getElementById("metric-users"),
        metricSpecialists: document.getElementById("metric-specialists"),
        metricCategories: document.getElementById("metric-categories"),
        metricBookings: document.getElementById("metric-bookings"),
        summaryGrid: document.getElementById("summary-grid"),
        userList: document.getElementById("user-list"),
        userForm: document.getElementById("admin-user-edit-form"),
        specialistList: document.getElementById("specialist-list"),
        createSpecialistForm: document.getElementById("admin-specialist-create-form"),
        specialistForm: document.getElementById("admin-specialist-edit-form"),
        createSpecialistCategory: document.getElementById("create-specialist-category"),
        specialistCategory: document.getElementById("edit-specialist-category"),
        categoryManagementList: document.getElementById("category-management-list"),
        categoryForm: document.getElementById("admin-category-form"),
        newCategoryButton: document.getElementById("new-category-button"),
        bookingList: document.getElementById("admin-booking-list"),
        bookingFilterButtons: Array.from(document.querySelectorAll("[data-admin-booking-filter]"))
    };

    const views = {
        summary: Boolean(elements.summaryGrid || elements.metricUsers),
        users: Boolean(elements.userList && elements.userForm),
        specialists: Boolean(elements.specialistList && elements.createSpecialistForm && elements.specialistForm),
        categories: Boolean(elements.categoryManagementList && elements.categoryForm),
        bookings: Boolean(elements.bookingList)
    };

    app.consumeFlash("toast");
    renderHeader();
    bindEvents();
    await refreshCurrentPage();

    function bindEvents() {
        elements.logoutButton?.addEventListener("click", onLogout);
        elements.refreshButton?.addEventListener("click", refreshCurrentPage);
        elements.userList?.addEventListener("click", onSelectUser);
        elements.specialistList?.addEventListener("click", onSelectSpecialist);
        elements.categoryManagementList?.addEventListener("click", onSelectCategory);
        elements.bookingList?.addEventListener("click", onBookingAction);
        elements.userForm?.addEventListener("submit", onSaveUser);
        elements.createSpecialistForm?.addEventListener("submit", onCreateSpecialist);
        elements.specialistForm?.addEventListener("submit", onSaveSpecialist);
        elements.categoryForm?.addEventListener("submit", onSaveCategory);
        elements.newCategoryButton?.addEventListener("click", onStartNewCategory);
        elements.bookingFilterButtons.forEach((button) => button.addEventListener("click", onSelectBookingFilter));
    }

    function renderHeader() {
        elements.headerFullName.textContent = state.currentUser.fullName;
        elements.headerRole.textContent = state.currentUser.role;
        elements.headerRole.className = `role-pill ${state.currentUser.role}`;
    }

    async function refreshCurrentPage() {
        try {
            if (views.summary || views.specialists || views.categories) {
                await loadCategories();
            }

            if (views.summary) {
                await loadSummary();
            }

            if (views.users) {
                await loadUsers();
            }

            if (views.specialists) {
                await loadSpecialists();
            }

            if (views.bookings) {
                await loadBookings();
            }
        } catch (error) {
            app.showToast(error.message, "error", "toast");
        }
    }

    async function onLogout() {
        try {
            await app.api("/api/auth/logout", { method: "POST" });
        } catch (error) {
            console.warn(error.message);
        } finally {
            app.clearSession();
            app.queueFlash("Signed out successfully.", "success");
            window.location.replace("/login.html");
        }
    }

    async function loadSummary() {
        state.summary = await app.api("/api/reports/summary");
        renderSummary();
    }

    function renderSummary() {
        if (!state.summary) {
            return;
        }

        if (elements.metricUsers) {
            elements.metricUsers.textContent = String(state.summary.totalUsers || 0);
        }
        if (elements.metricSpecialists) {
            elements.metricSpecialists.textContent = String(state.summary.totalSpecialists || 0);
        }
        if (elements.metricCategories) {
            elements.metricCategories.textContent = String(state.categories.length);
        }
        if (elements.metricBookings) {
            elements.metricBookings.textContent = String(state.summary.totalBookings || 0);
        }

        if (!elements.summaryGrid) {
            return;
        }

        const entries = [
            ["Total Users", state.summary.totalUsers],
            ["Total Specialists", state.summary.totalSpecialists],
            ["Available Time Slots", state.summary.totalAvailableSlots],
            ["Total Bookings", state.summary.totalBookings],
            ["Confirmed Revenue", app.formatCurrency(state.summary.confirmedRevenue)]
        ];

        Object.entries(state.summary.bookingsByStatus || {}).forEach(([status, value]) => {
            entries.push([`Status ${status}`, value]);
        });

        elements.summaryGrid.innerHTML = entries.map(([label, value]) => `
            <article class="stat-card">
                <span>${app.escapeHtml(label)}</span>
                <strong>${app.escapeHtml(String(value))}</strong>
            </article>
        `).join("");
    }

    async function loadCategories() {
        state.categories = await app.api("/api/categories");

        if (!state.categories.some((category) => category.id === state.selectedCategoryId)) {
            state.selectedCategoryId = state.categories.length ? state.categories[0].id : null;
        }

        renderCategories();
        renderCategoryOptions();
        populateCategoryForm();
        renderSummary();
    }

    function renderCategories() {
        if (elements.metricCategories) {
            elements.metricCategories.textContent = String(state.categories.length);
        }

        if (!views.categories) {
            return;
        }

        if (!state.categories.length) {
            elements.categoryManagementList.innerHTML = '<div class="empty-state">No categories are configured yet</div>';
            return;
        }

        elements.categoryManagementList.innerHTML = state.categories.map((category) => `
            <article class="management-card ${state.selectedCategoryId === category.id ? "active" : ""}">
                <button type="button" class="management-select" data-category-id="${category.id}">
                    <div class="card-head">
                        <div>
                            <h3>${app.escapeHtml(category.name)}</h3>
                            <p>${app.escapeHtml(category.description || "No description available")}</p>
                        </div>
                        <span class="status-pill ${category.active ? "ACTIVE" : "INACTIVE"}">${category.active ? "ACTIVE" : "INACTIVE"}</span>
                    </div>
                    <div class="meta-block">
                        <span>Category ID: <strong>${category.id}</strong></span>
                    </div>
                </button>
            </article>
        `).join("");
    }

    function renderCategoryOptions() {
        if (!views.specialists) {
            return;
        }

        if (!state.categories.length) {
            elements.createSpecialistCategory.innerHTML = '<option value="">No categories available</option>';
            elements.specialistCategory.innerHTML = '<option value="">No categories available</option>';
            return;
        }

        const options = state.categories.map((category) =>
            `<option value="${category.id}">${app.escapeHtml(category.name)}</option>`
        ).join("");

        elements.createSpecialistCategory.innerHTML = options;
        elements.specialistCategory.innerHTML = options;
        populateSpecialistForm();
    }

    function populateCategoryForm() {
        if (!views.categories) {
            return;
        }

        const category = state.categories.find((item) => item.id === state.selectedCategoryId);

        if (!category) {
            elements.categoryForm.reset();
            document.getElementById("edit-category-id").value = "";
            document.getElementById("edit-category-active").value = "true";
            return;
        }

        document.getElementById("edit-category-id").value = String(category.id);
        document.getElementById("edit-category-name").value = category.name;
        document.getElementById("edit-category-description").value = category.description || "";
        document.getElementById("edit-category-active").value = String(category.active);
    }

    async function loadUsers() {
        state.users = await app.api("/api/users");

        if (!state.users.some((user) => user.id === state.selectedUserId)) {
            state.selectedUserId = state.users.length ? state.users[0].id : null;
        }

        renderUsers();
        populateUserForm();
    }

    function renderUsers() {
        if (!views.users) {
            return;
        }

        if (!state.users.length) {
            elements.userList.innerHTML = '<div class="empty-state">No user accounts found</div>';
            return;
        }

        elements.userList.innerHTML = state.users.map((user) => `
            <article class="management-card ${state.selectedUserId === user.id ? "active" : ""}">
                <button type="button" class="management-select" data-user-id="${user.id}">
                    <div class="card-head">
                        <div>
                            <h3>${app.escapeHtml(user.fullName)}</h3>
                            <p>@${app.escapeHtml(user.username)}</p>
                        </div>
                        <span class="status-pill ${user.active ? "ACTIVE" : "INACTIVE"}">${user.active ? "ACTIVE" : "INACTIVE"}</span>
                    </div>
                    <div class="meta-block">
                        <span>Role: <strong>${user.role}</strong></span>
                        <span>Email: <strong>${app.escapeHtml(user.email)}</strong></span>
                        <span>Phone: <strong>${app.escapeHtml(user.phone || "-")}</strong></span>
                    </div>
                </button>
            </article>
        `).join("");
    }

    function populateUserForm() {
        if (!views.users) {
            return;
        }

        const user = state.users.find((item) => item.id === state.selectedUserId);
        const disabled = !user;

        Array.from(elements.userForm.elements).forEach((field) => {
            if (field.name !== "") {
                field.disabled = disabled;
            }
        });

        if (!user) {
            elements.userForm.reset();
            return;
        }

        document.getElementById("edit-user-id").value = String(user.id);
        document.getElementById("edit-user-username").value = user.username;
        document.getElementById("edit-user-full-name").value = user.fullName;
        document.getElementById("edit-user-email").value = user.email;
        document.getElementById("edit-user-phone").value = user.phone || "";
        document.getElementById("edit-user-role").value = user.role;
        document.getElementById("edit-user-active").value = String(user.active);
    }

    async function loadSpecialists() {
        state.specialists = await app.api("/api/specialists/manage");

        if (!state.specialists.some((profile) => profile.id === state.selectedSpecialistId)) {
            state.selectedSpecialistId = state.specialists.length ? state.specialists[0].id : null;
        }

        renderSpecialists();
        populateSpecialistForm();
    }

    function renderSpecialists() {
        if (!views.specialists) {
            return;
        }

        if (!state.specialists.length) {
            elements.specialistList.innerHTML = '<div class="empty-state">No specialist profiles found</div>';
            return;
        }

        elements.specialistList.innerHTML = state.specialists.map((profile) => `
            <article class="management-card ${state.selectedSpecialistId === profile.id ? "active" : ""}">
                <button type="button" class="management-select" data-specialist-id="${profile.id}">
                    <div class="card-head">
                        <div>
                            <h3>${app.escapeHtml(profile.fullName)}</h3>
                            <p>${app.escapeHtml(profile.categoryName)}</p>
                        </div>
                        <span class="status-pill ${profile.status}">${profile.status}</span>
                    </div>
                    <div class="meta-block">
                        <span>Profile ID: <strong>${profile.id}</strong></span>
                        <span>User ID: <strong>${profile.userId}</strong></span>
                        <span>Professional Title / Certification: <strong>${app.escapeHtml(profile.level)}</strong></span>
                        <span>Base Fee: <strong>${app.formatCurrency(profile.baseFee)}</strong></span>
                    </div>
                </button>
            </article>
        `).join("");
    }

    function populateSpecialistForm() {
        if (!views.specialists) {
            return;
        }

        const profile = state.specialists.find((item) => item.id === state.selectedSpecialistId);
        const disabled = !profile;

        Array.from(elements.specialistForm.elements).forEach((field) => {
            if (field.name !== "") {
                field.disabled = disabled;
            }
        });

        if (!profile) {
            elements.specialistForm.reset();
            return;
        }

        document.getElementById("edit-specialist-full-name").value = profile.fullName || "";
        document.getElementById("edit-specialist-id").value = String(profile.id);
        document.getElementById("edit-specialist-user-id").value = String(profile.userId);
        document.getElementById("edit-specialist-category").value = String(profile.categoryId);
        document.getElementById("edit-specialist-level").value = profile.level;
        document.getElementById("edit-specialist-base-fee").value = String(profile.baseFee);
        document.getElementById("edit-specialist-fee-currency").value = "USD";
        document.getElementById("edit-specialist-status").value = profile.status;
        document.getElementById("edit-specialist-bio").value = profile.bio || "";
    }

    async function loadBookings() {
        const query = state.selectedBookingStatus === "ALL"
            ? ""
            : `?status=${encodeURIComponent(state.selectedBookingStatus)}`;
        state.bookings = await app.api(`/api/bookings/manage${query}`);
        renderBookings();
    }

    function renderBookings() {
        if (!views.bookings) {
            return;
        }

        elements.bookingFilterButtons.forEach((button) => {
            const active = button.dataset.adminBookingFilter === state.selectedBookingStatus;
            button.classList.toggle("active", active);
            if (active) {
                button.setAttribute("aria-current", "page");
            } else {
                button.removeAttribute("aria-current");
            }
        });

        if (!state.bookings.length) {
            elements.bookingList.innerHTML = '<div class="empty-state">No bookings match the current filter</div>';
            return;
        }

        elements.bookingList.innerHTML = state.bookings.map((booking) => `
            <article class="management-card booking-admin-card">
                <div class="card-head">
                    <div>
                        <h3>${app.escapeHtml(booking.topic)}</h3>
                        <p>${app.escapeHtml(booking.customerName)} with ${app.escapeHtml(booking.specialistName)}</p>
                    </div>
                    <span class="status-pill ${booking.status}">${booking.status}</span>
                </div>
                <div class="booking-admin-meta">
                    <span>Booking ID: <strong>${booking.id}</strong></span>
                    <span>Customer ID: <strong>${booking.customerId}</strong></span>
                    <span>Specialist ID: <strong>${booking.specialistId}</strong></span>
                    <span>Slot ID: <strong>${booking.slotId}</strong></span>
                    <span>Start Time: <strong>${app.formatDateTime(booking.startTime)}</strong></span>
                    <span>End Time: <strong>${app.formatDateTime(booking.endTime)}</strong></span>
                    <span>Price: <strong>${app.formatCurrency(booking.price)}</strong></span>
                </div>
                <div class="notice-card booking-admin-note-block">
                    <strong>Notes</strong>
                    <p>${app.escapeHtml(booking.notes || "No customer notes were provided.")}</p>
                    <strong>Last Action</strong>
                    <p>${app.escapeHtml(booking.lastActionReason || "No workflow action has been recorded yet.")}</p>
                </div>
                ${renderBookingActions(booking)}
            </article>
        `).join("");
    }

    function renderBookingActions(booking) {
        const actions = [];

        if (booking.status === "PENDING") {
            actions.push('<button type="button" data-booking-action="confirm">Confirm</button>');
            actions.push('<button class="ghost-button" type="button" data-booking-action="reject">Reject</button>');
            actions.push('<button class="ghost-button" type="button" data-booking-action="cancel">Cancel</button>');
        } else if (booking.status === "CONFIRMED") {
            actions.push('<button type="button" data-booking-action="complete">Mark Completed</button>');
            actions.push('<button class="ghost-button" type="button" data-booking-action="cancel">Cancel</button>');
        }

        if (!actions.length) {
            return '<div class="booking-admin-actions booking-admin-actions-empty"><span class="muted-label">No administrative actions are available for this booking.</span></div>';
        }

        return `
            <div class="booking-admin-actions" data-booking-id="${booking.id}">
                ${actions.join("")}
            </div>
        `;
    }

    function onSelectUser(event) {
        const button = event.target.closest("[data-user-id]");
        if (!button) {
            return;
        }

        state.selectedUserId = Number(button.dataset.userId);
        renderUsers();
        populateUserForm();
    }

    function onSelectSpecialist(event) {
        const button = event.target.closest("[data-specialist-id]");
        if (!button) {
            return;
        }

        state.selectedSpecialistId = Number(button.dataset.specialistId);
        renderSpecialists();
        populateSpecialistForm();
    }

    function onSelectCategory(event) {
        const button = event.target.closest("[data-category-id]");
        if (!button) {
            return;
        }

        state.selectedCategoryId = Number(button.dataset.categoryId);
        renderCategories();
        populateCategoryForm();
    }

    function onSelectBookingFilter(event) {
        const button = event.currentTarget;
        state.selectedBookingStatus = button.dataset.adminBookingFilter || "ALL";
        loadBookings().catch((error) => app.showToast(error.message, "error", "toast"));
    }

    function onStartNewCategory() {
        state.selectedCategoryId = null;
        renderCategories();
        populateCategoryForm();
    }

    async function onSaveUser(event) {
        event.preventDefault();
        const userId = Number(document.getElementById("edit-user-id").value);
        const payload = app.formToObject(event.currentTarget);
        payload.active = payload.active === "true";

        try {
            const updated = await app.api(`/api/users/${userId}`, {
                method: "PUT",
                body: JSON.stringify(payload)
            });
            state.selectedUserId = updated.id;
            app.showToast("User account updated successfully.", "success", "toast");
            await loadUsers();

            if (updated.id === state.currentUser.id) {
                try {
                    state.currentUser = await app.fetchCurrentUser();
                    if (state.currentUser.role !== "ADMIN") {
                        app.queueFlash("Your account role changed. Returning to the role workspace.", "success");
                        window.location.replace(app.workspacePathForRole(state.currentUser.role));
                        return;
                    }
                    renderHeader();
                } catch (error) {
                    app.clearSession();
                    app.queueFlash(error.message || "The current administrator session is no longer valid.", "error");
                    window.location.replace("/login.html");
                    return;
                }
            }
        } catch (error) {
            app.showToast(error.message, "error", "toast");
        }
    }

    async function onCreateSpecialist(event) {
        event.preventDefault();
        const form = event.currentTarget;
        const payload = app.formToObject(form);
        payload.userId = Number(payload.userId);
        payload.categoryId = Number(payload.categoryId);
        payload.baseFee = Number(payload.baseFee);

        app.clearFormErrors(form);

        try {
            const created = await app.api("/api/specialists", {
                method: "POST",
                body: JSON.stringify(payload)
            });
            form.reset();
            app.clearFormErrors(form);
            state.selectedSpecialistId = created.id;
            app.showToast("Specialist profile created successfully.", "success", "toast");
            await loadSpecialists();
        } catch (error) {
            app.renderFormErrors(form, error);
        }
    }

    async function onSaveSpecialist(event) {
        event.preventDefault();
        const specialistId = Number(document.getElementById("edit-specialist-id").value);
        const payload = app.formToObject(event.currentTarget);
        payload.categoryId = Number(payload.categoryId);
        payload.baseFee = Number(payload.baseFee);

        try {
            const updated = await app.api(`/api/specialists/${specialistId}`, {
                method: "PUT",
                body: JSON.stringify(payload)
            });
            state.selectedSpecialistId = updated.id;
            app.showToast("Specialist profile updated successfully.", "success", "toast");
            await loadSpecialists();
        } catch (error) {
            app.showToast(error.message, "error", "toast");
        }
    }

    async function onSaveCategory(event) {
        event.preventDefault();
        const payload = app.formToObject(event.currentTarget);
        payload.active = payload.active === "true";

        try {
            let saved;
            if (state.selectedCategoryId) {
                saved = await app.api(`/api/categories/${state.selectedCategoryId}`, {
                    method: "PUT",
                    body: JSON.stringify(payload)
                });
            } else {
                saved = await app.api("/api/categories", {
                    method: "POST",
                    body: JSON.stringify(payload)
                });
            }
            state.selectedCategoryId = saved.id;
            app.showToast("Category saved successfully.", "success", "toast");
            await loadCategories();
            if (views.specialists) {
                await loadSpecialists();
            }
        } catch (error) {
            app.showToast(error.message, "error", "toast");
        }
    }

    async function onBookingAction(event) {
        const button = event.target.closest("[data-booking-action]");
        if (!button) {
            return;
        }

        const action = button.dataset.bookingAction;
        const actionContainer = button.closest("[data-booking-id]");
        const bookingId = actionContainer ? Number(actionContainer.dataset.bookingId) : NaN;

        if (!bookingId) {
            return;
        }

        let endpoint = "";
        let body;
        let successMessage = "";

        if (action === "confirm") {
            endpoint = `/api/bookings/${bookingId}/confirm`;
            successMessage = "Booking confirmed successfully.";
        } else if (action === "complete") {
            endpoint = `/api/bookings/${bookingId}/complete`;
            successMessage = "Booking marked as completed.";
        } else if (action === "reject") {
            const reason = window.prompt("Enter a rejection reason (optional):", "");
            if (reason === null) {
                return;
            }
            endpoint = `/api/bookings/${bookingId}/reject`;
            body = JSON.stringify({ reason });
            successMessage = "Booking rejected successfully.";
        } else if (action === "cancel") {
            const reason = window.prompt("Enter a cancellation reason (optional):", "");
            if (reason === null) {
                return;
            }
            endpoint = `/api/bookings/${bookingId}/cancel`;
            body = JSON.stringify({ reason });
            successMessage = "Booking cancelled successfully.";
        } else {
            return;
        }

        try {
            await app.api(endpoint, body ? { method: "POST", body } : { method: "POST" });
            app.showToast(successMessage, "success", "toast");
            await loadBookings();
            if (views.summary) {
                await loadSummary();
            }
        } catch (error) {
            app.showToast(error.message, "error", "toast");
        }
    }
});
