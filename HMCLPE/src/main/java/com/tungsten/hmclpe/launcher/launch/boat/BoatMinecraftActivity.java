package com.tungsten.hmclpe.launcher.launch.boat;

import android.annotation.SuppressLint;
import android.graphics.SurfaceTexture;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.drawerlayout.widget.DrawerLayout;

import com.tungsten.hmclpe.R;
import com.tungsten.hmclpe.control.MenuHelper;
import com.tungsten.hmclpe.control.view.LayoutPanel;
import com.tungsten.hmclpe.launcher.launch.GameLaunchSetting;

import java.util.Timer;
import java.util.TimerTask;

import cosine.boat.BoatActivity;
import cosine.boat.BoatInput;
import cosine.boat.keyboard.BoatKeycodes;

public class BoatMinecraftActivity extends BoatActivity implements View.OnTouchListener {

    private GameLaunchSetting gameLaunchSetting;

    private DrawerLayout drawerLayout;
    private LayoutPanel baseLayout;

    private ImageView mouseCursor;
    private Button baseTouchPad;

    private int initialX;
    private int initialY;
    private int baseX;
    private int baseY;

    private long downTime;
    private Timer longClickTimer;

    private boolean customSettingPointer = false;
    private boolean padSettingPointer = false;

    private int cursorMode = BoatInput.CursorEnabled;

    public MenuHelper menuHelper;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        gameLaunchSetting = GameLaunchSetting.getGameLaunchSetting(getIntent().getExtras().getString("setting_path"));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            if (gameLaunchSetting.fullscreen) {
                getWindow().getAttributes().layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            } else {
                getWindow().getAttributes().layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_NEVER;
            }
        }
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN, WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);

        setContentView(cosine.boat.R.layout.activity_boat);

        DrawerLayout.LayoutParams params = new DrawerLayout.LayoutParams(DrawerLayout.LayoutParams.MATCH_PARENT, DrawerLayout.LayoutParams.MATCH_PARENT);

        drawerLayout = (DrawerLayout) getLayoutInflater().inflate(R.layout.activity_minecraft,null) ;
        addContentView(drawerLayout,params);

        baseLayout = findViewById(R.id.base_layout);

        mouseCursor = findViewById(R.id.mouse_cursor);
        baseTouchPad = findButton(R.id.base_touch_pad);

        scaleFactor = gameLaunchSetting.scaleFactor;

        this.setBoatCallback(new BoatCallback() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                surface.setDefaultBufferSize((int) (width * scaleFactor), (int) (height * scaleFactor));
                BoatActivity.setBoatNativeWindow(new Surface(surface));
                startGame(gameLaunchSetting.javaPath,
                        gameLaunchSetting.home,
                        GameLaunchSetting.isHighVersion(gameLaunchSetting),
                        BoatLauncher.getMcArgs(gameLaunchSetting,BoatMinecraftActivity.this,(int) (width * scaleFactor), (int) (height * scaleFactor),gameLaunchSetting.server),
                        gameLaunchSetting.boatRenderer);
            }

            @Override
            public void onCursorModeChange(int mode) {
                cursorMode = mode;
                cursorModeHandler.sendEmptyMessage(mode);
            }
        });

        init();

        menuHelper = new MenuHelper(this,this,drawerLayout,baseLayout,false,gameLaunchSetting.controlLayout);
    }

    @SuppressLint("HandlerLeak")
    private Handler cursorModeHandler = new Handler(){
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            if (msg.what == BoatInput.CursorDisabled){
                mouseCursor.setVisibility(View.INVISIBLE);
            }
            if (msg.what == BoatInput.CursorEnabled){
                mouseCursor.setVisibility(View.VISIBLE);
            }
        }
    };

    @SuppressLint("ClickableViewAccessibility")
    private Button findButton(int id){
        Button button = findViewById(id);
        button.setOnTouchListener(this);
        return button;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (v == baseTouchPad){
            if (cursorMode == BoatInput.CursorDisabled){
                switch(event.getActionMasked()){
                    case MotionEvent.ACTION_DOWN:
                        initialX = (int)event.getX();
                        initialY = (int)event.getY();
                        downTime = System.currentTimeMillis();
                        longClickTimer = new Timer();
                        longClickTimer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                BoatInput.setMouseButton(BoatInput.Button1,true);
                            }
                        },400);
                    case MotionEvent.ACTION_MOVE:
                        if (!customSettingPointer){
                            padSettingPointer = true;
                            BoatInput.setPointer(baseX + (int)event.getX() -initialX, baseY + (int)event.getY() - initialY);
                        }
                        if (Math.abs(event.getX() - initialX) > 10 && Math.abs(event.getY() - initialY) > 10){
                            longClickTimer.cancel();
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                        longClickTimer.cancel();
                        if (padSettingPointer){
                            baseX += ((int)event.getX() - initialX);
                            baseY += ((int)event.getY() - initialY);
                            BoatInput.setPointer(baseX,baseY);
                            padSettingPointer = false;
                        }
                        BoatInput.setMouseButton(BoatInput.Button1,false);
                        if (Math.abs(event.getX() - initialX) <= 10 && Math.abs(event.getY() - initialY) <= 10 && System.currentTimeMillis() - downTime <= 200){
                            BoatInput.setMouseButton(BoatInput.Button3,true);
                            BoatInput.setMouseButton(BoatInput.Button3,false);
                        }
                        break;
                    default:
                        break;
                }
            }
            else {
                baseX = (int)event.getX();
                baseY = (int)event.getY();
                BoatInput.setPointer((int) (baseX * scaleFactor),(int) (baseY * scaleFactor));
                if (event.getActionMasked() == MotionEvent.ACTION_DOWN){
                    BoatInput.setMouseButton(BoatInput.Button1,true);
                }
                if (event.getActionMasked() == MotionEvent.ACTION_UP){
                    BoatInput.setMouseButton(BoatInput.Button1,false);
                }
            }
            mouseCursor.setX(event.getX());
            mouseCursor.setY(event.getY());
            return true;
        }
        return false;
    }

    @Override
    public void onBackPressed() {
        BoatInput.setKey(BoatKeycodes.BOAT_KEYBOARD_Escape,0,true);
        BoatInput.setKey(BoatKeycodes.BOAT_KEYBOARD_Escape,0,false);
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            if (gameLaunchSetting.fullscreen) {
                getWindow().getAttributes().layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            } else {
                getWindow().getAttributes().layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_NEVER;
            }
        }
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN, WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);
    }
}
