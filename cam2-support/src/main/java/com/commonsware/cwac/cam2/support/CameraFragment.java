/**
 * Copyright (c) 2015 CommonsWare, LLC
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.commonsware.cwac.cam2.support;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import com.commonsware.cwac.cam2.CameraController;
import com.commonsware.cwac.cam2.CameraEngine;
import com.commonsware.cwac.cam2.CameraView;
import com.commonsware.cwac.cam2.PictureTransaction;
import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;
import java.io.File;
import java.util.ArrayList;
import de.greenrobot.event.EventBus;

/**
 * Fragment for displaying a camera preview, with hooks to allow
 * you (or the user) to take a picture.
 */
public class CameraFragment extends Fragment {
  /**
   * Interface that all hosting activities must implement.
   */
  public interface Contract {
    /**
     * Used by CameraFragment to indicate that the user has
     * taken a photo, for activities that wish to take a specific
     * action at this point (e.g., set a result and finish).
     */
    void completeRequest();
  }

  private CameraController ctrl;
  private ViewGroup previewStack;

  /**
   * Standard fragment entry point.
   *
   * @param savedInstanceState State of a previous instance
   */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setRetainInstance(true);
  }

  /**
   * Standard lifecycle method, for when the fragment becomes
   * attached to an activity. Used here to validate the contract.
   *
   * @param activity the hosting activity
   */
  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);

    if (!(activity instanceof Contract)) {
      throw new IllegalArgumentException("Hosting activity must implement CameraFragment.Contract");
    }
  }

  /**
   * Standard lifecycle method, for when the fragment moves into
   * the started state. Passed along to the CameraController.
   */
  @Override
  public void onStart() {
    super.onStart();

    EventBus.getDefault().register(this);

    if (ctrl!=null) {
      ctrl.start();
    }
  }

  /**
   * Standard lifecycle method, for when the fragment moves into
   * the stopped state. Passed along to the CameraController.
   */
  @Override
  public void onStop() {
    if (ctrl!=null) {
      ctrl.stop();
    }

    EventBus.getDefault().unregister(this);

    super.onStop();
  }

  /**
   * Standard lifecycle method, for when the fragment is utterly,
   * ruthlessly destroyed. Passed along to the CameraController,
   * because why should the fragment have all the fun?
   */
  @Override
  public void onDestroy() {
    if (ctrl!=null) {
      ctrl.destroy();
    }

    super.onDestroy();
  }

  /**
   * Standard callback method to create the UI managed by
   * this fragment.
   *
   * @param inflater Used to inflate layouts
   * @param container Parent of the fragment's UI (eventually)
   * @param savedInstanceState State of a previous instance
   * @return the UI being managed by this fragment
   */
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View v=inflater.inflate(R.layout.cwac_cam2_fragment, container, false);

    previewStack=(ViewGroup)v.findViewById(R.id.cwac_cam2_preview_stack);

    FloatingActionButton fab=(FloatingActionButton)v.findViewById(R.id.cwac_cam2_picture);

    fab.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        File dcim=Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
        File dir=new File(dcim, "Cam2");

        dir.mkdirs();

        File testOutput=new File(dir, "test.jpg");

        PictureTransaction xact=new PictureTransaction.Builder()
            .toFile(testOutput.getAbsolutePath(), true).build();

        ctrl.takePicture(xact);
      }
    });

    fab=(FloatingActionButton)v.findViewById(R.id.cwac_cam2_switch_camera);
    fab.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        ctrl.switchCamera();
      }
    });

    changeMenuIconAnimation((FloatingActionMenu)v.findViewById(R.id.cwac_cam2_settings));

    return(v);
  }

  /**
   * @return the CameraController this fragment delegates to
   */
  public CameraController getController() {
    return(ctrl);
  }

  /**
   * Establishes the controller that this fragment delegates to
   *
   * @param ctrl the controller that this fragment delegates to
   */
  public void setController(CameraController ctrl) {
    this.ctrl=ctrl;
  }

  @SuppressWarnings("unused")
  public void onEventMainThread(CameraController.ControllerReadyEvent event) {
    ArrayList<CameraView> cameraViews=new ArrayList<CameraView>();
    CameraView cv=(CameraView)previewStack.getChildAt(0);

    cv.setEngine(ctrl.getEngine());
    cameraViews.add(cv);

    for (int i=1;i<event.getNumberOfCameras();i++) {
      cv=new CameraView(getActivity());
      cv.setVisibility(View.INVISIBLE);
      previewStack.addView(cv);
      cv.setEngine(ctrl.getEngine());
      cameraViews.add(cv);
    }

    ctrl.setCameraViews(cameraViews);
  }

  @SuppressWarnings("unused")
  public void onEventMainThread(CameraEngine.PictureTakenEvent event) {
    // TODO: figure out how to handle this, as I cannot quit
    // immediately, as otherwise the rest of the camera system
    // gets cranky

    // getContract().completeRequest();
    android.util.Log.e(getClass().getSimpleName(), "picture taken!");
  }

  private Contract getContract() {
    return((Contract)getActivity());
  }

  // based on https://goo.gl/3IUM8K

  private void changeMenuIconAnimation(final FloatingActionMenu menu) {
    AnimatorSet set=new AnimatorSet();
    final ImageView v=menu.getMenuIconView();
    ObjectAnimator scaleOutX=ObjectAnimator.ofFloat(v, "scaleX", 1.0f, 0.2f);
    ObjectAnimator scaleOutY=ObjectAnimator.ofFloat(v, "scaleY", 1.0f, 0.2f);
    ObjectAnimator scaleInX=ObjectAnimator.ofFloat(v, "scaleX", 0.2f, 1.0f);
    ObjectAnimator scaleInY=ObjectAnimator.ofFloat(v, "scaleY", 0.2f, 1.0f);

    scaleOutX.setDuration(50);
    scaleOutY.setDuration(50);

    scaleInX.setDuration(150);
    scaleInY.setDuration(150);
    scaleInX.addListener(new AnimatorListenerAdapter() {
      @Override
      public void onAnimationStart(Animator animation) {
        v.setImageResource(menu.isOpened()
            ? R.drawable.cwac_cam2_ic_close
            : R.drawable.cwac_cam2_ic_action_settings);
      }
    });

    set.play(scaleOutX).with(scaleOutY);
    set.play(scaleInX).with(scaleInY).after(scaleOutX);
    set.setInterpolator(new OvershootInterpolator(2));
    menu.setIconToggleAnimatorSet(set);
  }
}