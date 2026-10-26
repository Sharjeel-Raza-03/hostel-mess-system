"use strict";

// The single place that calls fetch(). It adds the JWT, reads the base URL
// from config.js, and sends the user back to the login page on a 401.

class ApiError extends Error {
  constructor(status, message) {
    super(message);
    this.name = "ApiError";
    this.status = status;
  }
}

async function request(method, path, body) {
  const headers = {};
  const token = localStorage.getItem("hm_token");
  if (token) {
    headers["Authorization"] = "Bearer " + token;
  }
  const options = { method: method, headers: headers };
  if (body !== undefined) {
    headers["Content-Type"] = "application/json";
    options.body = JSON.stringify(body);
  }

  let response;
  try {
    response = await fetch(API_BASE_URL + path, options);
  } catch (err) {
    throw new ApiError(0, "Cannot reach the server. Check that the backend is running and try again.");
  }

  // An expired or invalid token: clear it and go back to the login page.
  // (Login and register calls are excluded, a wrong password is also a 401.)
  if (response.status === 401 && token && !path.startsWith("/auth/")) {
    localStorage.removeItem("hm_token");
    localStorage.removeItem("hm_role");
    localStorage.removeItem("hm_name");
    window.location.href = "index.html?expired=1";
    throw new ApiError(401, "Your session has expired. Please log in again.");
  }

  let data = null;
  const text = await response.text();
  if (text) {
    try {
      data = JSON.parse(text);
    } catch (err) {
      data = null;
    }
  }

  if (!response.ok) {
    const message = data && data.message ? data.message : "Request failed (" + response.status + ")";
    throw new ApiError(response.status, message);
  }
  return data;
}

const api = {
  get: (path) => request("GET", path),
  post: (path, body) => request("POST", path, body),
  put: (path, body) => request("PUT", path, body),
  del: (path) => request("DELETE", path),

  // Builds "?a=1&b=2" and skips empty values.
  qs: (params) => {
    const parts = [];
    Object.keys(params).forEach((key) => {
      const value = params[key];
      if (value !== undefined && value !== null && String(value) !== "") {
        parts.push(encodeURIComponent(key) + "=" + encodeURIComponent(value));
      }
    });
    return parts.length ? "?" + parts.join("&") : "";
  },
};
