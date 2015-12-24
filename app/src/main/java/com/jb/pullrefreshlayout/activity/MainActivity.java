package com.jb.pullrefreshlayout.activity;

import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.jb.pullrefreshlayout.R;
import com.jb.pullrefreshlayout.customview.PullRefreshLayout;
import com.jb.pullrefreshlayout.customview.handler.PullRefreshDataHandler;
import com.jb.pullrefreshlayout.customview.header.WaterDropDrawable;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements PullRefreshDataHandler, PullRefreshLayout.OnHideHeaderWhenRefreshingListener {

    ListView mListView;
    PullRefreshLayout mPullRefreshLayout;

    ArrayAdapter<String> mAdapter;

    List<String> mData = new ArrayList<>(50);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mPullRefreshLayout = (PullRefreshLayout) findViewById(R.id.pull_refresh);

        WaterDropDrawable header = new WaterDropDrawable();
        mPullRefreshLayout.setHeader(header, ViewGroup.LayoutParams.MATCH_PARENT, 200);
        // 设置刷新时能上滑隐藏头部
        mPullRefreshLayout.setCanHideHeaderWhenRefreshing(true);
        // 设置当正在刷新时隐藏头部的监听
        mPullRefreshLayout.setOnHideHeaderWhenRefreshingListener(this);

        mPullRefreshLayout.setContent(R.layout.list_view);

        mPullRefreshLayout.addRefreshUIHandler(header);
        mPullRefreshLayout.setRefreshDataHandler(this);

        mListView = (ListView) mPullRefreshLayout.getContent();
        mAdapter = new ArrayAdapter<>(this, android.R.layout.activity_list_item, android.R.id.text1, mData);
        mListView.setAdapter(mAdapter);

        addNewData();
    }

    /**
     * 添加10条新数据
     */
    private void addNewData() {
        int count = mData.size() + 1;
        for (int i = count; i < count + 10; i++) {
            mData.add(0, i + "");
        }
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean checkCanDoRefresh(PullRefreshLayout parent, View header, View content) {
        // 始终能够进行数据刷新
        return true;
    }

    @Override
    public void onRefreshBegin(PullRefreshLayout parent) {
        // 开始进行数据刷新
        addDataHandler.sendEmptyMessageDelayed(AddDataHandler.MSG_ADD_DATA, 2000);
    }

    @Override
    public void onHideHeaderWhenRefreshing(PullRefreshLayout parent) {
        // 当正在刷新时隐藏头部，就取消当前的刷新行为
        addDataHandler.removeMessages(AddDataHandler.MSG_ADD_DATA);
        // 取消行为注意要记得设置刷新结束，否则下次始终无法再进行刷新
        parent.refreshComplete();
    }

    AddDataHandler addDataHandler = new AddDataHandler(this);

    static class AddDataHandler extends Handler {
        public static int MSG_ADD_DATA = 0x534;

        WeakReference<MainActivity> reference;

        public AddDataHandler(MainActivity activity) {
            reference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            if (reference.get() == null) {
                return;
            }
            if (msg.what == MSG_ADD_DATA) {
                reference.get().addNewData();
                // 完成刷新
                reference.get().mPullRefreshLayout.refreshComplete();
            }
        }
    }
}
