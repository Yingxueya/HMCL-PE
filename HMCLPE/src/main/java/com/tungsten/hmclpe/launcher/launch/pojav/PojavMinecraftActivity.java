package com.tungsten.hmclpe.launcher.launch.pojav;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;

import com.tungsten.hmclpe.R;
import com.tungsten.hmclpe.launcher.launch.GameLaunchSetting;

import net.kdt.pojavlaunch.BaseMainActivity;
import net.kdt.pojavlaunch.LWJGLGLFWKeycode;
import net.kdt.pojavlaunch.utils.MCOptionUtils;

import org.lwjgl.glfw.CallbackBridge;

import java.util.Timer;
import java.util.TimerTask;

public class PojavMinecraftActivity extends BaseMainActivity implements View.OnTouchListener {

    private RelativeLayout baseLayout;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(net.kdt.pojavlaunch.R.layout.activity_pojav);

        GameLaunchSetting gameLaunchSetting = GameLaunchSetting.getGameLaunchSetting(getIntent().getExtras().getString("setting_path"));

        CallbackBridge.windowWidth = gameLaunchSetting.width;
        CallbackBridge.windowHeight = gameLaunchSetting.height;

        MCOptionUtils.load(gameLaunchSetting.game_directory);
        MCOptionUtils.set("overrideWidth", String.valueOf(CallbackBridge.windowWidth));
        MCOptionUtils.set("overrideHeight", String.valueOf(CallbackBridge.windowHeight));
        MCOptionUtils.save(gameLaunchSetting.game_directory);

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);

        baseLayout = (RelativeLayout) getLayoutInflater().inflate(R.layout.activity_minecraft,null) ;
        addContentView(baseLayout,params);

        mouseCursor = findViewById(R.id.mouse_cursor);
        baseTouchPad = findButton(R.id.base_touch_pad);

        mouseCallback = new MouseCallback() {
            @Override
            public void onMouseModeChange(boolean mode) {
                if (mode){
                    mouseCursor.setVisibility(View.VISIBLE);
                }
                else {
                    mouseCursor.setVisibility(View.INVISIBLE);
                }
            }
        };

        init(gameLaunchSetting.game_directory,
                gameLaunchSetting.javaPath,
                gameLaunchSetting.home,
                PojavLauncher.isHighVersion(gameLaunchSetting),
                PojavLauncher.getMcArgs(gameLaunchSetting, PojavMinecraftActivity.this),
                gameLaunchSetting.pojavRenderer,
                mouseCursor);

    }

    @SuppressLint("ClickableViewAccessibility")
    private Button findButton(int id){
        Button button = findViewById(id);
        button.setOnTouchListener(this);
        return button;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (v == baseTouchPad){
            if (CallbackBridge.isGrabbing()){
                switch(event.getActionMasked()){
                    case MotionEvent.ACTION_DOWN:
                        initialX = (int)event.getX();
                        initialY = (int)event.getY();
                        downTime = System.currentTimeMillis();
                        longClickTimer = new Timer();
                        longClickTimer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                CallbackBridge.sendMouseButton(LWJGLGLFWKeycode.GLFW_MOUSE_BUTTON_LEFT,true);
                            }
                        },400);
                    case MotionEvent.ACTION_MOVE:
                        if (!customSettingPointer){
                            padSettingPointer = true;
                            CallbackBridge.sendCursorPos(baseX + (int)event.getX() -initialX, baseY + (int)event.getY() - initialY);
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
                            CallbackBridge.sendCursorPos(baseX,baseY);
                            padSettingPointer = false;
                        }
                        CallbackBridge.sendMouseButton(LWJGLGLFWKeycode.GLFW_MOUSE_BUTTON_LEFT,false);
                        if (Math.abs(event.getX() - initialX) <= 10 && Math.abs(event.getY() - initialY) <= 10 && System.currentTimeMillis() - downTime <= 200){
                            CallbackBridge.sendMouseKeycode(LWJGLGLFWKeycode.GLFW_MOUSE_BUTTON_RIGHT);
                        }
                        break;
                    default:
                        break;
                }
            }
            else {
                baseX = (int)event.getX();
                baseY = (int)event.getY();
                CallbackBridge.sendCursorPos(baseX,baseY);
                if (event.getActionMasked() == MotionEvent.ACTION_DOWN){
                    CallbackBridge.sendMouseButton(LWJGLGLFWKeycode.GLFW_MOUSE_BUTTON_LEFT,true);
                }
                if (event.getActionMasked() == MotionEvent.ACTION_UP){
                    CallbackBridge.sendMouseButton(LWJGLGLFWKeycode.GLFW_MOUSE_BUTTON_LEFT,false);
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
        CallbackBridge.sendKeyPress(LWJGLGLFWKeycode.GLFW_KEY_ESCAPE);
    }

}