package com.tungsten.hmclpe.launcher;

import static com.tungsten.hmclpe.manifest.info.AppInfo.IS_PUBLIC_VERSION;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.appthemeengine.ATE;
import com.afollestad.appthemeengine.Config;
import com.tencent.connect.common.Constants;
import com.tencent.tauth.IUiListener;
import com.tencent.tauth.Tencent;
import com.tencent.tauth.UiError;
import com.tungsten.hmclpe.R;
import com.tungsten.hmclpe.launcher.dialogs.FirstLaunchDialog;
import com.tungsten.hmclpe.launcher.dialogs.VerifyDialog;
import com.tungsten.hmclpe.launcher.dialogs.account.SkinPreviewDialog;
import com.tungsten.hmclpe.manifest.AppManifest;
import com.tungsten.hmclpe.launcher.setting.InitializeSetting;
import com.tungsten.hmclpe.launcher.setting.game.PrivateGameSetting;
import com.tungsten.hmclpe.launcher.setting.game.PublicGameSetting;
import com.tungsten.hmclpe.launcher.setting.launcher.LauncherSetting;
import com.tungsten.hmclpe.launcher.uis.game.download.DownloadUrlSource;
import com.tungsten.hmclpe.launcher.uis.tools.UIManager;
import com.tungsten.hmclpe.launcher.uis.universal.setting.right.launcher.ExteriorSettingUI;
import com.tungsten.hmclpe.update.UpdateChecker;
import com.tungsten.hmclpe.utils.LocaleUtils;
import com.tungsten.hmclpe.utils.animation.CustomAnimationUtils;

