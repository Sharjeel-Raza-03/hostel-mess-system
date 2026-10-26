"use strict";

// Login, logout and the role guard (Auth), plus small shared page helpers (UI).
// Server data is always put into the page with textContent, never innerHTML.

const MONTH_NAMES = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];

const Auth = {
  TOKEN_KEY: "hm_token",
  ROLE_KEY: "hm_role",
  NAME_KEY: "hm_name",

  getToken() { return localStorage.getItem(Auth.TOKEN_KEY); },
  getRole() { return localStorage.getItem(Auth.ROLE_KEY); },
  getName() { return localStorage.getItem(Auth.NAME_KEY) || ""; },

  saveSession(result) {
    localStorage.setItem(Auth.TOKEN_KEY, result.token);
    localStorage.setItem(Auth.ROLE_KEY, result.role);
    localStorage.setItem(Auth.NAME_KEY, result.fullName);
  },

  clearSession() {
    localStorage.removeItem(Auth.TOKEN_KEY);
    localStorage.removeItem(Auth.ROLE_KEY);
    localStorage.removeItem(Auth.NAME_KEY);
  },

  homeFor(role) {
    return role === "ADMIN" ? "admin-dashboard.html" : "student-dashboard.html";
  },

  async login(email, password) {
    const result = await api.post("/auth/login", { email: email.trim(), password: password });
    Auth.saveSession(result);
    window.location.href = Auth.homeFor(result.role);
  },

  async register(fullName, roomNo, email, password) {
    await api.post("/auth/register", {
      fullName: fullName.trim(),
      roomNo: roomNo.trim(),
      email: email.trim(),
      password: password,
    });
  },

  logout() {
    Auth.clearSession();
    window.location.href = "index.html";
  },

  // Returns true when the visitor may stay on the page. Otherwise it redirects.
  requireRole(roles) {
    const allowed = Array.isArray(roles) ? roles : [roles];
    const token = Auth.getToken();
    const role = Auth.getRole();
    if (!token || !role) {
      window.location.replace("index.html");
      return false;
    }
    if (!allowed.includes(role)) {
      window.location.replace(Auth.homeFor(role));
      return false;
    }
    document.body.classList.remove("guard");
    return true;
  },

  setupNav() {
    const name = UI.$("navName");
    if (name) {
      name.textContent = Auth.getName() + " (" + Auth.getRole() + ")";
    }
    const button = UI.$("logoutBtn");
    if (button) {
      button.addEventListener("click", Auth.logout);
    }
  },
};

const UI = {
  $(id) { return document.getElementById(id); },

  el(tag, className, text) {
    const node = document.createElement(tag);
    if (className) node.className = className;
    if (text !== undefined && text !== null) node.textContent = text;
    return node;
  },

  clear(node) { node.replaceChildren(); },

  showMessage(id, text, type) {
    const node = UI.$(id);
    if (!node) return;
    node.textContent = text;
    node.className = "message " + (type || "info");
    node.hidden = false;
  },

  clearMessage(id) {
    const node = UI.$(id);
    if (!node) return;
    node.textContent = "";
    node.hidden = true;
  },

  today() {
    const now = new Date();
    return new Date(now.getTime() - now.getTimezoneOffset() * 60000).toISOString().slice(0, 10);
  },

  fillMonths(select) {
    MONTH_NAMES.forEach((name, index) => {
      const option = UI.el("option", null, name);
      option.value = String(index + 1);
      select.appendChild(option);
    });
  },

  fmtDate(iso) {
    if (!iso) return "-";
    const parts = String(iso).slice(0, 10).split("-");
    if (parts.length !== 3) return String(iso);
    return parts[2] + " " + MONTH_NAMES[parseInt(parts[1], 10) - 1] + " " + parts[0];
  },

  fmtDateTime(iso) {
    if (!iso) return "-";
    const text = String(iso);
    return UI.fmtDate(text) + " " + text.slice(11, 16);
  },

  fmtMonth(month, year) { return MONTH_NAMES[month - 1] + " " + year; },

  money(value) {
    const number = Number(value);
    return Number.isFinite(number) ? number.toFixed(2) : "0.00";
  },

  yesNo(value) { return value ? "Yes" : "No"; },

  badge(text, cssClass) { return UI.el("span", "badge " + cssClass, text); },

  leaveBadge(status) {
    const map = {
      PENDING: ["Pending", "badge-warn"],
      APPROVED: ["Approved", "badge-ok"],
      REJECTED: ["Rejected", "badge-bad"],
    };
    const entry = map[status] || [status, "badge-dark"];
    return UI.badge(entry[0], entry[1]);
  },

  medicineBadge(status) {
    const map = {
      AVAILABLE: ["Available", "badge-ok"],
      LOW_STOCK: ["Low stock", "badge-warn"],
      OUT_OF_STOCK: ["Out of stock", "badge-bad"],
      EXPIRED: ["Expired", "badge-dark"],
    };
    const entry = map[status] || [status, "badge-dark"];
    return UI.badge(entry[0], entry[1]);
  },

  // A table row from a list of strings or DOM nodes.
  row(values) {
    const tr = UI.el("tr");
    values.forEach((value) => {
      const td = UI.el("td");
      if (value instanceof Node) {
        td.appendChild(value);
      } else {
        td.textContent = value === null || value === undefined || value === "" ? "-" : String(value);
      }
      tr.appendChild(td);
    });
    return tr;
  },

  emptyRow(tbody, columns, text) {
    const tr = UI.el("tr");
    const td = UI.el("td", "empty", text);
    td.colSpan = columns;
    tr.appendChild(td);
    tbody.appendChild(tr);
  },
};
