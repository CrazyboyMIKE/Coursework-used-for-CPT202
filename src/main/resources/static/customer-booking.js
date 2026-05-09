document.addEventListener("DOMContentLoaded", async () => {
    const app = window.ProjectApp;
    const currentUser = await app.requireAuth("/login.html");

    if (!currentUser) {
        return;
    }

    if (currentUser.role !== "CUSTOMER") {
        app.queueFlash("Customer access is required for the booking center.", "error");
        window.location.replace(app.workspacePathForRole(currentUser.role));
        return;
    }

    const params = new URLSearchParams(window.location.search);
    const initialSpecialistId = Number(params.get("specialistId"));

    const state = {
        currentUser,
        bookingCandidates: [],
        selectedBookingSpecialist: null,
        selectedBookingSlots: [],
        selectedSlotId: null,
        selectedSlotWindowStart: todayDateValue(),
        customerStatusFilter: "ALL",
        bookingSearchRequestId: 0,
        bookingSearchTimer: null
    };

    const elements = {
        headerFullName: document.getElementById("header-full-name"),
        headerRole: document.getElementById("header-role"),
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
        customerBookings: document.getElementById("customer-bookings")
    };

    app.consumeFlash("toast");
    renderHeader();
    bindEvents();
    resetBookingComposer();
    renderCustomerFilterButtons();
    await refreshCustomerWorkspace();

    if (initialSpecialistId) {
        await prefillSpecialist(initialSpecialistId);
    }

    function bindEvents() {
        document.getElementById("logout-button").addEventListener("click", onLogout);
        document.getElementById("refresh-customer-bookings").addEventListener("click", refreshCustomerWorkspace);
        elements.bookingForm.addEventListener("submit", onCreateBooking);
        elements.bookingSpecialistQuery.addEventListener("input", onBookingSpecialistQueryInput);
        elements.bookingSpecialistSuggestions.addEventListener("click", onBookingSuggestionAction);
        elements.bookingAvailableSlots.addEventListener("click", onBookingSlotSelectionAction);
        elements.customerFilterBar.addEventListener("click", onCustomerFilterChange);
        elements.customerBookings.addEventListener("click", onCustomerBookingAction);
    }

    function renderHeader() {
        elements.headerFullName.textContent = state.currentUser.fullName;
        elements.headerRole.textContent = state.currentUser.role;
        elements.headerRole.className = `role-pill ${state.currentUser.role}`;
    }

    async function onLogout() {
        await app.logout();
    }

    async function prefillSpecialist(specialistId) {
        try {
            const specialist = await app.api(`/api/specialists/${specialistId}`);
            state.bookingCandidates = [specialist];
            renderBookingSuggestions(String(specialistId));
            await selectBookingSpecialist(specialist);
        } catch (error) {
            app.showToast(error.message || "Unable to prefill the selected specialist.", "error", "toast");
        }
    }

    async function onCreateBooking(event) {
        event.preventDefault();
        const form = event.currentTarget;
        app.clearFormErrors(form);

        if (!state.selectedBookingSpecialist) {
            app.setFormError(form, "Please choose a specialist before creating a booking.");
            return;
        }

        if (!state.selectedSlotId || !elements.bookingSlotId.value) {
            app.setFormError(form, "Please choose an available time slot before creating a booking.");
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
            app.showFormSuccess(form, "Booking submitted and waiting for confirmation.");
        }).catch((error) => {
            app.renderFormErrors(form, error, "Unable to submit the booking. Please review the form and try again.");
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
                    // Ignore missing exact matches and continue with keyword search.
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
        const specialist = state.bookingCandidates.find((item) => item.id === specialistId);

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

    async function refreshCustomerWorkspace() {
        elements.customerBookings.innerHTML = '<div class="empty-state loading-state">Loading booking history...</div>';
        try {
            const bookings = await app.api(buildCustomerBookingsUrl());
            renderCustomerBookings(bookings);
        } catch (error) {
            elements.customerBookings.innerHTML = `
                <div class="empty-state directory-empty">
                    <strong>Unable to load booking history</strong>
                    <span>${app.escapeHtml(app.friendlyErrorMessage(error, "Please refresh bookings again."))}</span>
                </div>
            `;
            app.showToast(app.friendlyErrorMessage(error), "error", "toast");
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
                app.showToast("Reschedule submitted. The booking is pending confirmation again.", "success", "toast");
            }).catch((error) => {
                app.showToast(error.message, "error", "toast");
            });
        }
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

    function renderCustomerFilterButtons() {
        elements.customerFilterBar.querySelectorAll("[data-customer-filter]").forEach((button) => {
            button.classList.toggle("active", button.dataset.customerFilter === state.customerStatusFilter);
        });
    }

    function buildCustomerBookingsUrl() {
        return app.buildQueryUrl("/api/bookings/me", {
            status: state.customerStatusFilter !== "ALL" ? state.customerStatusFilter : null
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

    function customerEmptyMessage() {
        return state.customerStatusFilter === "ALL"
                ? "No appointments were found for the current account."
                : `No appointments were found with the status ${state.customerStatusFilter}.`;
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

    function hoursUntil(dateTime) {
        return (new Date(dateTime).getTime() - Date.now()) / 3600000;
    }

    function todayDateValue() {
        return new Date().toISOString().slice(0, 10);
    }

    function dateValueFromIso(value) {
        return value ? new Date(value).toISOString().slice(0, 10) : todayDateValue();
    }
});
