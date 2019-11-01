package com.walixiwa.dlnaserver.binder;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.walixiwa.dlnaserver.R;
import com.walixiwa.model.DeviceModel;

import me.drakeet.multitype.ItemViewBinder;

public class DlnaItemBinder extends ItemViewBinder<DeviceModel, DlnaItemBinder.ViewHolder> {
    private OnItemClickListener onItemClickListener;

    public DlnaItemBinder() {
    }

    @Override
    protected @NonNull
    ViewHolder onCreateViewHolder(@NonNull LayoutInflater inflater, @NonNull ViewGroup parent) {
        return new ViewHolder(inflater.inflate(R.layout.item_dlna_display, parent, false));
    }


    @Override
    protected void onBindViewHolder(@NonNull ViewHolder holder, @NonNull final DeviceModel deviceModel) {
        holder.tv_title.setText(deviceModel.getDeviceName());
        holder.v_line.setVisibility(holder.getAdapterPosition() == getAdapter().getItemCount() - 1 ? View.GONE : View.VISIBLE);
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (onItemClickListener != null) {
                    onItemClickListener.onItemClick(deviceModel);
                }
            }
        });
    }


    public class ViewHolder extends RecyclerView.ViewHolder {
        private TextView tv_title;
        private View v_line;

        private ViewHolder(View viewRoot) {
            super(viewRoot);
            tv_title = viewRoot.findViewById(R.id.tv_title);
            v_line = viewRoot.findViewById(R.id.v_line);
        }
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    public interface OnItemClickListener {
        void onItemClick(DeviceModel display);
    }
}