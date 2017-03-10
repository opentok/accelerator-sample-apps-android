package com.tokbox.android.accelerator.sample.ui;

import android.content.Context;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TableRow;

import com.tokbox.android.accelerator.sample.R;
import com.tokbox.android.otsdkwrapper.utils.MediaType;

import java.util.ArrayList;
import java.util.List;

public class ParticipantsAdapter extends RecyclerView.Adapter<ParticipantsAdapter.ParticipantViewHolder> {
    private List<Participant> mParticipantsList = new ArrayList<>();

    public ParticipantsAdapter(Context context, List<Participant> participantsList) throws Exception {
        if (participantsList == null) {
            throw new Exception("ParticipantsList cannot be null");
        }
        this.mParticipantsList = participantsList;
    }

    @Override
    public ParticipantViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(viewType, parent, false);

        return new ParticipantViewHolder(view);
    }

    @Override
    public int getItemViewType(int position) {
        return R.layout.grid_item;
    }

    @Override
    public void onBindViewHolder(ParticipantViewHolder holder, int position) {
        Log.i("MARINAS", "VIEW HOLDER");
        Participant participant = mParticipantsList.get(position);
        holder.container.removeAllViews();

        TableRow.LayoutParams params = new TableRow.LayoutParams(participant.getContainer().getWidth(), participant.getContainer().getHeight()); // (width, height)
        holder.container.setLayoutParams(params);

        if (!participant.getStatus().has(MediaType.VIDEO) || (participant.getType().equals(Participant.Type.REMOTE) && !participant.getStatus().subscribedTo(MediaType.VIDEO))) {
            holder.audiOnlyView.setVisibility(View.VISIBLE);
            holder.container.addView(holder.audiOnlyView, params);
        } else {
            holder.audiOnlyView.setVisibility(View.GONE);
            if (participant.getStatus().getView() != null) {
                ViewGroup parent = (ViewGroup) participant.getStatus().getView().getParent();
                if (parent != null) {
                    parent.removeView(participant.getStatus().getView());
                }
                holder.container.addView(participant.getStatus().getView());
            }
        }
    }

    @Override
    public int getItemCount() {
        return (null != mParticipantsList ? mParticipantsList.size() : 0);
    }


    class ParticipantViewHolder extends RecyclerView.ViewHolder {
        public RelativeLayout audiOnlyView;
        public RelativeLayout container;

        public ParticipantViewHolder(View view) {
            super(view);
            this.audiOnlyView = (RelativeLayout) view.findViewById(R.id.audioOnlyView);
            this.container = (RelativeLayout) view.findViewById(R.id.itemView);
        }
    }
}