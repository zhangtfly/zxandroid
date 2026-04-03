package com.example.myapplication.login;

import static android.text.InputType.TYPE_CLASS_TEXT;
import static android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD;
import static android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.myapplication.LanguageManager;
import com.example.myapplication.MainActivity;
import com.example.myapplication.R;
import com.example.myapplication.ThemeManager;
import com.example.myapplication.base.BaseActivity;
import com.example.myapplication.http.api.LoginPhoneV2Api;
import com.example.myapplication.http.api.LoginV2Api;
import com.example.myapplication.http.api.LoginVisitorV2Api;
import com.example.myapplication.http.api.RegisterApi;
import com.example.myapplication.http.api.RequestCodeApi;
import com.example.myapplication.http.model.HttpData;
import com.example.myapplication.login.bean.LoginV2Bean;
import com.example.myapplication.login.bean.RequestCodeBean;
import com.example.myapplication.login.utils.MD5Utils;
import com.example.myapplication.login.utils.RegexUtil;
import com.example.myapplication.login.view.CountdownView;
import com.example.myapplication.utils.TokenManager;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.hjq.http.EasyHttp;
import com.hjq.http.listener.HttpCallbackProxy;
import com.hjq.shape.layout.ShapeLinearLayout;
import com.hjq.shape.view.ShapeEditText;
import com.hjq.shape.view.ShapeTextView;
import com.hjq.toast.Toaster;

import java.util.Objects;

