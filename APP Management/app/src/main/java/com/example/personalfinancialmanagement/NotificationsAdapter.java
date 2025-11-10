package com.example.personalfinancialmanagement;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

class NotificationsAdapter extends RecyclerView.Adapter<NotificationsAdapter.ViewHolder> {

    interface Listener {
        void onToggleRead(AppNotification item, boolean markRead);
        void onDelete(AppNotification item);
        void onAction(AppNotification item);
    }

    private final ArrayList<AppNotification> items = new ArrayList<>();
    private final Listener listener;

    NotificationsAdapter(Listener listener) {
        this.listener = listener;
    }

    void submit(List<AppNotification> notificationList) {
        items.clear();
        if (notificationList != null) {
            items.addAll(notificationList);
        }
        notifyDataSetChanged();
    }

    AppNotification getItem(int position) {
        return items.get(position);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AppNotification item = items.get(position);
        holder.bind(item, listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final View unreadBadge;
        private final ImageView icon;
        private final TextView title;
        private final TextView message;
        private final TextView time;
        private final TextView category;
        private final TextView btnAction;
        private final TextView btnToggleRead;
        private final ImageButton btnDelete;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            unreadBadge = itemView.findViewById(R.id.view_unread);
            icon = itemView.findViewById(R.id.iv_icon);
            title = itemView.findViewById(R.id.tv_title);
            message = itemView.findViewById(R.id.tv_message);
            time = itemView.findViewById(R.id.tv_time);
            category = itemView.findViewById(R.id.tv_category);
            btnAction = itemView.findViewById(R.id.btn_action);
            btnToggleRead = itemView.findViewById(R.id.btn_toggle_read);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }

        void bind(AppNotification item, Listener listener) {
            title.setText(item.title);
            message.setText(item.message);
            time.setText(formatRelative(item.createdAtUtc));

            boolean unread = !item.isRead;
            unreadBadge.setVisibility(unread ? View.VISIBLE : View.GONE);
            title.setTextColor(ContextCompat.getColor(itemView.getContext(),
                    unread ? R.color.primaryBlue : R.color.textPrimary));

            category.setText(getCategoryLabel(item.type));

            int iconRes = getIconRes(item.type);
            icon.setImageResource(iconRes);

            if (item.actionLabel != null && !item.actionLabel.trim().isEmpty()) {
                btnAction.setVisibility(View.VISIBLE);
                btnAction.setText(item.actionLabel);
                btnAction.setOnClickListener(v -> listener.onAction(item));
            } else {
                btnAction.setVisibility(View.GONE);
                btnAction.setOnClickListener(null);
            }

            btnToggleRead.setText(unread ? "Mark read" : "Mark unread");
            btnToggleRead.setOnClickListener(v -> listener.onToggleRead(item, unread));
            itemView.setOnClickListener(v -> listener.onToggleRead(item, unread));

            btnDelete.setOnClickListener(v -> listener.onDelete(item));
        }

        private int getIconRes(String type) {
            if (NotificationRepository.TYPE_BUDGET.equals(type)) return R.drawable.ic_wallet;
            if (NotificationRepository.TYPE_GOAL.equals(type)) return R.drawable.ic_star_outline;
            if (NotificationRepository.TYPE_REMINDER.equals(type)) return R.drawable.ic_calendar;
            if (NotificationRepository.TYPE_SYSTEM.equals(type)) return R.drawable.ic_settings_gear_outline;
            return R.drawable.ic_bell;
        }

        private String getCategoryLabel(String type) {
            if (NotificationRepository.TYPE_BUDGET.equals(type)) return "Budget";
            if (NotificationRepository.TYPE_GOAL.equals(type)) return "Goal";
            if (NotificationRepository.TYPE_REMINDER.equals(type)) return "Reminder";
            if (NotificationRepository.TYPE_SYSTEM.equals(type)) return "Product";
            return "General";
        }

        private String formatRelative(long timestamp) {
            long now = System.currentTimeMillis();
            long diff = Math.max(0, now - timestamp);
            long minutes = TimeUnit.MILLISECONDS.toMinutes(diff);
            if (minutes < 1) return "Just now";
            if (minutes < 60) return String.format(Locale.getDefault(), "%dm ago", minutes);
            long hours = TimeUnit.MINUTES.toHours(minutes);
            if (hours < 24) return String.format(Locale.getDefault(), "%dh ago", hours);
            long days = TimeUnit.HOURS.toDays(hours);
            if (days < 7) return String.format(Locale.getDefault(), "%dd ago", days);
            java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("MMM d", Locale.getDefault());
            return fmt.format(new java.util.Date(timestamp));
        }
    }
}
