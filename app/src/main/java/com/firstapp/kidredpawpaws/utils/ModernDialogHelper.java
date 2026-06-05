package com.firstapp.kidredpawpaws.utils;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firstapp.kidredpawpaws.R;

import java.util.List;

public class ModernDialogHelper {

    public interface OnItemSelectedListener {
        void onItemSelected(int position);
    }

    /**
     * Shows a modern info dialog with a title, message, and a close button.
     */
    public static void showInfoDialog(Context context, String title, String message) {
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_modern_info, null);
        dialog.setContentView(view);

        TextView tvTitle = view.findViewById(R.id.tv_dialog_title);
        TextView tvMessage = view.findViewById(R.id.tv_dialog_message);
        Button btnClose = view.findViewById(R.id.btn_dialog_positive);

        tvTitle.setText(title);
        tvMessage.setText(message);
        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
        setupDialogWindow(context, dialog);
    }

    /**
     * Shows a modern details dialog with a vertical list of label-value pairs.
     */
    public static void showDetailsDialog(Context context, String title, List<DetailItem> details) {
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_modern_details, null);
        dialog.setContentView(view);

        TextView tvTitle = view.findViewById(R.id.tv_details_title);
        LinearLayout container = view.findViewById(R.id.ll_details_container);
        Button btnClose = view.findViewById(R.id.btn_details_close);

        tvTitle.setText(title);
        btnClose.setOnClickListener(v -> dialog.dismiss());

        for (DetailItem item : details) {
            View detailRow = LayoutInflater.from(context).inflate(R.layout.item_dialog_detail_row, container, false);
            TextView tvLabel = detailRow.findViewById(R.id.tv_detail_label);
            TextView tvValue = detailRow.findViewById(R.id.tv_detail_value);
            tvLabel.setText(item.label);
            tvValue.setText(item.value);
            container.addView(detailRow);
        }

        dialog.show();
        setupDialogWindow(context, dialog);
    }

    /**
     * Shows a modern list selection dialog.
     */
    public static void showListDialog(Context context, String title, List<String> items, int selectedIndex, OnItemSelectedListener listener) {
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_modern_list, null);
        dialog.setContentView(view);

        TextView tvTitle = view.findViewById(R.id.tv_dialog_list_title);
        RecyclerView rv = view.findViewById(R.id.rv_dialog_list);
        Button btnCancel = view.findViewById(R.id.btn_dialog_list_cancel);

        tvTitle.setText(title);
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        rv.setLayoutManager(new LinearLayoutManager(context));
        rv.setAdapter(new SimpleListAdapter(items, selectedIndex, position -> {
            listener.onItemSelected(position);
            dialog.dismiss();
        }));

        dialog.show();
        setupDialogWindow(context, dialog);
    }

    /**
     * Configures the dialog window for transparency and mobile-friendly sizing.
     * Sets width to 90% and height to WRAP_CONTENT, and makes background transparent.
     */
    public static void setupDialogWindow(Context context, Dialog dialog) {
        Window window = dialog.getWindow();
        if (window != null) {
            DisplayMetrics metrics = context.getResources().getDisplayMetrics();
            int width = (int) (metrics.widthPixels * 0.90);
            
            window.setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT);
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }

    public static class DetailItem {
        public String label;
        public String value;
        public DetailItem(String label, String value) {
            this.label = label;
            this.value = value;
        }
    }

    private static class SimpleListAdapter extends RecyclerView.Adapter<SimpleListAdapter.ViewHolder> {
        private final List<String> items;
        private final int selectedIndex;
        private final OnItemSelectedListener listener;

        public SimpleListAdapter(List<String> items, int selectedIndex, OnItemSelectedListener listener) {
            this.items = items;
            this.selectedIndex = selectedIndex;
            this.listener = listener;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_dialog_list, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            holder.tv.setText(items.get(position));
            
            if (position == selectedIndex) {
                holder.ivCheck.setVisibility(View.VISIBLE);
                holder.clickTarget.setBackgroundResource(R.drawable.bg_modal_chip_unselected);
            } else {
                holder.ivCheck.setVisibility(View.GONE);
                TypedValue outValue = new TypedValue();
                holder.itemView.getContext().getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
                holder.clickTarget.setBackgroundResource(outValue.resourceId);
            }
            
            holder.clickTarget.setOnClickListener(v -> listener.onItemSelected(position));
        }

        @Override
        public int getItemCount() { return items.size(); }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tv;
            ImageView ivCheck;
            View clickTarget;
            
            ViewHolder(View v) { 
                super(v); 
                tv = v.findViewById(R.id.tv_item_text); 
                ivCheck = v.findViewById(R.id.iv_item_check);
                if (v instanceof ViewGroup && ((ViewGroup) v).getChildCount() > 0) {
                    clickTarget = ((ViewGroup) v).getChildAt(0);
                } else {
                    clickTarget = v;
                }
            }
        }
    }
}
