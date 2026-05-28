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
        directorySpecialists: [],
        bookingCandidates: [],
        selectedBookingSpecialist: null,
        selectedBookingSlots: [],
        selectedSlotId: null,
        selectedSlotWindowStart: todayDateValue(),
        customerStatusFilter: "ALL",
        specialistStatusFilter: "PENDING",
        specialistProfile: null,
        specialistSelectedConsultationId: null,
        specialistCalendarMode: "WEEK",
        specialistCalendarStart: todayDateValue(),
        notifications: [],
        openNotificationIds: new Set(),
        earningsEntries: [],
        bookingSearchRequestId: 0,
        bookingSearchTimer: null
    };

    const elements = {
        anchorDirectory: document.getElementById("anchor-directory"),
        anchorCustomerDirectory: document.getElementById("anchor-customer-directory"),
        anchorCustomerBookings: document.getElementById("anchor-customer-bookings"),
        anchorSpecialistDirectory: document.getElementById("anchor-specialist-directory"),
        anchorSpecialist: document.getElementById("anchor-specialist"),
        sideDirectory: document.getElementById("side-directory"),
        sideCustomerDirectory: document.getElementById("side-customer-directory"),
        sideCustomerBookings: document.getElementById("side-customer-bookings"),
        sideSpecialistDirectory: document.getElementById("side-specialist-directory"),
        sideSpecialist: document.getElementById("side-specialist"),
        directoryPanel: document.getElementById("directory"),
        customerPanel: document.getElementById("customer-panel"),
        specialistPanel: document.getElementById("specialist-panel"),
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
        recurringSlotForm: document.getElementById("recurring-slot-form"),
        categoryCount: document.getElementById("category-count"),
        dashboardSubtitle: document.getElementById("dashboard-subtitle"),
        specialistResults: document.getElementById("specialist-results"),
        searchCategory: document.getElementById("search-category"),
        bookingForm: document.getElementById("booking-form"),
        bookingSpecialistQuery: document.getElementById("booking-specialist-query"),
        bookingSpecialistSuggestions: document.getElementById("booking-specialist-suggestions"),
        bookingSpecialistSummary: document.getElementById("booking-specialist-summary"),
        bookingSlotBrowserTitle: document.getElementById("booking-slot-browser-title"),
        bookingAvailableSlots: document.getElementById("booking-available-slots"),
        bookingSlotSummary: document.getElementById("booking-slot-summary"),
        bookingSpecialistId: document.getElementById("booking-specialist-id"),
        bookingSlotId: document.getElementById("booking-slot-id"),
        customerFilterBar: document.getElementById("customer-filter-bar"),
        customerBookings: document.getElementById("customer-bookings"),
        specialistFilterBar: document.getElementById("specialist-filter-bar"),
        specialistProfile: document.getElementById("specialist-profile"),
        specialistProfileForm: document.getElementById("specialist-profile-form"),
        specialistProfileCategory: document.getElementById("specialist-profile-category"),
        specialistProfileLevel: document.getElementById("specialist-profile-level"),
        specialistProfileBaseFee: document.getElementById("specialist-profile-base-fee"),
        specialistProfileBio: document.getElementById("specialist-profile-bio"),
        specialistSlots: document.getElementById("specialist-slots"),
        specialistCalendarCaption: document.getElementById("specialist-calendar-caption"),
        specialistCalendarMode: document.getElementById("specialist-calendar-mode"),
        specialistCalendarDate: document.getElementById("specialist-calendar-date"),
        specialistCalendarPrevious: document.getElementById("specialist-calendar-previous"),
        specialistCalendarToday: document.getElementById("specialist-calendar-today"),
        specialistCalendarNext: document.getElementById("specialist-calendar-next"),
        specialistSchedule: document.getElementById("specialist-schedule"),
        specialistConsultationDetail: document.getElementById("specialist-consultation-detail"),
        specialistConsultationFilterForm: document.getElementById("specialist-consultation-filter-form"),
        clearSpecialistConsultationDates: document.getElementById("clear-specialist-consultation-dates"),
        specialistEarnings: document.getElementById("specialist-earnings"),
        specialistEarningsDetail: document.getElementById("specialist-earnings-detail"),
        earningsFilterForm: document.getElementById("earnings-filter-form"),
        notificationList: document.getElementById("notification-list")
    };

    app.consumeFlash("toast");
    bindEvents();
    renderSession();
    startAutoRefresh();

    await loadCategories();
    await refreshRoleWorkspace();
    await refreshNotifications();

    function bindEvents() {
        document.getElementById("logout-button").addEventListener("click", onLogout);
        elements.profileForm.addEventListener("submit", onSaveProfile);
        document.getElementById("specialist-search-form").addEventListener("submit", onSearchSpecialists);
        elements.bookingForm.addEventListener("submit", onCreateBooking);
        elements.bookingSpecialistQuery.addEventListener("input", onBookingSpecialistQueryInput);
        elements.bookingSpecialistSuggestions.addEventListener("click", onBookingSuggestionAction);
        elements.bookingAvailableSlots.addEventListener("click", onBookingSlotSelectionAction);
        elements.slotForm.addEventListener("submit", onCreateSlot);
        elements.recurringSlotForm.addEventListener("submit", onCreateRecurringSlots);
        elements.specialistProfileForm.addEventListener("submit", onSaveSpecialistProfile);
        elements.slotForm.querySelectorAll("input").forEach((input) => {
            input.addEventListener("input", () => validateSlotForm(elements.slotForm, false));
        });
        elements.customerFilterBar.addEventListener("click", onCustomerFilterChange);
        elements.specialistFilterBar.addEventListener("click", onSpecialistFilterChange);
        elements.specialistConsultationFilterForm.addEventListener("submit", onFilterSpecialistConsultations);
        elements.clearSpecialistConsultationDates.addEventListener("click", clearSpecialistConsultationDates);
        document.getElementById("refresh-customer-bookings").addEventListener("click", refreshCustomerWorkspace);
        document.getElementById("refresh-specialist-schedule").addEventListener("click", refreshSpecialistWorkspace);
        elements.specialistCalendarMode.addEventListener("click", onSpecialistCalendarModeChange);
        elements.specialistCalendarDate.addEventListener("change", onSpecialistCalendarDateChange);
        elements.specialistCalendarPrevious.addEventListener("click", () => shiftSpecialistCalendar(-1));
        elements.specialistCalendarToday.addEventListener("click", () => setSpecialistCalendarStart(todayDateValue()));
        elements.specialistCalendarNext.addEventListener("click", () => shiftSpecialistCalendar(1));
        document.getElementById("refresh-notifications").addEventListener("click", refreshNotifications);
        elements.earningsFilterForm.addEventListener("submit", onFilterEarnings);
        elements.specialistEarnings.addEventListener("click", onEarningsEntryClick);
        elements.specialistResults.addEventListener("click", onSpecialistAction);
        elements.customerBookings.addEventListener("click", onCustomerBookingAction);
        elements.specialistSchedule.addEventListener("click", onSpecialistBookingAction);
        elements.specialistConsultationDetail.addEventListener("click", onSpecialistDetailAction);
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
        renderProfileForm();
        resetBookingComposer();
        renderCustomerFilterButtons();
        renderSpecialistFilterButtons();
        renderSpecialistCalendarControls();

        const isCustomer = state.user.role === "CUSTOMER";
        const isSpecialist = state.user.role === "SPECIALIST";
        toggleElementVisibility(false, elements.directoryPanel);
        toggleElementVisibility(false, elements.anchorDirectory);
        toggleElementVisibility(false, elements.sideDirectory);
        toggleElementVisibility(isCustomer, elements.anchorCustomerDirectory);
        toggleElementVisibility(isCustomer, elements.anchorCustomerBookings);
        toggleElementVisibility(isCustomer, elements.sideCustomerDirectory);
        toggleElementVisibility(isCustomer, elements.sideCustomerBookings);
        toggleElementVisibility(isSpecialist, elements.anchorSpecialistDirectory);
        toggleElementVisibility(isSpecialist, elements.sideSpecialistDirectory);
        toggleElementVisibility(false, elements.customerPanel);
        toggleRoleVisibility(state.user.role === "SPECIALIST", elements.specialistPanel, elements.anchorSpecialist, elements.sideSpecialist);
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

    function toggleElementVisibility(show, element) {
        if (!element) {
            return;
        }
        element.classList.toggle("hidden", !show);
    }

    function roleSubtitle(role) {
        if (role === "CUSTOMER") {
            return "You are signed in to your customer profile center. Use the dedicated directory and booking pages for searching specialists and managing appointments.";
        }
        if (role === "SPECIALIST") {
            return "You are signed in as a specialist and can review your published timetable, create new slots, and manage booking requests.";
        }
        return "Manage booking workflows, specialist schedules, and category configuration in one place.";
    }

    async function onLogout() {
        await app.logout();
    }

    function renderProfileForm() {
        elements.profileFormFullName.value = state.user.fullName || "";
        elements.profileFormEmail.value = state.user.email || "";
        elements.profileFormPhone.value = state.user.phone || "";
    }

    async function onSaveProfile(event) {
        event.preventDefault();
        const form = event.currentTarget;

        await app.withFormLoading(form, "Saving...", async () => {
            state.user = await app.api("/api/users/me", {
                method: "PUT",
                body: JSON.stringify(app.formToObject(form))
            });
            renderSession();
            await refreshRoleWorkspace();
            app.showFormSuccess(form, "Profile updated successfully.");
        }).catch((error) => {
            app.showToast(error.message, "error", "toast");
        });
    }

    async function loadCategories() {
        try {
            state.categories = await app.api("/api/categories");
            elements.categoryCount.textContent = String(state.categories.length);
            renderCategoryOptions();
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

        const profileOptions = state.categories.map((category) =>
                `<option value="${category.id}">${app.escapeHtml(category.name)}</option>`
        ).join("");

        elements.searchCategory.innerHTML = searchOptions;
        elements.specialistProfileCategory.innerHTML = profileOptions || '<option value="">No categories available</option>';
        if (state.specialistProfile) {
            elements.specialistProfileCategory.value = String(state.specialistProfile.categoryId);
        }
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
            state.directorySpecialists = specialists;
            renderSpecialists(specialists);
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
                    <span class="badge">${app.escapeHtml(specialist.level)}</span>
                </div>
                <div class="meta-block">
                    <span>Specialist ID: <strong>${specialist.id}</strong></span>
                    <span>Base Fee: <strong>${app.formatCurrency(specialist.baseFee, specialist.feeCurrency)}</strong></span>
                    <span>Status: <span class="status-pill ${specialist.status}">${specialist.status}</span></span>
                </div>
                <p>${app.escapeHtml(specialist.bio || "No bio available")}</p>
                <div class="action-row">
                    <a class="secondary-link compact-link" href="/specialist-detail.html?id=${specialist.id}">View Profile & Availability</a>
                    <button class="ghost-button" type="button" data-action="prepare-booking" data-id="${specialist.id}">Open Booking Assistant</button>
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
        const specialist = state.directorySpecialists.find((item) => item.id === specialistId) || null;

        if (action === "prepare-booking") {
            if (!specialist) {
                app.showToast("Unable to load the selected specialist into the booking assistant.", "error", "toast");
                return;
            }

            await selectBookingSpecialist(specialist);
            app.showToast("Specialist loaded into the booking assistant. Select a time slot next.", "success", "toast");
        }
    }

    async function onCreateBooking(event) {
        event.preventDefault();
        const form = event.currentTarget;

        if (!state.selectedBookingSpecialist) {
            app.showToast("Please choose a specialist before creating a booking.", "error", "toast");
            return;
        }

        if (!state.selectedSlotId || !elements.bookingSlotId.value) {
            app.showToast("Please choose an available time slot before creating a booking.", "error", "toast");
            return;
        }

        const payload = app.formToObject(form);
        payload.specialistId = Number(payload.specialistId);
        payload.slotId = Number(payload.slotId);

        await app.withFormLoading(form, "Submitting...", async () => {
            await app.api("/api/bookings", {
                method: "POST",
                body: JSON.stringify(payload)
            });
            resetBookingComposer();
            await refreshCustomerWorkspace();
            await searchSpecialists();
            app.showFormSuccess(form, "Booking submitted and waiting for confirmation.");
        }).catch((error) => {
            app.showToast(error.message, "error", "toast");
        });
    }

    function onBookingSpecialistQueryInput(event) {
        const query = event.currentTarget.value.trim();
        window.clearTimeout(state.bookingSearchTimer);
        clearBookingSelection(true);

        if (!query) {
            state.bookingCandidates = [];
            renderBookingSuggestions();
            return;
        }

        state.bookingSearchTimer = window.setTimeout(() => {
            loadBookingSpecialistCandidates(query);
        }, 220);
    }

    async function loadBookingSpecialistCandidates(query) {
        const requestId = ++state.bookingSearchRequestId;
        const trimmedQuery = query.trim();

        if (!trimmedQuery) {
            state.bookingCandidates = [];
            renderBookingSuggestions();
            return;
        }

        try {
            const candidates = [];

            if (/^\d+$/.test(trimmedQuery)) {
                try {
                    const exact = await app.api(`/api/specialists/${Number(trimmedQuery)}`);
                    candidates.push(exact);
                } catch (error) {
                    // Ignore missing exact-match specialists and continue with keyword search.
                }
            }

            const keywordMatches = await app.api(`/api/specialists?keyword=${encodeURIComponent(trimmedQuery)}`);
            const unique = [];
            const seen = new Set();

            candidates.concat(keywordMatches).forEach((specialist) => {
                if (!specialist || seen.has(specialist.id)) {
                    return;
                }
                seen.add(specialist.id);
                unique.push(specialist);
            });

            if (requestId !== state.bookingSearchRequestId) {
                return;
            }

            state.bookingCandidates = unique.slice(0, 8);
            renderBookingSuggestions(trimmedQuery);
        } catch (error) {
            if (requestId !== state.bookingSearchRequestId) {
                return;
            }

            state.bookingCandidates = [];
            renderBookingSuggestions(trimmedQuery, error.message || "Unable to load specialist matches.");
        }
    }

    function renderBookingSuggestions(query = "", errorMessage = "") {
        if (!query && !state.bookingCandidates.length) {
            elements.bookingSpecialistSuggestions.className = "booking-lookup-list empty-state";
            elements.bookingSpecialistSuggestions.textContent = "No specialist search has been started yet";
            return;
        }

        if (errorMessage) {
            elements.bookingSpecialistSuggestions.className = "booking-lookup-list empty-state";
            elements.bookingSpecialistSuggestions.textContent = errorMessage;
            return;
        }

        if (!state.bookingCandidates.length) {
            elements.bookingSpecialistSuggestions.className = "booking-lookup-list empty-state";
            elements.bookingSpecialistSuggestions.textContent = `No specialists match "${query}".`;
            return;
        }

        elements.bookingSpecialistSuggestions.className = "booking-lookup-list";
        elements.bookingSpecialistSuggestions.innerHTML = state.bookingCandidates.map((specialist) => `
            <button
                type="button"
                class="booking-lookup-item ${state.selectedBookingSpecialist && state.selectedBookingSpecialist.id === specialist.id ? "active" : ""}"
                data-booking-specialist-id="${specialist.id}"
            >
                <div class="card-head">
                    <div>
                        <strong>${app.escapeHtml(specialist.fullName)}</strong>
                        <p>${app.escapeHtml(specialist.categoryName)}</p>
                    </div>
                    <span class="status-pill ${specialist.status}">${specialist.status}</span>
                </div>
                <div class="meta-block">
                    <span>Specialist ID: <strong>${specialist.id}</strong></span>
                    <span>Title / Certification: <strong>${app.escapeHtml(specialist.level)}</strong></span>
                    <span>Base Fee: <strong>${app.formatCurrency(specialist.baseFee, specialist.feeCurrency)}</strong></span>
                </div>
            </button>
        `).join("");
    }

    async function onBookingSuggestionAction(event) {
        const button = event.target.closest("[data-booking-specialist-id]");
        if (!button) {
            return;
        }

        const specialistId = Number(button.dataset.bookingSpecialistId);
        const specialist = state.bookingCandidates.find((item) => item.id === specialistId)
                || state.directorySpecialists.find((item) => item.id === specialistId);

        if (!specialist) {
            app.showToast("Unable to load the selected specialist.", "error", "toast");
            return;
        }

        await selectBookingSpecialist(specialist);
    }

    async function selectBookingSpecialist(specialist) {
        state.selectedBookingSpecialist = specialist;
        state.selectedSlotId = null;
        state.selectedBookingSlots = [];
        elements.bookingSpecialistId.value = String(specialist.id);
        elements.bookingSlotId.value = "";
        elements.bookingSpecialistQuery.value = `${specialist.fullName} (ID ${specialist.id})`;
        elements.bookingSlotBrowserTitle.textContent = `Available appointment times for ${specialist.fullName}`;
        renderBookingSuggestions(elements.bookingSpecialistQuery.value);
        renderBookingSpecialistSummary();
        renderBookingSlotSummary();
        updateBookingSubmitState();

        if (specialist.status !== "ACTIVE") {
            renderBookingAvailableSlots("This specialist is currently unavailable for new bookings.");
            return;
        }

        try {
            state.selectedBookingSlots = await app.api(`/api/slots/specialists/${specialist.id}?fromDate=${state.selectedSlotWindowStart}&days=7`);
            renderBookingAvailableSlots();
        } catch (error) {
            state.selectedBookingSlots = [];
            renderBookingAvailableSlots(error.message || "Unable to load appointment times.");
        }
    }

    function renderBookingSpecialistSummary() {
        if (!state.selectedBookingSpecialist) {
            elements.bookingSpecialistSummary.classList.add("hidden");
            elements.bookingSpecialistSummary.innerHTML = "";
            return;
        }

        const specialist = state.selectedBookingSpecialist;
        elements.bookingSpecialistSummary.classList.remove("hidden");
        elements.bookingSpecialistSummary.innerHTML = `
            <div class="card-head">
                <div>
                    <strong>${app.escapeHtml(specialist.fullName)}</strong>
                    <p>${app.escapeHtml(specialist.categoryName)}</p>
                </div>
                <span class="status-pill ${specialist.status}">${specialist.status}</span>
            </div>
            <div class="meta-block">
                <span>Specialist ID: <strong>${specialist.id}</strong></span>
                <span>Professional Title / Certification: <strong>${app.escapeHtml(specialist.level)}</strong></span>
                <span>Base Fee: <strong>${app.formatCurrency(specialist.baseFee, specialist.feeCurrency)}</strong></span>
                <span>Booking Eligibility: <strong>${specialist.status === "ACTIVE" ? "Specialist can accept bookings" : "Specialist is currently unavailable"}</strong></span>
            </div>
        `;
    }

    function renderBookingAvailableSlots(errorMessage = "") {
        if (!state.selectedBookingSpecialist) {
            elements.bookingAvailableSlots.className = "slot-list empty-state";
            elements.bookingAvailableSlots.textContent = "No specialist selected yet";
            return;
        }

        if (state.selectedBookingSpecialist.status !== "ACTIVE") {
            elements.bookingAvailableSlots.className = "slot-list empty-state";
            elements.bookingAvailableSlots.textContent = "This specialist is currently unavailable for new bookings.";
            return;
        }

        if (errorMessage) {
            elements.bookingAvailableSlots.className = "slot-list empty-state";
            elements.bookingAvailableSlots.textContent = errorMessage;
            return;
        }

        if (!state.selectedBookingSlots.length) {
            elements.bookingAvailableSlots.className = "slot-list empty-state";
            elements.bookingAvailableSlots.textContent = "No appointment times are published in the current 7-day window.";
            return;
        }

        elements.bookingAvailableSlots.className = "slot-list";
        elements.bookingAvailableSlots.innerHTML = state.selectedBookingSlots.map((slot) => `
            <article class="slot-item ${state.selectedSlotId === slot.id ? "selected" : ""} ${slot.status !== "AVAILABLE" ? "slot-muted" : ""}">
                <div class="slot-time">${app.formatDateTime(slot.startTime)} - ${app.formatDateTime(slot.endTime)}</div>
                <div class="meta-block">
                    <span>Time Slot ID: <strong>${slot.id}</strong></span>
                    <span>Status: <span class="status-pill ${slot.status}">${slot.status}</span></span>
                    <span>Estimated Price: <strong>${app.formatCurrency(estimateBookingPrice(state.selectedBookingSpecialist, slot), state.selectedBookingSpecialist.feeCurrency)}</strong></span>
                    <span>Can Book: <strong>${slot.status === "AVAILABLE" ? "Yes" : "No"}</strong></span>
                </div>
                <button
                    class="ghost-button"
                    type="button"
                    data-action="choose-booking-slot"
                    data-slot-id="${slot.id}"
                    ${slot.status !== "AVAILABLE" ? "disabled" : ""}
                >${slot.status === "AVAILABLE" ? "Choose This Appointment" : "Unavailable"}</button>
            </article>
        `).join("");
    }

    function onBookingSlotSelectionAction(event) {
        const button = event.target.closest("[data-action='choose-booking-slot']");
        if (!button) {
            return;
        }

        const slotId = Number(button.dataset.slotId);
        const slot = state.selectedBookingSlots.find((item) => item.id === slotId);

        if (!slot) {
            app.showToast("The selected appointment time could not be found.", "error", "toast");
            return;
        }

        applyBookingSlotSelection(slot);
        app.showToast(`Appointment time ${slot.id} selected.`, "success", "toast");
    }

    function applyBookingSlotSelection(slot) {
        state.selectedSlotId = slot.id;
        elements.bookingSlotId.value = String(slot.id);
        renderBookingAvailableSlots();
        renderBookingSlotSummary();
        updateBookingSubmitState();
    }

    function renderBookingSlotSummary() {
        if (!state.selectedBookingSpecialist || !state.selectedSlotId) {
            elements.bookingSlotSummary.classList.add("hidden");
            elements.bookingSlotSummary.innerHTML = "";
            return;
        }

        const slot = state.selectedBookingSlots.find((item) => item.id === state.selectedSlotId);

        if (!slot) {
            elements.bookingSlotSummary.classList.add("hidden");
            elements.bookingSlotSummary.innerHTML = "";
            return;
        }

        elements.bookingSlotSummary.classList.remove("hidden");
        elements.bookingSlotSummary.innerHTML = `
            <div class="card-head">
                <div>
                    <strong>Selected Appointment</strong>
                    <p>${app.escapeHtml(state.selectedBookingSpecialist.fullName)}</p>
                </div>
                <span class="status-pill ${slot.status}">${slot.status}</span>
            </div>
            <div class="meta-block">
                <span>Time Slot ID: <strong>${slot.id}</strong></span>
                <span>Time: <strong>${app.formatDateTime(slot.startTime)} - ${app.formatDateTime(slot.endTime)}</strong></span>
                <span>Estimated Price: <strong>${app.formatCurrency(estimateBookingPrice(state.selectedBookingSpecialist, slot), state.selectedBookingSpecialist.feeCurrency)}</strong></span>
                <span>Availability: <strong>${slot.status === "AVAILABLE" ? "Ready to book" : "Not available for booking"}</strong></span>
            </div>
        `;
    }

    function clearBookingSelection(preserveQuery = false) {
        state.selectedBookingSpecialist = null;
        state.selectedBookingSlots = [];
        state.selectedSlotId = null;
        elements.bookingSpecialistId.value = "";
        elements.bookingSlotId.value = "";
        elements.bookingSlotBrowserTitle.textContent = "Choose a specialist to load appointment times";

        if (!preserveQuery) {
            elements.bookingSpecialistQuery.value = "";
        }

        renderBookingSpecialistSummary();
        renderBookingAvailableSlots();
        renderBookingSlotSummary();
        updateBookingSubmitState();
    }

    function resetBookingComposer() {
        state.bookingCandidates = [];
        elements.bookingForm.reset();
        clearBookingSelection(false);
        renderBookingSuggestions();
    }

    function updateBookingSubmitState() {
        const submitButton = elements.bookingForm.querySelector("button[type='submit']");
        submitButton.disabled = !(
            state.selectedBookingSpecialist
            && state.selectedBookingSpecialist.status === "ACTIVE"
            && elements.bookingSpecialistId.value
            && elements.bookingSlotId.value
        );
    }

    function estimateBookingPrice(specialist, slot) {
        const minutes = Math.max(0, Math.round((new Date(slot.endTime) - new Date(slot.startTime)) / 60000));
        const hours = minutes / 60;
        let amount = Number(specialist.baseFee || 0) * hours;
        const day = new Date(slot.startTime).getDay();

        if (day === 0 || day === 6) {
            amount *= 1.15;
        }

        return Math.round(amount * 100) / 100;
    }

    async function refreshRoleWorkspace() {
        if (state.user.role === "SPECIALIST") {
            await refreshSpecialistWorkspace();
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
                    <span>Price: <strong>${app.formatCurrency(booking.price, booking.feeCurrency)}</strong></span>
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
            await app.withButtonLoading(button, "Cancelling...", async () => {
                const confirmation = await app.confirmAction({
                    title: "Cancel booking",
                    message: `Cancel booking #${bookingId}? The time slot will be released when possible.`,
                    confirmLabel: "Cancel Booking",
                    reasonLabel: "Cancellation Reason",
                    reasonPlaceholder: reason || "Optional note for this cancellation",
                    danger: true
                });
                if (!confirmation.confirmed) {
                    return;
                }

                await app.api(`/api/bookings/${bookingId}/cancel`, {
                    method: "POST",
                    body: JSON.stringify({ reason: confirmation.reason || reason })
                });
                await refreshCustomerWorkspace();
                await searchSpecialists();
                app.showToast("Booking cancelled successfully.", "success", "toast");
            }).catch((error) => {
                app.showToast(error.message, "error", "toast");
            });
            return;
        }

        if (button.dataset.action === "load-reschedule") {
            const specialistId = Number(button.dataset.specialistId);
            const windowStart = dateValueFromIso(button.dataset.startTime);
            await app.withButtonLoading(button, "Loading...", async () => {
                const slots = await app.api(`/api/slots/specialists/${specialistId}?status=AVAILABLE&fromDate=${windowStart}&days=7`);
                const select = document.getElementById(`reschedule-select-${bookingId}`);
                select.innerHTML = ['<option value="">Select a new time slot</option>']
                        .concat(slots.map((slot) =>
                                `<option value="${slot.id}">${slot.id} / ${app.formatDateTime(slot.startTime)}</option>`
                        ))
                        .join("");
                app.showToast("Available reschedule slots loaded.", "success", "toast");
            }).catch((error) => {
                app.showToast(error.message, "error", "toast");
            });
            return;
        }

        if (button.dataset.action === "reschedule-booking") {
            const select = document.getElementById(`reschedule-select-${bookingId}`);
            const newSlotId = Number(select.value);

            if (!newSlotId) {
                app.showToast("Please select a new time slot first.", "error", "toast");
                return;
            }

            await app.withButtonLoading(button, "Submitting...", async () => {
                const confirmation = await app.confirmAction({
                    title: "Reschedule booking",
                    message: `Submit booking #${bookingId} for the selected new time slot? It will return to pending confirmation.`,
                    confirmLabel: "Submit Reschedule"
                });
                if (!confirmation.confirmed) {
                    return;
                }

                await app.api(`/api/bookings/${bookingId}/reschedule`, {
                    method: "POST",
                    body: JSON.stringify({ newSlotId })
                });
                await refreshCustomerWorkspace();
                await searchSpecialists();
                app.showToast("Reschedule submitted. The booking is pending confirmation again.", "success", "toast");
            }).catch((error) => {
                app.showToast(error.message, "error", "toast");
            });
        }
    }

    async function refreshSpecialistWorkspace() {
        if (state.user.role !== "SPECIALIST") {
            return;
        }

        try {
            state.specialistProfile = await app.api("/api/specialists/me");
            renderSpecialistProfile();
            const slots = await app.api(
                    `/api/slots/specialists/${state.specialistProfile.id}?fromDate=${state.specialistCalendarStart}&days=${specialistCalendarDays()}`
            );
            renderSpecialistSlots(slots);
            const bookings = await app.api(buildSpecialistScheduleUrl());
            renderSpecialistSchedule(bookings);
            await refreshEarnings();
        } catch (error) {
            app.showToast(error.message, "error", "toast");
        }
    }

    function onSpecialistCalendarModeChange(event) {
        const button = event.target.closest("[data-specialist-calendar-mode]");
        if (!button || button.dataset.specialistCalendarMode === state.specialistCalendarMode) {
            return;
        }

        state.specialistCalendarMode = button.dataset.specialistCalendarMode;
        renderSpecialistCalendarControls();
        refreshSpecialistWorkspace();
    }

    function onSpecialistCalendarDateChange(event) {
        setSpecialistCalendarStart(event.currentTarget.value || todayDateValue());
    }

    function shiftSpecialistCalendar(direction) {
        const days = state.specialistCalendarMode === "DAY" ? direction : direction * 7;
        setSpecialistCalendarStart(addDays(state.specialistCalendarStart, days));
    }

    function setSpecialistCalendarStart(dateValue) {
        state.specialistCalendarStart = dateValue;
        renderSpecialistCalendarControls();
        refreshSpecialistWorkspace();
    }

    function specialistCalendarDays() {
        return state.specialistCalendarMode === "DAY" ? 1 : 7;
    }

    function renderSpecialistCalendarControls() {
        elements.specialistCalendarDate.value = state.specialistCalendarStart;
        elements.specialistCalendarMode.querySelectorAll("[data-specialist-calendar-mode]").forEach((button) => {
            button.classList.toggle("active", button.dataset.specialistCalendarMode === state.specialistCalendarMode);
        });
        elements.specialistCalendarCaption.textContent = state.specialistCalendarMode === "DAY"
                ? "Published time slots for the selected day, including reserved appointments."
                : "Published time slots for the selected week, including reserved appointments.";
    }

    function renderSpecialistProfile() {
        if (!state.specialistProfile) {
            elements.specialistProfile.innerHTML = '<div class="empty-state">No specialist profile is linked to the current account</div>';
            elements.specialistProfileForm.reset();
            return;
        }

        const profile = state.specialistProfile;
        elements.specialistProfile.innerHTML = `
            <div><span class="muted-label">Specialist ID</span><strong>${profile.id}</strong></div>
            <div><span class="muted-label">Name</span><strong>${app.escapeHtml(profile.fullName)}</strong></div>
            <div><span class="muted-label">Category</span><strong>${app.escapeHtml(profile.categoryName)}</strong></div>
            <div><span class="muted-label">Professional Title / Certification</span><strong>${app.escapeHtml(profile.level)}</strong></div>
            <div><span class="muted-label">Base Fee</span><strong>${app.formatCurrency(profile.baseFee, profile.feeCurrency)}</strong></div>
            <div><span class="muted-label">Status</span><strong>${profile.status}</strong></div>
            <div><span class="muted-label">Notes</span><strong>${app.escapeHtml(profile.bio || "No notes available")}</strong></div>
        `;
        elements.specialistProfileCategory.value = String(profile.categoryId);
        elements.specialistProfileLevel.value = profile.level || "";
        elements.specialistProfileBaseFee.value = String(profile.baseFee);
        elements.specialistProfileBio.value = profile.bio || "";
    }

    async function onSaveSpecialistProfile(event) {
        event.preventDefault();
        const form = event.currentTarget;
        const payload = app.formToObject(form);
        payload.categoryId = Number(payload.categoryId);
        payload.baseFee = Number(payload.baseFee);
        const feeChanged = state.specialistProfile
                && Number(state.specialistProfile.baseFee) !== payload.baseFee;

        if (feeChanged) {
            const confirmation = await app.confirmAction({
                title: "Update future booking rate",
                message: "Save the new base fee? It will apply to new or rescheduled bookings only. Existing booking prices will remain unchanged.",
                confirmLabel: "Update Rate"
            });
            if (!confirmation.confirmed) {
                return;
            }
        }

        app.clearFormErrors(form);
        await app.withFormLoading(form, "Saving...", async () => {
            state.specialistProfile = await app.api("/api/specialists/me", {
                method: "PUT",
                body: JSON.stringify(payload)
            });
            renderSpecialistProfile();
            app.showFormSuccess(
                    form,
                    feeChanged
                            ? "Professional profile updated. Existing booking prices remain unchanged; the new rate applies to new or rescheduled bookings."
                            : "Professional profile updated successfully."
            );
        }).catch((error) => {
            app.renderFormErrors(form, error, "Unable to save the professional profile. Please review the form.");
        });
    }

    function renderSpecialistSlots(slots) {
        if (!state.specialistProfile) {
            elements.specialistSlots.innerHTML = '<div class="empty-state">A specialist profile is required before timetable data can be shown.</div>';
            return;
        }

        const slotsByDay = slots.reduce((groups, slot) => {
            const key = localDateKey(new Date(slot.startTime));
            groups.set(key, (groups.get(key) || []).concat(slot));
            return groups;
        }, new Map());
        const days = Array.from({ length: specialistCalendarDays() }, (_, offset) => {
            const date = new Date(`${state.specialistCalendarStart}T00:00:00`);
            date.setDate(date.getDate() + offset);
            return date;
        });

        elements.specialistSlots.className = "slot-calendar";
        elements.specialistSlots.innerHTML = days.map((day) => {
            const key = localDateKey(day);
            const daySlots = (slotsByDay.get(key) || [])
                    .slice()
                    .sort((left, right) => new Date(left.startTime) - new Date(right.startTime));

            return `
                <article class="calendar-day ${daySlots.length ? "" : "calendar-day-empty"}">
                    <div class="calendar-day-head">
                        <strong>${formatCalendarDay(day)}</strong>
                        <span>${daySlots.length} slot${daySlots.length === 1 ? "" : "s"}</span>
                    </div>
                    <div class="calendar-slot-stack">
                        ${daySlots.length ? daySlots.map((slot) => `
                            <div class="calendar-slot ${slot.status}">
                                <span>${formatTimeRange(slot.startTime, slot.endTime)}</span>
                                <span class="status-pill ${slot.status}">${slot.status}</span>
                                <small>ID ${slot.id} / ${specialistSlotAvailability(slot.status)}</small>
                            </div>
                        `).join("") : '<p class="calendar-empty-note">No published slots</p>'}
                    </div>
                </article>
            `;
        }).join("");
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

        await app.withFormLoading(form, "Creating...", async () => {
            await app.api(`/api/slots/specialists/${state.specialistProfile.id}`, {
                method: "POST",
                body: JSON.stringify(payload)
            });
            form.reset();
            app.clearFormErrors(form);
            await refreshSpecialistWorkspace();
            app.showFormSuccess(form, "Time slot created successfully.");
        }).catch((error) => {
            app.renderFormErrors(form, error);
        });
    }

    async function onCreateRecurringSlots(event) {
        event.preventDefault();
        if (!state.specialistProfile) {
            app.showToast("The current account is not linked to a specialist profile.", "error", "toast");
            return;
        }

        const form = event.currentTarget;
        const payload = app.formToObject(form);
        if (payload.endTime <= payload.startTime) {
            app.setFieldError(form, "endTime", "End time must be later than the start time.");
            app.setFormError(form, "Please correct the highlighted fields and try again.");
            return;
        }

        app.clearFormErrors(form);
        await app.withFormLoading(form, "Creating...", async () => {
            const result = await app.api(`/api/slots/specialists/${state.specialistProfile.id}/recurring`, {
                method: "POST",
                body: JSON.stringify(payload)
            });
            await refreshSpecialistWorkspace();
            app.showFormSuccess(
                    form,
                    `${result.createdCount} weekly slots created; ${result.skippedCount} conflicts skipped; ${result.replacedCount} available slots replaced.`
            );
        }).catch((error) => {
            app.renderFormErrors(form, error, "Unable to create recurring availability.");
        });
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
                    <span>Price: <strong>${app.formatCurrency(booking.price, booking.feeCurrency)}</strong></span>
                    <span>Scheduled Length: <strong>${formatDuration(booking.startTime, booking.endTime)}</strong></span>
                    <span>Notes: ${app.escapeHtml(booking.notes || "None")}</span>
                </div>
                <div class="action-grid">
                    <button class="ghost-button" type="button" data-action="view-consultation-details" data-booking-id="${booking.id}">View Details</button>
                    ${booking.status === "PENDING" ? `<input id="specialist-reason-${booking.id}" type="text" minlength="5" maxlength="255" placeholder="Rejection reason (at least 5 characters)">` : ""}
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

        if (button.dataset.action === "view-consultation-details") {
            await app.withButtonLoading(button, "Loading...", async () => {
                const booking = await app.api(`/api/bookings/details/${bookingId}`);
                state.specialistSelectedConsultationId = bookingId;
                renderSpecialistConsultationDetail(booking);
            }).catch((error) => {
                app.showToast(error.message, "error", "toast");
            });
            return;
        }

        if (button.dataset.action === "confirm-booking") {
            await app.withButtonLoading(button, "Confirming...", async () => {
                const confirmation = await app.confirmAction({
                    title: "Confirm booking",
                    message: `Confirm booking #${bookingId} and keep the time slot reserved?`,
                    confirmLabel: "Confirm Booking"
                });
                if (!confirmation.confirmed) {
                    return;
                }

                await app.api(`/api/bookings/${bookingId}/confirm`, { method: "POST" });
                await refreshSpecialistWorkspace();
                app.showToast("Booking confirmed successfully.", "success", "toast");
            }).catch((error) => {
                app.showToast(error.message, "error", "toast");
            });
            return;
        }

        if (button.dataset.action === "reject-booking") {
            const reason = document.getElementById(`specialist-reason-${bookingId}`).value;
            await app.withButtonLoading(button, "Rejecting...", async () => {
                const confirmation = await app.confirmAction({
                    title: "Reject booking",
                    message: `Reject booking #${bookingId} and release the time slot?`,
                    confirmLabel: "Reject Booking",
                    reasonLabel: "Rejection Reason",
                    reasonPlaceholder: reason || "Reason required (at least 5 characters)",
                    danger: true
                });
                if (!confirmation.confirmed) {
                    return;
                }

                const rejectionReason = confirmation.reason || reason;
                if (rejectionReason.trim().length < 5) {
                    app.showToast("Please provide a rejection reason of at least 5 characters.", "error", "toast");
                    return;
                }
                await app.api(`/api/bookings/${bookingId}/reject`, {
                    method: "POST",
                    body: JSON.stringify({ reason: rejectionReason })
                });
                await refreshSpecialistWorkspace();
                app.showToast("Booking rejected successfully.", "success", "toast");
            }).catch((error) => {
                app.showToast(error.message, "error", "toast");
            });
            return;
        }

        if (button.dataset.action === "complete-booking") {
            await app.withButtonLoading(button, "Completing...", async () => {
                const confirmation = await app.confirmAction({
                    title: "Complete booking",
                    message: `Mark booking #${bookingId} as completed? This will close the linked time slot.`,
                    confirmLabel: "Mark Completed"
                });
                if (!confirmation.confirmed) {
                    return;
                }

                await app.api(`/api/bookings/${bookingId}/complete`, { method: "POST" });
                await refreshSpecialistWorkspace();
                app.showToast("Booking marked as completed.", "success", "toast");
            }).catch((error) => {
                app.showToast(error.message, "error", "toast");
            });
        }
    }

    function renderSpecialistConsultationDetail(booking) {
        elements.specialistConsultationDetail.classList.remove("hidden");
        elements.specialistConsultationDetail.innerHTML = `
            <div class="card-head">
                <div>
                    <p class="eyebrow">Consultation Detail</p>
                    <h3>${app.escapeHtml(booking.topic)}</h3>
                </div>
                <span class="status-pill ${booking.status}">${booking.status}</span>
            </div>
            <div class="booking-detail-grid">
                <span>Booking ID <strong>${booking.id}</strong></span>
                <span>Customer <strong>${app.escapeHtml(booking.customerName)}</strong></span>
                <span>Start Time <strong>${app.formatDateTime(booking.startTime)}</strong></span>
                <span>End Time <strong>${app.formatDateTime(booking.endTime)}</strong></span>
                <span>Consultation Fee <strong>${app.formatCurrency(booking.price, booking.feeCurrency)}</strong></span>
                <span>Status <strong>${booking.status}</strong></span>
            </div>
            <p class="detail-note"><strong>Notes:</strong> ${app.escapeHtml(booking.notes || "None")}</p>
            <div class="card-actions">
                <button class="ghost-button" type="button" data-close-specialist-detail>Back to Consultation List</button>
            </div>
        `;
    }

    function onSpecialistDetailAction(event) {
        if (event.target.closest("[data-close-specialist-detail]")) {
            closeSpecialistConsultationDetail();
        }
    }

    function closeSpecialistConsultationDetail() {
        state.specialistSelectedConsultationId = null;
        elements.specialistConsultationDetail.classList.add("hidden");
        elements.specialistConsultationDetail.innerHTML = "";
    }

    async function refreshNotifications() {
        try {
            state.notifications = await app.api("/api/notifications/me");
            renderNotifications();
        } catch (error) {
            elements.notificationList.innerHTML = '<div class="empty-state">Unable to load notifications.</div>';
        }
    }

    function renderNotifications() {
        if (!state.notifications.length) {
            elements.notificationList.innerHTML = '<div class="empty-state">No notifications have been received.</div>';
            return;
        }
        elements.notificationList.innerHTML = state.notifications.map((notification) => {
            const isOpen = state.openNotificationIds.has(notification.id);
            return `
            <article class="notification-item ${notification.read ? "" : "unread"}">
                <div class="card-head">
                    <strong>${app.escapeHtml(notification.title)}</strong>
                    <span class="status-pill ${notification.read ? "CLOSED" : "ACTIVE"}">${notification.read ? "READ" : "NEW"}</span>
                </div>
                ${isOpen ? `<p>${app.escapeHtml(notification.message)}</p>` : ""}
                <div class="card-actions">
                    <span class="muted-label">${app.formatDateTime(notification.createdAt)}</span>
                    <button class="ghost-button" type="button" data-notification-open="${notification.id}">${isOpen ? "Close Message" : "Open Message"}</button>
                </div>
            </article>
        `;
        }).join("");
    }

    elements.notificationList.addEventListener("click", async (event) => {
        const button = event.target.closest("[data-notification-open]");
        if (!button) {
            return;
        }
        const notificationId = Number(button.dataset.notificationOpen);
        if (state.openNotificationIds.has(notificationId)) {
            state.openNotificationIds.delete(notificationId);
            renderNotifications();
            return;
        }
        state.openNotificationIds.add(notificationId);
        const notification = state.notifications.find((item) => item.id === notificationId);
        if (!notification || notification.read) {
            renderNotifications();
            return;
        }
        await app.withButtonLoading(button, "Opening...", async () => {
            const updated = await app.api(`/api/notifications/${notificationId}/read`, { method: "POST" });
            state.notifications = state.notifications.map((item) => item.id === notificationId ? updated : item);
            renderNotifications();
        }).catch((error) => app.showToast(error.message, "error", "toast"));
    });

    async function onFilterEarnings(event) {
        event.preventDefault();
        await refreshEarnings();
    }

    async function refreshEarnings() {
        const values = app.formToObject(elements.earningsFilterForm);
        const url = app.buildQueryUrl("/api/reports/my-earnings", values);
        const earnings = await app.api(url);
        state.earningsEntries = earnings.entries;
        elements.specialistEarningsDetail.classList.add("hidden");
        elements.specialistEarningsDetail.innerHTML = "";
        elements.specialistEarnings.innerHTML = `
            <div class="earnings-total">${app.formatCurrency(earnings.totalEarnings, earnings.currency)}</div>
            ${earnings.entries.length ? earnings.entries.map((entry, index) => `
                <button class="earnings-entry" type="button" data-earnings-index="${index}">
                    <strong>${app.escapeHtml(entry.topic)}</strong>
                    <span>${app.escapeHtml(entry.customerName)} / ${app.formatDateTime(entry.startTime)}</span>
                    <span>${app.formatCurrency(entry.amount, entry.currency)}</span>
                </button>
            `).join("") : '<div class="empty-state">No completed consultation income in this date range.</div>'}
        `;
    }

    function onEarningsEntryClick(event) {
        const button = event.target.closest("[data-earnings-index]");
        if (!button) {
            return;
        }
        const entry = state.earningsEntries[Number(button.dataset.earningsIndex)];
        if (!entry) {
            return;
        }
        elements.specialistEarningsDetail.classList.remove("hidden");
        elements.specialistEarningsDetail.innerHTML = `
            <p class="eyebrow">Revenue Detail</p>
            <h4>${app.escapeHtml(entry.topic)}</h4>
            <div class="booking-detail-grid">
                <span>Customer <strong>${app.escapeHtml(entry.customerName)}</strong></span>
                <span>Start Time <strong>${app.formatDateTime(entry.startTime)}</strong></span>
                <span>End Time <strong>${app.formatDateTime(entry.endTime)}</strong></span>
                <span>Duration <strong>${entry.durationMinutes} minutes</strong></span>
                <span>Unit Price <strong>${app.formatCurrency(entry.unitPrice, entry.currency)} / hour</strong></span>
                <span>Total Revenue <strong>${app.formatCurrency(entry.amount, entry.currency)}</strong></span>
            </div>
            <p class="detail-note">Pricing multiplier: ${entry.pricingMultiplier}. Revenue includes completed consultations only.</p>
        `;
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
        closeSpecialistConsultationDetail();
        renderSpecialistFilterButtons();
        refreshSpecialistWorkspace();
    }

    function onFilterSpecialistConsultations(event) {
        event.preventDefault();
        closeSpecialistConsultationDetail();
        refreshSpecialistWorkspace();
    }

    function clearSpecialistConsultationDates() {
        elements.specialistConsultationFilterForm.reset();
        closeSpecialistConsultationDetail();
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

    function buildCustomerBookingsUrl() {
        return app.buildQueryUrl("/api/bookings/me", {
            status: state.customerStatusFilter !== "ALL" ? state.customerStatusFilter : null
        });
    }

    function buildSpecialistScheduleUrl() {
        const dates = app.formToObject(elements.specialistConsultationFilterForm);
        return app.buildQueryUrl("/api/bookings/schedule", {
            status: state.specialistStatusFilter !== "ALL" ? state.specialistStatusFilter : null,
            fromDate: dates.fromDate || null,
            toDate: dates.toDate || null
        });
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

    function specialistSlotAvailability(status) {
        if (status === "AVAILABLE") {
            return "Open for booking";
        }
        if (status === "RESERVED") {
            return "Reserved by an appointment";
        }
        return "Closed";
    }

    function specialistSlotStatusNote(status) {
        if (status === "AVAILABLE") {
            return "This slot is visible to customers and can still be booked.";
        }
        if (status === "RESERVED") {
            return "This slot is currently tied to an active booking request or confirmed appointment.";
        }
        return "This slot is no longer available for booking.";
    }

    function localDateKey(date) {
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, "0");
        const day = String(date.getDate()).padStart(2, "0");
        return `${year}-${month}-${day}`;
    }

    function formatCalendarDay(date) {
        return new Intl.DateTimeFormat("en-US", {
            weekday: "short",
            month: "short",
            day: "2-digit"
        }).format(date);
    }

    function formatTimeRange(startTime, endTime) {
        const formatter = new Intl.DateTimeFormat("en-US", {
            hour: "2-digit",
            minute: "2-digit"
        });
        return `${formatter.format(new Date(startTime))} - ${formatter.format(new Date(endTime))}`;
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
        return localDateKey(new Date());
    }

    function addDays(value, days) {
        const date = new Date(`${value}T00:00:00`);
        date.setDate(date.getDate() + days);
        return localDateKey(date);
    }

    function dateValueFromIso(value) {
        return value ? localDateKey(new Date(value)) : todayDateValue();
    }

});
