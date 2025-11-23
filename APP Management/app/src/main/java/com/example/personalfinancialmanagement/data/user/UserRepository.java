package com.example.personalfinancialmanagement.data.user;

import android.content.Context;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import com.example.personalfinancialmanagement.auth.PasswordHasher;
import com.example.personalfinancialmanagement.SessionManager;

/**
 * Calls the Node.js + MongoDB backend for auth while keeping the same method names.
 */
public class UserRepository {
    private static final String TAG = "UserRepository";
    // Default for emulator; tries to read BuildConfig.API_BASE_URL via reflection to avoid IDE sync issues.
    private static final String BASE_URL = resolveBaseUrl();

    private final Context context;
    private final SessionManager session;
    private String lastError;

    public UserRepository(Context context) {
        this.context = context.getApplicationContext();
        this.session = new SessionManager(this.context);
    }

    public String getLastError() {
        return lastError;
    }

    public long register(String username, String password, String email) {
        lastError = null;
        if (username == null || username.trim().isEmpty() || password == null || password.isEmpty()) {
            lastError = "Username và mật khẩu bắt buộc";
            return -1L;
        }
        JSONObject body = new JSONObject();
        try {
            body.put("username", username);
            body.put("password", password);
            if (email != null && !email.trim().isEmpty()) {
                body.put("email", email);
            }
        } catch (JSONException e) {
            return -1L;
        }

        try {
            ApiResult result = request("POST", "/auth/register", body);
            if (result.statusCode == 201 && result.json != null) {
                saveTokenIfPresent(result.json);
                JSONObject user = result.json.optJSONObject("user");
                if (user != null) {
                    return user.optLong("id", -1L);
                }
            }
            if (result.statusCode == 409) {
                lastError = "Tên đăng nhập hoặc email đã tồn tại";
                return -1L; // exists
            }
            lastError = extractMessage(result.json, "Đăng ký thất bại (mã " + result.statusCode + ")");
        } catch (IOException e) {
            Log.e(TAG, "register error", e);
            lastError = "Lỗi kết nối máy chủ";
        }
        return -1L;
    }

    public User login(String username, String password) {
        lastError = null;
        if (username == null || username.trim().isEmpty() || password == null || password.isEmpty()) {
            return null;
        }
        JSONObject body = new JSONObject();
        try {
            body.put("username", username);
            body.put("password", password);
        } catch (JSONException e) {
            return null;
        }

        try {
            ApiResult result = request("POST", "/auth/login", body);
            if (result.statusCode == 200 && result.json != null) {
                saveTokenIfPresent(result.json);
                JSONObject user = result.json.optJSONObject("user");
                if (user != null) {
                    long id = user.optLong("id", -1L);
                    String uname = user.optString("username", username);
                    String email = user.optString("email", null);
                    // Keep passwordHash field populated for downstream code, although backend stores bcrypt.
                    return new User(id, uname, PasswordHasher.sha256(password), null, email, null);
                }
            }
            lastError = extractMessage(result.json, "Sai thông tin đăng nhập");
        } catch (IOException e) {
            Log.e(TAG, "login error", e);
            lastError = "Lỗi kết nối máy chủ";
        }
        return null;
    }

    public User findById(long id) {
        lastError = null;
        if (id <= 0) return null;
        try {
            ApiResult result = request("GET", "/auth/user/" + id, null);
            if (result.statusCode == 200 && result.json != null) {
                JSONObject user = result.json.optJSONObject("user");
                if (user != null) {
                    long uid = user.optLong("id", -1L);
                    String uname = user.optString("username", "");
                    String email = user.optString("email", null);
                    return new User(uid, uname, "", null, email, null);
                }
            }
            lastError = extractMessage(result.json, "Không tìm thấy người dùng");
        } catch (IOException e) {
            Log.e(TAG, "findById error", e);
            lastError = "Lỗi kết nối máy chủ";
        }
        return null;
    }

