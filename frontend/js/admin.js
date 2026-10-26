"use strict";

(function () {
  if (!Auth.requireRole("ADMIN")) return;
  Auth.setupNav();

  // ---------- Leave requests ----------
  const leaveFilter = UI.$("leaveFilter");

  function leaveRow(l) {
    const person = UI.el("div");
    person.appendChild(UI.el("strong", null, l.studentName));
    person.appendChild(UI.el("div", "muted small", "Room " + (l.roomNo || "-")));

    const review = UI.el("div", "actions");
    if (l.status === "PENDING") {
      const remark = UI.el("input");
      remark.type = "text";
      remark.placeholder = "Remark (optional)";
      remark.maxLength = 255;
      const approve = UI.el("button", "btn btn-small btn-ok", "Approve");
      approve.type = "button";
      const reject = UI.el("button", "btn btn-small btn-danger", "Reject");
      reject.type = "button";
      approve.addEventListener("click", () => reviewLeave(l.id, "approve", remark.value, [approve, reject]));
      reject.addEventListener("click", () => reviewLeave(l.id, "reject", remark.value, [approve, reject]));
      review.append(remark, approve, reject);
    } else {
      const note = (l.adminRemark ? l.adminRemark : "No remark") + (l.reviewedByName ? " (by " + l.reviewedByName + ")" : "");
      review.appendChild(UI.el("span", "muted small", note));
    }

    return UI.row([
      person,
      UI.fmtDate(l.fromDate) + " to " + UI.fmtDate(l.toDate),
      l.reason,
      UI.leaveBadge(l.status),
      review,
    ]);
  }

  async function loadLeaves() {
    const body = UI.$("leavesBody");
    UI.clear(body);
    try {
      const status = leaveFilter.value === "ALL" ? "" : leaveFilter.value;
      const list = await api.get("/admin/leaves" + api.qs({ status: status }));
      if (list.length === 0) {
        UI.emptyRow(body, 5, "No leave requests found.");
        return;
      }
      list.forEach((l) => body.appendChild(leaveRow(l)));
    } catch (err) {
      UI.showMessage("leaveMsg", err.message, "error");
    }
  }

  async function reviewLeave(id, action, remark, buttons) {
    UI.clearMessage("leaveMsg");
    buttons.forEach((b) => { b.disabled = true; });
    try {
      await api.put("/admin/leaves/" + id + "/" + action, { remark: remark.trim() });
      await loadLeaves();
      UI.showMessage("leaveMsg",
        action === "approve"
          ? "Leave approved. Meals, mess counts and bills were updated."
          : "Leave rejected.",
        "success");
    } catch (err) {
      UI.showMessage("leaveMsg", err.message, "error");
      buttons.forEach((b) => { b.disabled = false; });
    }
  }

  leaveFilter.addEventListener("change", () => {
    UI.clearMessage("leaveMsg");
    loadLeaves();
  });
  UI.$("leaveReloadBtn").addEventListener("click", () => {
    UI.clearMessage("leaveMsg");
    loadLeaves();
  });

  // ---------- Daily mess count and meals ----------
  const countDate = UI.$("countDate");
  countDate.value = UI.today();

  async function loadCount() {
    UI.clearMessage("countMsg");
    const body = UI.$("mealsBody");
    UI.clear(body);
    const date = countDate.value;
    if (!date) {
      UI.showMessage("countMsg", "Choose a date.", "error");
      return;
    }
    try {
      const count = await api.get("/admin/mess-count" + api.qs({ date: date }));
      UI.$("countBreakfast").textContent = count.breakfastCount;
      UI.$("countLunch").textContent = count.lunchCount;
      UI.$("countDinner").textContent = count.dinnerCount;

      const meals = await api.get("/admin/meals" + api.qs({ date: date }));
      if (meals.length === 0) {
        UI.emptyRow(body, 6, "No meal records for this date.");
        return;
      }
      meals.forEach((m) => {
        body.appendChild(UI.row([
          m.studentName,
          m.roomNo,
          UI.yesNo(m.breakfast),
          UI.yesNo(m.lunch),
          UI.yesNo(m.dinner),
          m.onLeave ? UI.badge("On leave", "badge-warn") : "Normal",
        ]));
      });
    } catch (err) {
      UI.showMessage("countMsg", err.message, "error");
    }
  }

  UI.$("countLoadBtn").addEventListener("click", loadCount);

  // ---------- Bills and monthly report ----------
  const billMonth = UI.$("billMonth");
  const billYear = UI.$("billYear");
  UI.fillMonths(billMonth);
  const now = new Date();
  billMonth.value = String(now.getMonth() + 1);
  billYear.value = String(now.getFullYear());

  function period() {
    return { month: billMonth.value, year: billYear.value };
  }

  function renderBills(list) {
    const body = UI.$("billsBody");
    UI.clear(body);
    if (list.length === 0) {
      UI.emptyRow(body, 6, "No bills for this month. Click Generate bills.");
      return;
    }
    list.forEach((b) => {
      body.appendChild(UI.row([
        b.studentName,
        b.roomNo,
        b.totalMeals,
        b.leaveDays,
        UI.money(b.mealRate),
        UI.money(b.totalAmount),
      ]));
    });
  }

  async function loadReport() {
    const report = await api.get("/admin/reports/monthly" + api.qs(period()));
    UI.$("reportStudents").textContent = report.studentsBilled;
    UI.$("reportMeals").textContent = report.totalMeals;
    UI.$("reportLeave").textContent = report.totalLeaveDays;
    UI.$("reportRevenue").textContent = UI.money(report.totalRevenue);
  }

  async function loadBills() {
    UI.clearMessage("billMsg");
    try {
      renderBills(await api.get("/admin/bills" + api.qs(period())));
      await loadReport();
    } catch (err) {
      UI.showMessage("billMsg", err.message, "error");
    }
  }

  async function generateBills() {
    UI.clearMessage("billMsg");
    const button = UI.$("billGenerateBtn");
    button.disabled = true;
    try {
      renderBills(await api.post("/admin/bills/generate" + api.qs(period())));
      await loadReport();
      UI.showMessage("billMsg", "Bills generated for " + UI.fmtMonth(parseInt(billMonth.value, 10), billYear.value) + ".", "success");
    } catch (err) {
      UI.showMessage("billMsg", err.message, "error");
    } finally {
      button.disabled = false;
    }
  }

  UI.$("billLoadBtn").addEventListener("click", loadBills);
  UI.$("billGenerateBtn").addEventListener("click", generateBills);

  // ---------- Rates and low-stock threshold ----------
  async function loadRates() {
    try {
      const rates = await api.get("/admin/settings/rates");
      UI.$("rateBreakfast").value = rates.breakfastRate;
      UI.$("rateLunch").value = rates.lunchRate;
      UI.$("rateDinner").value = rates.dinnerRate;
      UI.$("rateThreshold").value = rates.lowStockThreshold;
    } catch (err) {
      UI.showMessage("rateMsg", err.message, "error");
    }
  }

  UI.$("rateForm").addEventListener("submit", async (event) => {
    event.preventDefault();
    UI.clearMessage("rateMsg");
    const button = UI.$("rateSaveBtn");
    button.disabled = true;
    try {
      const breakfast = parseFloat(UI.$("rateBreakfast").value);
      const lunch = parseFloat(UI.$("rateLunch").value);
      const dinner = parseFloat(UI.$("rateDinner").value);
      const threshold = parseInt(UI.$("rateThreshold").value, 10);
      if ([breakfast, lunch, dinner].some((n) => Number.isNaN(n) || n < 0)) {
        throw new Error("Enter a valid rate (0 or more) for every meal.");
      }
      if (Number.isNaN(threshold) || threshold < 0) {
        throw new Error("Enter a valid low-stock threshold (0 or more).");
      }
      await api.put("/admin/settings/rates", {
        breakfastRate: breakfast,
        lunchRate: lunch,
        dinnerRate: dinner,
        lowStockThreshold: threshold,
      });
      UI.showMessage("rateMsg", "Saved. Existing bills change only when you generate them again.", "success");
      await loadRates();
      loadAlerts();
    } catch (err) {
      UI.showMessage("rateMsg", err.message, "error");
    } finally {
      button.disabled = false;
    }
  });

  // ---------- Medicine alerts ----------
  async function loadAlerts() {
    const body = UI.$("alertsBody");
    UI.clear(body);
    try {
      const list = await api.get("/admin/medicines/alerts");
      UI.$("alertLow").textContent = list.filter((m) => m.status === "LOW_STOCK").length;
      UI.$("alertOut").textContent = list.filter((m) => m.status === "OUT_OF_STOCK").length;
      UI.$("alertExpired").textContent = list.filter((m) => m.status === "EXPIRED").length;
      if (list.length === 0) {
        UI.emptyRow(body, 4, "No alerts. All medicines are in good shape.");
        return;
      }
      list.forEach((m) => {
        body.appendChild(UI.row([
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

  loadLeaves();
  loadCount();
  loadBills();
  loadRates();
  loadAlerts();
})();
