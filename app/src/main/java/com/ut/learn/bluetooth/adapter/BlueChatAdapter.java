package com.ut.learn.bluetooth.adapter;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.ut.learn.bluetooth.R;
import com.ut.learn.bluetooth.bean.MsgEntity;

import java.util.List;

/**
 * Created by admin on 2017/1/3.
 */

public class BlueChatAdapter extends BaseAdapter {
    private List<MsgEntity> mList;
    private Context mContext;
    private boolean isSelf;

    public BlueChatAdapter(Context context,List<MsgEntity> list){
        mContext = context;
        mList = list;
        isSelf = false;
    }
    @Override
    public int getCount() {
        return mList.size();
    }

    @Override
    public Object getItem(int position) {
        return mList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder vh = null;
        MsgEntity msgEntity = mList.get(position);
        if(convertView == null || (vh = (ViewHolder) convertView.getTag()).isOther != msgEntity.type){
            vh = new ViewHolder();
            if(msgEntity.type ==MsgEntity.SELF){
                convertView = View.inflate(mContext, R.layout.item_left, null);
                vh.tv = (TextView) convertView.findViewById(R.id.tv_self);
                vh.iv = (ImageView) convertView.findViewById(R.id.iv_self);
                vh.isOther = MsgEntity.SELF;
            }else{
                convertView = View.inflate(mContext, R.layout.item_right, null);
                vh.tv = (TextView) convertView.findViewById(R.id.tv_other);
                vh.iv = (ImageView) convertView.findViewById(R.id.iv_other);
                vh.isOther = MsgEntity.OTHER;
            }
            convertView.setTag(vh);
        }else{
            vh = (ViewHolder) convertView.getTag();
        }
        vh.tv.setText(msgEntity.msg);
        Log.e("adapter", "显示时:"+mList.get(position));
        return convertView;
    }

    class ViewHolder{
        TextView tv;
        ImageView iv;
        int isOther;
    }
}
