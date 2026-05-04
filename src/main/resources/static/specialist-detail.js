document.addEventListener("DOMContentLoaded", async () => {
    const app = window.ProjectApp;
    const params = new URLSearchParams(window.location.search);
    const specialistId = Number(params.get("id"));

    const elements = {
        title: document.getElementById("specialist-detail-title"),
        state: document.getElementById("specialist-detail-state"),
        meta: document.getElementById("specialist-detail-meta"),
        notice: document.getElementById("specialist-detail-notice"),
        actions: document.getElementById("specialist-detail-actions"),
        slots: document.getElementById("specialist-detail-slots")
    };

    if (!specialistId) {
        renderMissing("No specialist ID was provided in the page URL.");
        return;
    }

    try {
        const specialist = await app.api(`/api/specialists/${specialistId}`);
        renderSpecialist(specialist);
        await loadAvailableSlots(specialist.id);
    } catch (error) {
        renderMissing(error.message || "Unable to load the specialist profile.");
    }

    async function loadAvailableSlots(id) {
        try {
            const slots = await app.api(`/api/slots/specialists/${id}?status=AVAILABLE`);

            if (!slots.length) {
                elements.slots.innerHTML = '<div class="empty-state">No upcoming available time slots are published for this specialist.</div>';
                return;
            }

            elements.slots.innerHTML = slots.map((slot) => `
                <article class="slot-item">
                    <div class="slot-time">${app.formatDateTime(slot.startTime)} - ${app.formatDateTime(slot.endTime)}</div>
                    <div class="meta-block">
                        <span>Time Slot ID: <strong>${slot.id}</strong></span>
                        <span>Status: <span class="status-pill ${slot.status}">${slot.status}</span></span>
                    </div>
                </article>
            `).join("");
        } catch (error) {
            elements.slots.innerHTML = `<div class="empty-state">${app.escapeHtml(error.message || "Unable to load time slots.")}</div>`;
        }
    }

    function renderSpecialist(specialist) {
        elements.title.textContent = specialist.fullName;
        elements.state.classList.add("hidden");
        elements.meta.classList.remove("hidden");
        elements.meta.innerHTML = `
            <div><span class="muted-label">Specialist ID</span><strong>${specialist.id}</strong></div>
            <div><span class="muted-label">Category</span><strong>${app.escapeHtml(specialist.categoryName)}</strong></div>
            <div><span class="muted-label">Professional Title / Certification</span><strong>${app.escapeHtml(specialist.level)}</strong></div>
            <div><span class="muted-label">Base Fee</span><strong>${app.formatCurrency(specialist.baseFee, specialist.feeCurrency)}</strong></div>
            <div><span class="muted-label">Status</span><strong>${specialist.status}</strong></div>
            <div><span class="muted-label">Notes</span><strong>${app.escapeHtml(specialist.bio || "No notes have been published yet.")}</strong></div>
        `;

        elements.actions.classList.remove("hidden");
        elements.actions.innerHTML = specialist.status === "ACTIVE"
                ? `<a class="button-link" href="/customer-booking.html?specialistId=${specialist.id}">Book This Specialist</a>`
                : `<a class="secondary-link" href="/customer-directory.html">Back to Specialist Directory</a>`;

        const notices = [];
        if (specialist.status !== "ACTIVE") {
            notices.push("This specialist is currently unavailable for new bookings.");
        }
        if (!specialist.bio) {
            notices.push("This profile is incomplete because the public bio has not been provided yet.");
        }

        if (!notices.length) {
            elements.notice.classList.add("hidden");
            elements.notice.textContent = "";
            return;
        }

        elements.notice.classList.remove("hidden");
        elements.notice.innerHTML = notices.map((notice) => `<p>${app.escapeHtml(notice)}</p>`).join("");
    }

    function renderMissing(message) {
        elements.title.textContent = "Specialist profile unavailable";
        elements.state.textContent = message;
        elements.meta.classList.add("hidden");
        elements.notice.classList.add("hidden");
        elements.actions.classList.add("hidden");
        elements.slots.innerHTML = '<div class="empty-state">Time slot data is unavailable because the profile could not be loaded.</div>';
    }
});
