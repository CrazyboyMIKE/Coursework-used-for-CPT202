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
        creatingCategory: false,
        selectedBookingStatus: "ALL",
        userPage: createPageState(),
        specialistPage: createPageState(),
        bookingPage: createPageState(),
        categoryPage: createPageState()
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
        bookingFilterButtons: Array.from(document.querySelectorAll("[data-admin-booking-filter]")),
        adminSearchForms: Array.from(document.querySelectorAll("[data-admin-search]")),
        userSearch: document.getElementById("user-search"),
        specialistSearch: document.getElementById("specialist-search"),
        bookingSearch: document.getElementById("booking-search"),
        categorySearch: document.getElementById("category-search"),
        userPagination: document.getElementById("user-pagination"),
        specialistPagination: document.getElementById("specialist-pagination"),
        bookingPagination: document.getElementById("booking-pagination"),
        categoryPagination: document.getElementById("category-pagination")
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
        elements.refreshButton?.addEventListener("click", (event) =>
                app.withButtonLoading(event.currentTarget, "Refreshing...", refreshCurrentPage)
        );
        elements.userList?.addEventListener("click", onSelectUser);
        elements.specialistList?.addEventListener("click", onSelectSpecialist);
        elements.categoryManagementList?.addEventListener("click", onSelectCategory);
        elements.bookingList?.addEventListener("click", onBookingAction);
        elements.userPagination?.addEventListener("click", onPaginationAction);
        elements.specialistPagination?.addEventListener("click", onPaginationAction);
        elements.bookingPagination?.addEventListener("click", onPaginationAction);
        elements.categoryPagination?.addEventListener("click", onPaginationAction);
        elements.adminSearchForms.forEach((form) => form.addEventListener("submit", onAdminSearch));
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

    function createPageState() {
        return {
            page: 0,
            size: 8,
            keyword: "",
            totalElements: 0,
            totalPages: 0,
            first: true,
            last: true
        };
    }

    function adminPageParams(pageState) {
        const params = new URLSearchParams();
        params.set("page", String(pageState.page));
        params.set("size", String(pageState.size));
        if (pageState.keyword) {
            params.set("keyword", pageState.keyword);
        }
        return params;
    }

    function normalizePageResponse(response, pageState) {
        if (Array.isArray(response)) {
            return {
                content: response,
                page: 0,
                size: response.length || pageState.size,
                totalElements: response.length,
                totalPages: response.length ? 1 : 0,
                first: true,
                last: true
            };
        }
        return response;
    }

    function applyPageMeta(pageState, page) {
        pageState.page = page.page || 0;
        pageState.size = page.size || pageState.size;
        pageState.totalElements = page.totalElements || 0;
        pageState.totalPages = page.totalPages || 0;
        pageState.first = Boolean(page.first);
        pageState.last = Boolean(page.last);
    }

    function renderLoading(target, message) {
        if (target) {
            target.innerHTML = `<div class="empty-state loading-state">${app.escapeHtml(message)}</div>`;
        }
    }

    function renderPagination(scope, pageState, target) {
        if (!target) {
            return;
        }

        const total = pageState.totalElements || 0;
        const totalPages = Math.max(pageState.totalPages || 0, total ? 1 : 0);

        if (!total) {
            target.innerHTML = "";
            return;
        }

        const currentPage = pageState.page || 0;
        const start = currentPage * pageState.size + 1;
        const end = Math.min(total, (currentPage + 1) * pageState.size);
        target.innerHTML = `
            <span>Showing ${start}-${end} of ${total}</span>
            <div class="pagination-actions">
                <button class="ghost-button" type="button" data-page-scope="${scope}" data-page-target="${currentPage - 1}" ${pageState.first ? "disabled" : ""}>Previous</button>
                <strong>Page ${currentPage + 1} / ${totalPages}</strong>
                <button class="ghost-button" type="button" data-page-scope="${scope}" data-page-target="${currentPage + 1}" ${pageState.last ? "disabled" : ""}>Next</button>
            </div>
        `;
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
        await app.logout();
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

        if (!state.creatingCategory && !state.categories.some((category) => category.id === state.selectedCategoryId)) {
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

        const filteredCategories = state.categories.filter((category) => matchesCategorySearch(category));
        const totalPages = Math.ceil(filteredCategories.length / state.categoryPage.size);
        if (state.categoryPage.page >= totalPages) {
            state.categoryPage.page = Math.max(totalPages - 1, 0);
        }

        const start = state.categoryPage.page * state.categoryPage.size;
        const categories = filteredCategories.slice(start, start + state.categoryPage.size);
        state.categoryPage.totalElements = filteredCategories.length;
        state.categoryPage.totalPages = totalPages;
        state.categoryPage.first = state.categoryPage.page === 0;
        state.categoryPage.last = state.categoryPage.page >= Math.max(totalPages - 1, 0);

        if (!state.creatingCategory && !categories.some((category) => category.id === state.selectedCategoryId)) {
            state.selectedCategoryId = categories.length ? categories[0].id : null;
        }

        if (!filteredCategories.length) {
            elements.categoryManagementList.innerHTML = state.categoryPage.keyword
                    ? '<div class="empty-state">No categories match the current search</div>'
                    : '<div class="empty-state">No categories are configured yet</div>';
            renderPagination("categories", state.categoryPage, elements.categoryPagination);
            return;
        }

        elements.categoryManagementList.innerHTML = categories.map((category) => `
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
        renderPagination("categories", state.categoryPage, elements.categoryPagination);
    }

    function matchesCategorySearch(category) {
        const keyword = state.categoryPage.keyword;
        if (!keyword) {
            return true;
        }

        return [category.name, category.description, category.active ? "active" : "inactive"]
                .filter(Boolean)
                .some((value) => String(value).toLowerCase().includes(keyword));
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
        renderLoading(elements.userList, "Loading user accounts...");
        const params = adminPageParams(state.userPage);
        const page = normalizePageResponse(await app.api(`/api/users?${params.toString()}`), state.userPage);
        state.users = page.content || [];
        applyPageMeta(state.userPage, page);

        if (!state.users.some((user) => user.id === state.selectedUserId)) {
            state.selectedUserId = state.users.length ? state.users[0].id : null;
        }

        renderUsers();
        renderPagination("users", state.userPage, elements.userPagination);
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
        renderLoading(elements.specialistList, "Loading specialist profiles...");
        const params = adminPageParams(state.specialistPage);
        const page = normalizePageResponse(await app.api(`/api/specialists/manage?${params.toString()}`), state.specialistPage);
        state.specialists = page.content || [];
        applyPageMeta(state.specialistPage, page);

        if (!state.specialists.some((profile) => profile.id === state.selectedSpecialistId)) {
            state.selectedSpecialistId = state.specialists.length ? state.specialists[0].id : null;
        }

        renderSpecialists();
        renderPagination("specialists", state.specialistPage, elements.specialistPagination);
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
        renderLoading(elements.bookingList, "Loading bookings...");
        const params = adminPageParams(state.bookingPage);
        if (state.selectedBookingStatus !== "ALL") {
            params.set("status", state.selectedBookingStatus);
        }
        const page = normalizePageResponse(await app.api(`/api/bookings/manage?${params.toString()}`), state.bookingPage);
        state.bookings = page.content || [];
        applyPageMeta(state.bookingPage, page);
        renderBookings();
        renderPagination("bookings", state.bookingPage, elements.bookingPagination);
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

        state.creatingCategory = false;
        state.selectedCategoryId = Number(button.dataset.categoryId);
        renderCategories();
        populateCategoryForm();
    }

    function onSelectBookingFilter(event) {
        const button = event.currentTarget;
        state.selectedBookingStatus = button.dataset.adminBookingFilter || "ALL";
        state.bookingPage.page = 0;
        loadBookings().catch((error) => app.showToast(error.message, "error", "toast"));
    }

    function onAdminSearch(event) {
        event.preventDefault();
        const form = event.currentTarget;
        const scope = form.dataset.adminSearch;
        const keyword = (form.querySelector('input[type="search"]')?.value || "").trim().toLowerCase();

        app.withFormLoading(form, "Searching...", async () => {
            if (scope === "users") {
                state.userPage.keyword = keyword;
                state.userPage.page = 0;
                await loadUsers();
            } else if (scope === "specialists") {
                state.specialistPage.keyword = keyword;
                state.specialistPage.page = 0;
                await loadSpecialists();
            } else if (scope === "bookings") {
                state.bookingPage.keyword = keyword;
                state.bookingPage.page = 0;
                await loadBookings();
            } else if (scope === "categories") {
                state.categoryPage.keyword = keyword;
                state.categoryPage.page = 0;
                renderCategories();
                populateCategoryForm();
            }
        }).catch((error) => app.showToast(error.message, "error", "toast"));
    }

    function onPaginationAction(event) {
        const button = event.target.closest("[data-page-scope]");
        if (!button || button.disabled) {
            return;
        }

        const scope = button.dataset.pageScope;
        const targetPage = Math.max(Number(button.dataset.pageTarget || 0), 0);

        app.withButtonLoading(button, "Loading...", async () => {
            if (scope === "users") {
                state.userPage.page = targetPage;
                await loadUsers();
            } else if (scope === "specialists") {
                state.specialistPage.page = targetPage;
                await loadSpecialists();
            } else if (scope === "bookings") {
                state.bookingPage.page = targetPage;
                await loadBookings();
            } else if (scope === "categories") {
                state.categoryPage.page = targetPage;
                renderCategories();
                populateCategoryForm();
            }
        }).catch((error) => app.showToast(error.message, "error", "toast"));
    }

    function onStartNewCategory() {
        state.creatingCategory = true;
        state.selectedCategoryId = null;
        renderCategories();
        populateCategoryForm();
    }

    async function onSaveUser(event) {
        event.preventDefault();
        const form = event.currentTarget;
        const userId = Number(document.getElementById("edit-user-id").value);
        const payload = app.formToObject(form);
        payload.active = payload.active === "true";

        app.clearFormErrors(form);
        await app.withFormLoading(form, "Saving...", async () => {
            const updated = await app.api(`/api/users/${userId}`, {
                method: "PUT",
                body: JSON.stringify(payload)
            });
            state.selectedUserId = updated.id;
            app.showFormSuccess(form, "User account updated successfully.");
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
        }).catch((error) => {
            app.renderFormErrors(form, error, "Unable to save the user account. Please review the form.");
        });
    }

    async function onCreateSpecialist(event) {
        event.preventDefault();
        const form = event.currentTarget;
        const payload = app.formToObject(form);
        payload.userId = Number(payload.userId);
        payload.categoryId = Number(payload.categoryId);
        payload.baseFee = Number(payload.baseFee);

        app.clearFormErrors(form);

        await app.withFormLoading(form, "Creating...", async () => {
            const created = await app.api("/api/specialists", {
                method: "POST",
                body: JSON.stringify(payload)
            });
            form.reset();
            app.clearFormErrors(form);
            state.selectedSpecialistId = created.id;
            app.showFormSuccess(form, "Specialist profile created successfully.");
            await loadSpecialists();
        }).catch((error) => {
            app.renderFormErrors(form, error);
        });
    }

    async function onSaveSpecialist(event) {
        event.preventDefault();
        const form = event.currentTarget;
        const specialistId = Number(document.getElementById("edit-specialist-id").value);
        const currentProfile = state.specialists.find((profile) => profile.id === specialistId);
        const payload = app.formToObject(form);
        payload.categoryId = Number(payload.categoryId);
        payload.baseFee = Number(payload.baseFee);

        app.clearFormErrors(form);
        if (currentProfile && currentProfile.status !== payload.status) {
            const confirmation = await app.confirmAction({
                title: "Change specialist status",
                message: `Change ${currentProfile.fullName} from ${currentProfile.status} to ${payload.status}? This controls visibility in the public specialist directory.`,
                confirmLabel: "Change Status",
                danger: payload.status === "INACTIVE"
            });
            if (!confirmation.confirmed) {
                return;
            }
        }

        await app.withFormLoading(form, "Saving...", async () => {
            const updated = await app.api(`/api/specialists/${specialistId}`, {
                method: "PUT",
                body: JSON.stringify(payload)
            });
            state.selectedSpecialistId = updated.id;
            app.showFormSuccess(form, "Specialist profile updated successfully.");
            await loadSpecialists();
        }).catch((error) => {
            app.renderFormErrors(form, error, "Unable to save the specialist profile. Please review the form.");
        });
    }

    async function onSaveCategory(event) {
        event.preventDefault();
        const form = event.currentTarget;
        const payload = app.formToObject(form);
        payload.active = payload.active === "true";

        app.clearFormErrors(form);
        await app.withFormLoading(form, "Saving...", async () => {
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
            state.creatingCategory = false;
            state.selectedCategoryId = saved.id;
            app.showFormSuccess(form, "Category saved successfully.");
            await loadCategories();
            if (views.specialists) {
                await loadSpecialists();
            }
        }).catch((error) => {
            app.renderFormErrors(form, error, "Unable to save the category. Please review the form.");
        });
    }

    async function onBookingAction(event) {
        const button = event.target.closest("[data-booking-action]");
        if (!button) {
            return;
        }

        const action = button.dataset.bookingAction;
        const actionContainer = button.closest("[data-booking-id]");
        const bookingId = actionContainer ? Number(actionContainer.dataset.bookingId) : NaN;
        const booking = state.bookings.find((item) => item.id === bookingId);

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
            endpoint = `/api/bookings/${bookingId}/reject`;
            successMessage = "Booking rejected successfully.";
        } else if (action === "cancel") {
            endpoint = `/api/bookings/${bookingId}/cancel`;
            successMessage = "Booking cancelled successfully.";
        } else {
            return;
        }

        await app.withButtonLoading(button, "Working...", async () => {
            const confirmation = await confirmBookingWorkflowAction(action, booking);
            if (!confirmation.confirmed) {
                return;
            }

            if (action === "reject" || action === "cancel") {
                body = JSON.stringify({ reason: confirmation.reason });
            }

            await app.api(endpoint, body ? { method: "POST", body } : { method: "POST" });
            app.showToast(successMessage, "success", "toast");
            await loadBookings();
            if (views.summary) {
                await loadSummary();
            }
        }).catch((error) => {
            app.showToast(error.message, "error", "toast");
        });
    }

    function confirmBookingWorkflowAction(action, booking) {
        const topic = booking ? `"${booking.topic}"` : `booking #${booking?.id || ""}`;
        const labels = {
            confirm: {
                title: "Confirm booking",
                message: `Confirm ${topic} and keep the slot reserved for the customer?`,
                confirmLabel: "Confirm Booking"
            },
            complete: {
                title: "Mark booking completed",
                message: `Mark ${topic} as completed? This will close the linked time slot.`,
                confirmLabel: "Mark Completed"
            },
            reject: {
                title: "Reject booking",
                message: `Reject ${topic} and release the linked time slot?`,
                confirmLabel: "Reject Booking",
                reasonLabel: "Rejection Reason",
                reasonPlaceholder: "Optional note for this action",
                danger: true
            },
            cancel: {
                title: "Cancel booking",
                message: `Cancel ${topic} and release the linked time slot when possible?`,
                confirmLabel: "Cancel Booking",
                reasonLabel: "Cancellation Reason",
                reasonPlaceholder: "Optional note for this action",
                danger: true
            }
        };

        return app.confirmAction(labels[action] || {});
    }
});
