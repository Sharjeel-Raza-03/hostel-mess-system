"use strict";

// Admin medicine inventory: add, edit, update stock, delete, and alerts.

(function () {
  if (!Auth.requireRole("ADMIN")) return;
  Auth.setupNav();

  const form = UI.$("medForm");
  const fields = {
    id: UI.$("medId"),
    name: UI.$("medName"),
    category: UI.$("medCategory"),
    description: UI.$("medDescription"),
    quantity: UI.$("medQuantity"),
    unit: UI.$("medUnit"),
    expiry: UI.$("medExpiry"),
  };
  const search = UI.$("search");
  const body = UI.$("medsBody");

  function resetForm() {
    form.reset();
    fields.id.value = "";
    UI.$("formTitle").textContent = "Add a medicine";
    UI.$("saveBtn").textContent = "Add medicine";
    UI.$("cancelEditBtn").hidden = true;
  }

  function startEdit(m) {
    UI.clearMessage("formMsg");
    fields.id.value = m.id;
    fields.name.value = m.name;
    fields.category.value = m.category || "";
    fields.description.value = m.description || "";
    fields.quantity.value = m.quantity;
    fields.unit.value = m.unit || "";
    fields.expiry.value = m.expiryDate;
    UI.$("formTitle").textContent = "Edit medicine";
    UI.$("saveBtn").textContent = "Save changes";
    UI.$("cancelEditBtn").hidden = false;
    form.scrollIntoView({ behavior: "smooth", block: "start" });
  }

  async function loadCategories() {
    try {
      const list = await api.get("/medicines/categories");
      const datalist = UI.$("categoryList");
      UI.clear(datalist);
      list.forEach((name) => {
        const option = UI.el("option");
        option.value = name;
        datalist.appendChild(option);
      });
    } catch (err) {
      // The list is only a convenience for typing, so ignore errors here.
    }
  }

  function medRow(m) {
    const nameBox = UI.el("div");
    nameBox.appendChild(UI.el("strong", null, m.name));
    if (m.description) {
      nameBox.appendChild(UI.el("div", "muted small", m.description));
    }

    const qty = UI.el("input", "qty-input");
    qty.type = "number";
    qty.min = "0";
    qty.step = "1";
    qty.value = m.quantity;
    const update = UI.el("button", "btn btn-small btn-outline", "Update");
    update.type = "button";
    update.addEventListener("click", () => updateStock(m, qty, update));
    const stock = UI.el("div", "stock-edit");
    stock.append(qty, update);
    if (m.unit) {
      stock.appendChild(UI.el("span", "muted small", m.unit));
    }

    const edit = UI.el("button", "btn btn-small btn-outline", "Edit");
    edit.type = "button";
    edit.addEventListener("click", () => startEdit(m));
    const remove = UI.el("button", "btn btn-small btn-danger", "Delete");
    remove.type = "button";
    remove.addEventListener("click", () => deleteMedicine(m, remove));
    const actions = UI.el("div", "actions");
    actions.append(edit, remove);

    return UI.row([nameBox, m.category, stock, UI.fmtDate(m.expiryDate), UI.medicineBadge(m.status), actions]);
  }

  async function loadMedicines() {
    UI.clear(body);
    try {
      const list = await api.get("/medicines" + api.qs({ search: search.value.trim() }));
      if (list.length === 0) {
        UI.emptyRow(body, 6, "No medicines found.");
        return;
      }
      list.forEach((m) => body.appendChild(medRow(m)));
    } catch (err) {
      UI.showMessage("tableMsg", err.message, "error");
    }
  }

  async function loadAlerts() {
    const alertsBody = UI.$("alertsBody");
    UI.clear(alertsBody);
    try {
      const list = await api.get("/admin/medicines/alerts");
      if (list.length === 0) {
        UI.emptyRow(alertsBody, 4, "No alerts. All medicines are in good shape.");
        return;
      }
      list.forEach((m) => {
        alertsBody.appendChild(UI.row([
          m.name,
          m.quantity + (m.unit ? " " + m.unit : ""),
          UI.fmtDate(m.expiryDate),
          UI.medicineBadge(m.status),
        ]));
      });
    } catch (err) {
      UI.showMessage("alertMsg", err.message, "error");
    }
  }

  async function refreshAll() {
    await Promise.all([loadMedicines(), loadAlerts(), loadCategories()]);
  }

  async function updateStock(m, input, button) {
    UI.clearMessage("tableMsg");
    const quantity = parseInt(input.value, 10);
    if (Number.isNaN(quantity) || quantity < 0) {
      UI.showMessage("tableMsg", "Quantity must be 0 or more.", "error");
      return;
    }
    button.disabled = true;
    try {
      await api.put("/admin/medicines/" + m.id + "/stock", { quantity: quantity });
      await refreshAll();
      UI.showMessage("tableMsg", "Stock updated for " + m.name + ".", "success");
    } catch (err) {
      UI.showMessage("tableMsg", err.message, "error");
      button.disabled = false;
    }
  }

  async function deleteMedicine(m, button) {
    if (!window.confirm("Delete " + m.name + " (expires " + UI.fmtDate(m.expiryDate) + ")?")) return;
    UI.clearMessage("tableMsg");
    button.disabled = true;
    try {
      await api.del("/admin/medicines/" + m.id);
      await refreshAll();
      UI.showMessage("tableMsg", m.name + " was deleted.", "success");
    } catch (err) {
      UI.showMessage("tableMsg", err.message, "error");
      button.disabled = false;
    }
  }

  form.addEventListener("submit", async (event) => {
    event.preventDefault();
    UI.clearMessage("formMsg");
    const button = UI.$("saveBtn");
    button.disabled = true;
    try {
      const payload = {
        name: fields.name.value.trim(),
        category: fields.category.value.trim(),
        description: fields.description.value.trim(),
        quantity: parseInt(fields.quantity.value, 10),
        unit: fields.unit.value.trim(),
        expiryDate: fields.expiry.value,
      };
      if (!payload.name) throw new Error("Enter the medicine name.");
      if (Number.isNaN(payload.quantity) || payload.quantity < 0) throw new Error("Quantity must be 0 or more.");
      if (!payload.expiryDate) throw new Error("Choose the expiry date.");

      const id = fields.id.value;
      if (id) {
        await api.put("/admin/medicines/" + id, payload);
      } else {
        await api.post("/admin/medicines", payload);
      }
      const wasEdit = Boolean(id);
      resetForm();
      await refreshAll();
      UI.showMessage("formMsg", wasEdit ? "Changes saved." : "Medicine added.", "success");
    } catch (err) {
      UI.showMessage("formMsg", err.message, "error");
    } finally {
      button.disabled = false;
    }
  });

  UI.$("cancelEditBtn").addEventListener("click", () => {
    resetForm();
    UI.clearMessage("formMsg");
  });

  let timer = null;
  search.addEventListener("input", () => {
    clearTimeout(timer);
    timer = setTimeout(loadMedicines, 300);
  });

  resetForm();
  refreshAll();
})();
