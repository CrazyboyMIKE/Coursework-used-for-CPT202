document.addEventListener("DOMContentLoaded", async () => {
    const app = window.ProjectApp;
    const user = await app.requireAuth("/login.html");

    if (!user) {
        return;
    }

    if (user.role === "ADMIN") {
        window.location.replace("/admin.html");
        return;
    }

    const state = {
        user,
        categories: [],
        selectedSpecialistId: null,
        selectedSpecialistName: "",
        selectedSlotId: null,
        selectedSlots: [],
        selectedSlotWindowStart: todayDateValue(),
        customerStatusFilter: "ALL",
        specialistStatusFilter: "PENDING",
        specialistProfile: null
    };

    const elements = {
        anchorCustomer: document.getElementById("anchor-customer"),
        anchorSpecialist: document.getElementById("anchor-specialist"),
        anchorAdmin: document.getElementById("anchor-admin"),
        sideCustomer: document.getElementById("side-customer"),
        sideSpecialist: document.getElementById("side-specialist"),
        sideAdmin: document.getElementById("side-admin"),
        customerPanel: document.getElementById("customer-panel"),
        specialistPanel: document.getElementById("specialist-panel"),
        adminPanel: document.getElementById("admin-panel"),
        headerFullName: document.getElementById("header-full-name"),
        headerRole: document.getElementById("header-role"),
        profileUsername: document.getElementById("profile-username"),
        profileEmail: document.getElementById("profile-email"),
        profilePhone: document.getElementById("profile-phone"),
        profileForm: document.getElementById("profile-form"),
        profileFormFullName: document.getElementById("profile-form-full-name"),
        profileFormEmail: document.getElementById("profile-form-email"),
        profileFormPhone: document.getElementById("profile-form-phone"),
        slotForm: document.getElementById("slot-form"),
        categoryCount: document.getElementById("category-count"),
        dashboardSubtitle: document.getElementById("dashboard-subtitle"),
        specialistResults: document.getElementById("specialist-results"),
        slotResults: document.getElementById("slot-results"),
        slotBoardTitle: document.getElementById("slot-board-title"),
        slotStartDate: document.getElementById("slot-start-date"),
        slotDateRefresh: document.getElementById("slot-date-refresh"),
        searchCategory: document.getElementById("search-category"),
        adminCategorySelect: document.getElementById("admin-category-select"),
        bookingSpecialistId: document.getElementById("booking-specialist-id"),
        bookingSlotId: document.getElementById("booking-slot-id"),
        customerFilterBar: document.getElementById("customer-filter-bar"),
        customerBookings: document.getElementById("customer-bookings"),
        specialistFilterBar: document.getElementById("specialist-filter-bar"),
        specialistProfile: document.getElementById("specialist-profile"),
        specialistSchedule: document.getElementById("specialist-schedule"),
        summaryGrid: document.getElementById("summary-grid"),
        categoryList: document.getElementById("category-list"),
        createdUserTip: document.getElementById("created-user-tip")
    };

    app.consumeFlash("toast");
    bindEvents();
    renderSession();
    startAutoRefresh();

    await loadCategories();
    await searchSpecialists();
    await refreshRoleWorkspace();

    function bindEvents() {
        document.getElementById("logout-button").addEventListener("click", onLogout);
        elements.profileForm.addEventListener("submit", onSaveProfile);
        document.getElementById("specialist-search-form").addEventListener("submit", onSearchSpecialists);
        document.getElementById("booking-form").addEventListener("submit", onCreateBooking);
        elements.slotForm.addEventListener("submit", onCreateSlot);
        elements.slotForm.querySelectorAll("input").forEach((input) => {
            input.addEventListener("input", () => validateSlotForm(elements.slotForm, false));
        });
        elements.slotDateRefresh.addEventListener("click", onRefreshSlotWindow);
        elements.slotStartDate.addEventListener("change", onRefreshSlotWindow);
        elements.customerFilterBar.addEventListener("click", onCustomerFilterChange);
        elements.specialistFilterBar.addEventListener("click", onSpecialistFilterChange);
        document.getElementById("category-form").addEventListener("submit", onCreateCategory);
        document.getElementById("admin-user-form").addEventListener("submit", onCreateUser);
        document.getElementById("admin-specialist-form").addEventListener("submit", onCreateSpecialistProfile);
        document.getElementById("refresh-customer-bookings").addEventListener("click", refreshCustomerWorkspace);
        document.getElementById("refresh-specialist-schedule").addEventListener("click", refreshSpecialistWorkspace);
        document.getElementById("refresh-admin-summary").addEventListener("click", refreshAdminWorkspace);
        elements.specialistResults.addEventListener("click", onSpecialistAction);
        elements.slotResults.addEventListener("click", onSlotAction);
        elements.customerBookings.addEventListener("click", onCustomerBookingAction);
        elements.specialistSchedule.addEventListener("click", onSpecialistBookingAction);
    }

    function renderSession() {
        elements.headerFullName.textContent = state.user.fullName;
        elements.headerRole.textContent = state.user.role;
        elements.headerRole.className = `role-pill ${state.user.role}`;
        elements.profileUsername.textContent = state.user.username;
        elements.profileEmail.textContent = state.user.email || "-";
        elements.profilePhone.textContent = state.user.phone || "-";
        elements.profileUsername.title = state.user.username || "";
        elements.profileEmail.title = state.user.email || "";
        elements.profilePhone.title = state.user.phone || "";
        elements.dashboardSubtitle.textContent = roleSubtitle(state.user.role);
        elements.slotStartDate.value = state.selectedSlotWindowStart;
        renderProfileForm();
        renderCustomerFilterButtons();
        renderSpecialistFilterButtons();

        toggleRoleVisibility(state.user.role === "CUSTOMER", elements.customerPanel, elements.anchorCustomer, elements.sideCustomer);
        toggleRoleVisibility(state.user.role === "SPECIALIST", elements.specialistPanel, elements.anchorSpecialist, elements.sideSpecialist);
        toggleRoleVisibility(state.user.role === "ADMIN", elements.adminPanel, elements.anchorAdmin, elements.sideAdmin);
    }

    function startAutoRefresh() {
        if (state.user.role !== "SPECIALIST") {
            return;
        }

        window.setInterval(() => {
            refreshSpecialistWorkspace();
        }, 30000);
    }

    function toggleRoleVisibility(show, panel, anchor, side) {
        panel.classList.toggle("hidden", !show);
        anchor.classList.toggle("hidden", !show);
        side.classList.toggle("hidden", !show);
    }

    function roleSubtitle(role) {
        if (role === "CUSTOMER") {
            return "You are signed in as a customer and can create, cancel, and reschedule bookings.";
        }
        if (role === "SPECIALIST") {
            return "You are signed in as a specialist and can manage time slots and handle pending bookings.";
        }
        if (role === "ADMIN") {
            return "You are signed in as an administrator and can manage categories, accounts, specialist profiles, and summary metrics.";
        }
        return "Manage booking workflows, specialist schedules, and category configuration in one place.";
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

    function renderProfileForm() {
        elements.profileFormFullName.value = state.user.fullName || "";
        elements.profileFormEmail.value = state.user.email || "";
        elements.profileFormPhone.value = state.user.phone || "";
    }

    async function onSaveProfile(event) {
        event.preventDefault();
        const form = event.currentTarget;

        try {
            state.user = await app.api("/api/users/me", {
                method: "PUT",
                body: JSON.stringify(app.formToObject(form))
            });
            renderSession();
            await refreshRoleWorkspace();
            await searchSpecialists();
            app.showToast("Profile updated successfully.", "success", "toast");
        } catch (error) {
            app.showToast(error.message, "error", "toast");
        }
    }

    async function loadCategories() {
        try {
            state.categories = await app.api("/api/categories");
            elements.categoryCount.textContent = String(state.categories.length);
            renderCategoryOptions();
            renderCategories();
        } catch (error) {
            app.showToast(error.message, "error", "toast");
        }
    }

    function renderCategoryOptions() {
        const searchOptions = ['<option value="">All Categories</option>']
                .concat(state.categories.map((category) =>
                        `<option value="${category.id}">${app.escapeHtml(category.name)}</option>`
                ))
                .join("");

        const adminOptions = state.categories.map((category) =>
                `<option value="${category.id}">${app.escapeHtml(category.name)}</option>`
        ).join("");

        elements.searchCategory.innerHTML = searchOptions;
        elements.adminCategorySelect.innerHTML = adminOptions || '<option value="">Create a category first</option>';
    }

    function renderCategories() {
        if (!state.categories.length) {
            elements.categoryList.innerHTML = '<div class="empty-state">No categories available yet</div>';
            return;
        }

        elements.categoryList.innerHTML = state.categories.map((category) => `
            <article class="category-chip">
                <div class="card-head">
                    <strong>${app.escapeHtml(category.name)}</strong>
                    <span class="status-pill ${category.active ? "ACTIVE" : "INACTIVE"}">${category.active ? "ACTIVE" : "INACTIVE"}</span>
                </div>
                <p>${app.escapeHtml(category.description || "No description available")}</p>
                <small>ID: ${category.id}</small>
            </article>
        `).join("");
    }

    async function onSearchSpecialists(event) {
        event.preventDefault();
        await searchSpecialists();
    }

    async function searchSpecialists() {
        const params = new URLSearchParams();
        const keyword = document.getElementById("search-keyword").value.trim();
        const categoryId = elements.searchCategory.value;
        const level = document.getElementById("search-level").value;
        const minFee = document.getElementById("search-min-fee").value;
        const maxFee = document.getElementById("search-max-fee").value;
        const availableAt = document.getElementById("search-available-at").value;

        if (keyword) {
            params.set("keyword", keyword);
        }
        if (categoryId) {
            params.set("categoryId", categoryId);
        }
        if (level) {
            params.set("level", level);
        }
        if (minFee) {
            params.set("minFee", minFee);
        }
        if (maxFee) {
            params.set("maxFee", maxFee);
        }
        if (availableAt) {
            params.set("availableAt", app.toApiDateTime(availableAt));
        }

        try {
            const url = params.toString() ? `/api/specialists?${params.toString()}` : "/api/specialists";
            const specialists = await app.api(url);
            renderSpecialists(specialists);
            await reloadSelectedSlots();
        } catch (error) {
            app.showToast(error.message, "error", "toast");
        }
    }

    async function reloadSelectedSlots() {
        if (!state.selectedSpecialistId) {
            return;
        }

        try {
            state.selectedSlots = await app.api(buildSlotWindowUrl(state.selectedSpecialistId));
            renderSlots();
        } catch (error) {
            app.showToast(error.message, "error", "toast");
        }
    }

    function renderSpecialists(specialists) {
        if (!specialists.length) {
            elements.specialistResults.innerHTML = '<div class="empty-state">No specialists match the current filters</div>';
            return;
        }

        elements.specialistResults.innerHTML = specialists.map((specialist) => `
            <article class="specialist-card">
                <div class="card-head">
                    <div>
                        <h3>${app.escapeHtml(specialist.fullName)}</h3>
                        <p>${app.escapeHtml(specialist.categoryName)}</p>
                    </div>
                    <span class="badge">${specialist.level}</span>
                </div>
                <div class="meta-block">
                    <span>Specialist ID: <strong>${specialist.id}</strong></span>
                    <span>Base Fee: <strong>${app.formatCurrency(specialist.baseFee)}</strong></span>
                    <span>Status: <span class="status-pill ${specialist.status}">${specialist.status}</span></span>
                </div>
                <p>${app.escapeHtml(specialist.bio || "No bio available")}</p>
                <div class="action-row">
                    <a class="secondary-link compact-link" href="/specialist-detail.html?id=${specialist.id}">View Profile</a>
                    <button type="button" data-action="load-slots" data-id="${specialist.id}" data-name="${app.escapeHtml(specialist.fullName)}">View Time Slots</button>
                    <button class="ghost-button" type="button" data-action="prepare-booking" data-id="${specialist.id}">Write to Booking Form</button>
                </div>
            </article>
        `).join("");
    }

    async function onSpecialistAction(event) {
        const button = event.target.closest("[data-action]");
        if (!button) {
            return;
        }

        const action = button.dataset.action;
        const specialistId = Number(button.dataset.id);

        if (action === "prepare-booking") {
            elements.bookingSpecialistId.value = String(specialistId);
            app.showToast("Specialist ID added. Please select a time slot next.", "success", "toast");
            return;
        }

        if (action === "load-slots") {
            state.selectedSpecialistId = specialistId;
            state.selectedSpecialistName = button.dataset.name;
            elements.slotBoardTitle.textContent = `7-day slot window for ${button.dataset.name}`;
            elements.bookingSpecialistId.value = String(specialistId);

            try {
                state.selectedSlots = await app.api(buildSlotWindowUrl(specialistId));
                renderSlots();
            } catch (error) {
                app.showToast(error.message, "error", "toast");
            }
        }
    }

    function renderSlots() {
        if (!state.selectedSlots.length) {
            elements.slotResults.innerHTML = '<div class="empty-state">No time slots were published in the selected 7-day window.</div>';
            return;
        }

        elements.slotResults.innerHTML = state.selectedSlots.map((slot) => `
            <article class="slot-item ${state.selectedSlotId === slot.id ? "selected" : ""} ${slot.status !== "AVAILABLE" ? "slot-muted" : ""}">
                <div class="slot-time">${app.formatDateTime(slot.startTime)} - ${app.formatDateTime(slot.endTime)}</div>
                <div class="meta-block">
                    <span>Time Slot ID: <strong>${slot.id}</strong></span>
                    <span>Status: <span class="status-pill ${slot.status}">${slot.status}</span></span>
                </div>
                <button
                    class="ghost-button"
                    type="button"
                    data-action="select-slot"
                    data-slot-id="${slot.id}"
                    data-specialist-id="${slot.specialistId}"
                    ${slot.status !== "AVAILABLE" ? "disabled" : ""}
                    title="${slot.status === "AVAILABLE" ? "Select this slot for booking" : "This slot is not available for booking"}"
                >${slot.status === "AVAILABLE" ? "Select This Slot" : "Unavailable"}</button>
            </article>
        `).join("");
    }

    function onSlotAction(event) {
        const button = event.target.closest("[data-action='select-slot']");
        if (!button) {
            return;
        }

        state.selectedSlotId = Number(button.dataset.slotId);
        elements.bookingSpecialistId.value = button.dataset.specialistId;
        elements.bookingSlotId.value = button.dataset.slotId;
        renderSlots();
        app.showToast(`Time slot ${button.dataset.slotId} selected.`, "success", "toast");
    }

    async function onCreateBooking(event) {
        event.preventDefault();
        const form = event.currentTarget;
        const payload = app.formToObject(form);
        payload.specialistId = Number(payload.specialistId);
        payload.slotId = Number(payload.slotId);

        try {
            await app.api("/api/bookings", {
                method: "POST",
                body: JSON.stringify(payload)
            });
            form.reset();
            elements.bookingSpecialistId.value = "";
            elements.bookingSlotId.value = "";
            state.selectedSlotId = null;
            await refreshCustomerWorkspace();
            await searchSpecialists();
            renderSlots();
            app.showToast("Booking submitted and waiting for confirmation.", "success", "toast");
        } catch (error) {
            app.showToast(error.message, "error", "toast");
        }
    }

    async function refreshRoleWorkspace() {
        if (state.user.role === "CUSTOMER") {
            await refreshCustomerWorkspace();
        }
        if (state.user.role === "SPECIALIST") {
            await refreshSpecialistWorkspace();
        }
        if (state.user.role === "ADMIN") {
            await refreshAdminWorkspace();
        }
    }

    async function refreshCustomerWorkspace() {
        if (state.user.role !== "CUSTOMER") {
            return;
        }

        try {
            const bookings = await app.api(buildCustomerBookingsUrl());
            renderCustomerBookings(bookings);
        } catch (error) {
            app.showToast(error.message, "error", "toast");
        }
    }

    function renderCustomerBookings(bookings) {
        if (!bookings.length) {
            elements.customerBookings.innerHTML = `<div class="empty-state">${customerEmptyMessage()}</div>`;
            return;
        }

        elements.customerBookings.innerHTML = bookings.map((booking) => `
            <article class="booking-card">
                <div class="card-head">
                    <div>
                        <h3>${app.escapeHtml(booking.topic)}</h3>
                        <p>${app.escapeHtml(booking.specialistName)} / ${app.formatDateTime(booking.startTime)}</p>
                    </div>
                    <span class="status-pill ${booking.status}">${booking.status}</span>
                </div>
                <div class="meta-block">
                    <span>Booking ID: <strong>${booking.id}</strong></span>
                    <span>Time Slot ID: <strong>${booking.slotId}</strong></span>
                    <span>Price: <strong>${app.formatCurrency(booking.price)}</strong></span>
                    <span>Scheduled Length: <strong>${formatDuration(booking.startTime, booking.endTime)}</strong></span>
                    <span>Notes: ${app.escapeHtml(booking.notes || "None")}</span>
                </div>
                <div class="action-grid">
                    <input id="customer-reason-${booking.id}" type="text" placeholder="Cancellation reason or note">
                    <button
                        class="ghost-button"
                        type="button"
                        data-action="cancel-booking"
                        data-booking-id="${booking.id}"
                        ${canCustomerCancel(booking) ? "" : "disabled"}
                        title="${app.escapeHtml(customerCancellationHint(booking))}"
                    >Cancel Booking</button>
                    <button
                        class="ghost-button"
                        type="button"
                        data-action="load-reschedule"
                        data-booking-id="${booking.id}"
                        data-specialist-id="${booking.specialistId}"
                        data-start-time="${booking.startTime}"
                        ${canCustomerReschedule(booking) ? "" : "disabled"}
                        title="${app.escapeHtml(customerRescheduleHint(booking))}"
                    >Load Reschedule Slots</button>
                    <select id="reschedule-select-${booking.id}">
                        <option value="">Select a new time slot</option>
                    </select>
                    <button
                        type="button"
                        data-action="reschedule-booking"
                        data-booking-id="${booking.id}"
                        ${canCustomerReschedule(booking) ? "" : "disabled"}
                        title="${app.escapeHtml(customerRescheduleHint(booking))}"
                    >Submit Reschedule</button>
                </div>
            </article>
        `).join("");
    }

    async function onCustomerBookingAction(event) {
        const button = event.target.closest("[data-action]");
        if (!button) {
            return;
        }

        const bookingId = Number(button.dataset.bookingId);

        if (button.dataset.action === "cancel-booking") {
            const reason = document.getElementById(`customer-reason-${bookingId}`).value;
            try {
                await app.api(`/api/bookings/${bookingId}/cancel`, {
                    method: "POST",
                    body: JSON.stringify({ reason })
                });
                await refreshCustomerWorkspace();
                await searchSpecialists();
                app.showToast("Booking cancelled successfully.", "success", "toast");
            } catch (error) {
                app.showToast(error.message, "error", "toast");
            }
            return;
        }

        if (button.dataset.action === "load-reschedule") {
            const specialistId = Number(button.dataset.specialistId);
            const windowStart = dateValueFromIso(button.dataset.startTime);
            try {
                const slots = await app.api(`/api/slots/specialists/${specialistId}?status=AVAILABLE&fromDate=${windowStart}&days=7`);
                const select = document.getElementById(`reschedule-select-${bookingId}`);
                select.innerHTML = ['<option value="">Select a new time slot</option>']
                        .concat(slots.map((slot) =>
                                `<option value="${slot.id}">${slot.id} / ${app.formatDateTime(slot.startTime)}</option>`
                        ))
                        .join("");
                app.showToast("Available reschedule slots loaded.", "success", "toast");
            } catch (error) {
                app.showToast(error.message, "error", "toast");
            }
            return;
        }

        if (button.dataset.action === "reschedule-booking") {
            const select = document.getElementById(`reschedule-select-${bookingId}`);
            const newSlotId = Number(select.value);

            if (!newSlotId) {
                app.showToast("Please select a new time slot first.", "error", "toast");
                return;
            }

            try {
                await app.api(`/api/bookings/${bookingId}/reschedule`, {
                    method: "POST",
                    body: JSON.stringify({ newSlotId })
                });
                await refreshCustomerWorkspace();
                await searchSpecialists();
                app.showToast("Reschedule submitted. The booking is pending confirmation again.", "success", "toast");
            } catch (error) {
                app.showToast(error.message, "error", "toast");
            }
        }
    }

    async function refreshSpecialistWorkspace() {
        if (state.user.role !== "SPECIALIST") {
            return;
        }

        try {
            state.specialistProfile = await app.api("/api/specialists/me");
            renderSpecialistProfile();
            const bookings = await app.api(buildSpecialistScheduleUrl());
            renderSpecialistSchedule(bookings);
        } catch (error) {
            app.showToast(error.message, "error", "toast");
        }
    }

    function renderSpecialistProfile() {
        if (!state.specialistProfile) {
            elements.specialistProfile.innerHTML = '<div class="empty-state">No specialist profile is linked to the current account</div>';
            return;
        }

        const profile = state.specialistProfile;
        elements.specialistProfile.innerHTML = `
            <div><span class="muted-label">Specialist ID</span><strong>${profile.id}</strong></div>
            <div><span class="muted-label">Name</span><strong>${app.escapeHtml(profile.fullName)}</strong></div>
            <div><span class="muted-label">Category</span><strong>${app.escapeHtml(profile.categoryName)}</strong></div>
            <div><span class="muted-label">Level</span><strong>${profile.level}</strong></div>
            <div><span class="muted-label">Base Fee</span><strong>${app.formatCurrency(profile.baseFee)}</strong></div>
            <div><span class="muted-label">Status</span><strong>${profile.status}</strong></div>
            <div><span class="muted-label">Bio</span><strong>${app.escapeHtml(profile.bio || "No bio available")}</strong></div>
        `;
    }

    async function onCreateSlot(event) {
        event.preventDefault();
        if (!state.specialistProfile) {
            app.showToast("The current account is not linked to a specialist profile.", "error", "toast");
            return;
        }

        const form = event.currentTarget;
        const payload = validateSlotForm(form, true);
        if (!payload) {
            return;
        }

        try {
            await app.api(`/api/slots/specialists/${state.specialistProfile.id}`, {
                method: "POST",
                body: JSON.stringify(payload)
            });
            form.reset();
            app.clearFormErrors(form);
            await refreshSpecialistWorkspace();
            await searchSpecialists();
            app.showToast("Time slot created successfully.", "success", "toast");
        } catch (error) {
            app.renderFormErrors(form, error);
        }
    }

    function validateSlotForm(form, showSummary) {
        const now = new Date();
        const raw = app.formToObject(form);
        const start = raw.startTime ? new Date(raw.startTime) : null;
        const end = raw.endTime ? new Date(raw.endTime) : null;
        let valid = true;

        app.clearFormErrors(form);

        if (!raw.startTime) {
            app.setFieldError(form, "startTime", "Start time is required.");
            valid = false;
        }

        if (!raw.endTime) {
            app.setFieldError(form, "endTime", "End time is required.");
            valid = false;
        }

        if (start && !Number.isNaN(start.getTime()) && start <= now) {
            app.setFieldError(form, "startTime", "Start time must be in the future.");
            valid = false;
        }

        if (start && end && !Number.isNaN(start.getTime()) && !Number.isNaN(end.getTime()) && end <= start) {
            app.setFieldError(form, "endTime", "End time must be later than the start time.");
            valid = false;
        }

        if (!valid) {
            if (showSummary) {
                app.setFormError(form, "Please correct the highlighted fields and try again.");
            }
            return null;
        }

        return {
            startTime: app.toApiDateTime(raw.startTime),
            endTime: app.toApiDateTime(raw.endTime)
        };
    }

    function renderSpecialistSchedule(bookings) {
        if (!bookings.length) {
            elements.specialistSchedule.innerHTML = `<div class="empty-state">${specialistEmptyMessage()}</div>`;
            return;
        }

        elements.specialistSchedule.innerHTML = bookings.map((booking) => `
            <article class="booking-card">
                <div class="card-head">
                    <div>
                        <h3>${app.escapeHtml(booking.topic)}</h3>
                        <p>${app.escapeHtml(booking.customerName)} / ${app.formatDateTime(booking.startTime)}</p>
                    </div>
                    <span class="status-pill ${booking.status}">${booking.status}</span>
                </div>
                <div class="meta-block">
                    <span>Booking ID: <strong>${booking.id}</strong></span>
                    <span>Time Slot ID: <strong>${booking.slotId}</strong></span>
                    <span>Price: <strong>${app.formatCurrency(booking.price)}</strong></span>
                    <span>Scheduled Length: <strong>${formatDuration(booking.startTime, booking.endTime)}</strong></span>
                    <span>Notes: ${app.escapeHtml(booking.notes || "None")}</span>
                </div>
                <div class="action-grid">
                    ${booking.status === "PENDING" ? `<input id="specialist-reason-${booking.id}" type="text" placeholder="Optional rejection reason">` : ""}
                    ${renderSpecialistActions(booking)}
                </div>
            </article>
        `).join("");
    }

    async function onSpecialistBookingAction(event) {
        const button = event.target.closest("[data-action]");
        if (!button) {
            return;
        }

        const bookingId = Number(button.dataset.bookingId);

        if (button.dataset.action === "confirm-booking") {
            try {
                await app.api(`/api/bookings/${bookingId}/confirm`, { method: "POST" });
                await refreshSpecialistWorkspace();
                app.showToast("Booking confirmed successfully.", "success", "toast");
            } catch (error) {
                app.showToast(error.message, "error", "toast");
            }
            return;
        }

        if (button.dataset.action === "reject-booking") {
            const reason = document.getElementById(`specialist-reason-${bookingId}`).value;
            try {
                await app.api(`/api/bookings/${bookingId}/reject`, {
                    method: "POST",
                    body: JSON.stringify({ reason })
                });
                await refreshSpecialistWorkspace();
                await searchSpecialists();
                app.showToast("Booking rejected successfully.", "success", "toast");
            } catch (error) {
                app.showToast(error.message, "error", "toast");
            }
            return;
        }

        if (button.dataset.action === "complete-booking") {
            try {
                await app.api(`/api/bookings/${bookingId}/complete`, { method: "POST" });
                await refreshSpecialistWorkspace();
                app.showToast("Booking marked as completed.", "success", "toast");
            } catch (error) {
                app.showToast(error.message, "error", "toast");
            }
        }
    }

    function onRefreshSlotWindow() {
        state.selectedSlotWindowStart = elements.slotStartDate.value || todayDateValue();
        elements.slotStartDate.value = state.selectedSlotWindowStart;
        reloadSelectedSlots();
    }

    function onCustomerFilterChange(event) {
        const button = event.target.closest("[data-customer-filter]");
        if (!button) {
            return;
        }

        state.customerStatusFilter = button.dataset.customerFilter;
        renderCustomerFilterButtons();
        refreshCustomerWorkspace();
    }

    function onSpecialistFilterChange(event) {
        const button = event.target.closest("[data-specialist-filter]");
        if (!button) {
            return;
        }

        state.specialistStatusFilter = button.dataset.specialistFilter;
        renderSpecialistFilterButtons();
        refreshSpecialistWorkspace();
    }

    function renderCustomerFilterButtons() {
        elements.customerFilterBar.querySelectorAll("[data-customer-filter]").forEach((button) => {
            button.classList.toggle("active", button.dataset.customerFilter === state.customerStatusFilter);
        });
    }

    function renderSpecialistFilterButtons() {
        elements.specialistFilterBar.querySelectorAll("[data-specialist-filter]").forEach((button) => {
            button.classList.toggle("active", button.dataset.specialistFilter === state.specialistStatusFilter);
        });
    }

    function buildSlotWindowUrl(specialistId) {
        return `/api/slots/specialists/${specialistId}?fromDate=${state.selectedSlotWindowStart}&days=7`;
    }

    function buildCustomerBookingsUrl() {
        const params = new URLSearchParams();
        if (state.customerStatusFilter !== "ALL") {
            params.set("status", state.customerStatusFilter);
        }
        return params.toString() ? `/api/bookings/me?${params.toString()}` : "/api/bookings/me";
    }

    function buildSpecialistScheduleUrl() {
        const params = new URLSearchParams();
        if (state.specialistStatusFilter !== "ALL") {
            params.set("status", state.specialistStatusFilter);
        }
        return params.toString() ? `/api/bookings/schedule?${params.toString()}` : "/api/bookings/schedule";
    }

    function canCustomerCancel(booking) {
        if (booking.status === "COMPLETED" || booking.status === "CANCELLED" || booking.status === "REJECTED") {
            return false;
        }

        const hoursUntilStart = hoursUntil(booking.startTime);
        if (hoursUntilStart <= 0) {
            return false;
        }

        return booking.status !== "CONFIRMED" || hoursUntilStart > 24;
    }

    function customerCancellationHint(booking) {
        if (canCustomerCancel(booking)) {
            return "Cancellation is available for this appointment.";
        }
        if (booking.status === "CONFIRMED") {
            return "Confirmed appointments can only be cancelled more than 24 hours before the consultation starts.";
        }
        if (booking.status === "COMPLETED") {
            return "Completed appointments can no longer be changed.";
        }
        if (booking.status === "CANCELLED" || booking.status === "REJECTED") {
            return "This appointment is already closed.";
        }
        return "Only future appointments can be cancelled.";
    }

    function canCustomerReschedule(booking) {
        if (booking.status === "COMPLETED" || booking.status === "CANCELLED" || booking.status === "REJECTED") {
            return false;
        }
        return hoursUntil(booking.startTime) > 0;
    }

    function customerRescheduleHint(booking) {
        if (canCustomerReschedule(booking)) {
            return "Load available future slots for rescheduling.";
        }
        if (booking.status === "COMPLETED") {
            return "Completed appointments can no longer be changed.";
        }
        if (booking.status === "CANCELLED" || booking.status === "REJECTED") {
            return "Closed appointments cannot be rescheduled.";
        }
        return "Only future appointments can be rescheduled.";
    }

    function renderSpecialistActions(booking) {
        if (booking.status === "PENDING") {
            return `
                <button class="ghost-button" type="button" data-action="confirm-booking" data-booking-id="${booking.id}">Accept</button>
                <button class="ghost-button" type="button" data-action="reject-booking" data-booking-id="${booking.id}">Decline</button>
            `;
        }
        if (booking.status === "CONFIRMED") {
            return `<button type="button" data-action="complete-booking" data-booking-id="${booking.id}">Confirm Completion</button>`;
        }
        return `<span class="muted-label">No action is required for this booking state.</span>`;
    }

    function customerEmptyMessage() {
        return state.customerStatusFilter === "ALL"
                ? "No appointments were found for the current account."
                : `No appointments were found with the status ${state.customerStatusFilter}.`;
    }

    function specialistEmptyMessage() {
        const map = {
            PENDING: "No pending confirmation requests are waiting right now.",
            CONFIRMED: "No confirmed consultations are currently assigned.",
            COMPLETED: "No completed consultations are available in the current view.",
            REJECTED: "No declined consultation requests are available in the current view."
        };
        return map[state.specialistStatusFilter] || "No consultations match the current specialist filter.";
    }

    function formatDuration(startTime, endTime) {
        const minutes = Math.max(0, Math.round((new Date(endTime) - new Date(startTime)) / 60000));
        if (minutes >= 60 && minutes % 60 === 0) {
            return `${minutes / 60} hour${minutes === 60 ? "" : "s"}`;
        }
        if (minutes > 60) {
            const hours = Math.floor(minutes / 60);
            const remainingMinutes = minutes % 60;
            return `${hours} hour${hours === 1 ? "" : "s"} ${remainingMinutes} min`;
        }
        return `${minutes} min`;
    }

    function hoursUntil(dateTime) {
        return (new Date(dateTime).getTime() - Date.now()) / 3600000;
    }

    function todayDateValue() {
        return new Date().toISOString().slice(0, 10);
    }

    function dateValueFromIso(value) {
        return value ? new Date(value).toISOString().slice(0, 10) : todayDateValue();
    }

    async function refreshAdminWorkspace() {
        if (state.user.role !== "ADMIN") {
            return;
        }

        try {
            const summary = await app.api("/api/reports/summary");
            renderSummary(summary);
            await loadCategories();
        } catch (error) {
            app.showToast(error.message, "error", "toast");
        }
    }

    function renderSummary(summary) {
        const entries = [
            ["Total Users", summary.totalUsers],
            ["Total Specialists", summary.totalSpecialists],
            ["Available Time Slots", summary.totalAvailableSlots],
            ["Total Bookings", summary.totalBookings],
            ["Confirmed Revenue", app.formatCurrency(summary.confirmedRevenue)]
        ];

        Object.entries(summary.bookingsByStatus || {}).forEach(([status, value]) => {
            entries.push([`Status ${status}`, value]);
        });

        elements.summaryGrid.innerHTML = entries.map(([label, value]) => `
            <article class="stat-card">
                <span>${app.escapeHtml(label)}</span>
                <strong>${app.escapeHtml(String(value))}</strong>
            </article>
        `).join("");
    }

    async function onCreateCategory(event) {
        event.preventDefault();
        const form = event.currentTarget;

        try {
            await app.api("/api/categories", {
                method: "POST",
                body: JSON.stringify(app.formToObject(form))
            });
            form.reset();
            await refreshAdminWorkspace();
            app.showToast("Category created successfully.", "success", "toast");
        } catch (error) {
            app.showToast(error.message, "error", "toast");
        }
    }

    async function onCreateUser(event) {
        event.preventDefault();
        const form = event.currentTarget;

        try {
            const created = await app.api("/api/users", {
                method: "POST",
                body: JSON.stringify(app.formToObject(form))
            });
            form.reset();
            elements.createdUserTip.textContent = `Latest created user ID: ${created.id} (${created.username})`;
            app.showToast("User account created successfully.", "success", "toast");
        } catch (error) {
            app.showToast(error.message, "error", "toast");
        }
    }

    async function onCreateSpecialistProfile(event) {
        event.preventDefault();
        const form = event.currentTarget;
        const payload = app.formToObject(form);
        payload.userId = Number(payload.userId);
        payload.categoryId = Number(payload.categoryId);
        payload.baseFee = Number(payload.baseFee);

        try {
            await app.api("/api/specialists", {
                method: "POST",
                body: JSON.stringify(payload)
            });
            form.reset();
            await searchSpecialists();
            app.showToast("Specialist profile created successfully.", "success", "toast");
        } catch (error) {
            app.showToast(error.message, "error", "toast");
        }
    }
});
