document.addEventListener("DOMContentLoaded", async () => {
    const app = window.ProjectApp;
    const currentUser = await app.requireAuth("/login.html");

    if (!currentUser) {
        return;
    }

    if (currentUser.role !== "CUSTOMER") {
        app.queueFlash("Customer access is required for the specialist directory.", "error");
        window.location.replace(app.workspacePathForRole(currentUser.role));
        return;
    }

    const state = {
        currentUser,
        categories: [],
        specialists: []
    };

    const elements = {
        headerFullName: document.getElementById("header-full-name"),
        headerRole: document.getElementById("header-role"),
        searchCategory: document.getElementById("search-category"),
        specialistResults: document.getElementById("specialist-results")
    };

    app.consumeFlash("toast");
    renderHeader();
    bindEvents();
    await loadCategories();
    await searchSpecialists();

    function bindEvents() {
        document.getElementById("logout-button").addEventListener("click", onLogout);
        document.getElementById("specialist-search-form").addEventListener("submit", onSearchSpecialists);
    }

    function renderHeader() {
        elements.headerFullName.textContent = state.currentUser.fullName;
        elements.headerRole.textContent = state.currentUser.role;
        elements.headerRole.className = `role-pill ${state.currentUser.role}`;
    }

    async function onLogout() {
        await app.logout();
    }

    async function loadCategories() {
        try {
            state.categories = await app.api("/api/categories");
            elements.searchCategory.innerHTML = ['<option value="">All Categories</option>']
                    .concat(state.categories.map((category) =>
                            `<option value="${category.id}">${app.escapeHtml(category.name)}</option>`
                    ))
                    .join("");
        } catch (error) {
            app.showToast(error.message, "error", "toast");
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
            state.specialists = await app.api(url);
            renderSpecialists();
        } catch (error) {
            app.showToast(error.message, "error", "toast");
        }
    }

    function renderSpecialists() {
        if (!state.specialists.length) {
            elements.specialistResults.innerHTML = '<div class="empty-state">No specialists match the current filters.</div>';
            return;
        }

        elements.specialistResults.innerHTML = state.specialists.map((specialist) => `
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
                    <a class="button-link compact-link" href="/customer-booking.html?specialistId=${specialist.id}">Book This Specialist</a>
                </div>
            </article>
        `).join("");
    }
});
