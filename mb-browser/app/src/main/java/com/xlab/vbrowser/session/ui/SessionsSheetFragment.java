/*Copyright by MonnyLab*/

package com.xlab.vbrowser.session.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.widget.ImageButton;
import android.widget.Toast;

import com.xlab.vbrowser.session.Source;
import com.xlab.vbrowser.trackers.GaReport;
import com.xlab.vbrowser.utils.BackgroundTask;
import com.xlab.vbrowser.utils.IBackgroundTask;
import com.xlab.vbrowser.R;
import com.xlab.vbrowser.activity.MainActivity;
import com.xlab.vbrowser.locale.LocaleAwareFragment;
import com.xlab.vbrowser.session.Session;
import com.xlab.vbrowser.session.SessionManager;
import com.xlab.vbrowser.utils.OneShotOnPreDrawListener;
import com.xlab.vbrowser.utils.Settings;
import com.xlab.vbrowser.utils.UrlConstants;
import com.xlab.vbrowser.web.WebViewProvider;

public class SessionsSheetFragment extends LocaleAwareFragment implements View.OnClickListener {
    public static final String FRAGMENT_TAG = "tab_sheet";

    private static final int ANIMATION_DURATION = 200;

    private View backgroundView;
    private View cardView;
    private View progressOverlayView;
    private View signOutButtonView;
    private View newTabButtonView;
    private ImageButton incognitoButtonView;
    private RecyclerView sessionView;
    private boolean isAnimating;

