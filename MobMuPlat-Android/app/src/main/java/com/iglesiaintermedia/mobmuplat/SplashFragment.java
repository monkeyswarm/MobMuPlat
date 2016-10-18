package com.iglesiaintermedia.mobmuplat;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;

public class SplashFragment extends Fragment {

    View rootView;
    ImageView ringView, titleView, crossView, resistorView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_splash, container,
                false);
        ringView = (ImageView)rootView.findViewById(R.id.imageViewRing);
        titleView = (ImageView)rootView.findViewById(R.id.imageViewTitle);
        crossView = (ImageView)rootView.findViewById(R.id.imageViewCross);
        resistorView = (ImageView)rootView.findViewById(R.id.imageViewResistor);

        rootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                rootView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                animate();
            }
        });

        return rootView;
    }


    public void animate() {
        // fromX, toX, fromY, toY
        TranslateAnimation translateAnimationRing =
                new TranslateAnimation(Animation.ABSOLUTE, rootView.getWidth() / 2 - ringView.getWidth() / 2,
                        Animation.ABSOLUTE, rootView.getWidth() / 2 - ringView.getWidth() / 2,
                        Animation.ABSOLUTE, -ringView.getHeight() ,
                        Animation.ABSOLUTE, rootView.getHeight() / 2 - ringView.getHeight() / 2);
        translateAnimationRing.setDuration(2000);
        translateAnimationRing.setFillAfter(true);
        translateAnimationRing.setFillEnabled(true);

        TranslateAnimation translateAnimationTitle =
                new TranslateAnimation(Animation.ABSOLUTE, rootView.getWidth() / 2 - titleView.getWidth() / 2,
                        Animation.ABSOLUTE, rootView.getWidth() / 2 - titleView.getWidth() / 2,
                        Animation.ABSOLUTE, rootView.getHeight() + titleView.getHeight() ,
                        Animation.ABSOLUTE, rootView.getHeight() / 2 + ringView.getHeight() / 2 + 20 );// below ringview titleView.getHeight() / 2);
        translateAnimationTitle.setDuration(2000);
        translateAnimationTitle.setFillAfter(true);
        translateAnimationTitle.setFillEnabled(true);

        TranslateAnimation translateAnimationCross =
                new TranslateAnimation(Animation.ABSOLUTE,  -crossView.getWidth(),
                        Animation.ABSOLUTE, rootView.getWidth() / 2 - ringView.getWidth() / 6 - crossView.getWidth() / 2 ,
                        Animation.ABSOLUTE, rootView.getHeight() / 2 - crossView.getHeight() / 2,
                        Animation.ABSOLUTE, rootView.getHeight() / 2 - crossView.getHeight() / 2);
        translateAnimationCross.setDuration(2000);
        translateAnimationCross.setFillAfter(true);
        translateAnimationCross.setFillEnabled(true);

        TranslateAnimation translateAnimationResistor =
                new TranslateAnimation(Animation.ABSOLUTE, rootView.getWidth() + resistorView.getWidth(),
                        Animation.ABSOLUTE, rootView.getWidth() / 2 + ringView.getWidth() / 6 - resistorView.getWidth() / 2,
                        Animation.ABSOLUTE, rootView.getHeight() / 2 - resistorView.getHeight() / 2,
                        Animation.ABSOLUTE, rootView.getHeight() / 2 - resistorView.getHeight() / 2);
        translateAnimationResistor.setDuration(2000);
        translateAnimationResistor.setFillAfter(true);
        translateAnimationResistor.setFillEnabled(true);

        ringView.startAnimation(translateAnimationRing);
        titleView.startAnimation(translateAnimationTitle);
        crossView.startAnimation(translateAnimationCross);
        resistorView.startAnimation(translateAnimationResistor);
    }
}
