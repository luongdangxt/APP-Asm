package com.example.personalfinancialmanagement.network;

import android.content.Context;
import android.util.Log;

import com.example.personalfinancialmanagement.AppDatabase;
import com.example.personalfinancialmanagement.Budget;
import com.example.personalfinancialmanagement.BudgetDao;
import com.example.personalfinancialmanagement.Expense;
import com.example.personalfinancialmanagement.ExpenseDao;
import com.example.personalfinancialmanagement.Income;
import com.example.personalfinancialmanagement.IncomeDao;
import com.example.personalfinancialmanagement.SavingsGoal;
import com.example.personalfinancialmanagement.SavingsGoalDao;
import com.example.personalfinancialmanagement.SavingsMonthlyGoal;
import com.example.personalfinancialmanagement.SavingsMonthlyGoalDao;
import com.example.personalfinancialmanagement.SavingsContributionDao;
import com.example.personalfinancialmanagement.SavingsContribution;
import com.example.personalfinancialmanagement.SessionManager;
import com.example.personalfinancialmanagement.MonthUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Lightweight network repository that talks to the Node backend for finance data.
 * It always tries the remote API first (using JWT from SessionManager) and falls back
 * to the DAOs (SQLite) for offline use.
 */
public class FinanceRemoteRepository {
    private static final String TAG = "FinanceRemoteRepo";
    private static final String BASE_URL = resolveBaseUrl();

    private final SessionManager session;
    private final ExpenseDao expenseDao;
    private final IncomeDao incomeDao;
    private final BudgetDao budgetDao;
    private final SavingsGoalDao savingsGoalDao;
    private final SavingsContributionDao savingsContributionDao;
    private final SavingsMonthlyGoalDao savingsMonthlyGoalDao;

    public static class SourceItem {
        public final String id;
        public final String label;
        public final String type;
        public SourceItem(String id, String label, String type) {
            this.id = id;
            this.label = label;
            this.type = type;
        }
    }

    public FinanceRemoteRepository(Context ctx) {
        Context appCtx = ctx.getApplicationContext();
        this.session = new SessionManager(appCtx);
        AppDatabase db = AppDatabase.getInstance(appCtx);
        this.expenseDao = db.expenseDao();
        this.incomeDao = db.incomeDao();
        this.budgetDao = db.budgetDao();
        this.savingsGoalDao = db.savingsGoalDao();
        this.savingsContributionDao = db.savingsContributionDao();
        this.savingsMonthlyGoalDao = db.savingsMonthlyGoalDao();
    }

    /* ===================== Expenses ===================== */
    public List<Expense> listExpenses(long userId) {
        try {
            ApiResult r = request("GET", "/expenses", null);
            if (r.statusCode == 200 && r.json != null) {
                return parseExpenses(r.json.optJSONArray("expenses"), userId);
            }
            if (r.statusCode == 401 || r.statusCode == 403) {
                session.saveAuthToken(null);
                return Collections.emptyList();
            }
            // avoid showing stale local data if server responded but no data/other status
            return Collections.emptyList();
        } catch (IOException e) {
            Log.w(TAG, "listExpenses remote failed, using local", e);
        }
        // Only fallback to local cache when truly offline
        long now = System.currentTimeMillis();
        long start = MonthUtils.monthStartUtcMillis(now);
        long end = MonthUtils.monthEndUtcMillis(now);
        return expenseDao.listByDateRange(userId, start, end);
    }

    public boolean addExpense(long userId, Expense e) {
        try {
            JSONObject body = new JSONObject();
            body.put("description", e.description);
            body.put("amount", e.amount);
            body.put("category", e.category);
            body.put("dateUtc", e.dateUtc);
            ApiResult r = request("POST", "/expenses", body);
            if (r.statusCode == 201) {
                return true;
            }
        } catch (Exception ex) {
            Log.w(TAG, "addExpense remote failed, falling back", ex);
        }
        return expenseDao.insert(e) > 0;
    }