import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    public LinearLayout launcherLayout;

    public boolean isLoaded = false;
    public boolean dialogMode = false;

    public LauncherSetting launcherSetting;
    public PublicGameSetting publicGameSetting;
    public PrivateGameSetting privateGameSetting;

    public UpdateChecker updateChecker;

    public Toolbar appBar;
    public LinearLayout appBarTitle;
    public ImageButton backToLastUI;
    public TextView currentUIText;
    public ImageButton backToHome;
    public ImageButton closeCurrentUI;
    public ImageButton backToDesktop;
    public ImageButton closeApp;

    public RelativeLayout uiContainer;
    public UIManager uiManager;

    public Config exteriorConfig;

    VerifyDialog verifyDialog;
    private Tencent mTencent;
    private IUiListener iUiListener;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        launcherLayout = findViewById(R.id.launcher_layout);

        init();

        startVerify();

        showEula();
    }

    public void init(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            if (getIntent().getExtras().getBoolean("fullscreen")) {
                getWindow().getAttributes().layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            } else {
                getWindow().getAttributes().layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_NEVER;
            }
        }
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN, WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);
        new Thread(() -> {
            AppManifest.initializeManifest(MainActivity.this);
            launcherSetting = InitializeSetting.initializeLauncherSetting();
            publicGameSetting = InitializeSetting.initializePublicGameSetting(MainActivity.this,MainActivity.this);
            privateGameSetting = InitializeSetting.initializePrivateGameSetting(MainActivity.this);

            runOnUiThread(() -> {
                updateChecker = new UpdateChecker(MainActivity.this,MainActivity.this);
            });

            DownloadUrlSource.getBalancedSource(MainActivity.this);

            loadingHandler.sendEmptyMessage(0);
        }).start();
    }

    @SuppressLint("HandlerLeak")
    public final Handler loadingHandler = new Handler(){
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            if (msg.what == 0){
                if (!isLoaded) {
                    exteriorConfig = ATE.config(MainActivity.this, null);

                    appBar = findViewById(R.id.app_bar);
                    appBarTitle = findViewById(R.id.app_bar_title);
                    backToLastUI = findViewById(R.id.back_to_last_ui);
                    currentUIText = findViewById(R.id.text_current_ui);
                    backToHome = findViewById(R.id.back_to_home);
                    closeCurrentUI = findViewById(R.id.close_current_ui);
                    backToDesktop = findViewById(R.id.back_to_desktop);
                    closeApp = findViewById(R.id.close_app);

                    backToLastUI.setOnClickListener(MainActivity.this);
                    backToHome.setOnClickListener(MainActivity.this);
                    closeCurrentUI.setOnClickListener(MainActivity.this);
                    backToDesktop.setOnClickListener(MainActivity.this);
                    closeApp.setOnClickListener(MainActivity.this);

                    uiContainer = findViewById(R.id.main_ui_container);
                    uiManager = new UIManager(MainActivity.this,MainActivity.this);

                    exteriorConfig.primaryColor(Color.parseColor(ExteriorSettingUI.getThemeColor(MainActivity.this,launcherSetting.launcherTheme)));
                    exteriorConfig.accentColor(Color.parseColor(ExteriorSettingUI.getThemeColor(MainActivity.this,launcherSetting.launcherTheme)));
                    exteriorConfig.apply(MainActivity.this);
                    appBar.setBackgroundColor(launcherSetting.transBar ? getResources().getColor(R.color.launcher_ui_background) : Color.parseColor(ExteriorSettingUI.getThemeColor(MainActivity.this,launcherSetting.launcherTheme)));

                    isLoaded = true;
                    onLoad();
                }
            }
        }
    };

    public void onLoad() {
        uiManager.gameManagerUI.gameManagerUIManager.versionSettingUI.onLoaded();
        uiManager.downloadUI.downloadUIManager.downloadMinecraftUI.onLoaded();
        uiManager.settingUI.settingUIManager.universalGameSettingUI.onLoaded();
        uiManager.mainUI.customTheme();
    }

    public void showBarTitle(String title,boolean home,boolean close) {
        if (isLoaded){
            CustomAnimationUtils.hideViewToLeft(appBarTitle,this,this,true);
            CustomAnimationUtils.showViewFromRight(backToLastUI,this,this,true);
            if (home){
                CustomAnimationUtils.showViewFromRight(backToHome,this,this,true);
            }
            else {
                CustomAnimationUtils.hideViewToLeft(backToHome,this,this,true);
            }
            if (close){
                CustomAnimationUtils.showViewFromRight(closeCurrentUI,this,this,true);
            }
            else {
                CustomAnimationUtils.hideViewToLeft(closeCurrentUI,this,this,true);
            }
            CustomAnimationUtils.showViewFromRight(currentUIText,this,this,true);
            currentUIText.setText(title);
        }
    }

    public void hideBarTitle() {
        if (isLoaded){
            CustomAnimationUtils.showViewFromLeft(appBarTitle,this,this,true);
            CustomAnimationUtils.hideViewToLeft(backToLastUI,this,this,true);
            if (backToHome.getVisibility() == View.VISIBLE){
                CustomAnimationUtils.hideViewToLeft(backToHome,this,this,true);
            }
            if (closeCurrentUI.getVisibility() == View.VISIBLE){
                CustomAnimationUtils.hideViewToLeft(closeCurrentUI,this,this,true);
            }
            CustomAnimationUtils.hideViewToLeft(currentUIText,this,this,true);
            currentUIText.setText("");
        }
    }

    public void backToLastUI() {
        if (isLoaded){
            if (uiManager.currentUI == uiManager.mainUI){
                backToDeskTop();
            }
            else {
                uiManager.uis.get(uiManager.uis.size() - 1).onStop();
                uiManager.uis.remove(uiManager.uis.size() - 1);
                uiManager.currentUI = uiManager.uis.get(uiManager.uis.size() - 1);
                uiManager.uis.get(uiManager.uis.size() - 1).onStart();
            }
        }
    }

    public void backToHome() {
        uiManager.switchMainUI(uiManager.mainUI);
        uiManager.uis.clear();
        uiManager.uis.add(uiManager.mainUI);
    }

    public void closeCurrentUI() {
        uiManager.removeUIIfExist(uiManager.exportWorldUI);
        uiManager.removeUIIfExist(uiManager.installPackageUI);
        uiManager.removeUIIfExist(uiManager.exportPackageTypeUI);
        uiManager.removeUIIfExist(uiManager.exportPackageInfoUI);
        uiManager.removeUIIfExist(uiManager.exportPackageFileUI);
        uiManager.removeUIIfExist(uiManager.installGameUI);
        uiManager.removeUIIfExist(uiManager.downloadForgeUI);
        uiManager.removeUIIfExist(uiManager.downloadFabricUI);
        uiManager.removeUIIfExist(uiManager.downloadFabricAPIUI);
        uiManager.removeUIIfExist(uiManager.downloadLiteLoaderUI);
        uiManager.removeUIIfExist(uiManager.downloadOptifineUI);
        uiManager.removeUIIfExist(uiManager.downloadQuiltUI);
        uiManager.removeUIIfExist(uiManager.downloadQuiltAPIUI);
        uiManager.uis.get(uiManager.uis.size() - 1).onStart();
        if (uiManager.currentUI == uiManager.exportWorldUI){
            uiManager.exportWorldUI.onStop();
        }
        if (uiManager.currentUI == uiManager.installPackageUI){
            uiManager.installPackageUI.onStop();
        }
        if (uiManager.currentUI == uiManager.exportPackageTypeUI){
            uiManager.exportPackageTypeUI.onStop();
        }
        if (uiManager.currentUI == uiManager.exportPackageInfoUI){
            uiManager.exportPackageInfoUI.onStop();
        }
        if (uiManager.currentUI == uiManager.exportPackageFileUI){
            uiManager.exportPackageFileUI.onStop();
        }
        if (uiManager.currentUI == uiManager.installGameUI){
            uiManager.installGameUI.onStop();
        }
        if (uiManager.currentUI == uiManager.downloadForgeUI){
            uiManager.downloadForgeUI.onStop();
        }
        if (uiManager.currentUI == uiManager.downloadFabricUI){
            uiManager.downloadFabricUI.onStop();
        }
        if (uiManager.currentUI == uiManager.downloadFabricAPIUI){
            uiManager.downloadFabricAPIUI.onStop();
        }
        if (uiManager.currentUI == uiManager.downloadLiteLoaderUI){
            uiManager.downloadLiteLoaderUI.onStop();
        }
        if (uiManager.currentUI == uiManager.downloadOptifineUI){
            uiManager.downloadOptifineUI.onStop();
        }
        if (uiManager.currentUI == uiManager.downloadQuiltUI){
            uiManager.downloadQuiltUI.onStop();
        }
        if (uiManager.currentUI == uiManager.downloadQuiltAPIUI){
            uiManager.downloadQuiltAPIUI.onStop();
        }
        uiManager.currentUI = uiManager.uis.get(uiManager.uis.size() - 1);
    }

    public void backToDeskTop() {
        Intent i = new Intent(Intent.ACTION_MAIN);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        i.addCategory(Intent.CATEGORY_HOME);
        startActivity(i);
    }

    @Override
    public void onBackPressed() {
        if (!dialogMode){
            backToLastUI();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (isLoaded){
            uiManager.onActivityResult(requestCode,resultCode,data);
        }
        if (SkinPreviewDialog.getInstance() != null) {
            SkinPreviewDialog.getInstance().onActivityResult(requestCode,resultCode,data);
        }
        if(requestCode == Constants.REQUEST_LOGIN && resultCode == Constants.ACTIVITY_OK) {
            Tencent.handleResultData(data, iUiListener);
        }
    }

    @Override
    public void onClick(View v) {
        if (v == backToLastUI){
            backToLastUI();
        }
        if (v == backToHome){
            backToHome();
        }
        if (v == closeCurrentUI){
            closeCurrentUI();
        }
        if (v == backToDesktop){
            backToDeskTop();
        }
        if (v == closeApp){
            finish();
            System.exit(0);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleUtils.setLanguage(base));
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        LocaleUtils.setLanguage(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isLoaded){
            uiManager.onPause();
        }
        if (SkinPreviewDialog.getInstance() != null) {
            SkinPreviewDialog.getInstance().onPause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isLoaded){
            uiManager.onResume();
        }
        if (SkinPreviewDialog.getInstance() != null) {
            SkinPreviewDialog.getInstance().onResume();
        }
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && launcherSetting != null) {
            if (launcherSetting.fullscreen) {
                getWindow().getAttributes().layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            } else {
                getWindow().getAttributes().layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_NEVER;
            }
        }
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN, WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    public void showEula() {
        SharedPreferences sharedPreferences;
        sharedPreferences = getSharedPreferences("global", MODE_PRIVATE);
        if (sharedPreferences.getInt("first_launch", 0) == 0) {
            FirstLaunchDialog dialog = new FirstLaunchDialog(this);
            dialog.show();
        }
    }

    @SuppressLint("CommitPrefEdits")
    public void startVerify() {
        if (!IS_PUBLIC_VERSION) {
            SharedPreferences sharedPreferences;
            SharedPreferences.Editor editor;
            sharedPreferences = getSharedPreferences("verify", MODE_PRIVATE);
            editor = sharedPreferences.edit();
            mTencent = Tencent.createInstance("102019361", this, "com.tungsten.filepicker.provider");
            String openid = sharedPreferences.getString("openid", "");
            String token = sharedPreferences.getString("token", "");
            String expires_in = sharedPreferences.getString("expires_in", "");
            if (!openid.equals("") && !token.equals("") && !expires_in.equals("")) {
                mTencent.setOpenId(openid);
                mTencent.setAccessToken(token,expires_in);
            }
            Tencent.setIsPermissionGranted(true);
            iUiListener = new IUiListener() {
                @Override
                public void onComplete(Object o) {
                    try {
                        JSONObject result = (JSONObject) o;
                        String openid = result.getString("openid");//openid作为设备码
                        String accessToken = result.getString("access_token");//token
                        String expires_in = result.getString("expires_in");//过期时间
                        mTencent.setOpenId(openid);
                        mTencent.setAccessToken(token, expires_in);
                        editor.putString("openid", openid);
                        editor.putString("token", accessToken);
                        editor.putString("expires_in", expires_in);
                        editor.apply();
                        verifyDialog.onLoginSuccess(openid);
                        Toast.makeText(MainActivity.this, getString(R.string.dialog_verify_login_success), Toast.LENGTH_SHORT).show();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onError(UiError uiError) {
                    Log.e("QQ login failed",uiError.errorDetail);
                    Toast.makeText(MainActivity.this, getString(R.string.dialog_verify_login_fail), Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onCancel() {
                    Log.e("QQ login canceled","login canceled");
                    Toast.makeText(MainActivity.this, getString(R.string.dialog_verify_login_fail), Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onWarning(int i) {
                    Log.e("Warning","onWarning" + i);
                }
            };
            verifyDialog = new VerifyDialog(this, this, mTencent, iUiListener, new VerifyInterface() {
                @Override
                public void onSuccess() {

                }

                @Override
                public void onCancel() {
                    System.exit(0);
                }
            });
            verifyDialog.show();
            if (mTencent.isSessionValid()) {
                Log.e("login","isSessionValid");
                verifyDialog.onLoginSuccess(openid);
                verifyDialog.verify();
            }
        }
    }

}