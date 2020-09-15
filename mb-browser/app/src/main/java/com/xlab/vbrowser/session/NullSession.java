/*Copyright by MonnyLab*/

package com.xlab.vbrowser.session;

import com.xlab.vbrowser.utils.UrlConstants;

/**
 * An "empty" session object that can be used instead of <code>null</code>.
 */
public class NullSession extends Session {
    public NullSession() {
        super(Source.NONE, UrlConstants.getHomeUrl());
    }
}