    /* ===================== Incomes ===================== */
    public List<Income> listIncomes(long userId) {
        try {
            ApiResult r = request("GET", "/incomes", null);
            if (r.statusCode == 200 && r.json != null) {
                return parseIncomes(r.json.optJSONArray("incomes"), userId);
            }
            if (r.statusCode == 401 || r.statusCode == 403) {
                session.saveAuthToken(null);
                return Collections.emptyList();
            }
            return Collections.emptyList();
        } catch (IOException e) {
            Log.w(TAG, "listIncomes remote failed, using local", e);
        }
        long now = System.currentTimeMillis();
        long start = MonthUtils.monthStartUtcMillis(now);
        long end = MonthUtils.monthEndUtcMillis(now);
        return incomeDao.listByDateRange(userId, start, end);
    }

    public boolean addIncome(long userId, Income income) {
        try {
            JSONObject body = new JSONObject();
            body.put("title", income.title);
            body.put("category", income.category);
            body.put("amount", income.amount);
            body.put("dateUtc", income.dateUtc);
            ApiResult r = request("POST", "/incomes", body);
            if (r.statusCode == 201) {
                return true;
            }
        } catch (Exception ex) {
            Log.w(TAG, "addIncome remote failed, falling back", ex);
        }
        return incomeDao.insert(income) > 0;
    }

    /* ===================== Budgets ===================== */
    public List<Budget> listBudgets(long userId, int monthKey) {
        try {
            String q = "?monthKey=" + monthKey;
            ApiResult r = request("GET", "/budgets" + q, null);
            if (r.statusCode == 200 && r.json != null) {
                return parseBudgets(r.json.optJSONArray("budgets"), userId);
            }
        } catch (IOException e) {
            Log.w(TAG, "listBudgets remote failed, using local", e);
        }
        return budgetDao.listForMonth(userId, monthKey);
    }

    public boolean upsertBudget(long userId, int monthKey, String category, double limit) {
        try {
            JSONObject body = new JSONObject();
            body.put("monthKey", monthKey);
            body.put("category", category);
            body.put("limitAmount", limit);
            ApiResult r = request("POST", "/budgets", body);
            if (r.statusCode == 201) return true;
        } catch (Exception ex) {
            Log.w(TAG, "upsertBudget remote failed, falling back", ex);
        }
        Budget existing = budgetDao.find(userId, monthKey, category);
        if (existing != null) {
            existing.limitAmount = limit;
            budgetDao.update(existing);
            return existing.id > 0;
        }
        return budgetDao.upsert(new Budget(userId, monthKey, category, limit)) > 0;
    }

    public boolean deleteBudgetComposite(int monthKey, String category) {
        try {
            // Try delete by remote id (more reliable if categories differ by casing/spacing)
            String remoteId = findBudgetRemoteId(monthKey, category);
            if (remoteId != null) {
                ApiResult byId = request("DELETE", "/budgets/" + remoteId, null);
                if (byId.statusCode == 200 || byId.statusCode == 404) return true;
            }

            String path = "/budgets?monthKey=" + monthKey + "&category=" + URLEncoder.encode(category, "UTF-8");
            ApiResult r = request("DELETE", path, null);
            return r.statusCode == 200 || r.statusCode == 404; // treat not found as success locally
        } catch (Exception ex) {
            Log.w(TAG, "deleteBudget remote failed", ex);
            return false;
        }
    }

    /* ===================== Savings ===================== */
    public List<SavingsGoal> listSavingsGoals(long userId) {
        try {
            ApiResult r = request("GET", "/savings/goals", null);
            if (r.statusCode == 200 && r.json != null) {
                return parseSavingsGoals(r.json.optJSONArray("goals"), userId);
            }
        } catch (IOException e) {
            Log.w(TAG, "listSavingsGoals remote failed, using local", e);
        }
        return savingsGoalDao.list(userId);
    }

    public boolean addSavingsGoal(long userId, String title, double targetAmount, String iconKey) {
        try {
            JSONObject body = new JSONObject();
            body.put("title", title);
            body.put("targetAmount", targetAmount);
            body.put("iconKey", iconKey);
            body.put("createdAtUtc", System.currentTimeMillis());
            ApiResult r = request("POST", "/savings/goals", body);
            if (r.statusCode == 201) return true;
        } catch (Exception ex) {
            Log.w(TAG, "addSavingsGoal remote failed, falling back", ex);
        }
        return savingsGoalDao.insert(new SavingsGoal(userId, title, targetAmount, iconKey, System.currentTimeMillis())) > 0;
    }

