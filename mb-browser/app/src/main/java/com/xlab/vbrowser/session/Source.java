/*Copyright by MonnyLab*/

package com.xlab.vbrowser.session;

/**
 * The source of a sessio: How was this session created? E.g. did we receive an Intent or did the
 * user start this session?
 */
public enum Source {
    /**
     * Intent.ACTION_VIEW
     */
    VIEW,

    /**
     * Intent.ACTION_SEND
     */
    SHARE,

    /**
     * Via text selection action ("Search privately")
     */
    TEXT_SELECTION,

    /**
     * Via a home screen shortcut.
     */
    HOME_SCREEN,

    /**
     * The user entered a URL (or search ter,s)
     */
    USER_ENTERED,

    /**
     * Custom tab from a third-party application.
     */
    CUSTOM_TAB,

    /**
     * Open as a new tab from the (context( menu.
     */
    MENU,

    /**
     * Open as a new tab from NewTab button on BottonNavigationBar
     */
    NEWTAB_BOTTOM_BAR,

    /**
     * Page requests a new tab when user open link or execute window.open
     */
    PAGE_REQUEST_NEWTAB,

    /**
     * Open first blank tab
     */
    FIRST_BLANK_TAB,

    /**
     * Open page from history
     */
    HISTORY,

    /**
     * Open page from bookmark
     */
    BOOKMARK,

    /**
     * Only used internally if we need to temporarily create a session object with no specific source.
     */
    NONE
}
