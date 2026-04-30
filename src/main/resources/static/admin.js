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
        summary: null,
        selectedUserId: null,
        selectedSpecialistId: null,
        selectedCategoryId: null
    };

    const elements = {
        headerFullName: document.getElementById("header-full-name"),
        headerRole: document.getElementById("header-role"),
        metricUsers: document.getElementById("metric-users"),
        metricSpecialists: document.getElementById("metric-specialists"),
        metricCategories: document.getElementById("metric-categories"),
        metricBookings: document.getElementById("metric-bookings"),
        summaryGrid: document.getElementById("summary-grid"),
        userList: document.getElementById("user-list"),
        specialistList: document.getElementById("specialist-list"),
        categoryManagementList: document.getElementById("category-management-list"),
        userForm: document.getElementById("admin-user-edit-form"),
        createSpecialistForm: document.getElementById("admin-specialist-create-form"),
        specialistForm: document.getElementById("admin-specialist-edit-form"),
        createSpecialistCategory: document.getElementById("create-specialist-category"),
        specialistCategory: document.getElementById("edit-specialist-category"),
        categoryForm: document.getElementById("admin-category-form")
    };

    app.consumeFlash("toast");
    renderHeader();
    bindEvents();
    await refreshPortal();

    function bindEvents() {
        document.getElementById("logout-button").addEventListener("click", onLogout);
        document.getElementById("refresh-admin-data").addEventListener("click", refreshPortal);
        elements.userList.addEventListener("click", onSelectUser);
        elements.specialistList.addEventListener("click", onSelectSpecialist);
        elements.categoryManagementList.addEventListener("click", onSelectCategory);
        elements.userForm.addEventListener("submit", onSaveUser);
        elements.createSpecialistForm.addEventListener("submit", onCreateSpecialist);
        elements.specialistForm.addEventListener("submit", onSaveSpecialist);
        elements.categoryForm.addEventListener("submit", onSaveCategory);
        document.getElementById("new-category-button").addEventListener("click", onStartNewCategory);
    }

    function renderHeader() {
        elements.headerFullName.textContent = state.currentUser.fullName;
        elements.headerRole.textContent = state.currentUser.role;
        elements.headerRole.className = `role-pill ${state.currentUser.role}`;
    }

    async function refreshPortal() {
        try {
            await Promise.all([
                loadSummary(),
                loadCategories(),
                loadUsers(),
                loadSpecialists()
            ]);
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

        elements.metricUsers.textContent = String(state.summary.totalUsers || 0);
        elements.metricSpecialists.textContent = String(state.summary.totalSpecialists || 0);
        elements.metricCategories.textContent = String(state.categories.length);
        elements.metricBookings.textContent = String(state.summary.totalBookings || 0);

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
    }

    function renderCategories() {
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
        elements.metricCategories.textContent = String(state.categories.length);
    }

    function renderCategoryOptions() {
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
        const category = state.categories.find((item) => item.id === state.selectedCategoryId);

        if (!category) {
            document.getElementById("edit-category-id").value = "";
            document.getElementById("edit-category-name").value = "";
            document.getElementById("edit-category-description").value = "";
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
                        <span>Level: <strong>${profile.level}</strong></span>
                        <span>Base Fee: <strong>${app.formatCurrency(profile.baseFee)}</strong></span>
                    </div>
                </button>
            </article>
        `).join("");
    }

    function populateSpecialistForm() {
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

        document.getElementById("edit-specialist-id").value = String(profile.id);
        document.getElementById("edit-specialist-user-id").value = String(profile.userId);
        document.getElementById("edit-specialist-category").value = String(profile.categoryId);
        document.getElementById("edit-specialist-level").value = profile.level;
        document.getElementById("edit-specialist-base-fee").value = String(profile.baseFee);
        document.getElementById("edit-specialist-status").value = profile.status;
        document.getElementById("edit-specialist-bio").value = profile.bio || "";
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
            await loadSpecialists();
            await loadSummary();
        } catch (error) {
            app.showToast(error.message, "error", "toast");
        }
    }
});
