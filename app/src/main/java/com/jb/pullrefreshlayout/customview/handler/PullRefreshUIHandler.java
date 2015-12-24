package com.jb.pullrefreshlayout.customview.handler;

import com.jb.pullrefreshlayout.customview.PullRefreshLayout;

/**
 *
 * Created by Jianbin on 2015/12/12.
 */
public interface PullRefreshUIHandler {

    void onUIReset(PullRefreshLayout parent);

    void onUIRefreshPrepare(PullRefreshLayout parent);

    void onUIRefreshBegin(PullRefreshLayout parent);

    void onUIRefreshComplete(PullRefreshLayout parent);

    void onUIPositionChange(PullRefreshLayout parent, boolean isUnderTouch, float currentHeaderHeightPx, float totalHeightPx);
}