    /** Fetch current user using stored JWT (returns null if missing/invalid). */
    public User fetchMeViaToken() {
        lastError = null;
        String token = session.getAuthToken();
        if (token == null || token.trim().isEmpty()) return null;
        try {
            ApiResult result = request("GET", "/auth/me", null);
            if (result.statusCode == 200 && result.json != null) {
                JSONObject user = result.json.optJSONObject("user");
                if (user != null) {
                    long uid = user.optLong("id", -1L);
                    String uname = user.optString("username", "");
                    String email = user.optString("email", null);
                    return new User(uid, uname, "", null, email, null);
                }
            }
            // if unauthorized, clear token
            if (result.statusCode == 401) session.saveAuthToken(null);
            lastError = extractMessage(result.json, "Không lấy được thông tin người dùng");
        } catch (IOException e) {
            Log.e(TAG, "fetchMeViaToken error", e);
            lastError = "Lỗi mạng";
        }
        return null;
    }

    private ApiResult request(String method, String path, JSONObject body) throws IOException {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(BASE_URL + path);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod(method);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(8000);
            conn.setRequestProperty("Accept", "application/json");
            String token = session.getAuthToken();
            if (token != null && !token.isEmpty()) {
                conn.setRequestProperty("Authorization", "Bearer " + token);
            }
            if (body != null) {
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                byte[] data = body.toString().getBytes(StandardCharsets.UTF_8);
                OutputStream os = conn.getOutputStream();
                os.write(data);
                os.flush();
                os.close();
            }

            int status = conn.getResponseCode();
            InputStream is = status >= 200 && status < 400 ? conn.getInputStream() : conn.getErrorStream();
            String text = readStream(is);
            JSONObject json = null;
            if (text != null && !text.isEmpty()) {
                try {
                    json = new JSONObject(text);
                } catch (JSONException ignored) { }
            }
            return new ApiResult(status, json);
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private String readStream(InputStream is) throws IOException {
        if (is == null) return "";
        BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line);
        }
        br.close();
        return sb.toString();
    }

    private String extractMessage(JSONObject json, String fallback) {
        if (json != null) {
            String msg = json.optString("message", null);
            if (msg != null && !msg.isEmpty()) return msg;
        }
        return fallback;
    }

    private static String resolveBaseUrl() {
        return com.example.personalfinancialmanagement.BuildConfig.API_BASE_URL;
    }

    private void saveTokenIfPresent(JSONObject json) {
        if (json == null) return;
        String token = json.optString("token", null);
        if (token != null && !token.isEmpty()) {
            session.saveAuthToken(token);
        }
    }

    /** Update profile fields (username/fullName/email/phone) via API; returns updated user or null. */
    public User updateProfile(User user) {
        lastError = null;
        if (user == null || user.id <= 0) { lastError = "Invalid user"; return null; }
        JSONObject body = new JSONObject();
        try {
            if (user.username != null) body.put("username", user.username);
            if (user.fullName != null) body.put("fullName", user.fullName);
            if (user.email != null) body.put("email", user.email);
            if (user.phone != null) body.put("phone", user.phone);
        } catch (JSONException ignored) { }
        try {
            ApiResult result = request("PUT", "/auth/profile", body);
            if (result.statusCode == 200 && result.json != null) {
                JSONObject js = result.json.optJSONObject("user");
                if (js != null) {
                    User u = new User(
                            js.optLong("id", user.id),
                            js.optString("username", user.username),
                            user.passwordHash,
                            user.fullName,
                            js.optString("email", user.email),
                            js.optString("phone", user.phone)
                    );
                    u.fullName = js.optString("fullName", user.fullName);
                    u.phone = js.optString("phone", user.phone);
                    return u;
                }
            }
            if (result.statusCode == 401) session.saveAuthToken(null);
            lastError = extractMessage(result.json, "Không cập nhật được hồ sơ");
        } catch (IOException e) {
            Log.e(TAG, "updateProfile error", e);
            lastError = "Lỗi mạng";
        }
        return null;
    }

    private static class ApiResult {
        final int statusCode;
        final JSONObject json;
        ApiResult(int statusCode, JSONObject json) {
            this.statusCode = statusCode;
            this.json = json;
        }
    }
}
