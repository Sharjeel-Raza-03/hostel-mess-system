// The only setting in the frontend.
//
// Local development: the Spring Boot backend on this computer.
//   Your backend runs on port 8081 (PORT=8081 in set-env.bat).
// AWS (Step 8): change this line to "/api" (same origin through Nginx).
//   Never leave a localhost address in the deployed copy.
const API_BASE_URL = "http://localhost:8081/api";
