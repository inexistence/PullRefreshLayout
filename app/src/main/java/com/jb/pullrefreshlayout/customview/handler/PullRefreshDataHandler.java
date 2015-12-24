package com.jb.pullrefreshlayout.customview.handler;

import android.view.View;

import com.jb.pullrefreshlayout.customview.PullRefreshLayout;

/**
 * Created by Jianbin on 2015/12/12.
 */
public interface PullRefreshDataHandler {
    boolean checkCanDoRefresh(PullRefreshLayout parent, View header, View content);
    void onRefreshBegin(PullRefreshLayout parent);
}
