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
import android.widget.TextView;

import com.tokbox.android.accelerator.sample.R;
import com.tokbox.android.otsdkwrapper.utils.MediaType;

import java.util.ArrayList;
import java.util.List;

public class ParticipantsAdapter extends RecyclerView.Adapter<ParticipantsAdapter.ParticipantViewHolder> {
    private Context context;
    private List<Participant> mParticipantsList = new ArrayList<Participant>();

    public ParticipantsAdapter(Context context, List<Participant> participantsList) throws Exception {
        this.context = context;
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
        Participant participant = mParticipantsList.get(position);
        holder.container.removeViewAt(holder.container.getChildCount()-1);

        TableRow.LayoutParams params = new TableRow.LayoutParams(participant.getContainer().getWidth(), participant.getContainer().getHeight()); // (width, height)
        holder.container.setLayoutParams(params);

        if (!participant.getStatus().has(MediaType.VIDEO) || participant.getStatus().subscribedTo(MediaType.VIDEO)){
            Log.i("MARINAS", "AUDIO ONLY");
            holder.container.getChildAt(holder.container.getChildCount()-1).setVisibility(View.GONE);
            holder.audiOnlyView.setVisibility(View.VISIBLE);
        }
        else {
            //holder.audiOnlyView.setVisibility(View.GONE);
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