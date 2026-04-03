package com.example.myapplication;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.myapplication.api.BaseResponse;
import com.example.myapplication.api.RetrofitClient;
import com.example.myapplication.api.model.LoginRequest;
import com.example.myapplication.api.model.LoginResponse;
import com.example.myapplication.utils.TokenManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private EditText etAccount;
    private EditText etPassword;
    private Button btnLogin;
    private Button btnVisitor;
    private TextView tvGoRegister;
    private TextView tvForgotPassword;
    private ImageButton btnTogglePassword;
    private TextView tabAccount;
    private TextView tabVerification;
    private TextView btnLangTibetan;
    private TextView btnLangChinese;
    private TextView btnLangEnglish;
    private TextView labelPassword;

    private TokenManager tokenManager;
    private String originalLoginButtonText;
    private boolean isPasswordVisible = false;
    private boolean isAccountLogin = true; // true = account login, false = verification login

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String language = LanguageManager.getInstance(this).getLanguage();
        int layoutId = getLayoutByLanguage(language);
        setContentView(layoutId);

        tokenManager = new TokenManager(this);

        if (tokenManager.isLoggedIn()) {
            finish();
            return;
        }

        initViews();
        setupListeners();
        updateLanguageButtons(language);
    }

    private int getLayoutByLanguage(String language) {
        switch (language) {
            case LanguageManager.LANGUAGE_CHINESE:
                return R.layout.activity_login_zh;
            case LanguageManager.LANGUAGE_ENGLISH:
                return R.layout.activity_login_en;
            case LanguageManager.LANGUAGE_TIBETAN:
            default:
                return R.layout.activity_login;
        }
    }

    private void initViews() {
        etAccount = findViewById(R.id.et_account);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);
        tvGoRegister = findViewById(R.id.tv_go_register);
        btnTogglePassword = findViewById(R.id.btn_toggle_password);
        tabAccount = findViewById(R.id.tab_account);
        tabVerification = findViewById(R.id.tab_verification);
        btnLangTibetan = findViewById(R.id.btn_lang_tibetan);
        btnLangChinese = findViewById(R.id.btn_lang_chinese);
        btnLangEnglish = findViewById(R.id.btn_lang_english);
        labelPassword = findViewById(R.id.label_password);
        btnVisitor = findViewById(R.id.btn_visitor);
        tvForgotPassword = findViewById(R.id.tv_forgot_password);

        originalLoginButtonText = btnLogin.getText().toString();
    }

    private void setupListeners() {
        btnLogin.setOnClickListener(v -> login());
        tvGoRegister.setOnClickListener(v -> goToRegister());
        btnVisitor.setOnClickListener(v -> visitorLogin());

        if (btnTogglePassword != null) {
            btnTogglePassword.setOnClickListener(v -> togglePasswordVisibility());
        }

        if (tabAccount != null) {
            tabAccount.setOnClickListener(v -> switchLoginMode(true));
        }

        if (tabVerification != null) {
            tabVerification.setOnClickListener(v -> switchLoginMode(false));
        }

        if (btnLangTibetan != null) {
            btnLangTibetan.setOnClickListener(v -> switchLanguage(LanguageManager.LANGUAGE_TIBETAN));
        }

        if (btnLangChinese != null) {
            btnLangChinese.setOnClickListener(v -> switchLanguage(LanguageManager.LANGUAGE_CHINESE));
        }

        if (btnLangEnglish != null) {
            btnLangEnglish.setOnClickListener(v -> switchLanguage(LanguageManager.LANGUAGE_ENGLISH));
        }

        if (tvForgotPassword != null) {
            tvForgotPassword.setOnClickListener(v -> {
                Toast.makeText(this, "请联系管理员重置密码", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void togglePasswordVisibility() {
        isPasswordVisible = !isPasswordVisible;
        if (isPasswordVisible) {
            etPassword.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            btnTogglePassword.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
        } else {
            etPassword.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
            btnTogglePassword.setImageResource(android.R.drawable.ic_menu_view);
        }
        etPassword.setSelection(etPassword.getText().length());
    }

    private void switchLoginMode(boolean isAccount) {
        this.isAccountLogin = isAccount;

        if (tabAccount != null && tabVerification != null) {
            if (isAccount) {
                tabAccount.setSelected(true);
                tabAccount.setTextColor(Color.WHITE);
                tabVerification.setSelected(false);
                tabVerification.setTextColor(getResources().getColor(R.color.login_text_secondary));

                // Update hint for account login
                String currentLang = LanguageManager.getInstance(this).getLanguage();
                if (LanguageManager.LANGUAGE_CHINESE.equals(currentLang)) {
                    etAccount.setHint("请输入用户名");
                } else if (LanguageManager.LANGUAGE_ENGLISH.equals(currentLang)) {
                    etAccount.setHint("Enter username");
                } else {
                    etAccount.setHint("སྤྱོད་མཁན་གྱི་མིང་འཇུག་རོགས");
                }
            } else {
                tabVerification.setSelected(true);
                tabVerification.setTextColor(Color.WHITE);
                tabAccount.setSelected(false);
                tabAccount.setTextColor(getResources().getColor(R.color.login_text_secondary));

                // Update hint for verification login
                String currentLang = LanguageManager.getInstance(this).getLanguage();
                if (LanguageManager.LANGUAGE_CHINESE.equals(currentLang)) {
                    etAccount.setHint("请输入手机号");
                } else if (LanguageManager.LANGUAGE_ENGLISH.equals(currentLang)) {
                    etAccount.setHint("Enter phone number");
                } else {
                    etAccount.setHint("ཁ་པར་ཨང་གྲངས་གཏག་རོགས།");

                }
            }
        }
    }

    private void switchLanguage(String language) {
        LanguageManager.getInstance(this).setLanguage(language);
        // Restart activity with new language
        recreate();
    }

    private void updateLanguageButtons(String currentLanguage) {
        if (btnLangTibetan == null || btnLangChinese == null || btnLangEnglish == null) return;

        btnLangTibetan.setSelected(LanguageManager.LANGUAGE_TIBETAN.equals(currentLanguage));
        btnLangChinese.setSelected(LanguageManager.LANGUAGE_CHINESE.equals(currentLanguage));
        btnLangEnglish.setSelected(LanguageManager.LANGUAGE_ENGLISH.equals(currentLanguage));

        if (btnLangTibetan.isSelected()) {
            btnLangTibetan.setTextColor(Color.WHITE);
            btnLangTibetan.setBackgroundColor(getResources().getColor(R.color.login_orange));
        } else {
            btnLangTibetan.setTextColor(getResources().getColor(R.color.login_text_secondary));
            btnLangTibetan.setBackgroundColor(Color.WHITE);
        }

        if (btnLangChinese.isSelected()) {
            btnLangChinese.setTextColor(Color.WHITE);
            btnLangChinese.setBackgroundColor(getResources().getColor(R.color.login_orange));
        } else {
            btnLangChinese.setTextColor(getResources().getColor(R.color.login_text_secondary));
            btnLangChinese.setBackgroundColor(Color.WHITE);
        }

        if (btnLangEnglish.isSelected()) {
            btnLangEnglish.setTextColor(Color.WHITE);
            btnLangEnglish.setBackgroundColor(getResources().getColor(R.color.login_orange));
        } else {
            btnLangEnglish.setTextColor(getResources().getColor(R.color.login_text_secondary));
            btnLangEnglish.setBackgroundColor(Color.WHITE);
        }
    }

    private void login() {
        String account = etAccount.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(account)) {
            String currentLang = LanguageManager.getInstance(this).getLanguage();
            if (LanguageManager.LANGUAGE_CHINESE.equals(currentLang)) {
                Toast.makeText(this, "请输入用户名", Toast.LENGTH_SHORT).show();
            } else if (LanguageManager.LANGUAGE_ENGLISH.equals(currentLang)) {
                Toast.makeText(this, "Please enter username", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, getString(R.string.hint_account), Toast.LENGTH_SHORT).show();
            }
            return;
        }

        if (TextUtils.isEmpty(password)) {
            String currentLang = LanguageManager.getInstance(this).getLanguage();
            if (LanguageManager.LANGUAGE_CHINESE.equals(currentLang)) {
                Toast.makeText(this, "请输入密码", Toast.LENGTH_SHORT).show();
            } else if (LanguageManager.LANGUAGE_ENGLISH.equals(currentLang)) {
                Toast.makeText(this, "Please enter password", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "གསང་ཨང་འཇུག་རོགས", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        btnLogin.setEnabled(false);
        btnLogin.setText(getString(R.string.logging_in));

        LoginRequest request = new LoginRequest(account, password);

        RetrofitClient.getInstance()
                .getApiService()
                .login(request)
                .enqueue(new Callback<BaseResponse<LoginResponse>>() {
                    @Override
                    public void onResponse(Call<BaseResponse<LoginResponse>> call, Response<BaseResponse<LoginResponse>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            BaseResponse<LoginResponse> baseResponse = response.body();
                            if (baseResponse.isSuccess()) {
                                LoginResponse loginResponse = baseResponse.getData();

                                tokenManager.saveUserInfo(
                                        loginResponse.getToken(),
                                        loginResponse.getId(),
                                        loginResponse.getUserName(),
                                        loginResponse.getUserAccount(),
                                        loginResponse.getUserAvatar(),
                                        ""
                                );

                                Toast.makeText(LoginActivity.this, getString(R.string.login_success), Toast.LENGTH_SHORT).show();
                                setResult(RESULT_OK);
                                finish();
                            } else {
                                btnLogin.setEnabled(true);
                                btnLogin.setText(originalLoginButtonText);
                                Toast.makeText(LoginActivity.this, baseResponse.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            btnLogin.setEnabled(true);
                            btnLogin.setText(originalLoginButtonText);
                            Toast.makeText(LoginActivity.this, getString(R.string.login_failed), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<BaseResponse<LoginResponse>> call, Throwable t) {
                        btnLogin.setEnabled(true);
                        btnLogin.setText(originalLoginButtonText);
                        Toast.makeText(LoginActivity.this, getString(R.string.network_error), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void visitorLogin() {
        // Visitor mode - skip login and go directly to main activity
        Toast.makeText(this, "游客模式登录中...", Toast.LENGTH_SHORT).show();
        setResult(RESULT_OK);
        finish();
    }

    private void goToRegister() {
        Intent intent = new Intent(this, RegisterActivity.class);
        startActivity(intent);
    }
}