    private MainActivity baseActivity;
    private Settings settings;
    private Context context;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_sessionssheet, container, false);
        context = getContext();
        settings = Settings.getInstance(context);

        baseActivity = (MainActivity) getActivity();
        backgroundView = view.findViewById(R.id.background);
        backgroundView.setOnClickListener(this);

        cardView = view.findViewById(R.id.card);
        cardView.getViewTreeObserver().addOnPreDrawListener(new OneShotOnPreDrawListener(cardView) {
            @Override
            protected void onPreDraw(View view) {
                playAnimation(false);
            }
        });

        progressOverlayView = view.findViewById(R.id.progressOverlayView);

        signOutButtonView = view.findViewById(R.id.signOutButtonView);
        signOutButtonView.setOnClickListener(this);

        newTabButtonView = view.findViewById(R.id.newTabButtonView);
        newTabButtonView.setOnClickListener(this);

        incognitoButtonView = view.findViewById(R.id.incognitoButtonView);
        incognitoButtonView.setOnClickListener(this);

        sessionView = view.findViewById(R.id.sessions);
        final SessionsAdapter sessionsAdapter = new SessionsAdapter(this);
        SessionManager.getInstance().getSessions().observe(this, sessionsAdapter);

        LinearLayoutManager layoutManager = new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false);
        sessionView.setLayoutManager(layoutManager);
        sessionView.setAdapter(sessionsAdapter);
        layoutManager.scrollToPositionWithOffset(SessionManager.getInstance().getPositionOfCurrentSession(), 0);

        applySwipeToDelete();

        GaReport.sendReportScreen(context, SessionsSheetFragment.class.getName());

        updateIncognitoState();

        return view;
    }

    private void updateIncognitoState() {
        incognitoButtonView.setImageDrawable(settings.isIncognitoEnabled() ? context.getDrawable(R.drawable.ic_incognito_enabled)
                : context.getDrawable(R.drawable.ic_incognito_normal));
    }

    private void applySwipeToDelete() {
        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(final RecyclerView.ViewHolder viewHolder, int direction) {
                final int position = viewHolder.getAdapterPosition(); //get position which is swipe

                if (direction == ItemTouchHelper.LEFT || direction == ItemTouchHelper.RIGHT) {    //if swipe left or right
                    Session session = SessionManager.getInstance().getSessions().getValue().get(viewHolder.getAdapterPosition());
                    SessionManager.getInstance().removeSession(session);
                    GaReport.sendReportEvent(getContext(), "deleted session on swipe", SessionsSheetFragment.class.getName());
                }
            }
        };
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleCallback);
        itemTouchHelper.attachToRecyclerView(sessionView); //set swipe to recylcerview
    }


    private Animator playAnimation(final boolean reverse) {
        isAnimating = true;

        final int offset = getResources().getDimensionPixelSize(R.dimen.floating_action_button_size) / 2;
        final int cx = cardView.getMeasuredWidth() - offset;
        final int cy = cardView.getMeasuredHeight() - offset;

        // The final radius is the diagonal of the card view -> sqrt(w^2 + h^2)
        final float fullRadius = (float) Math.sqrt(
                Math.pow(cardView.getWidth(), 2) + Math.pow(cardView.getHeight(), 2));

        final Animator sheetAnimator = ViewAnimationUtils.createCircularReveal(
                cardView, cx, cy, reverse ? fullRadius : 0, reverse ? 0 : fullRadius);
        sheetAnimator.setDuration(ANIMATION_DURATION);
        sheetAnimator.setInterpolator(new AccelerateInterpolator());
        sheetAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                try {
                    cardView.setVisibility(View.VISIBLE);
                }
                catch (java.lang.IllegalStateException e) {
                    //Ignore this known exception
                }
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                try {
                    isAnimating = false;

                    cardView.setVisibility(reverse ? View.GONE : View.VISIBLE);
                }
                catch (java.lang.IllegalStateException e) {
                    //Ignore this known exception
                }
            }
        });
        sheetAnimator.start();

        backgroundView.setAlpha(reverse ? 1f : 0f);
        backgroundView.animate()
                .alpha(reverse ? 0f : 1f)
                .setDuration(ANIMATION_DURATION)
                .start();

        return sheetAnimator;
    }

    /* package */ Animator animateAndDismiss() {
        final Animator animator = playAnimation(true);

        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                try {
                    final MainActivity activity = (MainActivity) getActivity();
                    if (activity != null) {
                        activity.getSupportFragmentManager()
                                .beginTransaction()
                                .remove(SessionsSheetFragment.this)
                                .commit();
                    }
                }
                catch(IllegalStateException e) {
                    //Ignore this known exception
                }
            }
        });

        return animator;
    }

    public boolean onBackPressed() {
        animateAndDismiss();
        return true;
    }

    @Override
    public void applyLocale() {}

    @Override
    public void onClick(View view) {
        if (isAnimating) {
            // Ignore touched while we are animating
            return;
        }

        switch (view.getId()) {
            case R.id.background:
                animateAndDismiss();
                break;

            case R.id.signOutButtonView:
                animateAndDismiss().addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animator) {
                        try {
                            backgroundView.setVisibility(View.GONE);
                            cardView.setVisibility(View.GONE);
                            progressOverlayView.setVisibility(View.VISIBLE);
                            new BackgroundTask(new IBackgroundTask() {
                                @Override
                                public void run() {
                                    WebViewProvider.signoutOfWebsites();
                                }

                                @Override
                                public void onComplete() {
                                    SessionManager.getInstance().removeAllSessions();
                                    //Show info
                                    if (baseActivity != null) {
                                        baseActivity.showEraseInfo(R.string.feedback_signout);
                                    }
                                }
                            }).execute();

                            GaReport.sendReportEvent(getContext(), "signOutQuickly", SessionsSheetFragment.class.getName());
                        }
                        catch(IllegalStateException e) {
                            //Ignore this known exception
                        }
                    }
                });

                break;

            case R.id.newTabButtonView:
                animateAndDismiss().addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        try {
                            super.onAnimationEnd(animation);
                            SessionManager.getInstance().createSession(Source.NEWTAB_BOTTOM_BAR, UrlConstants.getHomeUrl());
                            GaReport.sendReportEvent(getContext(), "NewTab", SessionsSheetFragment.class.getName());
                        }
                        catch(IllegalStateException e) {
                            //Ignore this known exception
                        }
                    }
                });

                break;

            case R.id.incognitoButtonView:
                settings.setIncognitoEnabled(!settings.isIncognitoEnabled());
                updateIncognitoState();
                animateAndDismiss();
                Toast.makeText(context, settings.isIncognitoEnabled() ? context.getString(R.string.incognito_on_info)
                                            : context.getString(R.string.incognito_off_info), Toast.LENGTH_SHORT).show();
                break;


            default:
                throw new IllegalStateException("Unhandled view in onClick()");
        }
    }
}