public class LoginV2Activity extends BaseActivity {
    private ShapeTextView accountLogin;
    private ShapeTextView codeLogin;
    private LinearLayout phoneLoginRl;
    private ShapeEditText edPhone;
    private ShapeEditText etCode;
    private CountdownView loginGetCode;
    private ImageView btnTogglePassword;
    private MaterialCheckBox rememberMe;
    private TextView forgetPassword;
    private ShapeTextView submitLogin;
    private ShapeTextView visitorLogin;
    private ShapeTextView languageBo;
    private ShapeTextView languageCn;
    private ShapeTextView languageEn;
    private TextView tvPhone;
    private TextView tvCode;
    private TextView tvAccount;
    private TextView tvPassword;
    private TextView visitorTip;
    private ShapeEditText etAccount;
    private ShapeEditText etPassword;
    private TextView tvOr;
    private TextView noAccount;
    private TextView registerNow;
    private String language;
    private LinearLayout codeLoginRl;
    private boolean isPasswordVisible = false;
    private boolean isRegisterPasswordVisible = false;
    private boolean isRegisterCofirmPasswordVisible = false;
    private String isType = "login";
    private boolean initLoginPhone = true;
    private ShapeLinearLayout loginLl;
    private LinearLayout registerLl;
    private TextView registerPhone;
    private ShapeEditText registerEtPhone;
    private TextView registerTvCode;
    private ShapeEditText registerEtCode;
    private CountdownView registerGetCode;
    private TextView registerTvPassword;
    private ShapeEditText registerPassword;
    private ImageView registerBtnTogglePassword;
    private TextView registerConfirmTvPassword;
    private ShapeEditText registerConfirmEtPassword;
    private ImageView registerConfirmBtnTogglePassword;
    private MaterialCheckBox checkLogin;
    private TextView userAgreement;
    private TextView userPolicy;
    private ShapeTextView submitRegister;
    private ShapeTextView submitForget;
    private LinearLayout loginShow;
    private LinearLayout registerShow;
    private TextView noAccountRegister;
    private TextView registerNowRegister;
    private TokenManager tokenManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        ThemeManager.getInstance(this).initializeTheme();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_v2);
        tokenManager = new TokenManager(this);

        if (tokenManager.isLoggedIn()) {
            finish();
            return;
        }
        language = LanguageManager.getInstance(this).getLanguage();
        initView();
        onclick();
    }

    private void initView() {
        accountLogin = (ShapeTextView) findViewById(R.id.account_login);
        codeLogin = (ShapeTextView) findViewById(R.id.code_login);
        phoneLoginRl = (LinearLayout) findViewById(R.id.phone_login_rl);
        edPhone = (ShapeEditText) findViewById(R.id.ed_phone);
        etCode = (ShapeEditText) findViewById(R.id.et_code);
        loginGetCode = (CountdownView) findViewById(R.id.login_get_code);
        btnTogglePassword = (ImageView) findViewById(R.id.btn_toggle_password);
        rememberMe = (MaterialCheckBox) findViewById(R.id.remember_me);
        forgetPassword = (TextView) findViewById(R.id.forget_password);
        submitLogin = (ShapeTextView) findViewById(R.id.submit_login);
        visitorLogin = (ShapeTextView) findViewById(R.id.visitor_login);
        languageBo = (ShapeTextView) findViewById(R.id.language_bo);
        languageCn = (ShapeTextView) findViewById(R.id.language_cn);
        languageEn = (ShapeTextView) findViewById(R.id.language_en);
        tvPhone = (TextView) findViewById(R.id.tv_phone);
        tvCode = (TextView) findViewById(R.id.tv_code);
        tvAccount = (TextView) findViewById(R.id.tv_account);
        tvPassword = (TextView) findViewById(R.id.tv_password);
        visitorTip = (TextView) findViewById(R.id.visitor_tip);
        etAccount = (ShapeEditText) findViewById(R.id.et_account);
        etPassword = (ShapeEditText) findViewById(R.id.et_password);
        tvOr = (TextView) findViewById(R.id.tv_or);
        noAccount = (TextView) findViewById(R.id.no_account);
        registerNow = (TextView) findViewById(R.id.register_now);
        codeLoginRl = (LinearLayout) findViewById(R.id.code_login_rl);

        loginLl = (ShapeLinearLayout) findViewById(R.id.login_ll);
        registerLl = (LinearLayout) findViewById(R.id.register_ll);
        registerPhone = (TextView) findViewById(R.id.register_phone);
        registerEtPhone = (ShapeEditText) findViewById(R.id.register_et_phone);
        registerTvCode = (TextView) findViewById(R.id.register_tv_code);
        registerEtCode = (ShapeEditText) findViewById(R.id.register_et_code);
        registerGetCode = (CountdownView) findViewById(R.id.register_get_code);
        registerTvPassword = (TextView) findViewById(R.id.register_tv_password);
        registerPassword = (ShapeEditText) findViewById(R.id.register_password);
        registerBtnTogglePassword = (ImageView) findViewById(R.id.register_btn_toggle_password);
        registerConfirmTvPassword = (TextView) findViewById(R.id.register_confirm_tv_password);
        registerConfirmEtPassword = (ShapeEditText) findViewById(R.id.register_confirm_et_password);
        registerConfirmBtnTogglePassword = (ImageView) findViewById(R.id.register_confirm_btn_toggle_password);
        checkLogin = (MaterialCheckBox) findViewById(R.id.check_login);
        userAgreement = (TextView) findViewById(R.id.user_agreement);
        userPolicy = (TextView) findViewById(R.id.user_policy);
        submitRegister = (ShapeTextView) findViewById(R.id.submit_register);
        submitForget = (ShapeTextView) findViewById(R.id.submit_forget);
        loginShow = (LinearLayout) findViewById(R.id.login_show);
        registerShow = (LinearLayout) findViewById(R.id.register_show);
        noAccountRegister = (TextView) findViewById(R.id.no_account_register);
        registerNowRegister = (TextView) findViewById(R.id.register_now_register);
        setLanguageUI(language);
        initLoginType(true);
        isType = "login";
        initType();
        if (tokenManager.getUserAccount() != null && tokenManager.getUserPassword() != null) {
            etAccount.setText(tokenManager.getUserAccount());
            etPassword.setText(tokenManager.getUserPassword());
            checkLogin.setChecked(true);
        }
    }

    private void onclick() {
        languageCn.setOnClickListener(view -> {
            setLanguageUI(LanguageManager.LANGUAGE_CHINESE);
        });
        languageBo.setOnClickListener(view -> {
            setLanguageUI(LanguageManager.LANGUAGE_TIBETAN);
        });
        languageEn.setOnClickListener(view -> {
            setLanguageUI(LanguageManager.LANGUAGE_ENGLISH);
        });
        accountLogin.setOnClickListener(view -> {
            initLoginPhone = true;
            initLoginType(true);
        });
        codeLogin.setOnClickListener(view -> {
            initLoginPhone = false;
            initLoginType(false);
        });
        btnTogglePassword.setOnClickListener(view -> {
            togglePasswordVisibility("loginPassword", isPasswordVisible, etPassword, btnTogglePassword);
        });
        registerBtnTogglePassword.setOnClickListener(view -> {
            togglePasswordVisibility("registerPassword", isRegisterPasswordVisible, registerPassword, registerBtnTogglePassword);
        });
        registerConfirmBtnTogglePassword.setOnClickListener(view -> {
            togglePasswordVisibility("confirmPassword", isRegisterCofirmPasswordVisible, registerConfirmEtPassword, registerConfirmBtnTogglePassword);
        });
        visitorLogin.setOnClickListener(view -> {
            visitorLoginSubmit();
        });
        forgetPassword.setOnClickListener(view -> {
            isType = "forget";
            initType();
        });
        registerNow.setOnClickListener(view -> {
            isType = "register";
            initType();
        });
        registerNowRegister.setOnClickListener(view -> {
            isType = "login";
            initType();
        });
        loginGetCode.setOnClickListener(view -> {
            requestCode(loginGetCode);
        });
        registerGetCode.setOnClickListener(view -> {
            requestCode(registerGetCode);
        });
        submitLogin.setOnClickListener(view -> {
            saveLogin();
        });
        submitRegister.setOnClickListener(view -> {
            saveRegister(true);
        });
        submitForget.setOnClickListener(view -> {
            saveRegister(false);
        });
        userAgreement.setOnClickListener(view -> {
            Intent intent2 = new Intent(this, PrivacyPolicyActivity.class);
            intent2.putExtra(PrivacyPolicyActivity.TYPE, PrivacyPolicyActivity.TYPE_USER);
            startActivity(intent2);
        });
        userPolicy.setOnClickListener(view -> {
            Intent intent1 = new Intent(this, PrivacyPolicyActivity.class);
            intent1.putExtra(PrivacyPolicyActivity.TYPE, PrivacyPolicyActivity.TYPE_PRIVACY);
            startActivity(intent1);
        });
    }

    // 游客登录
    private void visitorLoginSubmit() {
        EasyHttp.post(this)
                .api(new LoginVisitorV2Api())
                .request(new HttpCallbackProxy<HttpData<LoginV2Bean>>(this) {

                    @Override
                    public void onHttpSuccess(HttpData<LoginV2Bean> result) {
                        if ( result!=null&&result.getData() != null) {
                            tokenManager.saveUserInfo(
                                    result.getData().getToken(),
                                    result.getData().getId(),
                                    result.getData().getUserName(),
                                    null,
                                    result.getData().getUserAvatar(),
                                    null
                            );
                        }
                        startMain();
                    }
                });
    }

    // 注册或者忘记密码校验
    private void saveRegister(boolean isRegister) {
        String phone = Objects.requireNonNull(registerEtPhone.getText()).toString().trim();
        String code = Objects.requireNonNull(registerEtCode.getText()).toString().trim();
        String password = Objects.requireNonNull(registerPassword.getText()).toString().trim();
        String newPassword = Objects.requireNonNull(registerConfirmEtPassword.getText()).toString().trim();
        if (TextUtils.isEmpty(phone)) {
            showToastPhone();
            return;
        }
        if (!RegexUtil.isPhoneValid(phone)){
            showPhoneError();
            return;
        }
        if (TextUtils.isEmpty(code)) {
            showToastCode();
            return;
        }
        if (TextUtils.isEmpty(password)) {
            showToastPassword();
            return;
        }
        if (TextUtils.isEmpty(newPassword)) {
            if (LanguageManager.LANGUAGE_CHINESE.equals(language)) {
                Toaster.show("请确认密码");
            } else if (LanguageManager.LANGUAGE_ENGLISH.equals(language)) {
                Toaster.show("Please confirm your password");
            } else {
                Toaster.show("མིག་སྒྲིག་ལོག་གཏན་ནས་བཟུང་དགོས།");
            }
            return;
        }
        if (!RegexUtil.isPasswordValid(password)||!RegexUtil.isPasswordValid(newPassword)){
            showPasswordError();
            return;
        }

        nextRegisterLogin(isRegister);
    }

    private void nextRegisterLogin(boolean isRegister) {
        String phone = Objects.requireNonNull(registerEtPhone.getText()).toString().trim();
        String code = Objects.requireNonNull(registerEtCode.getText()).toString().trim();
        String password = Objects.requireNonNull(registerPassword.getText()).toString().trim();
        String newPassword = Objects.requireNonNull(registerConfirmEtPassword.getText()).toString().trim();
        EasyHttp.post(this)
                .api(new RegisterApi(isRegister).setPhoneNumber(phone)
                        .setVerifyCode(code)
                        .setNewPassword(MD5Utils.md5(password))
                        .setConfirmPassword(MD5Utils.md5(newPassword)))
                .request(new HttpCallbackProxy<HttpData<LoginV2Bean>>(this) {

                    @Override
                    public void onHttpSuccess(HttpData<LoginV2Bean> result) {
                        if ( result!=null&&result.getData() != null) {
                            tokenManager.saveUserInfo(
                                    result.getData().getToken(),
                                    result.getData().getId(),
                                    result.getData().getUserName(),
                                    null,
                                    result.getData().getUserAvatar(),
                                    null
                            );
                        }
                        startMain();
                    }
                });
    }

    private void showToastPassword() {
        if (LanguageManager.LANGUAGE_CHINESE.equals(language)) {
            Toaster.show("请输入密码");
        } else if (LanguageManager.LANGUAGE_ENGLISH.equals(language)) {
            Toaster.show("Please enter password");
        } else {
            Toaster.show("གསང་ཨང་འཇུག་རོགས");
        }
    }

    private void showToastCode() {
        if (LanguageManager.LANGUAGE_CHINESE.equals(language)) {
            Toaster.show("请输入验证码");
        } else if (LanguageManager.LANGUAGE_ENGLISH.equals(language)) {
            Toaster.show("Enter Verification Code");
        } else {
            Toaster.show("ར་སྤྲོད་ཨང་རྟགས་གཏག་རོགས།");
        }
    }

    private void showToastPhone() {
        if (LanguageManager.LANGUAGE_CHINESE.equals(language)) {
            Toaster.show("请输入手机号");
        } else if (LanguageManager.LANGUAGE_ENGLISH.equals(language)) {
            Toaster.show("Enter phone number");
        } else {
            Toaster.show("ཁ་པར་ཨང་གྲངས་གཏག་རོགས།");
        }
    }

    // 调用登录
    private void saveLogin() {
        String account = Objects.requireNonNull(etAccount.getText()).toString().trim();
        String password = Objects.requireNonNull(etPassword.getText()).toString().trim();
        String phone = Objects.requireNonNull(edPhone.getText()).toString().trim();
        String code = Objects.requireNonNull(etCode.getText()).toString().trim();
        if (initLoginPhone) {
            if (TextUtils.isEmpty(account)) {
                if (LanguageManager.LANGUAGE_CHINESE.equals(language)) {
                    Toaster.show("请输入用户名");
                } else if (LanguageManager.LANGUAGE_ENGLISH.equals(language)) {
                    Toaster.show("Please enter username");
                } else {
                    Toaster.show(getString(R.string.hint_account));
                }
                return;
            }
            if (TextUtils.isEmpty(password)) {
                showToastPassword();
                return;
            }
            if (!RegexUtil.isPasswordValid(password)) {
                showPasswordError();
                return;
            }
            nextLogin();
        } else {
            if (TextUtils.isEmpty(phone)) {
                showToastPhone();
                return;
            }
            if (!RegexUtil.isPhoneValid(phone)) {
                showPhoneError();
                return;
            }
            if (TextUtils.isEmpty(code)) {
                showToastCode();
                return;
            }
            nextLogin();
        }
    }

    private void showPasswordError() {
        if (LanguageManager.LANGUAGE_CHINESE.equals(language)) {
            Toaster.show("请输入大小写字母和数字（6-20位密码）");
        } else if (LanguageManager.LANGUAGE_ENGLISH.equals(language)) {
            Toaster.show("Please enter a 6-20 character password including uppercase and lowercase letters and numbers");
        } else {
            Toaster.show("གསལ་བྱེད་ཆེ་ཆུང་དང་གྲངས་ཀ་6-20བར་གཏག་རོགས།");
        }
    }

    private void showPhoneError() {
        if (LanguageManager.LANGUAGE_CHINESE.equals(language)) {
            Toaster.show("手机号错误");
        } else if (LanguageManager.LANGUAGE_ENGLISH.equals(language)) {
            Toaster.show("Invalid phone number");
        } else {
            Toaster.show("གཏོང་རྟགས་འགྲོ་སྐབས་ཡོད།");
        }
    }

    // 登录
    private void nextLogin() {
        if (initLoginPhone) {
            String account = Objects.requireNonNull(etAccount.getText()).toString().trim();
            String password = Objects.requireNonNull(etPassword.getText()).toString().trim();
            EasyHttp.post(this)
                    .api(new LoginV2Api().setUserAccount(account)
                            .setUserPassword(MD5Utils.md5(password))
                            .setRememberMe(String.valueOf(checkLogin.isChecked())))
                    .request(new HttpCallbackProxy<HttpData<LoginV2Bean>>(this) {

                        @Override
                        public void onHttpSuccess(HttpData<LoginV2Bean> result) {
                            if ( result!=null&&result.getData() != null) {
                                tokenManager.saveUserInfo(
                                        result.getData().getToken(),
                                        result.getData().getId(),
                                        result.getData().getUserName(),
                                        account,
                                        result.getData().getUserAvatar(),
                                        password
                                );
                            }
                            startMain();
                        }
                    });
        }else {
            String phone = Objects.requireNonNull(edPhone.getText()).toString().trim();
            String code = Objects.requireNonNull(etCode.getText()).toString().trim();
            EasyHttp.post(this)
                    .api(new LoginPhoneV2Api()
                            .setPhoneNumber(phone)
                            .setVerifyCode(code)
                            .setRememberMe("true"))
                    .request(new HttpCallbackProxy<HttpData<LoginV2Bean>>(this) {

                        @Override
                        public void onHttpSuccess(HttpData<LoginV2Bean> result) {
                            if ( result!=null&&result.getData() != null) {
                                tokenManager.saveUserInfo(
                                        result.getData().getToken(),
                                        result.getData().getId(),
                                        result.getData().getUserName(),
                                        null,
                                        result.getData().getUserAvatar(),
                                        null
                                );
                            }
                            startMain();
                        }
                    });
        }
    }

    private void startMain(){
        Intent intent =new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }
    // 获取验证码
    private void requestCode(CountdownView view) {
        String phone = Objects.requireNonNull(edPhone.getText()).toString().trim();
        String RegisterPhone = Objects.requireNonNull(registerEtPhone.getText()).toString().trim();
        if (loginGetCode == view) {
            if (TextUtils.isEmpty(phone)) {
                showToastPhone();
                return;
            }
            if (!RegexUtil.isPhoneValid(phone)){
                showPhoneError();
                return;
            }
            nextRequestCode(phone, view);
        } else if (registerGetCode == view) {
            if (TextUtils.isEmpty(RegisterPhone)) {
                showToastPhone();
                return;
            }
            if (!RegexUtil.isPhoneValid(RegisterPhone)){
                showPhoneError();
                return;
            }
            nextRequestCode(RegisterPhone, view);
        }
    }

    // 获取验证码
    private void nextRequestCode(String phone, CountdownView view) {
        EasyHttp.post(this)
                .api(new RequestCodeApi().setPhoneNumber(phone))
                .request(new HttpCallbackProxy<HttpData<RequestCodeBean>>(this) {

                    @Override
                    public void onHttpSuccess(HttpData<RequestCodeBean> result) {
                        if (result!=null&&result.getData() != null && result.getData().isSuccess()) {
                            view.start();
                        }
                        Toaster.show(result.getData().getMessage());
                    }
                });
    }

    // 切换登录 注册 忘记密码状态Ui
    private void initType() {
        switch (isType) {
            case "login":
                loginLl.setVisibility(View.VISIBLE);
                registerLl.setVisibility(View.GONE);
                loginShow.setVisibility(View.VISIBLE);
                registerShow.setVisibility(View.GONE);
                registerEtPhone.setText("");
                registerEtCode.setText("");
                registerPassword.setText("");
                registerConfirmEtPassword.setText("");
                registerGetCode.stop();
                break;
            case "register":
                loginLl.setVisibility(View.GONE);
                registerLl.setVisibility(View.VISIBLE);
                submitRegister.setVisibility(View.VISIBLE);
                submitForget.setVisibility(View.GONE);
                loginShow.setVisibility(View.GONE);
                registerShow.setVisibility(View.VISIBLE);
                edPhone.setText("");
                etCode.setText("");
                etPassword.setText("");
                etAccount.setText("");
                loginGetCode.stop();
                break;
            case "forget":
                loginLl.setVisibility(View.GONE);
                registerLl.setVisibility(View.VISIBLE);
                submitRegister.setVisibility(View.GONE);
                submitForget.setVisibility(View.VISIBLE);
                loginShow.setVisibility(View.GONE);
                registerShow.setVisibility(View.VISIBLE);
                edPhone.setText("");
                etCode.setText("");
                etPassword.setText("");
                etAccount.setText("");
                loginGetCode.stop();
                break;

        }
    }

    // 切换手机号验证码登录
    private void initLoginType(boolean isAccount) {
        accountLogin.setSelected(isAccount);
        codeLogin.setSelected(!isAccount);
        phoneLoginRl.setVisibility(isAccount ? View.VISIBLE : View.GONE);
        codeLoginRl.setVisibility(isAccount ? View.GONE : View.VISIBLE);
        if (isAccount) {
            edPhone.setText("");
            etCode.setText("");
            loginGetCode.stop();
            rememberMe.setVisibility(View.VISIBLE);
        } else {
            etPassword.setText("");
            etAccount.setText("");
            rememberMe.setVisibility(View.INVISIBLE);
        }
    }

    // 语言切换
    private void setLanguageUI(String language) {
        LanguageManager.getInstance(this).setLanguage(language);
        this.language = LanguageManager.getInstance(this).getLanguage();
        switch (language) {
            case LanguageManager.LANGUAGE_CHINESE:
                languageCn.setSelected(true);
                languageEn.setSelected(false);
                languageBo.setSelected(false);
                accountLogin.setText("账号登录");
                codeLogin.setText("验证码登录");
                tvPhone.setText("手机号");
                tvCode.setText("验证码");
                edPhone.setHint("请输入手机号");
                etCode.setHint("请输入验证码");
                tvAccount.setText("用户名");
                tvPassword.setText("密码");
                rememberMe.setText("记住我");
                forgetPassword.setText("忘记密码？");
                submitLogin.setText("登录");
                visitorLogin.setText("\uD83D\uDC64  游客访问");
                visitorTip.setText("游客每天可使用5次");
                loginGetCode.setText("获取验证码");
                etAccount.setHint("请输入用户名");
                etPassword.setHint("请输入用密码");
                tvOr.setText("或");
                noAccount.setText("还没有账号？");
                registerNow.setText("立即注册");

                registerPhone.setText("手机号");
                registerEtPhone.setHint("请输入手机号");
                registerTvCode.setText("验证码");
                registerEtCode.setHint("请输入验证码");
                registerGetCode.setText("获取验证码");
                registerTvPassword.setText("设置密码");
                registerPassword.setHint("请输入大小写字母和数字（6-20位密码）");
                registerConfirmTvPassword.setText("确认密码");
                registerConfirmEtPassword.setHint("请再次输入密码");
                checkLogin.setText("我已阅读并同意");
                userAgreement.setText("《用户服务协议》");
                userPolicy.setText("《隐私政策》");
                submitRegister.setText("注册");
                submitForget.setText("确定");
                noAccountRegister.setText("已有账号？");
                registerNowRegister.setText("去登录");
                break;
            case LanguageManager.LANGUAGE_ENGLISH:
                languageCn.setSelected(false);
                languageEn.setSelected(true);
                languageBo.setSelected(false);
                accountLogin.setText("Account Login");
                codeLogin.setText("SMS Login");
                tvPhone.setText("Phone");
                tvCode.setText("Verification Code");
                edPhone.setHint("Enter Phone");
                etCode.setHint("Enter Verification Code");
                tvAccount.setText("Username");
                tvPassword.setText("Password");
                rememberMe.setText("Remember me");
                forgetPassword.setText("Forgot password?");
                submitLogin.setText("Sign In");
                visitorLogin.setText("\uD83D\uDC64  Continue as Guest");
                visitorTip.setText("Guests can use 5 times per day");
                loginGetCode.setText("Get Code");
                etAccount.setHint("Enter username");
                etPassword.setHint("Enter Password");
                tvOr.setText("or");
                noAccount.setText("Don't have an account?");
                registerNow.setText("Register Now");

                registerPhone.setText("Phone");
                registerEtPhone.setHint("Enter Phone");
                registerTvCode.setText("Verification Code");
                registerEtCode.setHint("Enter Verification Code");
                registerGetCode.setText("Get Code");
                registerTvPassword.setText("Set Password");
                registerPassword.setHint("Please enter a 6-20 character password including uppercase and lowercase letters and numbers");
                registerConfirmTvPassword.setText("Confirm Password");
                registerConfirmEtPassword.setHint("Please enter the password again");
                checkLogin.setText("I have read and agree");
                userAgreement.setText("《User Service Agreement》");
                userPolicy.setText("《Privacy Policy》");
                submitRegister.setText("Register");
                submitForget.setText("OK");
                noAccountRegister.setText("Already have an account?");
                registerNowRegister.setText("Log In");
                break;
            case LanguageManager.LANGUAGE_TIBETAN:
            default:
                languageCn.setSelected(false);
                languageEn.setSelected(false);
                languageBo.setSelected(true);
                accountLogin.setText("ཨང་རྟགས་ཐོ་འཇུག");
                codeLogin.setText("ར་སྤྲོད་ཨང་རྟགས་ཐོ་འཇུག");
                tvPhone.setText("ཁ་པར་ཨང་གྲངས།");
                tvCode.setText("ར་སྤྲོད་ཨང་རྟགས།");
                edPhone.setHint("ཁ་པར་ཨང་གྲངས་གཏག་རོགས།");
                etCode.setHint("ར་སྤྲོད་ཨང་རྟགས་གཏག་རོགས།");
                tvAccount.setText("ཁ་པར་ཨང་གྲངས།");
                tvPassword.setText("གསང་གྲངས།");
                rememberMe.setText("རང་ཉིད་ཀྱི་ཆ་འཕྲིན་ངེས་པ།");
                forgetPassword.setText("གསང་གྲངས་བརྗེད་པ།");
                submitLogin.setText("ཐོ་འཇུག");
                visitorLogin.setText("\uD83D\uDC64  འགྲུལ་པའི་རྣམ་པ་ནས།");
                visitorTip.setText("འགྲུལ་པས་ཉིན་རེར་ཐེངས་5བེད་སྤྱོད་བྱེད་ཆོག");
                loginGetCode.setText("ར་སྤྲོད་ཨང་གྲངས་བསྡུ་ལེན།");
                etAccount.setHint("ཁ་པར་ཨང་གྲངས་གཏག་རོགས།");
                etPassword.setHint("གསང་གྲངས་གཏག་རོགས།");
                tvOr.setText("ཡང་ན།");
                noAccount.setText("ཨང་རྟགས་ཡོད་དམ།");
                registerNow.setText("ཐོ་འགོད།");

                registerPhone.setText("ཁ་པར་ཨང་གྲངས།");
                registerEtPhone.setHint("ཁ་པར་ཨང་གྲངས་གཏག་རོགས།");
                registerTvCode.setText("ར་སྤྲོད་ཨང་རྟགས།");
                registerEtCode.setHint("ར་སྤྲོད་ཨང་རྟགས་གཏག་རོགས།");
                registerGetCode.setText("ར་སྤྲོད་ཨང་གྲངས་བསྡུ་ལེན།");
                registerTvPassword.setText("གསང་གྲངས་སྒྲིག་འགོད།");
                registerPassword.setHint("གསལ་བྱེད་ཆེ་ཆུང་དང་གྲངས་ཀ་6-20བར་གཏག་རོགས།");
                registerConfirmTvPassword.setText("གསང་གྲངས་གཏན་འཁེལ།");
                registerConfirmEtPassword.setHint("གསང་གྲངས་ཡང་བསྐྱར་གཏག་རོགས།");
                // 藏文 —— 最终完美版
                checkLogin.setText("ངས་བཀླགས་ཤིང་དང་ལེན་བྱས་པ།་");
                userAgreement.setText("《སྤྱོད་མཁན་གྱི་ཞབས་ཞུའི་གྲོས་ཆིངས།》དང་");
                userPolicy.setText("《གསང་དོན་སྲིད་ཇུས།》");
                submitRegister.setText("ཐོ་འགོད།");
                submitForget.setText("གཏན་འཁེལ།");
                noAccountRegister.setText("ཨང་རྟགས་ཡོད་དམ།");
                registerNowRegister.setText("ཐོ་འཇུག");
                break;
        }
    }

    // 密码显示切换
    private void togglePasswordVisibility(String type, Boolean isPasswordVisible, ShapeEditText etPassword, ImageView btnTogglePassword) {
        switch (type) {
            case "loginPassword":
                this.isPasswordVisible = !isPasswordVisible;
                break;
            case "registerPassword":
                this.isRegisterPasswordVisible = !isPasswordVisible;
                break;
            case "confirmPassword":
                this.isRegisterCofirmPasswordVisible = !isPasswordVisible;
                break;
        }
        if (isPasswordVisible) {
            etPassword.setInputType(TYPE_CLASS_TEXT | TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            btnTogglePassword.setImageResource(R.drawable.password_off_ic);
        } else {
            etPassword.setInputType(TYPE_CLASS_TEXT | TYPE_TEXT_VARIATION_PASSWORD);
            btnTogglePassword.setImageResource(R.drawable.password_on_ic);
        }
        etPassword.setSelection(Objects.requireNonNull(etPassword.getText()).length());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        loginGetCode.stop();
        registerGetCode.stop();
    }
}
