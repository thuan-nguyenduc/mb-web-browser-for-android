/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 Copyright by MonnyLab */
package com.xlab.vbrowser.webview.matcher;


import android.net.Uri;
import android.text.TextUtils;

import com.xlab.vbrowser.utils.UrlUtils;
import com.xlab.vbrowser.webview.matcher.Trie.WhiteListTrie;
import com.xlab.vbrowser.webview.matcher.util.FocusString;

/* package-private */ class EntityList {

    private WhiteListTrie rootNode;

    public EntityList() {
        rootNode = WhiteListTrie.createRootNode();
    }

    public void putWhiteList(final FocusString revhost, final Trie whitelist) {
        rootNode.putWhiteList(revhost, whitelist);
    }

    public boolean isWhiteListed(final Uri site, final Uri resource) {
        if (TextUtils.isEmpty(site.getHost()) ||
                TextUtils.isEmpty(resource.getHost()) ||
                site.getScheme().equals("data")) {
            return false;
        }

        if (UrlUtils.isPermittedResourceProtocol(resource.getScheme()) &&
                UrlUtils.isSupportedProtocol(site.getScheme())) {
            final FocusString revSitehost = FocusString.create(site.getHost()).reverse();
            final FocusString revResourcehost = FocusString.create(resource.getHost()).reverse();

            return isWhiteListed(revSitehost, revResourcehost, rootNode);
        } else {
            // This might be some imaginary/custom protocol: theguardian.com loads
            // things like "nielsenwebid://nuid/999" and/or sets an iFrame URL to that:
            return false;
        }
    }

    private boolean isWhiteListed(final FocusString site, final FocusString resource, final Trie revHostTrie) {
        final WhiteListTrie next = (WhiteListTrie) revHostTrie.children.get(site.charAt(0));

        if (next == null) {
            // No matches
            return false;
        }

        if (next.whitelist != null &&
                next.whitelist.findNode(resource) != null) {
            return true;
        }

        if (site.length() == 1) {
            return false;
        }

        return isWhiteListed(site.substring(1), resource, next);
    }
}