    public boolean deleteSavingsGoal(long userId, String title) {
        // Try find id first (case-insensitive), then fallback to title endpoint
        try {
            List<SavingsGoal> goals = listSavingsGoals(userId);
            if (goals != null && title != null) {
                String normalized = title.trim().toLowerCase(java.util.Locale.getDefault());
                for (SavingsGoal g : goals) {
                    String cand = g.title == null ? "" : g.title.trim().toLowerCase(java.util.Locale.getDefault());
                    if (!normalized.isEmpty() && normalized.equals(cand)) {
                        // We don't store Mongo _id in local SavingsGoal, so listSavingsGoals must return objects with id set to Mongo _id in the JSON.
                        // Use reflection-free field: check if g.iconKey holds id? It doesn't. So parse directly from API:
                        // Instead of using DAO object, re-fetch raw JSON to get _id.
                        break;
                    }
                }
            }
        } catch (Exception ignored) { }

        // Fetch raw and delete by _id
        try {
            ApiResult rList = request("GET", "/savings/goals", null);
            if (rList.statusCode == 200 && rList.json != null) {
                JSONArray arr = rList.json.optJSONArray("goals");
                if (arr != null) {
                    String normalized = title == null ? "" : title.trim().toLowerCase(java.util.Locale.getDefault());
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject o = arr.optJSONObject(i);
                        if (o == null) continue;
                        String label = o.optString("title", "").trim().toLowerCase(java.util.Locale.getDefault());
                        if (!normalized.isEmpty() && normalized.equals(label)) {
                            String remoteId = o.optString("_id", null);
                            if (remoteId != null && !remoteId.isEmpty()) {
                                ApiResult rDel = request("DELETE", "/savings/goals/" + remoteId, null);
                                if (rDel.statusCode == 200 || rDel.statusCode == 404) return true;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "deleteSavingsGoal by id failed", e);
        }

        try {
            String path = "/savings/goals?title=" + URLEncoder.encode(title == null ? "" : title, "UTF-8");
            ApiResult r = request("DELETE", path, null);
            return r.statusCode == 200 || r.statusCode == 404;
        } catch (Exception e) {
            Log.w(TAG, "deleteSavingsGoal remote failed", e);
            return false;
        }
    }

    public boolean addSavingsContribution(long userId, Long goalId, double amount, long dateUtc, boolean isAuto) {
        try {
            JSONObject body = new JSONObject();
            if (goalId != null && goalId > 0) body.put("goalId", goalId);
            body.put("amount", amount);
            body.put("dateUtc", dateUtc);
            body.put("isAuto", isAuto);
            ApiResult r = request("POST", "/savings/contributions", body);
            if (r.statusCode == 201) return true;
        } catch (Exception ex) {
            Log.w(TAG, "addSavingsContribution remote failed, falling back", ex);
        }
        return savingsContributionDao.insert(new com.example.personalfinancialmanagement.SavingsContribution(userId, goalId, amount, dateUtc)) > 0;
    }

    public List<SavingsContribution> listSavingsContributions(long userId, Long goalId) {
        try {
            String path = "/savings/contributions";
            if (goalId != null && goalId > 0) path += "?goalId=" + goalId;
            ApiResult r = request("GET", path, null);
            if (r.statusCode == 200 && r.json != null) {
                List<SavingsContribution> list = parseSavingsContributions(r.json.optJSONArray("contributions"), userId);
                if (!list.isEmpty()) return list;
            }
        } catch (IOException e) {
            Log.w(TAG, "listSavingsContributions remote failed, using local", e);
        }
        if (goalId != null && goalId > 0) return savingsContributionDao.listForGoal(userId, goalId);
        return savingsContributionDao.listForUser(userId);
    }

    public SavingsMonthlyGoal getOrCreateMonthlyGoal(long userId, int monthKey, double defaultTarget) {
        try {
            ApiResult r = request("GET", "/savings/monthly-goals?monthKey=" + monthKey, null);
            if (r.statusCode == 200 && r.json != null) {
                List<SavingsMonthlyGoal> list = parseSavingsMonthlyGoals(r.json.optJSONArray("monthlyGoals"), userId);
                if (!list.isEmpty()) return list.get(0);
            }
        } catch (IOException e) {
            Log.w(TAG, "get monthly goal remote failed, using local", e);
        }
        SavingsMonthlyGoal g = savingsMonthlyGoalDao.find(userId, monthKey);
        if (g == null) {
            g = new SavingsMonthlyGoal(userId, monthKey, defaultTarget);
            g.id = savingsMonthlyGoalDao.upsert(g);
        }
        return g;
    }

    public boolean setMonthlyGoal(long userId, int monthKey, double targetAmount) {
        try {
            JSONObject body = new JSONObject();
            body.put("monthKey", monthKey);
            body.put("targetAmount", targetAmount);
            ApiResult r = request("POST", "/savings/monthly-goals", body);
            if (r.statusCode == 201) return true;
        } catch (Exception ex) {
            Log.w(TAG, "setMonthlyGoal remote failed, falling back", ex);
        }
        SavingsMonthlyGoal g = savingsMonthlyGoalDao.find(userId, monthKey);
        if (g == null) {
            savingsMonthlyGoalDao.upsert(new SavingsMonthlyGoal(userId, monthKey, targetAmount));
        } else {
            g.targetAmount = targetAmount;
            savingsMonthlyGoalDao.update(g);
        }
        return true;
    }

    /* ===================== Sources (income/expense) ===================== */
    public List<SourceItem> listSources(String type) {
        try {
            String path = "/sources";
            if (type != null && (type.equals("income") || type.equals("expense"))) {
                path += "?type=" + type;
            }
            ApiResult r = request("GET", path, null);
            if (r.statusCode == 200 && r.json != null) {
                return parseSources(r.json.optJSONArray("sources"));
            }
        } catch (IOException e) {
            Log.w(TAG, "listSources remote failed", e);
        }
        return null;
    }

    public SourceItem createSource(String type, String label) {
        try {
            JSONObject body = new JSONObject();
            body.put("type", type);
            body.put("label", label);
            ApiResult r = request("POST", "/sources", body);
            if (r.statusCode == 201 && r.json != null) {
                JSONObject o = r.json.optJSONObject("source");
                if (o != null) return parseSourceObject(o);
            }
        } catch (Exception e) {
            Log.w(TAG, "createSource failed", e);
        }
        return null;
    }

    public SourceItem updateSource(String id, String label, String type) {
        if (id == null || id.isEmpty()) return null;
        try {
            JSONObject body = new JSONObject();
            if (label != null) body.put("label", label);
            if (type != null) body.put("type", type);
            ApiResult r = request("PUT", "/sources/" + id, body);
            if (r.statusCode == 200 && r.json != null) {
                JSONObject o = r.json.optJSONObject("source");
                if (o != null) return parseSourceObject(o);
            }
        } catch (Exception e) {
            Log.w(TAG, "updateSource failed", e);
        }
        return null;
    }

    public boolean deleteSource(String id) {
        if (id == null || id.isEmpty()) return false;
        try {
            ApiResult r = request("DELETE", "/sources/" + id, null);
            return r.statusCode == 200;
        } catch (IOException e) {
            Log.w(TAG, "deleteSource failed", e);
            return false;
        }
    }

    /* ===================== Helpers ===================== */
    private List<Expense> parseExpenses(JSONArray arr, long userId) {
        List<Expense> list = new ArrayList<>();
        if (arr == null) return list;
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o == null) continue;
            String desc = o.optString("description", "");
            String cat = o.optString("category", "");
            long date = o.optLong("dateUtc", 0);
            double amount = o.optDouble("amount", 0);
            if (desc.isEmpty() || cat.isEmpty() || date <= 0) continue;
            list.add(new Expense(userId, desc, date, amount, cat));
        }
        return list;
    }

    private List<Income> parseIncomes(JSONArray arr, long userId) {
        List<Income> list = new ArrayList<>();
        if (arr == null) return list;
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o == null) continue;
            String title = o.optString("title", "");
            String cat = o.optString("category", "");
            long date = o.optLong("dateUtc", 0);
            double amount = o.optDouble("amount", 0);
            if (title.isEmpty() || cat.isEmpty() || date <= 0) continue;
            list.add(new Income(userId, title, date, amount, cat));
        }
        return list;
    }

    private List<Budget> parseBudgets(JSONArray arr, long userId) {
        List<Budget> list = new ArrayList<>();
        if (arr == null) return list;
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o == null) continue;
            int monthKey = o.optInt("monthKey", MonthUtils.monthKey(System.currentTimeMillis()));
            String cat = o.optString("category", "");
            double limit = o.optDouble("limitAmount", 0);
            if (cat.isEmpty()) continue;
            list.add(new Budget(userId, monthKey, cat, limit));
        }
        return list;
    }

    private List<SavingsGoal> parseSavingsGoals(JSONArray arr, long userId) {
        List<SavingsGoal> list = new ArrayList<>();
        if (arr == null) return list;
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o == null) continue;
            String title = o.optString("title", "");
            double target = o.optDouble("targetAmount", 0);
            String icon = o.optString("iconKey", "default");
            long created = o.optLong("createdAtUtc", System.currentTimeMillis());
            if (title.isEmpty()) continue;
            list.add(new SavingsGoal(userId, title, target, icon, created));
        }
        return list;
    }

    private List<SavingsMonthlyGoal> parseSavingsMonthlyGoals(JSONArray arr, long userId) {
        List<SavingsMonthlyGoal> list = new ArrayList<>();
        if (arr == null) return list;
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o == null) continue;
            int monthKey = o.optInt("monthKey", MonthUtils.monthKey(System.currentTimeMillis()));
            double target = o.optDouble("targetAmount", 0);
            list.add(new SavingsMonthlyGoal(userId, monthKey, target));
        }
        return list;
    }

    private List<SavingsContribution> parseSavingsContributions(JSONArray arr, long userId) {
        List<SavingsContribution> list = new ArrayList<>();
        if (arr == null) return list;
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o == null) continue;
            Long goalId = o.has("goalId") ? (o.isNull("goalId") ? null : o.optLong("goalId")) : null;
            double amount = o.optDouble("amount", 0);
            long date = o.optLong("dateUtc", 0);
            boolean isAuto = o.optBoolean("isAuto", false);
            if (date <= 0 || amount <= 0) continue;
            SavingsContribution c = new SavingsContribution(userId, goalId, amount, date);
            c.isAuto = isAuto;
            list.add(c);
        }
        return list;
    }

    private List<SourceItem> parseSources(JSONArray arr) {
        List<SourceItem> list = new ArrayList<>();
        if (arr == null) return list;
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o == null) continue;
            SourceItem item = parseSourceObject(o);
            if (item != null) list.add(item);
        }
        return list;
    }

    private SourceItem parseSourceObject(JSONObject o) {
        String id = o.optString("_id", "");
        String label = o.optString("label", "");
        String type = o.optString("type", "");
        if (label.isEmpty() || type.isEmpty()) return null;
        return new SourceItem(id, label, type);
    }

    private String findBudgetRemoteId(int monthKey, String category) {
        try {
            String path = "/budgets?monthKey=" + monthKey;
            ApiResult r = request("GET", path, null);
            if (r.statusCode == 200 && r.json != null) {
                JSONArray arr = r.json.optJSONArray("budgets");
                if (arr == null) return null;
                String normalized = category == null ? "" : category.trim().toLowerCase(Locale.getDefault());
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.optJSONObject(i);
                    if (o == null) continue;
                    String cat = o.optString("category", "").trim().toLowerCase(Locale.getDefault());
                    if (!normalized.isEmpty() && normalized.equals(cat)) {
                        String id = o.optString("_id", null);
                        if (id != null && !id.isEmpty()) return id;
                    }
                }
            }
        } catch (Exception ex) {
            Log.w(TAG, "findBudgetRemoteId failed", ex);
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
                try { json = new JSONObject(text); } catch (JSONException ignored) {}
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

    private static String resolveBaseUrl() {
        return com.example.personalfinancialmanagement.BuildConfig.API_BASE_URL;
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
