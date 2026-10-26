"use strict";

// Medicine search page for students and admins (read-only).

(function () {
  if (!Auth.requireRole(["STUDENT", "ADMIN"])) return;
  Auth.setupNav();

  const role = Auth.getRole();
  const back = UI.$("backLink");
  back.href = Auth.homeFor(role);
  back.textContent = role === "ADMIN" ? "Admin dashboard" : "My dashboard";
  if (role === "ADMIN") {
    UI.$("manageLink").hidden = false;
  }

  const search = UI.$("search");
  const category = UI.$("category");
  const availableOnly = UI.$("availableOnly");
  const body = UI.$("medsBody");

  async function loadCategories() {
    try {
      const list = await api.get("/medicines/categories");
      list.forEach((name) => {
        const option = UI.el("option", null, name);
        option.value = name;
        category.appendChild(option);
      });
    } catch (err) {
      UI.showMessage("medMsg", err.message, "error");
    }
  }

  async function loadMedicines() {
    UI.clear(body);
    UI.clearMessage("medMsg");
    try {
      const list = await api.get("/medicines" + api.qs({
        search: search.value.trim(),
        category: category.value,
        availableOnly: availableOnly.checked ? "true" : "false",
      }));
      if (list.length === 0) {
        UI.emptyRow(body, 5, "No medicines match your search.");
        return;
      }
      list.forEach((m) => {
        const nameBox = UI.el("div");
        nameBox.appendChild(UI.el("strong", null, m.name));
        if (m.description) {
          nameBox.appendChild(UI.el("div", "muted small", m.description));
        }
        body.appendChild(UI.row([
          nameBox,
          m.category,
          m.quantity + (m.unit ? " " + m.unit : ""),
          UI.fmtDate(m.expiryDate),
          UI.medicineBadge(m.status),
        ]));
      });
    } catch (err) {
      UI.showMessage("medMsg", err.message, "error");
    }
  }

  let timer = null;
  search.addEventListener("input", () => {
    clearTimeout(timer);
    timer = setTimeout(loadMedicines, 300);
  });
  category.addEventListener("change", loadMedicines);
  availableOnly.addEventListener("change", loadMedicines);
  UI.$("searchForm").addEventListener("submit", (event) => {
    event.preventDefault();
    loadMedicines();
  });

  loadCategories();
  loadMedicines();
})();
