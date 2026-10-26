"use strict";

(function () {
  if (!Auth.requireRole("STUDENT")) return;
  Auth.setupNav();

  // ---------- Meals ----------
  const mealMonth = UI.$("mealMonth");
  const mealYear = UI.$("mealYear");
  UI.fillMonths(mealMonth);
  const now = new Date();
  mealMonth.value = String(now.getMonth() + 1);
  mealYear.value = String(now.getFullYear());
  UI.$("mealDate").value = UI.today();

  async function loadMeals() {
    const body = UI.$("mealsBody");
    UI.clear(body);
    try {
      const list = await api.get("/meals/my" + api.qs({ month: mealMonth.value, year: mealYear.value }));
      if (list.length === 0) {
        UI.emptyRow(body, 5, "No meal records for this month.");
        return;
      }
      list.forEach((m) => {
        body.appendChild(UI.row([
          UI.fmtDate(m.mealDate),
          UI.yesNo(m.breakfast),
          UI.yesNo(m.lunch),
          UI.yesNo(m.dinner),
          m.onLeave ? UI.badge("On leave", "badge-warn") : "Normal",
        ]));
      });
    } catch (err) {
      UI.showMessage("mealMsg", err.message, "error");
    }
  }

  UI.$("mealLoadBtn").addEventListener("click", () => {
    UI.clearMessage("mealMsg");
    loadMeals();
  });

  UI.$("mealForm").addEventListener("submit", async (event) => {
    event.preventDefault();
    UI.clearMessage("mealMsg");
    const button = UI.$("mealSaveBtn");
    button.disabled = true;
    try {
      const date = UI.$("mealDate").value;
      if (!date) throw new Error("Choose a date.");
      await api.post("/meals", {
        mealDate: date,
        breakfast: UI.$("mealBreakfast").checked,
        lunch: UI.$("mealLunch").checked,
        dinner: UI.$("mealDinner").checked,
      });
      mealMonth.value = String(parseInt(date.slice(5, 7), 10));
      mealYear.value = date.slice(0, 4);
      await loadMeals();
      UI.showMessage("mealMsg", "Meals saved for " + UI.fmtDate(date) + ".", "success");
    } catch (err) {
      UI.showMessage("mealMsg", err.message, "error");
    } finally {
      button.disabled = false;
    }
  });

  // ---------- Leave ----------
  UI.$("leaveFrom").min = UI.today();
  UI.$("leaveTo").min = UI.today();

  async function loadLeaves() {
    const body = UI.$("leavesBody");
    UI.clear(body);
    try {
      const list = await api.get("/leaves/my");
      if (list.length === 0) {
        UI.emptyRow(body, 5, "You have not requested any leave yet.");
        return;
      }
      list.forEach((l) => {
        body.appendChild(UI.row([
          UI.fmtDate(l.fromDate),
          UI.fmtDate(l.toDate),
          l.reason,
          UI.leaveBadge(l.status),
          l.adminRemark,
        ]));
      });
    } catch (err) {
      UI.showMessage("leaveMsg", err.message, "error");
    }
  }

  UI.$("leaveForm").addEventListener("submit", async (event) => {
    event.preventDefault();
    UI.clearMessage("leaveMsg");
    const button = UI.$("leaveSaveBtn");
    button.disabled = true;
    try {
      const from = UI.$("leaveFrom").value;
      const to = UI.$("leaveTo").value;
      const reason = UI.$("leaveReason").value.trim();
      if (!from || !to) throw new Error("Choose both dates.");
      if (to < from) throw new Error("The end date cannot be before the start date.");
      if (!reason) throw new Error("Please enter a reason.");
      await api.post("/leaves", { fromDate: from, toDate: to, reason: reason });
      UI.$("leaveForm").reset();
      await loadLeaves();
      UI.showMessage("leaveMsg", "Leave request submitted. It is now pending approval.", "success");
    } catch (err) {
      UI.showMessage("leaveMsg", err.message, "error");
    } finally {
      button.disabled = false;
    }
  });

  // ---------- Bills ----------
  async function loadBills() {
    const body = UI.$("billsBody");
    UI.clear(body);
    try {
      const list = await api.get("/bills/my");
      if (list.length === 0) {
        UI.emptyRow(body, 6, "No bills yet. Your bill appears after the admin generates it or a leave is approved.");
        return;
      }
      list.forEach((b) => {
        body.appendChild(UI.row([
          UI.fmtMonth(b.billMonth, b.billYear),
          b.totalMeals,
          b.leaveDays,
          UI.money(b.mealRate),
          UI.money(b.totalAmount),
          UI.fmtDateTime(b.generatedAt),
        ]));
      });
    } catch (err) {
      UI.showMessage("billMsg", err.message, "error");
    }
  }

  UI.$("billsReloadBtn").addEventListener("click", () => {
    UI.clearMessage("billMsg");
    loadBills();
  });

  loadMeals();
  loadLeaves();
  loadBills();
})();
