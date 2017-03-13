package com.tokbox.android.accelerator.sample.ui;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
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
    private ParticipantAdapterListener mListener;

    public interface ParticipantAdapterListener {
        void mediaControlChanged(String remoteId);
    }

    public ParticipantsAdapter(Context context, List<Participant> participantsList, ParticipantAdapterListener listener) throws Exception {
        if (participantsList == null) {
            throw new Exception("ParticipantsList cannot be null");
        }
        this.mParticipantsList = participantsList;
        this.mListener = listener;
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
        holder.container.removeAllViews();

        //add id
        holder.id = participant.getId();
        holder.type = participant.getType();
        holder.listener = mListener;

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
        protected RelativeLayout audiOnlyView;
        protected RelativeLayout container;
        protected RelativeLayout controls;
        protected String id;
        protected Participant.Type type;
        protected ParticipantAdapterListener listener;

        public ParticipantViewHolder(View view) {
            super(view);
            this.audiOnlyView = (RelativeLayout) view.findViewById(R.id.audioOnlyView);
            this.container = (RelativeLayout) view.findViewById(R.id.itemView);
            this.controls = (RelativeLayout) view.findViewById(R.id.remoteControls);

            view.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View view){
                    if (type.equals(Participant.Type.REMOTE)) {
                        container.removeView(controls);
                        if (controls.getVisibility() == View.GONE) {
                            controls.setVisibility(View.VISIBLE);
                            container.addView(controls);
                        }
                        else {
                            controls.setVisibility(View.GONE);
                        }
                        listener.mediaControlChanged(id);
                    }
                }
            });
        }


    }


}