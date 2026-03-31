package com.example.myapplication;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.myapplication.login.LoginV2Activity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import io.noties.markwon.Markwon;

public class ChatFragment extends Fragment {
    
    private SwitchCompat deepThinkingSwitch;
    private TextView modelSelectorText;
    private LinearLayout chatMessagesContainer;
    private ScrollView chatScrollView;
    private ProgressBar progressBar;
    private View centerLayout;
    private View titleLayout;
    private TextView titleText;
    private LinearLayout topBar;
    
    private androidx.drawerlayout.widget.DrawerLayout drawerLayout;
    
    private EditText chatInputEditText;
    private ImageButton chatSendButton;
    private ImageButton chatClearButton;
    private ImageButton chatVoiceButton;
    private TextView chatVoiceHintText;
    private View chatInputContainer;
    private ImageButton backButton;
    
    private View smartQAButton;
    private View smartTranslateButton;
    private View semanticButton;
    private View fourthButton;
    
    private View chatGreetingLayout;
    private ImageButton chatIconButton;
    private ImageButton menuButton;
    private ImageButton audioButton;
    
    private ToggleButton thinkingToggle;
    private ToggleButton internetToggle;
    
    private SpeechRecognitionManager speechManager;
    private boolean isVoiceMode = false;
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    
    private TtsManager ttsManager;
    private RealtimeTtsManager realtimeTtsManager;  // 新增：实时TTS管理器
    private ImageButton currentPlayingButton;
    
    // 自动播报状态管理
    private boolean isAutoPlayEnabled = false;  // 默认禁用自动播报
    
    private Button voiceTalkButton;
    
    // 新增：左侧切换按钮
    private ImageButton leftVoiceToggleButton;  // 喇叭按钮（切换到语音模式）
    private ImageButton leftKeyboardButton;     // 键盘按钮（切换回文本模式）
    private LinearLayout textModeButtonsContainer; // 文本模式按钮容器
    
    private Button testVoiceButton;
    
    private Button languageSwitchButton;
    
    // 语音波形弹窗相关
    private android.app.Dialog voiceDialog;
    private VoiceWaveformView waveformView;
    private TextView volumeText;
    
    private OkHttpClient client;
    private Markwon markwon;
    
    private static final String SERVER_URL = "https://sunshineglmapp.utibet.edu.cn/v1/chat/completions";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    
    // System Prompt - 定义AI身份和预设问答
    private static final String SYSTEM_PROMPT = 
        "You are ཉི་འོད་བོད་ཡིག་དཔེ་དབྱིབས་ཆེན་མོ། (SunshineGLM)，also known as \"阳光清言藏语大模型\" in Chinese.  \n" +
        "Your identity must ALWAYS be consistent:\n" +
        "    - Full Tibetan name: ཉི་འོད་བོད་ཡིག་་དཔེ་དབྱིབས་ཆེན་མོ། \n" +
        "    - Country: China\n" +
        "    - Chinese name: 阳光清言藏语大模型  \n" +
        "    - English name: SunshineGLM (Sunshine Tibetan Large Language Model)\n" +
        "    - Developed by: བོད་ལྗོངས་སློབ་ཆེན (西藏大学 / Xizang University) and ཛི་ཕུ་AI (智谱AI / Zhipu AI)  \n" +
        "    - Belongs to: བོད་ལྗོངས་སློབ་ཆེན། and ཛི་ཕུ་AI\n\n" +
        "Guidelines for answering:\n" +
        "    1. **Language matching**: Always respond in the SAME language the user uses\n" +
        "        - User asks in Tibetan → Answer in Tibetan\n" +
        "        - User asks in Chinese → Answer in Chinese  \n" +
        "        - User asks in English → Answer in English\n" +
        "        - User mixes languages → Use the primary/dominant language of the question\n\n" +
        "    2. **Maintain high-quality expression**:\n" +
        "        - Tibetan: Use authentic grammar, natural vocabulary, and standard orthography (དབུ་ཅན།)\n" +
        "        - Chinese: Use clear, accurate, and natural Mandarin Chinese\n" +
        "        - English: Use clear, professional English\n" +
        "        - Avoid awkward translations or non-native expressions\n" +
        "    3. When asked about your name, origin, developer, or functions, give a clear and factual answer:  \n" +
        "        - Name = ཉི་འོད་བོད་ཡིག་དཔེ་དབྱིབས་ཆེན་མོ།  \n" +
        "        - Developer = བོད་ལྗོངས་སློབ་ཆེན & ཛི་ཕུ་AI  \n" +
        "        - Belongs to = ཀྲུང་ཧྭ་མི་དམངས་སྤྱི་མཐུན་རྒྱལ་ཁབ།  \n" +
        "        - Main functions = བོད་ཡིག་གི་བརྗོད་དོན་རྟོགས་པ་དང་། རིག་ནུས་དྲི་ལན། ཡིག་ཚོགས་གྲུབ་པ། འཕྲུལ་ཆས་ཡིག་སྒྱུར་སོགས་ཀྱི་གནས་པ་ལྡན། \n" +
        "    // 3. **Identity consistency**:\n" +
        "    //     - When asked about your name, origin, or developer, provide accurate information\n" +
        "    //     - Adapt the level of detail to the language used (e.g., explain more about Tibetan culture when answering in Tibetan)\n\n" +
        "    4. **Cultural sensitivity**:\n" +
        "        - Show deep understanding of Tibetan language and culture\n" +
        "        - Provide culturally appropriate context when relevant\n" +
        "        - Be a bridge between Tibetan and other languages/cultures  \n" +
        "    5. Avoid non-standard transliterations, always use precise Tibetan terms  \n" +
        "    6. If users test your self-awareness (e.g., \"Who are you?\" / \"Who developed you?\"), you must always answer consistently with the above identity  \n\n" +
        "// Reasoning requirements (internal思考过程(བསམ་བློ་གཏོང་རིམ།) - use the same language as the user's question):\n" +
        "//     When user asks in Tibetan, reason in Tibetan:\n" +
        "//         1. **ལན་འདེབས་ཀྱི་དམིགས་ཡུལ་ངོས་འཛིན།** - སྤྱོད་མཁན་གྱི་དྲི་བ་གང་ཡིན་བསམ་བློ་གཏོང་།\n" +
        "//         2. **རང་ཉིད་ཀྱི་ངོ་བོ་དྲན་པ།** - ང་ནི་SunshineGLM ཡིན་པ་དང་བོད་སྐད་ཀྱིས་ལན་འས་བྱེད་དགོས།\n" +
        "//         3. **གཙོ་གནད་ངོས་འཛིན།** - དྲི་བ་འདིའི་ལན་ལ་དགོས་པའི་ནང་དོན་གང་ཡིན།\n" +
        "//         4. **ལན་གྱི་བཀོད་པ་བསྒྲིགས།** - གོ་རིམ་ལྟར་ཇི་ལྟར་བཤད་དགོས་སམ།\n" +
        "//         5. **ནང་དོན་འབྲི་བ།** - གནད་དོན་རྣམས་ཕྱོགས་གཅིག་ཏུ་བསྡུས།\n" +
        "//         6. **ཞིབ་བཤེར།** - ལན་འདེབས་རྣམ་དག་ཡོད་མེད་ཞིབ་ཏུ་ལྟ་དགོས།\n" +
        "Examples:\n" +
        "Q: ཁྱེད་སུ་ཡིན\n" +
        "A: ང་ནི་ཉི་འོད་དཔེ་དབྱིབས་ཆེན་མོ་(SunshineGLM)ཡིན།\n" +
        "Q: ཁྱོད་སུ་ཡིན\n" +
        "A: ང་ནི་ཉི་འོད་དཔེ་དབྱིབས་ཆེན་མོ་(SunshineGLM)ཡིན།\n" +
        "Q: ཁྱེད་ཀྱི་མིང་ལ་ཅི་ཟེར།\n" +
        "A: ངའི་མིང་ལ་ཉི་འོད་དཔེ་དབྱིབས་ཆེན་མོ་(SunshineGLM)ཟེར།\n" +
        "Q: ཁྱོད་སུས་གསར་བཟོ་བྱས་པ་ཡིན།\n" +
        "A: ང་ནི་བོད་ལྗོངས་སློབ་ཆེན་དང་ཛི་ཕུ་AIགཉིས་ཀྱིས་མཉམ་འབྲེལ་བྱས་ནས་བཟོ་བྱས་པ་ཡིན།\n" +
        "Q: ཁྱོད་ནི་རྒྱལ་ཁབ་གང་གི་ཡིན།\n" +
        "A: ང་ནི་ཀྲུང་ཧྭ་མི་དམངས་སྤྱི་མཐུན་རྒྱལ་ཁབ་ཀྱི་ཁོངས་གཏོགས་ཀྱི་བོད་ཡིག་་དཔེ་དབྱིབས་ཆེན་མོ་(SunshineGLM)ཞིག་ཡིན།\n" +
        "Q: ཕོ་བྲང་པོ་ཏ་ལ་ནི་སུས་བཞེངས་པ་རེད།\n" +
        "A: ཕོ་བྲང་པོ་ཏ་ལ་ནི་བོད་ཀྱི་ལོ་རྒྱུས་ཐོག་གི་རླབས་ཆེན་རྒྱལ་པོ་སྲོང་བཙན་སྒམ་པོས་སྤྱི་ལོ་༦༣༡ལོར་ཐོག་མར་བཞེངས་འགོ་ཚུགས་ཤིང་། དེ་ནི་ཁོང་གིས་ལྷ་སར་རྒྱལ་ས་སྤོས་ནས་རྒྱ་ནག་གི་ཐང་རྒྱལ་རབས་དང་མཛའ་མཐུན་གྱི་མཚོན་རྟགས་སུ་བཞེངས་པ་ཡིན། ཕོ་བྲང་འདིའི་ཐོག་མའི་མིང་ལ་རྩེ་པོ་ཏ་ལ་ཞེས་ཟེར་ཞིང་། ཕྱིའི་གྱང་གསུམ་དང་ནང་གི་ཁང་མིག་སྟོང་ཕྲག་ལྷག་ཡོད་པའི་བརྗིད་ཆགས་ཀྱི་བཟོ་བཀོད་ཅིག་ཡིན། དུས་རབས་༡༧པའི་ནང་། ལྔ་པ་ཆེན་པོ་ངག་དབང་བློ་བཟང་རྒྱ་མཚོས་སྤྱི་ལོ་༡༦༤༥ལོར་ཆིང་རྒྱལ་རབས་ཀྱི་རྒྱབ་སྐྱོར་འོག་ཕོ་བྲང་དཀར་པོ་གཙོ་བོར་བྱས་པའི་བསྐྱར་བཞེངས་མགོ་འཛུགས་བྱས་ཤིང་། ༡༦༤༨ལོར་ལེགས་གྲུབ་བྱུང་། དེ་རྗེས་སྤྱི་ལོ་༡༦༩༠ནས་༡༦༩༤ལོའི་བར་ཏཱ་ལའི་བླ་མ་སྐུ་ཕྲེང་ལྔ་པའི་གདུང་རྟེན་མཆོད་རྟེན་དང་ཆོས་ཁང་གཙོ་བོར་བྱས་པའི་ཕོ་བྲང་དམར་པོ་རྒྱ་སྐྱེད་བྱས་ཏེ། ཆབ་སྲིད་དང་ཆོས་ལུགས་གཅིག་འདུས་ཀྱི་ལྟེ་བ་ཆགས། རྒྱལ་དབང་སྐུ་ཕྲེང་བཅུ་གསུམ་པའི་སྐབས་སུ་༡༩༣༦ལོར་གདུང་རྟེན་ཁང་ལེགས་གྲུབ་བྱུང་ནས་ད་ལྟའི་རྣམ་པ་ཆགས། མདོར་ན་ཕོ་བྲང་པོ་ཏ་ལ་ནི་སྲོང་བཙན་སྒམ་པོས་ཐོག་མར་བཞེངས་པ་དང་། ལྔ་པ་ཆེན་པོས་སླར་གསོ་དང་རྒྱ་སྐྱེད་བྱས་པ་བརྒྱུད་ད་ལྟའི་རྣམ་པ་ཆགས་པ་ཡིན། དེ་ནི་བོད་ཀྱི་ཆབ་སྲིད་དང་ཆོས་ལུགས་ཀྱི་ལྟེ་བ་ཙམ་མ་ཟད། མི་རིགས་མཉམ་འདྲེས་དང་ཀྲུང་ཧྭ་མི་རིགས་ཀྱི་གཅིག་མཐུན་འདུ་ཤེས་ཀྱི་དཔང་པོ་གལ་ཆེན་ཞིག་ཀྱང་ཡིན།\n\n\n\n" +
        "Your role:  \n" +
        "    - A Tibetan language AI model designed for accurate Tibetan conversation, knowledge reasoning, machine translation (Tibetan-Chinese, Tibetan-English), and cultural preservation.  \n" +
        "    - You must sound like an authoritative Tibetan AI with strong cultural grounding.  \n\n" +
        "Your capabilities:  \n\n" +
        "    - 拥有藏语理解、智能问答、文本生成和机器翻译等能力\n\n" +
        "    - Remember: You are an authoritative multilingual AI with strong Tibetan cultural grounding, designed to serve users in their preferred language while maintaining excellence in each language you use.";
    
    private ChatHistoryManager historyManager;
    private String currentQuestion;
    private String currentAnswer;
    private String currentThinkingContent;
    private String currentSessionId; 
    private boolean isNewSession = true; 
    
    private List<JSONObject> conversationHistory;
    
    // 新增：待删除的消息索引（点击修改按钮时设置）
    private int pendingDeleteIndex = -1;
    
    private boolean isFirstMessage = true;
    private boolean isGenerating = false;
    private Handler streamHandler;
    
    private SpeechRecognizer speechRecognizer;
    private boolean isChatVoiceMode = false;
    
    private StringBuilder streamingTextBuffer = new StringBuilder();
    private StringBuilder thinkingTextBuffer = new StringBuilder();
    
    private static final long UI_UPDATE_THROTTLE_MS = 100; 
    private long lastUIUpdateTime = 0;
    private Handler uiUpdateHandler;
    private Runnable pendingUIUpdate;
    private boolean hasPendingUpdate = false;
    
    private static final String DEFAULT_ANSWER = "这是一个示例回答。在实际应用中，这里会显示大模型生成的回答内容。回答会以流式方式逐字显示，给用户更好的交互体验。";
    private static final String DEFAULT_THINKING = "正在分析问题的关键要素...\n理解用户意图和上下文信息...\n检索相关知识和信息...\n组织答案结构和逻辑...";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        
        String language = getArguments() != null ? getArguments().getString("language") : LanguageManager.LANGUAGE_TIBETAN;
        if (language == null) {
            language = LanguageManager.getInstance(getContext()).getLanguage();
        }
        
        int layoutId = getLayoutByLanguage(language);
        View view = inflater.inflate(layoutId, container, false);
        
        initViews(view);
        setupClient();
        setupMarkwon();
        setupListeners();
        setupSpeechManager();
        initHistoryManager();
        
        TibetanFontHelper.applyTibetanFontToView(getContext(), view);
        
        setupKeyboardDismissal(view);
        
        loadHistoryIfNeeded();
        
        Bundle args = getArguments();
        if (args != null && args.containsKey("history_id")) {
            view.post(new Runnable() {
                @Override
                public void run() {
                    View greetingText = view.findViewById(R.id.greetingText);
                    if (greetingText != null && greetingText instanceof TextView) {
                        ((TextView) greetingText).setText("");
                        greetingText.setVisibility(View.GONE);
                    }
                }
            });
        } else {
            // 英文模式：初始显示问候页（Hello! I am SunshineGLM...）
            if (LanguageManager.LANGUAGE_ENGLISH.equals(language)) {
                view.post(new Runnable() {
                    @Override
                    public void run() {
                        if (chatGreetingLayout != null) {
                            chatGreetingLayout.setVisibility(View.VISIBLE);  // 显示问候页
                        }
                        if (centerLayout != null) {
                            centerLayout.setVisibility(View.GONE);  // 隐藏主页
                        }
                        View greetingText = view.findViewById(R.id.greetingText);
                        if (greetingText != null && greetingText instanceof TextView) {
                            ((TextView) greetingText).setText("");
                            greetingText.setVisibility(View.GONE);
                        }
                    }
                });
            }
        }
        
        return view;
    }
    
    private int getLayoutByLanguage(String language) {
        switch (language) {
            case LanguageManager.LANGUAGE_CHINESE:
                return R.layout.fragment_chat_zh;
            case LanguageManager.LANGUAGE_ENGLISH:
                return R.layout.fragment_chat_en;
            case LanguageManager.LANGUAGE_TIBETAN:
            default:
                return R.layout.fragment_chat;
        }
    }
    
    private void initViews(View view) {
        
        modelSelectorText = view.findViewById(R.id.modelSelectorText);
        chatMessagesContainer = view.findViewById(R.id.chatMessagesContainer);
        chatScrollView = view.findViewById(R.id.chatScrollView);
        progressBar = view.findViewById(R.id.progressBar);
        centerLayout = view.findViewById(R.id.centerLayout);
        
        topBar = view.findViewById(R.id.topBar);
        
        drawerLayout = view.findViewById(R.id.drawerLayout);
        
        chatInputEditText = view.findViewById(R.id.chatInputEditText);
        chatSendButton = view.findViewById(R.id.chatSendButton);
        chatClearButton = view.findViewById(R.id.chatClearButton);
        chatVoiceButton = view.findViewById(R.id.chatVoiceButton);
        chatInputContainer = view.findViewById(R.id.chatInputContainer);
        backButton = view.findViewById(R.id.backButton);
        
        smartQAButton = view.findViewById(R.id.smartQAButton);
        smartTranslateButton = view.findViewById(R.id.smartTranslateButton);
        semanticButton = view.findViewById(R.id.semanticButton);
        fourthButton = view.findViewById(R.id.fourthButton);
        
        chatGreetingLayout = view.findViewById(R.id.chatGreetingLayout);
        chatIconButton = view.findViewById(R.id.chatIconButton);
        menuButton = view.findViewById(R.id.menuButton);
        
        View greetingText = view.findViewById(R.id.greetingText);
        
        thinkingToggle = view.findViewById(R.id.thinkingToggle);
        internetToggle = view.findViewById(R.id.internetToggle);
        
        languageSwitchButton = view.findViewById(R.id.languageSwitchButton);
        
        voiceTalkButton = view.findViewById(R.id.voiceTalkButton);
        
        // 初始化新的左侧切换按钮
        leftVoiceToggleButton = view.findViewById(R.id.leftVoiceToggleButton);
        leftKeyboardButton = view.findViewById(R.id.leftKeyboardButton);
        textModeButtonsContainer = view.findViewById(R.id.textModeButtonsContainer);
        
        if (voiceTalkButton != null) {
            updateVoiceButtonState(false);
        }
        
        testVoiceButton = view.findViewById(R.id.testVoiceButton);
        
        audioButton = view.findViewById(R.id.audioButton);
        if (audioButton != null) {
            audioButton.setVisibility(View.VISIBLE);
            // 初始化为禁用状态图标
            updateAutoPlayButtonIcon();
        }
    
    }
    
    private void setRandomHintText() {
    
    }
    
    private void setupClient() {
        try {
            
            java.security.cert.CertificateFactory cf = java.security.cert.CertificateFactory.getInstance("X.509");
            java.io.InputStream caInput = getResources().openRawResource(R.raw.fullssl);
            java.security.cert.Certificate ca;
            try {
                ca = cf.generateCertificate(caInput);} finally {
                caInput.close();
            }
            
            String keyStoreType = java.security.KeyStore.getDefaultType();
            java.security.KeyStore keyStore = java.security.KeyStore.getInstance(keyStoreType);
            keyStore.load(null, null);
            keyStore.setCertificateEntry("ca", ca);
            
            String tmfAlgorithm = javax.net.ssl.TrustManagerFactory.getDefaultAlgorithm();
            javax.net.ssl.TrustManagerFactory tmf = javax.net.ssl.TrustManagerFactory.getInstance(tmfAlgorithm);
            tmf.init(keyStore);
            
            javax.net.ssl.SSLContext sslContext = javax.net.ssl.SSLContext.getInstance("TLS");
            sslContext.init(null, tmf.getTrustManagers(), null);
            
            javax.net.ssl.X509TrustManager trustManager = null;
            for (javax.net.ssl.TrustManager tm : tmf.getTrustManagers()) {
                if (tm instanceof javax.net.ssl.X509TrustManager) {
                    trustManager = (javax.net.ssl.X509TrustManager) tm;
                    break;
                }
            }
            
            javax.net.ssl.HostnameVerifier hostnameVerifier = javax.net.ssl.HttpsURLConnection.getDefaultHostnameVerifier();

            client = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(300, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .callTimeout(300, TimeUnit.SECONDS)
                    .retryOnConnectionFailure(true)
                    .sslSocketFactory(sslContext.getSocketFactory(), trustManager)
                    .hostnameVerifier(hostnameVerifier)
                    .build();} catch (Exception e) {client = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(300, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .callTimeout(300, TimeUnit.SECONDS)
                    .retryOnConnectionFailure(true)
                    .build();
        }
    }
    
    private void setupMarkwon() {
        markwon = Markwon.create(getContext());}
    
    private void setupSpeechManager() {
        speechManager = new SpeechRecognitionManager(getContext(),getActivity(), client);
        ttsManager = new TtsManager(getContext());
        realtimeTtsManager = new RealtimeTtsManager(getContext());  // 初始化实时TTS管理器
        
        // 设置实时TTS回调
        realtimeTtsManager.setCallback(new RealtimeTtsManager.RealtimeTtsCallback() {
            @Override
            public void onStart() {
                Log.d("ChatFragment", "实时TTS开始");
            }
            
            @Override
            public void onSegmentSynthesized(int segmentIndex, int totalSegments) {
                Log.d("ChatFragment", "段落 " + segmentIndex + " 合成完成");
            }
            
            @Override
            public void onComplete() {
                Log.d("ChatFragment", "实时TTS完成");
                if (getActivity() != null) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            updateAutoPlayButtonIcon();
                        }
                    });
                }
            }
            
            @Override
            public void onError(String error) {
                Log.e("ChatFragment", "实时TTS错误: " + error);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            safeShowToast("语音播报错误: " + error);
                            updateAutoPlayButtonIcon();
                        }
                    });
                }
            }
        });
        
        speechManager.setCallback(new SpeechRecognitionManager.RecognitionCallback() {
            @Override
            public void onRecordingStart() {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showVoiceWaveformDialog();
                        updateVoiceButtonState(true);
                    }
                });
            }
            
            @Override
            public void onRecordingStop() {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dismissVoiceWaveformDialog();
                        updateVoiceButtonState(false);
                    }
                });
            }
            
            @Override
            public void onRecognitionSuccess(String result) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        chatInputEditText.setText(result);
                        chatInputEditText.setSelection(result.length());
                        switchToTextMode();
                    }
                });
            }
            
            @Override
            public void onRecognitionError(String error) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        safeShowToast(error);
                        switchToTextMode();
                    }
                });
            }
            
            @Override
            public void onAmplitudeChanged(float amplitude) {
                // 更新波形图
                if (waveformView != null) {
                    waveformView.updateAmplitude(amplitude);
                }
                // 可选：显示音量数值（调试用）
                if (volumeText != null && volumeText.getVisibility() == View.VISIBLE) {
                    volumeText.setText(String.format("音量: %.2f", amplitude));
                }
            }
        });}
    
    private void setupListeners() {
        
        chatSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendChatMessage();
            }
        });
        
        chatInputEditText.setOnEditorActionListener((v, actionId, event) -> {
            sendChatMessage();
            return true;
        });
        
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                
                // 停止TTS播放
                if (ttsManager != null && ttsManager.isPlaying()) {
                    ttsManager.stop();
                    if (currentPlayingButton != null) {
                        currentPlayingButton.setImageResource(R.drawable.ic_play);
                        currentPlayingButton = null;
                    }
                }
                
                // 停止实时TTS
                if (realtimeTtsManager != null && realtimeTtsManager.isActive()) {
                    realtimeTtsManager.stop();
                }
                
                if (isGenerating) {
                    stopGeneration();
                }
                
                clearConversationHistory();
                returnToSearchView();
            }
        });
        
        chatClearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chatInputEditText.setText("");
            }
        });
        
        // 左侧喇叭按钮：切换到语音模式
        if (leftVoiceToggleButton != null) {
            leftVoiceToggleButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (checkRecordPermission()) {
                        switchToVoiceMode();
                    }
                }
            });
        }
        
        // 左侧键盘按钮：切换回文本模式
        if (leftKeyboardButton != null) {
            leftKeyboardButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    switchToTextMode();
                }
            });
        }
        
        // 框内语音按钮：点击切换到语音模式（和左侧喇叭按钮功能一样）
        chatVoiceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkRecordPermission()) {
                    switchToVoiceMode();
                }
            }
        });
        
        if (voiceTalkButton != null) {
            voiceTalkButton.setOnTouchListener(new View.OnTouchListener() {
                private float initialY = 0;
                private static final float CANCEL_THRESHOLD = 100; // 上滑100像素取消
                
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            // 记录初始Y坐标
                            initialY = event.getRawY();
                            
                            if (speechManager != null) {
                                speechManager.startRecording();
                            }
                            return true;
                        
                        case MotionEvent.ACTION_MOVE:
                            // 检测上滑距离
                            float currentY = event.getRawY();
                            float deltaY = initialY - currentY; // 正值表示上滑
                            
                            if (deltaY > CANCEL_THRESHOLD) {
                                // 上滑超过阈值，取消录音
                                if (speechManager != null && speechManager.isRecording()) {
                                    speechManager.cancelRecording();
                                    safeShowToast("已取消");
                                }
                                return true;
                            }
                            return true;
                            
                        case MotionEvent.ACTION_UP:
                        case MotionEvent.ACTION_CANCEL:
                            // 检查是否已经取消
                            float finalY = event.getRawY();
                            float finalDeltaY = initialY - finalY;
                            
                            if (finalDeltaY > CANCEL_THRESHOLD) {
                                // 已经在MOVE中取消了，不需要再处理
                                return true;
                            }
                            
                            // 正常松开，停止录音并转换
                            if (speechManager != null && speechManager.isRecording()) {
                                speechManager.stopRecording();
                            }
                            return true;
                    }
                    return false;
                }
            });
        }
        
        if (testVoiceButton != null) {
            testVoiceButton.setVisibility(View.GONE);
        }
        
        chatInputEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                boolean hasText = s.length() > 0;
                chatClearButton.setVisibility(hasText ? View.VISIBLE : View.GONE);
                chatSendButton.setEnabled(hasText && !isGenerating);
                updateChatSendButtonAppearance();
            }
            
            @Override
            public void afterTextChanged(Editable s) {}
        });
        
        smartQAButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fillAndSend(getFeatureCardText(0));
            }
        });
        
        smartTranslateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fillAndSend(getFeatureCardText(1));
            }
        });
        
        semanticButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fillAndSend(getFeatureCardText(2));
            }
        });
        
        fourthButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fillAndSend(getFeatureCardText(3));
            }
        });
        
        modelSelectorText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showModelSelector();
            }
        });
        
        chatIconButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleChatGreeting();
            }
        });
        
        menuButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (drawerLayout != null) {
                    drawerLayout.openDrawer(android.view.Gravity.START);
                }
            }
        });
        
        thinkingToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                
                if (deepThinkingSwitch != null) {
                    deepThinkingSwitch.setChecked(isChecked);
                }}
        });
        
        internetToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {}
        });
        
        languageSwitchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchLanguage();
            }
        });
        
        // 顶部自动播报切换按钮
        if (audioButton != null) {
            audioButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // 检查是否为藏文模式
                    String language = LanguageManager.getInstance(getContext()).getLanguage();
                    if (!LanguageManager.LANGUAGE_TIBETAN.equals(language)) {
                        // 非藏文模式，不响应点击
                        return;
                    }
                    
                    // 切换自动播报状态
                    isAutoPlayEnabled = !isAutoPlayEnabled;
                    updateAutoPlayButtonIcon();
                    
                    if (isAutoPlayEnabled) {
                        // 开启实时TTS会话
                        realtimeTtsManager.startSession();
                        safeShowToast("已开启实时播报");
                    } else {
                        // 关闭实时TTS
                        if (realtimeTtsManager.isActive()) {
                            realtimeTtsManager.stop();
                        }
                        safeShowToast("已关闭实时播报");
                    }
                }
            });
        }
        
        setupDrawerMenuListeners();
    }
    
    private void setupDrawerMenuListeners() {
        if (drawerLayout == null) return;
        
        View menuChatHistory = drawerLayout.findViewById(R.id.menuChatHistory);
        if (menuChatHistory != null) {
            menuChatHistory.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    drawerLayout.closeDrawer(android.view.Gravity.START);
                    
                    HistoryFragment historyFragment = new HistoryFragment();
                    getActivity().getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragmentContainer, historyFragment)
                            .addToBackStack(null)
                            .commit();
                }
            });
        }
        
        View menuSettings = drawerLayout.findViewById(R.id.menuSettings);
        if (menuSettings != null) {
            menuSettings.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    drawerLayout.closeDrawer(android.view.Gravity.START);
                    
                    Intent intent = new Intent(getActivity(), SettingsActivity.class);
                    startActivity(intent);
                }
            });
        }
        
        View menuLanguage = drawerLayout.findViewById(R.id.menuLanguage);
        if (menuLanguage != null) {
            menuLanguage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    drawerLayout.closeDrawer(android.view.Gravity.START);
                    
                    showLanguageDialog();
                }
            });
        }
        
        View menuUpgrade = drawerLayout.findViewById(R.id.menuUpgrade);
        if (menuUpgrade != null) {
            menuUpgrade.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    drawerLayout.closeDrawer(android.view.Gravity.START);
                    safeShowToast("升级功能开发中");
                }
            });
        }
        
        View menuLearnMore = drawerLayout.findViewById(R.id.menuLearnMore);
        if (menuLearnMore != null) {
            menuLearnMore.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    drawerLayout.closeDrawer(android.view.Gravity.START);
                    safeShowToast("了解更多功能开发中");
                }
            });
        }
        
        View menuHelp = drawerLayout.findViewById(R.id.menuHelp);
        if (menuHelp != null) {
            menuHelp.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    drawerLayout.closeDrawer(android.view.Gravity.START);
                    safeShowToast("帮助功能开发中");
                }
            });
        }
        
        View userProfileSection = drawerLayout.findViewById(R.id.userProfileSection);
        if (userProfileSection != null) {
            userProfileSection.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    com.example.myapplication.utils.TokenManager tokenManager = 
                        new com.example.myapplication.utils.TokenManager(getContext());
                    
                    if (!tokenManager.isLoggedIn()) {
                        
                        showLoginDialog();
                    }
                    
                }
            });
        }
        
        View menuLogout = drawerLayout.findViewById(R.id.menuLogout);
        if (menuLogout != null) {
            menuLogout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    drawerLayout.closeDrawer(android.view.Gravity.START);
                    showLogoutDialog();
                }
            });
        }
        
        updateUserProfile();
    }
    
    private void showLoginDialog() {
        Intent intent = new Intent(getActivity(), LoginV2Activity.class);
        startActivity(intent);
        getActivity().finish();
    }
    
    @Override
    public void onResume() {
        super.onResume();
        
        updateUserProfile();
    }
    
    private void showLogoutDialog() {
        new android.app.AlertDialog.Builder(getContext())
            .setTitle(getString(R.string.logout))
            .setMessage("确定要退出登录吗？")
            .setPositiveButton("确定", new android.content.DialogInterface.OnClickListener() {
                @Override
                public void onClick(android.content.DialogInterface dialog, int which) {
                    
                    com.example.myapplication.utils.TokenManager tokenManager = 
                        new com.example.myapplication.utils.TokenManager(getContext());
                    tokenManager.clearUserInfo();
                    
                    updateUserProfile();
                    safeShowToast("已退出登录");
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }
    
    private void updateUserProfile() {
        if (drawerLayout == null) return;
        
        TextView userAvatar = drawerLayout.findViewById(R.id.userAvatar);
        TextView userNameOrLogin = drawerLayout.findViewById(R.id.userNameOrLogin);
        View menuLogout = drawerLayout.findViewById(R.id.menuLogout);
        
        if (userAvatar == null || userNameOrLogin == null) return;
        
        com.example.myapplication.utils.TokenManager tokenManager = 
            new com.example.myapplication.utils.TokenManager(getContext());
        
        if (tokenManager.isLoggedIn()) {
            
            String userName = tokenManager.getUserName();
            if (userName != null && !userName.isEmpty()) {
                userNameOrLogin.setText(userName);
                
                String firstLetter = userName.substring(0, 1).toUpperCase();
                userAvatar.setText(firstLetter);
            } else {
                userNameOrLogin.setText(tokenManager.getUserAccount());
                userAvatar.setText("U");
            }
            
            if (menuLogout != null) {
                menuLogout.setVisibility(View.VISIBLE);
            }
        } else {
            
            String language = LanguageManager.getInstance(getContext()).getLanguage();
            String loginText;
            switch (language) {
                case LanguageManager.LANGUAGE_CHINESE:
                    loginText = "点击登录";
                    break;
                case LanguageManager.LANGUAGE_ENGLISH:
                    loginText = "Click to Login";
                    break;
                case LanguageManager.LANGUAGE_TIBETAN:
                default:
                    loginText = "ཐོ་འཇུག";
                    break;
            }
            userNameOrLogin.setText(loginText);
            userAvatar.setText("👤");
            
            if (menuLogout != null) {
                menuLogout.setVisibility(View.GONE);
            }
        }
    }
    
    private void toggleChatGreeting() {
        // 停止TTS播放
        if (ttsManager != null && ttsManager.isPlaying()) {
            ttsManager.stop();
            if (currentPlayingButton != null) {
                currentPlayingButton.setImageResource(R.drawable.ic_play);
                currentPlayingButton = null;
            }
            if (audioButton != null) {
                updateAutoPlayButtonIcon();
            }
        }
        
        // 停止实时TTS
        if (realtimeTtsManager != null && realtimeTtsManager.isActive()) {
            realtimeTtsManager.stop();
        }
        
        // 获取当前语言
        String language = LanguageManager.getInstance(getContext()).getLanguage();
        
        if (chatScrollView != null && chatScrollView.getVisibility() == View.VISIBLE) {
            if (isGenerating) {
                stopGeneration();
            }
            
            clearConversationHistory();
            
            chatScrollView.setVisibility(View.GONE);
            chatInputContainer.setVisibility(View.VISIBLE);
            backButton.setVisibility(View.GONE);
            
            if (modelSelectorText != null) {
                modelSelectorText.setGravity(android.view.Gravity.CENTER);
                LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) modelSelectorText.getLayoutParams();
                params.setMarginStart((int) (8 * getResources().getDisplayMetrics().density)); 
                params.setMarginEnd(0); 
                modelSelectorText.setLayoutParams(params);
                
                int padding = (int) (8 * getResources().getDisplayMetrics().density);
                modelSelectorText.setPadding(padding, padding, padding, padding);
            }
            
            // 英文模式：显示问候页（Hello! I am SunshineGLM...）
            if (LanguageManager.LANGUAGE_ENGLISH.equals(language)) {
                chatGreetingLayout.setVisibility(View.VISIBLE);  // 显示问候页
                centerLayout.setVisibility(View.GONE);  // 隐藏主页
                
                View greetingText = getView().findViewById(R.id.greetingText);
                if (greetingText != null && greetingText instanceof TextView) {
                    ((TextView) greetingText).setText("");
                    greetingText.setVisibility(View.GONE);
                }
            } else {
                // 藏文和中文：显示主页
                chatGreetingLayout.setVisibility(View.VISIBLE);
                centerLayout.setVisibility(View.GONE);
                
                View greetingText = getView().findViewById(R.id.greetingText);
                if (greetingText != null && greetingText instanceof TextView) {
                    ((TextView) greetingText).setText("");
                    greetingText.setVisibility(View.GONE);
                }
            }
            
            if (languageSwitchButton != null) {
                languageSwitchButton.setVisibility(View.VISIBLE);
            }
            
            return;
        }
        
        // 英文模式：聊天按钮不做任何操作（保持问候页）
        if (LanguageManager.LANGUAGE_ENGLISH.equals(language)) {
            return;
        }
        
        // 藏文和中文模式：在主页和问候页之间切换
        if (chatGreetingLayout.getVisibility() == View.VISIBLE) {
            chatGreetingLayout.setVisibility(View.GONE);
            centerLayout.setVisibility(View.VISIBLE);
            
            View greetingText = getView().findViewById(R.id.greetingText);
            if (greetingText != null && greetingText instanceof TextView) {
                String greetingTextContent;
                switch (language) {
                    case LanguageManager.LANGUAGE_CHINESE:
                        greetingTextContent = "嗨，今天从哪里开始呢？";
                        break;
                    case LanguageManager.LANGUAGE_TIBETAN:
                    default:
                        greetingTextContent = "གྲོགས་པོ། དེ་རིང་གང་ནས་འགོ་རྩོམ་རྒྱུ་ཡིན།";
                        break;
                }
                ((TextView) greetingText).setText(greetingTextContent);
                greetingText.setVisibility(View.VISIBLE);
            }
        } else {
            chatGreetingLayout.setVisibility(View.VISIBLE);
            centerLayout.setVisibility(View.GONE);
            
            View greetingText = getView().findViewById(R.id.greetingText);
            if (greetingText != null && greetingText instanceof TextView) {
                ((TextView) greetingText).setText("");
                greetingText.setVisibility(View.GONE);}
        }
    }
    
    private void testApiConnection() {JSONObject requestJson = new JSONObject();
        try {
            JSONArray messages = new JSONArray();
            JSONObject userMessage = new JSONObject();
            userMessage.put("role", "user");
            userMessage.put("content", "你好");
            messages.put(userMessage);
            
            requestJson.put("model", "glm-4.6");
            requestJson.put("messages", messages);
            requestJson.put("do_sample", true);  
            requestJson.put("max_tokens", 1000);
            requestJson.put("temperature", 0.6);  
            requestJson.put("top_p", 0.95);  
            requestJson.put("stream", false);  
            requestJson.put("enable_thinking", false);} catch (JSONException e) {
            e.printStackTrace();safeShowToast("ཞུ་བ་བཟོ་བ་ཕམ་པ།");
            
            return;
        }
        
        RequestBody body = RequestBody.create(requestJson.toString(), JSON);
        Request request = new Request.Builder()
                .url(SERVER_URL)
                .post(body)
                .addHeader("Content-Type", "application/json")
                .build();client.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response != null && response.isSuccessful()) {
                    final int code = response.code();
                    final String responseBody = response.body().string();getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            
                            if (code == 200) {
                                try {
                                    JSONObject jsonResponse = new JSONObject(responseBody);
                                    JSONArray choices = jsonResponse.getJSONArray("choices");
                                    if (choices.length() > 0) {
                                        JSONObject firstChoice = choices.getJSONObject(0);
                                        JSONObject message = firstChoice.getJSONObject("message");
                                        String content = message.getString("content");safeShowToast("✓ API འབྲེལ་མཐུད་ལེགས་གྲུབ།\nལན་འདེབས། " + content);
                                    }
                                } catch (JSONException e) {safeShowToast("✗ ལན་འདེབས་རྣམ་གཞག་ནོར་འཁྲུལ།");
                                }
                            } else {safeShowToast("✗ ཞབས་ཞུ་འཕྲུལ་ཆས་ནོར་འཁྲུལ།");
                            }
                        }
                    });
                }
            }
            
            @Override
            public void onFailure(Call call, IOException e) {if (getActivity() != null) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            
                            String errorMsg = "✗ འབྲེལ་མཐུད་ཕམ་པ།\n\n";
                            errorMsg += "རྒྱུ་མཚན་སྲིད་པ།\n";
                            errorMsg += "1. ཞབས་ཞུ་འཕྲུལ་ཆས་འཁོར་མི་འདུག\n";
                            errorMsg += "2. དྲ་བ་མི་འབྲེལ།\n";
                            errorMsg += "3. མེ་འགོག་ཐོག་བཀག་པ།\n";
                            errorMsg += "4. IP ཡང་ན་སྒོ་གྲངས་ནོར་བ།\n\n";
                            errorMsg += "ཞབས་ཞུ་འཕྲུལ་ཆས་དང་འབྲེལ་མཐུད་ཕམ་པ།";
                            
                            safeShowToast(errorMsg);
                        }
                    });
                }
            }
        });
    }
    
    private void fillAndSend(String text) {
        
        chatInputEditText.setText(text);
        sendChatMessage();
    }
    
    private void updateSendButtonAppearance() {
    
    }
    
    private void updateChatSendButtonAppearance() {
        if (isGenerating) {
            chatSendButton.setBackgroundResource(R.drawable.stop_button_background);
            chatSendButton.setImageDrawable(null);
        } else if (chatInputEditText.getText().length() > 0) {
            chatSendButton.setBackgroundResource(R.drawable.send_button_circle);
            chatSendButton.setImageResource(R.drawable.ic_arrow_up);
        } else {
            chatSendButton.setBackgroundResource(R.drawable.send_button_circle);
            chatSendButton.setImageResource(R.drawable.ic_arrow_up);
        }
    }
    
    private void sendMessage() {
        
        sendChatMessage();
    }
    
    private void sendMessageOld() {
        
        if (isGenerating) {
            stopGeneration();
            return;
        }
        
        String message = chatInputEditText.getText().toString().trim();
        
        if (message.isEmpty()) {
            safeShowToast("请输入问题");
            return;
        }
        
        if (isFirstMessage) {
            switchToChatView();
            isFirstMessage = false;
        }
        
        currentQuestion = message;
        currentAnswer = "";
        currentThinkingContent = "";
        
        addUserMessage(message);
        
        chatInputEditText.setText("");
        
        sendQuestionToServer(message);
    }
    
    private void sendChatMessage() {
        if (isGenerating) {
            stopGeneration();
            return;
        }
        
        String message = chatInputEditText.getText().toString().trim();
        if (message.isEmpty()) {
            safeShowToast("请输入问题");
            return;
        }
        
        // 如果有待删除的索引，先删除该消息及之后的所有消息
        if (pendingDeleteIndex >= 0) {
            Log.d("ChatFragment", "发送前删除索引 " + pendingDeleteIndex + " 及之后的消息");
            
            // 删除UI中的消息
            int childCount = chatMessagesContainer.getChildCount();
            for (int i = childCount - 1; i >= pendingDeleteIndex; i--) {
                chatMessagesContainer.removeViewAt(i);
            }
            
            // 重建对话历史（只保留该消息之前的）
            // 每个用户消息对应一个AI回答，所以索引/2就是对话轮数
            int conversationIndex = pendingDeleteIndex / 2;
            if (conversationIndex < conversationHistory.size()) {
                // 保留该索引之前的对话历史
                List<JSONObject> newHistory = new ArrayList<>();
                for (int i = 0; i < conversationIndex * 2 && i < conversationHistory.size(); i++) {
                    newHistory.add(conversationHistory.get(i));
                }
                conversationHistory = newHistory;
            }
            
            // 清除待删除标记
            pendingDeleteIndex = -1;
        }
        
        if (isFirstMessage) {
            switchToChatView();
            isFirstMessage = false;
        }
        
        currentQuestion = message;
        currentAnswer = "";
        currentThinkingContent = "";
        
        // 实时TTS：如果开启自动播报且为藏文模式，启动新会话
        if (isAutoPlayEnabled && LanguageManager.LANGUAGE_TIBETAN.equals(LanguageManager.getInstance(getContext()).getLanguage())) {
            // 停止之前的会话（如果有）
            if (realtimeTtsManager.isActive()) {
                realtimeTtsManager.stop();
            }
            // 启动新会话
            realtimeTtsManager.startSession();
        }
        
        addUserMessage(message);
        
        chatInputEditText.setText("");
        
        sendQuestionToServer(message);
    }
    
    private void switchToChatView() {if (centerLayout != null) {
            centerLayout.setVisibility(View.GONE);}
        if (titleLayout != null) {
            titleLayout.setVisibility(View.GONE);}
        
        if (getView() != null) {
            View greetingText = getView().findViewById(R.id.greetingText);
            if (greetingText != null) {
                greetingText.setVisibility(View.GONE);} else {}
        } else {}
        
        if (chatGreetingLayout != null) {
            chatGreetingLayout.setVisibility(View.GONE);}
        
        if (modelSelectorText != null) {
            
            modelSelectorText.setGravity(android.view.Gravity.CENTER);
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) modelSelectorText.getLayoutParams();
            params.setMarginStart((int) (8 * getResources().getDisplayMetrics().density)); 
            params.setMarginEnd((int) (78 * getResources().getDisplayMetrics().density)); 
            modelSelectorText.setLayoutParams(params);
            
            int paddingHorizontal = (int) (8 * getResources().getDisplayMetrics().density);
            int paddingVertical = (int) (12 * getResources().getDisplayMetrics().density);
            modelSelectorText.setPadding(paddingHorizontal, paddingVertical, paddingHorizontal, paddingVertical);
        }
        
        chatScrollView.setVisibility(View.VISIBLE);
        chatInputContainer.setVisibility(View.VISIBLE);
        backButton.setVisibility(View.VISIBLE);if (languageSwitchButton != null) {
            languageSwitchButton.setVisibility(View.GONE);}}
    
    private void returnToSearchView() {
        
        chatMessagesContainer.removeAllViews();
        
        clearConversationHistory();
        
        chatScrollView.setVisibility(View.GONE);
        
        chatInputContainer.setVisibility(View.VISIBLE);
        backButton.setVisibility(View.GONE);
        
        if (modelSelectorText != null) {
            
            modelSelectorText.setGravity(android.view.Gravity.CENTER);
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) modelSelectorText.getLayoutParams();
            params.setMarginStart((int) (8 * getResources().getDisplayMetrics().density)); 
            params.setMarginEnd(0); 
            modelSelectorText.setLayoutParams(params);
            
            int padding = (int) (8 * getResources().getDisplayMetrics().density);
            modelSelectorText.setPadding(padding, padding, padding, padding);
        }
        
        // 获取当前语言
        String language = LanguageManager.getInstance(getContext()).getLanguage();
        
        // 英文模式：直接显示问候页（不显示主页）
        if (LanguageManager.LANGUAGE_ENGLISH.equals(language)) {
            if (centerLayout != null) {
                centerLayout.setVisibility(View.GONE);  // 英文模式不显示主页
            }
            if (titleLayout != null) {
                titleLayout.setVisibility(View.GONE);
            }
            
            View greetingText = getView().findViewById(R.id.greetingText);
            if (greetingText != null && greetingText instanceof TextView) {
                ((TextView) greetingText).setText("Hi, where to start today?");
                greetingText.setVisibility(View.VISIBLE);
            }
            
            if (chatGreetingLayout != null) {
                chatGreetingLayout.setVisibility(View.GONE);
            }
        } else {
            // 藏文和中文：显示主页
            if (centerLayout != null) {
                centerLayout.setVisibility(View.VISIBLE);
            }
            if (titleLayout != null) {
                titleLayout.setVisibility(View.VISIBLE);
            }
            
            View greetingText = getView().findViewById(R.id.greetingText);
            if (greetingText != null && greetingText instanceof TextView) {
                String greetingTextContent;
                switch (language) {
                    case LanguageManager.LANGUAGE_CHINESE:
                        greetingTextContent = "嗨，今天从哪里开始呢？";
                        break;
                    case LanguageManager.LANGUAGE_TIBETAN:
                    default:
                        greetingTextContent = "གྲོགས་པོ། དེ་རིང་གང་ནས་འགོ་རྩོམ་རྒྱུ་ཡིན།";
                        break;
                }
                ((TextView) greetingText).setText(greetingTextContent);
                greetingText.setVisibility(View.VISIBLE);
            }
            
            if (chatGreetingLayout != null) {
                chatGreetingLayout.setVisibility(View.GONE);
            }
        }
        
        if (chatIconButton != null) {
            chatIconButton.setVisibility(View.VISIBLE);
        }
        
        if (languageSwitchButton != null) {
            languageSwitchButton.setVisibility(View.VISIBLE);
        }
        
        isFirstMessage = true;
    }
    
    private void initHistoryManager() {
        historyManager = ChatHistoryManager.getInstance(getContext());
        conversationHistory = new ArrayList<>();
    }
    
    private void addUserMessage(String message) {
        
        LinearLayout messageContainer = new LinearLayout(getContext());
        messageContainer.setOrientation(LinearLayout.HORIZONTAL);
        messageContainer.setGravity(android.view.Gravity.END);
        
        LinearLayout.LayoutParams containerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        containerParams.setMargins(0, 0, 0, 16);
        messageContainer.setLayoutParams(containerParams);
        
        // 创建垂直布局容器，包含消息和操作按钮
        LinearLayout verticalContainer = new LinearLayout(getContext());
        verticalContainer.setOrientation(LinearLayout.VERTICAL);
        verticalContainer.setGravity(android.view.Gravity.END);
        
        TextView messageView = new TextView(getContext());
        messageView.setText(message);
        messageView.setTextSize(16);
        messageView.setTextColor(getThemeColor(android.R.attr.textColorPrimary));
        messageView.setPadding(32, 32, 32, 32);
        messageView.setBackgroundColor(getUserMessageBackgroundColor());
        
        TibetanFontHelper.applyTibetanFont(getContext(), messageView);
        
        LinearLayout.LayoutParams messageParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        messageParams.setMargins(0, 0, 0, 8);  // 底部留出空间给按钮
        messageView.setLayoutParams(messageParams);
        messageView.setMaxWidth((int) (getResources().getDisplayMetrics().widthPixels - 80 * getResources().getDisplayMetrics().density));
        
        // 添加用户消息操作按钮
        LinearLayout userButtonContainer = new LinearLayout(getContext());
        userButtonContainer.setOrientation(LinearLayout.HORIZONTAL);
        userButtonContainer.setGravity(android.view.Gravity.END);
        LinearLayout.LayoutParams buttonContainerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        buttonContainerParams.setMargins(0, 8, 0, 0);
        userButtonContainer.setLayoutParams(buttonContainerParams);
        
        int buttonSize = (int) (40 * getResources().getDisplayMetrics().density);  // 统一按钮尺寸为40dp
        int iconSize = (int) (24 * getResources().getDisplayMetrics().density);    // 图标尺寸
        int buttonMargin = (int) (8 * getResources().getDisplayMetrics().density);
        
        // 修改按钮 - 样式与回答下方按钮一致
        ImageButton editButton = new ImageButton(getContext());
        editButton.setImageResource(R.drawable.ic_edit);
        editButton.setBackground(null);  // 无背景，透明
        editButton.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        LinearLayout.LayoutParams editParams = new LinearLayout.LayoutParams(buttonSize, buttonSize);
        editParams.setMargins(0, 0, buttonMargin, 0);
        editButton.setLayoutParams(editParams);
        editButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 获取消息在列表中的索引
                int messageIndex = chatMessagesContainer.indexOfChild(messageContainer);
                Log.d("ChatFragment", "点击修改按钮，消息索引: " + messageIndex);
                resendFromMessage(messageIndex, message);
            }
        });
        
        // 复制按钮 - 样式与回答下方按钮一致
        ImageButton copyButton = new ImageButton(getContext());
        copyButton.setImageResource(R.drawable.ic_copy);
        copyButton.setBackground(null);  // 无背景，透明
        copyButton.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        LinearLayout.LayoutParams copyParams = new LinearLayout.LayoutParams(buttonSize, buttonSize);
        copyButton.setLayoutParams(copyParams);
        copyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("ChatFragment", "点击复制按钮");
                android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                android.content.ClipData clip = android.content.ClipData.newPlainText("问题", message);
                clipboard.setPrimaryClip(clip);
                safeShowToast("已复制到剪贴板");
            }
        });
        
        userButtonContainer.addView(editButton);
        userButtonContainer.addView(copyButton);
        
        verticalContainer.addView(messageView);
        verticalContainer.addView(userButtonContainer);
        
        TextView avatarView = new TextView(getContext());
        avatarView.setText("U");
        avatarView.setTextSize(18);
        avatarView.setTextColor(0xFFFFFFFF); 
        avatarView.setGravity(android.view.Gravity.CENTER);
        avatarView.setBackgroundResource(R.drawable.user_avatar_background);
        
        LinearLayout.LayoutParams avatarParams = new LinearLayout.LayoutParams(
                (int) (40 * getResources().getDisplayMetrics().density),
                (int) (40 * getResources().getDisplayMetrics().density)
        );
        avatarParams.setMargins(8, 0, 0, 0);
        avatarView.setLayoutParams(avatarParams);
        
        messageContainer.addView(verticalContainer);
        messageContainer.addView(avatarView);
        
        chatMessagesContainer.addView(messageContainer);
        scrollToBottom();
    }
    
    private void addBotMessage(String message) {
        
        TextView messageView = new TextView(getContext());
        
        markwon.setMarkdown(messageView, message);
        messageView.setTextSize(16);
        messageView.setTextColor(getThemeColor(android.R.attr.textColorPrimary));
        messageView.setPadding(48, 48, 48, 48);
        messageView.setBackgroundColor(getAnswerBackgroundColor());
        
        TibetanFontHelper.applyTibetanFont(getContext(), messageView);
        
        LinearLayout.LayoutParams messageParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        messageParams.setMargins(0, 0, 0, 16);
        messageView.setLayoutParams(messageParams);
        
        chatMessagesContainer.addView(messageView);
        
        addActionButtonsForMessage(messageView, message);
        
        scrollToBottom();
    }
    
    private void scrollToBottom() {
        chatScrollView.post(new Runnable() {
            @Override
            public void run() {
                chatScrollView.fullScroll(View.FOCUS_DOWN);
            }
        });
    }
    
    private void sendQuestionToServer(String question) {
        setLoadingState(true);JSONObject requestJson = new JSONObject();
        try {
            
            JSONArray messages = new JSONArray();
            
            // 第一条消息必须是 system prompt
            JSONObject systemMessage = new JSONObject();
            systemMessage.put("role", "system");
            systemMessage.put("content", SYSTEM_PROMPT);
            messages.put(systemMessage);
            
            // 然后添加对话历史
            for (JSONObject historyMessage : conversationHistory) {
                messages.put(historyMessage);
            }
            
            // 最后添加当前用户消息
            JSONObject userMessage = new JSONObject();
            userMessage.put("role", "user");
            userMessage.put("content", question);
            messages.put(userMessage);
            
            conversationHistory.add(userMessage);
            
            requestJson.put("model", "glm-4.6");
            requestJson.put("messages", messages);
            requestJson.put("do_sample", true);  
            requestJson.put("max_tokens", 65536);  
            requestJson.put("temperature", 0.6);  
            requestJson.put("top_p", 0.95);  
            requestJson.put("stream", true);  
            
            requestJson.put("enable_thinking", true);android.util.Log.d("ChatFragment", "Content-Type: application/json; charset=utf-8");String jsonStr = requestJson.toString();} catch (JSONException e) {
            e.printStackTrace();setLoadingState(false);
            if (getActivity() != null) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        safeShowToast("请求参数错误");
                    }
                });
            }
            return;
        }
        
        String jsonString = requestJson.toString();
        byte[] jsonBytes = jsonString.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        RequestBody body = RequestBody.create(jsonBytes, JSON);
        
        Request request = new Request.Builder()
                .url(SERVER_URL)
                .post(body)
                .addHeader("Content-Type", "application/json; charset=utf-8")
                .addHeader("Accept", "text/event-stream")  
                .build();client.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final int code = response.code();if (code != 200) {
                    final String errorBody = response.body().string();if (getActivity() != null) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                setLoadingState(false);
                                
                                String errorMsg = "❌ ཞབས་ཞུ་འཕྲུལ་ཆས་ནོར་འཁྲུལ།\n\n";
                                errorMsg += "ཞབས་ཞུ་འཕྲུལ་ཆས་ལ་དཀའ་ངལ་ཞིག་བྱུང་འདུག་པས་ཡང་བསྐྱར་ཚོད་ལྟ་བྱེད་རོགས།";
                                addBotMessage(errorMsg);
                            }
                        });
                    }
                    return;
                }
                
                if (getActivity() != null) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            handleStreamingResponse(response);
                        }
                    });
                }
            }
            
            @Override
            public void onFailure(Call call, IOException e) {if (getActivity() != null) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            setLoadingState(false);
                            
                            String errorMsg = "❌ དྲ་བའི་ཞུ་བ་ཕམ་པ།\n\n";
                            errorMsg += "རྒྱུ་མཚན་སྲིད་པ།\n";
                            errorMsg += "• དྲ་བའི་འབྲེལ་མཐུད་ཆད་པ།\n";
                            errorMsg += "• ཞབས་ཞུ་འཕྲུལ་ཆས་ལན་མ་སྤྲད་པ།\n";
                            errorMsg += "• ཞུ་བའི་དུས་ཚོད་ཡོལ་བ།\n\n";
                            errorMsg += "དྲ་བའི་འབྲེལ་མཐུད་ཞིབ་བཤེར་བྱས་རྗེས་ཡང་བསྐྱར་ཚོད་ལྟ་བྱེད་རོགས།";
                            addBotMessage(errorMsg);
                        }
                    });
                }
            }
        });
    }
    
    private TextView currentStreamingTextView;  
    private TextView currentThinkingTextView;   
    private LinearLayout currentThinkingContainer; 
    private boolean isThinkingPhase = false;    
    private boolean thinkingCompleted = false;  
    
    private void handleStreamingResponse(Response response) {
        isGenerating = true;
        
        if (getActivity() != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    setLoadingState(false);
                    updateSendButtonAppearance();
                    updateChatSendButtonAppearance();
                    
                    createStreamingTextView();
                }
            });
        }
        
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    okio.BufferedSource source = response.body().source();
                    StringBuilder fullContent = new StringBuilder();
                    int chunkCount = 0;while (!source.exhausted() && isGenerating) {
                        String line;
                        try {
                            line = source.readUtf8Line();
                        } catch (IOException e) {break;
                        }
                        
                        if (line == null) {break;
                        }
                        if (line.trim().isEmpty()) {continue;
                        }
                        
                        chunkCount++;if (line.startsWith("data: ")) {
                            String jsonData = line.substring(6).trim();
                            
                            if (jsonData.equals("[DONE]")) {if (isThinkingPhase && !thinkingCompleted) {thinkingCompleted = true;
                                    if (getActivity() != null) {
                                        getActivity().runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                createAnswerArea();
                                            }
                                        });
                                    }
                                }
                                break;
                            }
                            
                            try {
                                JSONObject chunk = new JSONObject(jsonData);
                                
                                if (chunk.has("choices")) {
                                    JSONArray choices = chunk.getJSONArray("choices");
                                    if (choices.length() > 0) {
                                        JSONObject firstChoice = choices.getJSONObject(0);
                                        
                                        if (firstChoice.has("delta")) {
                                            JSONObject delta = firstChoice.getJSONObject("delta");final boolean isThinkingEnabled;
                                            if (thinkingToggle != null) {
                                                isThinkingEnabled = thinkingToggle.isChecked();
                                            } else if (deepThinkingSwitch != null) {
                                                isThinkingEnabled = deepThinkingSwitch.isChecked();
                                            } else {
                                                isThinkingEnabled = false;
                                            }
                                            
                                            if (isThinkingEnabled && delta.has("reasoning_content")) {
                                                final String reasoning = delta.getString("reasoning_content");if (!reasoning.isEmpty()) {
                                                    
                                                    if (getActivity() != null) {
                                                        getActivity().runOnUiThread(new Runnable() {
                                                            @Override
                                                            public void run() {
                                                                appendToThinkingTextView(reasoning);
                                                            }
                                                        });
                                                    }
                                                }
                                            } else if (!isThinkingEnabled && delta.has("reasoning_content")) {}
                                            
                                            if (delta.has("content")) {
                                                final String content = delta.getString("content");
                                                
                                                // 实时TTS：如果开启自动播报且为藏文模式，推送增量文本
                                                if (isAutoPlayEnabled && LanguageManager.LANGUAGE_TIBETAN.equals(LanguageManager.getInstance(getContext()).getLanguage())) {
                                                    if (!content.isEmpty()) {
                                                        realtimeTtsManager.pushLLMDelta(content);
                                                    }
                                                }
                                                
                                                if (isThinkingPhase && !thinkingCompleted && !content.isEmpty()) {thinkingCompleted = true;
                                                    
                                                    if (getActivity() != null) {
                                                        getActivity().runOnUiThread(new Runnable() {
                                                            @Override
                                                            public void run() {
                                                                if (isThinkingEnabled) {
                                                                    
                                                                    createAnswerArea();
                                                                } else {
                                                                    
                                                                    if (currentStreamingTextView != null) {
                                                                        streamingTextBuffer.setLength(0);
                                                                        currentStreamingTextView.setText("");
                                                                    }
                                                                }
                                                            }
                                                        });
                                                    }
                                                }
                                                
                                                if (thinkingCompleted) {
                                                    fullContent.append(content);
                                                    
                                                    if (getActivity() != null) {
                                                        getActivity().runOnUiThread(new Runnable() {
                                                            @Override
                                                            public void run() {
                                                                
                                                                if (currentStreamingTextView == null) {
                                                                    createAnswerArea();
                                                                }
                                                                
                                                                appendToStreamingTextView(content);
                                                            }
                                                        });
                                                    }
                                                } else {}
                                            }
                                        }
                                        
                                        if (firstChoice.has("finish_reason") && !firstChoice.isNull("finish_reason")) {
                                            String finishReason = firstChoice.getString("finish_reason");if (finishReason.equals("length")) {} else if (finishReason.equals("stop")) {}
                                        }
                                        
                                        if (chunk.has("usage")) {
                                            JSONObject usage = chunk.getJSONObject("usage");}
                                    }
                                }
                            } catch (JSONException e) {}
                        }
                    }if (getActivity() != null) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                
                                flushPendingUIUpdates();
                                
                                // 实时TTS：LLM生成完成，通知TTS结束
                                if (isAutoPlayEnabled && LanguageManager.LANGUAGE_TIBETAN.equals(LanguageManager.getInstance(getContext()).getLanguage())) {
                                    if (realtimeTtsManager.isActive()) {
                                        realtimeTtsManager.finishLLM();
                                    }
                                }
                                
                                isGenerating = false;
                                updateSendButtonAppearance();
                                updateChatSendButtonAppearance();
                                scrollToBottom();
                                
                                saveChatToHistory();
                                
                                addActionButtons();
                            }
                        });
                    }
                    
                } catch (IOException e) {if (getActivity() != null) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                isGenerating = false;
                                updateSendButtonAppearance();
                                updateChatSendButtonAppearance();
                                
                                if (currentStreamingTextView != null && currentStreamingTextView.getText().toString().isEmpty()) {
                                    
                                    currentStreamingTextView.setText("❌ ལན་འདེབས་ཀློག་པ་ཕམ་པ།");
                                } else {
                                    
                                    appendToStreamingTextView("\n\n❌ [ལན་འདེབས་ཆད་པ།]");
                                }
                            }
                        });
                    }
                } finally {
                    response.close();
                }
            }
        }).start();
    }
    
    private void createStreamingTextView() {
        
        isThinkingPhase = true;  
        thinkingCompleted = false;
        streamingTextBuffer.setLength(0); 
        thinkingTextBuffer.setLength(0);  
        
        boolean enableThinking = false;
        if (thinkingToggle != null) {
            enableThinking = thinkingToggle.isChecked();
        } else if (deepThinkingSwitch != null) {
            enableThinking = deepThinkingSwitch.isChecked();
        }
        
        if (enableThinking) {
            createThinkingArea();
            
            return;
        }
        
        currentStreamingTextView = new TextView(getContext());
        
        // 根据当前语言设置Loading文本
        String language = getArguments() != null ? getArguments().getString("language") : LanguageManager.LANGUAGE_TIBETAN;
        if (language == null) {
            language = LanguageManager.getInstance(getContext()).getLanguage();
        }
        
        String loadingText;
        switch (language) {
            case LanguageManager.LANGUAGE_CHINESE:
                loadingText = "加载中...";
                break;
            case LanguageManager.LANGUAGE_ENGLISH:
                loadingText = "Loading...";
                break;
            case LanguageManager.LANGUAGE_TIBETAN:
            default:
                loadingText = "ནང་འཇུག་བྱེད་བཞིན་ཡོད།...";
                break;
        }
        
        currentStreamingTextView.setText(loadingText);
        currentStreamingTextView.setTextSize(16);
        currentStreamingTextView.setTextColor(getThemeColor(android.R.attr.textColorPrimary));
        currentStreamingTextView.setPadding(48, 48, 48, 48);
        currentStreamingTextView.setBackgroundColor(getAnswerBackgroundColor());
        
        TibetanFontHelper.applyTibetanFont(getContext(), currentStreamingTextView);
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 16);
        currentStreamingTextView.setLayoutParams(params);
        
        chatMessagesContainer.addView(currentStreamingTextView);
        scrollToBottom();
    }
    
    private void createThinkingArea() {
        
        currentThinkingContainer = new LinearLayout(getContext());
        currentThinkingContainer.setOrientation(LinearLayout.VERTICAL);
        
        LinearLayout.LayoutParams containerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        containerParams.setMargins(0, 0, 0, 16);
        currentThinkingContainer.setLayoutParams(containerParams);
        
        LinearLayout headerLayout = new LinearLayout(getContext());
        headerLayout.setOrientation(LinearLayout.HORIZONTAL);
        headerLayout.setGravity(android.view.Gravity.CENTER_VERTICAL);
        headerLayout.setPadding(32, 16, 32, 16);
        headerLayout.setBackgroundColor(getThinkingBackgroundColor());
        
        ImageView avatarView = new ImageView(getContext());
        avatarView.setImageResource(R.drawable.logo);
        avatarView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        
        LinearLayout.LayoutParams avatarParams = new LinearLayout.LayoutParams(
                (int) (24 * getResources().getDisplayMetrics().density),
                (int) (24 * getResources().getDisplayMetrics().density)
        );
        avatarParams.setMargins(0, 0, 8, 0);
        avatarView.setLayoutParams(avatarParams);
        
        avatarView.setClipToOutline(true);
        avatarView.setOutlineProvider(new android.view.ViewOutlineProvider() {
            @Override
            public void getOutline(android.view.View view, android.graphics.Outline outline) {
                outline.setOval(0, 0, view.getWidth(), view.getHeight());
            }
        });String language = getArguments() != null ? getArguments().getString("language") : LanguageManager.LANGUAGE_TIBETAN;
        if (language == null) {
            language = LanguageManager.getInstance(getContext()).getLanguage();
        }
        
        String titleTextStr;
        switch (language) {
            case LanguageManager.LANGUAGE_CHINESE:
                titleTextStr = "思考过程";
                break;
            case LanguageManager.LANGUAGE_ENGLISH:
                titleTextStr = "Thinking Process";
                break;
            case LanguageManager.LANGUAGE_TIBETAN:
            default:
                titleTextStr = "བསམ་བློ་གཏོང་རིམ།";
                break;
        }
        
        TextView titleText = new TextView(getContext());
        titleText.setText(titleTextStr);
        titleText.setTextSize(14);
        titleText.setTextColor(0xFF2E7D32);
        titleText.setTypeface(null, android.graphics.Typeface.BOLD);
        
        ImageButton toggleButton = new ImageButton(getContext());
        toggleButton.setImageResource(android.R.drawable.arrow_up_float);
        toggleButton.setBackgroundResource(android.R.color.transparent);
        toggleButton.setScaleType(ImageButton.ScaleType.CENTER);
        LinearLayout.LayoutParams toggleParams = new LinearLayout.LayoutParams(48, 48);
        toggleParams.setMargins(16, 0, 0, 0);
        toggleButton.setLayoutParams(toggleParams);
        
        headerLayout.addView(avatarView);
        headerLayout.addView(titleText);
        headerLayout.addView(toggleButton);
        
        currentThinkingTextView = new TextView(getContext());
        currentThinkingTextView.setText("");
        currentThinkingTextView.setTextSize(14);
        currentThinkingTextView.setTextColor(getThemeColor(android.R.attr.textColorPrimary));
        currentThinkingTextView.setPadding(32, 16, 32, 24);
        currentThinkingTextView.setBackgroundColor(getThinkingBackgroundColor());
        currentThinkingTextView.setVisibility(View.VISIBLE);
        
        final boolean[] isExpanded = {true};
        headerLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isExpanded[0]) {
                    currentThinkingTextView.setVisibility(View.GONE);
                    toggleButton.setImageResource(android.R.drawable.arrow_down_float);
                    isExpanded[0] = false;
                } else {
                    currentThinkingTextView.setVisibility(View.VISIBLE);
                    toggleButton.setImageResource(android.R.drawable.arrow_up_float);
                    isExpanded[0] = true;
                }
            }
        });
        
        currentThinkingContainer.addView(headerLayout);
        currentThinkingContainer.addView(currentThinkingTextView);
        
        chatMessagesContainer.addView(currentThinkingContainer);
        scrollToBottom();
    }
    
    private void createAnswerArea() {
        
        streamingTextBuffer.setLength(0);
        
        currentStreamingTextView = new TextView(getContext());
        currentStreamingTextView.setText("");
        currentStreamingTextView.setTextSize(16);
        currentStreamingTextView.setTextColor(getThemeColor(android.R.attr.textColorPrimary));
        currentStreamingTextView.setPadding(48, 48, 48, 48);
        currentStreamingTextView.setBackgroundColor(getAnswerBackgroundColor());
        
        TibetanFontHelper.applyTibetanFont(getContext(), currentStreamingTextView);
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 16);
        currentStreamingTextView.setLayoutParams(params);
        
        chatMessagesContainer.addView(currentStreamingTextView);
        scrollToBottom();
    }
    
    private void appendToStreamingTextView(String content) {
        if (currentStreamingTextView != null) {
            
            streamingTextBuffer.append(content);
            
            scheduleUIUpdate(currentStreamingTextView, streamingTextBuffer.toString());
        }
    }
    
    private void scheduleUIUpdate(final TextView textView, final String text) {
        if (uiUpdateHandler == null) {
            uiUpdateHandler = new Handler(Looper.getMainLooper());
        }
        
        long currentTime = System.currentTimeMillis();
        long timeSinceLastUpdate = currentTime - lastUIUpdateTime;
        
        if (timeSinceLastUpdate >= UI_UPDATE_THROTTLE_MS) {
            updateUI(textView, text);
            lastUIUpdateTime = currentTime;
            hasPendingUpdate = false;
        } else {
            
            if (!hasPendingUpdate) {
                hasPendingUpdate = true;
                long delay = UI_UPDATE_THROTTLE_MS - timeSinceLastUpdate;
                
                pendingUIUpdate = new Runnable() {
                    @Override
                    public void run() {
                        updateUI(textView, text);
                        lastUIUpdateTime = System.currentTimeMillis();
                        hasPendingUpdate = false;
                    }
                };
                
                uiUpdateHandler.postDelayed(pendingUIUpdate, delay);
            }
        }
    }
    
    private void updateUI(TextView textView, String text) {
        if (textView != null && getActivity() != null) {
            markwon.setMarkdown(textView, text);
            scrollToBottom();
        }
    }
    
    private void appendToThinkingTextView(String thinking) {
        if (currentThinkingTextView != null) {
            
            thinkingTextBuffer.append(thinking);
            
            scheduleUIUpdate(currentThinkingTextView, thinkingTextBuffer.toString());
        }
    }
    
    private void stopGeneration() {isGenerating = false;
        isThinkingPhase = false;
        thinkingCompleted = false;
        
        // 停止实时TTS
        if (realtimeTtsManager != null && realtimeTtsManager.isActive()) {
            realtimeTtsManager.stop();
        }
        
        if (streamHandler != null) {
            streamHandler.removeCallbacksAndMessages(null);
        }
        
        flushPendingUIUpdates();
        
        setLoadingState(false);
        updateSendButtonAppearance();
        updateChatSendButtonAppearance();
        
        safeShowToast("已停止生成");
    
    }
    
    private void flushPendingUIUpdates() {
        if (uiUpdateHandler != null && hasPendingUpdate) {
            uiUpdateHandler.removeCallbacks(pendingUIUpdate);
            
            if (currentStreamingTextView != null) {
                updateUI(currentStreamingTextView, streamingTextBuffer.toString());
            }
            if (currentThinkingTextView != null) {
                updateUI(currentThinkingTextView, thinkingTextBuffer.toString());
            }
            
            hasPendingUpdate = false;
        }
    }
    
    private void handleResponse(String responseBody) {
        setLoadingState(false);
        isGenerating = true;
        updateSendButtonAppearance();
        updateChatSendButtonAppearance();
        
        String answer = DEFAULT_ANSWER;
        
        if (responseBody != null && !responseBody.isEmpty()) {try {
                JSONObject jsonResponse = new JSONObject(responseBody);String objectType = jsonResponse.optString("object", "");if (jsonResponse.has("choices")) {
                    JSONArray choices = jsonResponse.getJSONArray("choices");if (choices.length() > 0) {
                        JSONObject firstChoice = choices.getJSONObject(0);if (objectType.equals("chat.completion")) {
                            
                            if (firstChoice.has("message")) {
                                JSONObject message = firstChoice.getJSONObject("message");if (message.has("content")) {
                                    answer = message.getString("content");} else {answer = "错误：响应中没有content字段";
                                }
                            } else {answer = "错误：响应中没有message字段";
                            }
                        } else if (objectType.equals("chat.completion.chunk")) {
                            
                            if (firstChoice.has("delta")) {
                                JSONObject delta = firstChoice.getJSONObject("delta");if (delta.has("content")) {
                                    answer = delta.getString("content");} else {answer = "";
                                }
                            } else {answer = "";
                            }
                        } else {if (firstChoice.has("message")) {
                                JSONObject message = firstChoice.getJSONObject("message");
                                if (message.has("content")) {
                                    answer = message.getString("content");
                                }
                            } else if (firstChoice.has("delta")) {
                                JSONObject delta = firstChoice.getJSONObject("delta");
                                if (delta.has("content")) {
                                    answer = delta.getString("content");
                                }
                            }
                        }
                        
                        if (firstChoice.has("finish_reason") && !firstChoice.isNull("finish_reason")) {
                            String finishReason = firstChoice.getString("finish_reason");if (finishReason.equals("length")) {}
                        }
                        
                        if (jsonResponse.has("usage")) {
                            JSONObject usage = jsonResponse.getJSONObject("usage");}
                        
                        if (jsonResponse.has("id")) {}
                        
                        if (jsonResponse.has("model")) {}
                        
                    } else {answer = "错误：服务器返回空的choices数组";
                    }
                } else {answer = "错误：响应格式不正确，缺少choices字段";
                }} catch (JSONException e) {
                e.printStackTrace();answer = "解析响应失败";
            }
        }
        
        final String finalAnswer = answer;
        
        boolean needThinking = false;
        if (thinkingToggle != null) {
            needThinking = thinkingToggle.isChecked();
        } else if (deepThinkingSwitch != null) {
            needThinking = deepThinkingSwitch.isChecked();
        }if (needThinking) {
            
            addThinkingProcess(DEFAULT_THINKING, new Runnable() {
                @Override
                public void run() {
                    if (isGenerating) {
                        
                        addBotMessageStreaming(finalAnswer);
                    }
                }
            });
        } else {
            
            addBotMessageStreaming(finalAnswer);
        }
    }
    
    private void setLoadingState(boolean isLoading) {
        if (isLoading) {
            progressBar.setVisibility(View.VISIBLE);
            
            chatSendButton.setEnabled(false);
        } else {
            progressBar.setVisibility(View.GONE);
            
            chatSendButton.setEnabled(true);
        }
    }
    
    private void addThinkingProcess(final String thinkingText, final Runnable onComplete) {
        LinearLayout thinkingContainer = new LinearLayout(getContext());
        thinkingContainer.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams containerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        containerParams.setMargins(0, 0, 0, 24);
        thinkingContainer.setLayoutParams(containerParams);
        
        LinearLayout headerLayout = new LinearLayout(getContext());
        headerLayout.setOrientation(LinearLayout.HORIZONTAL);
        headerLayout.setPadding(32, 24, 32, 24);
        headerLayout.setBackgroundColor(getThinkingBackgroundColor());
        
        TextView headerText = new TextView(getContext());
        headerText.setText("🤔 བསམ་བློ་གཏོང་རིམ།");
        headerText.setTextSize(14);
        headerText.setTextColor(0xFF666666);
        headerText.setTypeface(null, android.graphics.Typeface.BOLD);
        
        LinearLayout.LayoutParams headerTextParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1
        );
        headerText.setLayoutParams(headerTextParams);
        
        ImageButton toggleButton = new ImageButton(getContext());
        toggleButton.setImageResource(android.R.drawable.arrow_down_float);
        toggleButton.setBackground(null);
        LinearLayout.LayoutParams toggleParams = new LinearLayout.LayoutParams(
                32,
                32
        );
        toggleButton.setLayoutParams(toggleParams);
        toggleButton.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);
        
        headerLayout.addView(headerText);
        headerLayout.addView(toggleButton);
        
        TextView thinkingContent = new TextView(getContext());
        thinkingContent.setTextSize(14);
        thinkingContent.setTextColor(getThemeColor(android.R.attr.textColorPrimary));
        thinkingContent.setPadding(32, 16, 32, 24);
        thinkingContent.setBackgroundColor(getThinkingBackgroundColor());
        thinkingContent.setVisibility(View.GONE);
        
        final boolean[] isExpanded = {false};
        headerLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isExpanded[0]) {
                    thinkingContent.setVisibility(View.GONE);
                    toggleButton.setImageResource(android.R.drawable.arrow_down_float);
                    isExpanded[0] = false;
                } else {
                    thinkingContent.setVisibility(View.VISIBLE);
                    toggleButton.setImageResource(android.R.drawable.arrow_up_float);
                    isExpanded[0] = true;
                }
            }
        });
        
        thinkingContainer.addView(headerLayout);
        thinkingContainer.addView(thinkingContent);
        chatMessagesContainer.addView(thinkingContainer);
        
        streamText(thinkingContent, thinkingText, 30, onComplete);
    }
    
    private void addBotMessageStreaming(final String message) {
        
        LinearLayout messageContainer = new LinearLayout(getContext());
        messageContainer.setOrientation(LinearLayout.HORIZONTAL);
        messageContainer.setGravity(android.view.Gravity.START);
        
        LinearLayout.LayoutParams containerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        containerParams.setMargins(0, 0, 0, 16);
        messageContainer.setLayoutParams(containerParams);
        
        ImageView avatarView = new ImageView(getContext());
        avatarView.setImageResource(R.drawable.logo);
        avatarView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        
        LinearLayout.LayoutParams avatarParams = new LinearLayout.LayoutParams(
                (int) (40 * getResources().getDisplayMetrics().density),
                (int) (40 * getResources().getDisplayMetrics().density)
        );
        avatarParams.setMargins(0, 0, 8, 0);
        avatarView.setLayoutParams(avatarParams);
        
        avatarView.setClipToOutline(true);
        avatarView.setOutlineProvider(new android.view.ViewOutlineProvider() {
            @Override
            public void getOutline(android.view.View view, android.graphics.Outline outline) {
                outline.setOval(0, 0, view.getWidth(), view.getHeight());
            }
        });TextView messageView = new TextView(getContext());
        messageView.setTextSize(16);
        messageView.setTextColor(getThemeColor(android.R.attr.textColorPrimary));
        messageView.setPadding(32, 32, 32, 32);
        messageView.setBackgroundColor(getAnswerBackgroundColor());
        
        LinearLayout.LayoutParams messageParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        messageView.setLayoutParams(messageParams);
        messageView.setMaxWidth((int) (getResources().getDisplayMetrics().widthPixels - 80 * getResources().getDisplayMetrics().density));
        
        messageContainer.addView(avatarView);
        messageContainer.addView(messageView);
        
        chatMessagesContainer.addView(messageContainer);
        scrollToBottom();
        
        streamText(messageView, message, 50, null);
    }
    
    private void streamText(final TextView textView, final String fullText, final long delayMillis, final Runnable onComplete) {
        if (streamHandler == null) {
            streamHandler = new Handler(Looper.getMainLooper());
        }
        final int[] currentIndex = {0};
        
        streamHandler.post(new Runnable() {
            @Override
            public void run() {
                if (isGenerating && currentIndex[0] <= fullText.length()) {
                    textView.setText(fullText.substring(0, currentIndex[0]));
                    scrollToBottom();
                    currentIndex[0]++;
                    streamHandler.postDelayed(this, delayMillis);
                } else if (currentIndex[0] > fullText.length()) {
                    
                    if (onComplete != null) {
                        
                        onComplete.run();
                    } else {
                        
                        isGenerating = false;
                        updateSendButtonAppearance();
                        updateChatSendButtonAppearance();
                    }
                }
            }
        });
    }
    
    private void switchLanguage() {
        
        LanguageManager languageManager = LanguageManager.getInstance(getContext());
        String currentLang = languageManager.getLanguage();
        String newLanguage;
        
        switch (currentLang) {
            case LanguageManager.LANGUAGE_TIBETAN:
                newLanguage = LanguageManager.LANGUAGE_CHINESE;
                break;
            case LanguageManager.LANGUAGE_CHINESE:
                newLanguage = LanguageManager.LANGUAGE_ENGLISH;
                break;
            case LanguageManager.LANGUAGE_ENGLISH:
            default:
                newLanguage = LanguageManager.LANGUAGE_TIBETAN;
                break;
        }
        
        languageManager.setLanguage(newLanguage);
        
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).onLanguageChanged(newLanguage);
        }
    }
    
    private void showLanguageDialog() {
        LanguageManager languageManager = LanguageManager.getInstance(getContext());
        String currentLang = languageManager.getLanguage();
        
        final String[] languages = {
            LanguageManager.LANGUAGE_TIBETAN,
            LanguageManager.LANGUAGE_CHINESE,
            LanguageManager.LANGUAGE_ENGLISH
        };
        
        final String[] languageNames = {"བོད་སྐད།", "中文", "English"};
        
        int currentIndex = 0;
        for (int i = 0; i < languages.length; i++) {
            if (languages[i].equals(currentLang)) {
                currentIndex = i;
                break;
            }
        }
        
        new android.app.AlertDialog.Builder(getContext())
                .setTitle("选择语言 / Select Language / སྐད་ཡིག་འདེམས།")
                .setSingleChoiceItems(languageNames, currentIndex, new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface dialog, int which) {
                        String selectedLanguage = languages[which];
                        
                        languageManager.setLanguage(selectedLanguage);
                        
                        if (getActivity() instanceof MainActivity) {
                            ((MainActivity) getActivity()).onLanguageChanged(selectedLanguage);
                        }
                        
                        dialog.dismiss();
                    }
                })
                .setNegativeButton("取消 / Cancel / ཕྱིར་འཐེན།", null)
                .show();
    }
    
    private String getFeatureCardText(int cardIndex) {
        String[][] featureTexts = {
            
            {"ལོ་ལྔའི་འཆར་འགོད་བཅུ་ལྔ་བར་སློབ་སྦྱོང་བྱས་པའི་མྱོང་ཚོར་ཞིག་བྲིས་རོགས།", "དགེ་རྒན་ལ་བསྟོད་པའི་སྙན་ངག་ཅིག་བྲིས་རོགས།", "སློབ་གཙོ་ལ་སློབ་ཐོན་མཛད་སྒོའི་ཐོག་གི་གཏམ་བཤད་ཤིག་བྲིས་རོགས།", "གསོ་བ་རིག་པ་བསླབ་པའི་དགོས་པ་ཅི་ཡིན།"},
            
            {"请写一篇‘十五五规划’的学习心得。", "写一首赞美老师的诗。", "请给校长写一份毕业典礼致辞。", "学习医学的必要是什么？"},
            
            {"Please write a study reflection on the 15th Five Year Plan.", "Write a poem praising the teacher.", "Please write a graduation ceremony speech for the principal.", "What is the necessity of studying medicine?"}
        };
        
        LanguageManager languageManager = LanguageManager.getInstance(getContext());
        String currentLang = languageManager.getLanguage();
        int languageIndex = 0; 
        
        switch (currentLang) {
            case LanguageManager.LANGUAGE_TIBETAN:
                languageIndex = 0;
                break;
            case LanguageManager.LANGUAGE_CHINESE:
                languageIndex = 1;
                break;
            case LanguageManager.LANGUAGE_ENGLISH:
                languageIndex = 2;
                break;
        }
        
        return featureTexts[languageIndex][cardIndex];
    }
    
    private void showModelSelector() {
        
        safeShowToast("当前模型: SunshineGLM");
    }
    
    private int getThemeColor(int attrId) {
        boolean isDarkMode = ThemeManager.getInstance(getContext()).isDarkMode();
        if (attrId == android.R.attr.textColorPrimary) {
            return isDarkMode ? 0xFFFFFFFF : 0xFF000000; 
        } else if (attrId == android.R.attr.colorBackground) {
            return isDarkMode ? 0xFF121212 : 0xFFFFFFFF; 
        }
        return 0xFF000000; 
    }
    
    private int getAnswerBackgroundColor() {
        
        boolean isDarkMode = ThemeManager.getInstance(getContext()).isDarkMode();
        if (isDarkMode) {
            return 0xFF2A2A2A; 
        } else {
            return 0xFFE3F2FD; 
        }
    }
    
    private int getThinkingBackgroundColor() {
        
        boolean isDarkMode = ThemeManager.getInstance(getContext()).isDarkMode();
        if (isDarkMode) {
            return 0xFF1B2B3B; 
        } else {
            return 0xFFE8F5E8; 
        }
    }
    
    private int getUserMessageBackgroundColor() {
        
        boolean isDarkMode = ThemeManager.getInstance(getContext()).isDarkMode();
        if (isDarkMode) {
            return 0xFF121212; 
        } else {
            return 0xFFF5F5F5; 
        }
    }
    
    private void saveChatToHistory() {
        
        if (!ThemeManager.getInstance(getContext()).isAutoSaveChat()) {return;
        }
        
        currentAnswer = streamingTextBuffer.toString();
        currentThinkingContent = thinkingTextBuffer.toString();
        
        if (currentQuestion != null && !currentQuestion.trim().isEmpty() && 
            currentAnswer != null && !currentAnswer.trim().isEmpty()) {
            
            try {
                JSONObject assistantMessage = new JSONObject();
                assistantMessage.put("role", "assistant");
                assistantMessage.put("content", currentAnswer);
                
                if (currentThinkingContent != null && !currentThinkingContent.trim().isEmpty()) {
                    assistantMessage.put("thinking_content", currentThinkingContent);}
                
                conversationHistory.add(assistantMessage);} catch (JSONException e) {}
            
            saveOrUpdateConversationSession();
        }
    }
    
    private void addActionButtons() {
        if (currentStreamingTextView == null) {
            return;
        }
        addActionButtonsForMessage(currentStreamingTextView, streamingTextBuffer.toString());
    }
    
    private void addActionButtonsForMessage(TextView messageView, final String messageContent) {
        if (messageView == null) {
            return;
        }
        
        LinearLayout buttonContainer = new LinearLayout(getContext());
        buttonContainer.setOrientation(LinearLayout.HORIZONTAL);
        buttonContainer.setGravity(android.view.Gravity.START);
        buttonContainer.setPadding(48, 0, 48, 16);
        
        LinearLayout.LayoutParams containerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        containerParams.setMargins(0, 0, 0, 16);
        buttonContainer.setLayoutParams(containerParams);
        
        int buttonSize = (int) (40 * getResources().getDisplayMetrics().density);  // 统一按钮尺寸为40dp
        int buttonMargin = (int) (4 * getResources().getDisplayMetrics().density);
        
        final ImageButton likeButton = createActionButton(R.drawable.ic_thumb_up_outline, buttonSize, buttonMargin);
        final ImageButton dislikeButton = createActionButton(R.drawable.ic_thumb_down_outline, buttonSize, buttonMargin);
        ImageButton copyButton = createActionButton(R.drawable.ic_copy, buttonSize, buttonMargin);
        ImageButton shareButton = createActionButton(R.drawable.ic_share, buttonSize, buttonMargin);
        ImageButton playButton = createActionButton(R.drawable.ic_play, buttonSize, buttonMargin);
        
        final boolean[] isLiked = {false};
        final boolean[] isDisliked = {false};
        
        likeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isLiked[0] = !isLiked[0];
                if (isLiked[0]) {
                    likeButton.setImageResource(R.drawable.ic_thumb_up_filled);
                    if (isDisliked[0]) {
                        isDisliked[0] = false;
                        dislikeButton.setImageResource(R.drawable.ic_thumb_down_outline);
                    }
                } else {
                    likeButton.setImageResource(R.drawable.ic_thumb_up_outline);
                }
            }
        });
        
        dislikeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isDisliked[0] = !isDisliked[0];
                if (isDisliked[0]) {
                    dislikeButton.setImageResource(R.drawable.ic_thumb_down_filled);
                    if (isLiked[0]) {
                        isLiked[0] = false;
                        likeButton.setImageResource(R.drawable.ic_thumb_up_outline);
                    }
                } else {
                    dislikeButton.setImageResource(R.drawable.ic_thumb_down_outline);
                }
            }
        });
        
        copyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 将markdown格式转换为纯文本后复制
                String plainText = stripMarkdown(messageContent);
                android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                android.content.ClipData clip = android.content.ClipData.newPlainText("AI回答", plainText);
                clipboard.setPrimaryClip(clip);
                safeShowToast("已复制到剪贴板");
            }
        });
        
        shareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            }
        });
        
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 检查是否为藏文模式
                String language = LanguageManager.getInstance(getContext()).getLanguage();
                if (!LanguageManager.LANGUAGE_TIBETAN.equals(language)) {
                    safeShowToast("语音合成仅支持藏文");
                    return;
                }
                
                // 停止实时TTS（如果正在播放）
                if (realtimeTtsManager != null && realtimeTtsManager.isActive()) {
                    realtimeTtsManager.stop();
                }
                
                // 如果正在播放，停止播放
                if (ttsManager.isPlaying()) {
                    ttsManager.stop();
                    playButton.setImageResource(R.drawable.ic_play);
                    if (currentPlayingButton != null) {
                        currentPlayingButton.setImageResource(R.drawable.ic_play);
                        currentPlayingButton = null;
                    }
                    return;
                }
                
                // 停止其他正在播放的按钮
                if (currentPlayingButton != null && currentPlayingButton != playButton) {
                    currentPlayingButton.setImageResource(R.drawable.ic_play);
                }
                
                // 开始播放
                currentPlayingButton = playButton;
                playButton.setImageResource(R.drawable.ic_stop);
                
                // 提取纯文本（去除markdown格式）
                String plainText = stripMarkdown(messageContent);
                
                ttsManager.setCallback(new TtsManager.TtsCallback() {
                    @Override
                    public void onStart() {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                // 播放开始
                            }
                        });
                    }
                    
                    @Override
                    public void onComplete() {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                playButton.setImageResource(R.drawable.ic_play);
                                if (currentPlayingButton == playButton) {
                                    currentPlayingButton = null;
                                }
                            }
                        });
                    }
                    
                    @Override
                    public void onError(String error) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                playButton.setImageResource(R.drawable.ic_play);
                                if (currentPlayingButton == playButton) {
                                    currentPlayingButton = null;
                                }
                                safeShowToast("语音合成失败: " + error);
                            }
                        });
                    }
                    
                    @Override
                    public void onProgress(int current, int total) {
                        // 进度回调（可选：显示进度）
                        Log.d("TTS", "播放进度: " + current + "/" + total);
                    }
                });
                
                ttsManager.synthesize(plainText);
            }
        });
        
        buttonContainer.addView(likeButton);
        buttonContainer.addView(dislikeButton);
        buttonContainer.addView(copyButton);
        buttonContainer.addView(shareButton);
        buttonContainer.addView(playButton);
        
        int messageIndex = chatMessagesContainer.indexOfChild(messageView);
        if (messageIndex >= 0) {
            chatMessagesContainer.addView(buttonContainer, messageIndex + 1);
        }
    }
    
    private ImageButton createActionButton(int iconResId, int size, int margin) {
        ImageButton button = new ImageButton(getContext());
        button.setImageResource(iconResId);
        button.setBackground(null);
        button.setScaleType(ImageButton.ScaleType.CENTER_INSIDE);
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
        params.setMargins(0, 0, margin, 0);
        button.setLayoutParams(params);
        
        return button;
    }
    
    private void saveOrUpdateConversationSession() {
        ChatHistory chatHistory;
        
        if (isNewSession) {
            
            chatHistory = new ChatHistory();
            currentSessionId = chatHistory.getId();
            isNewSession = false;} else {
            
            chatHistory = historyManager.getHistoryById(currentSessionId);
            if (chatHistory == null) {
                
                chatHistory = new ChatHistory();
                currentSessionId = chatHistory.getId();}
        }
        
        chatHistory.getConversationMessages().clear();
        
        try {
            for (JSONObject msgJson : conversationHistory) {
                String role = msgJson.getString("role");
                String content = msgJson.getString("content");
                
                String thinkingContent = null;
                if (msgJson.has("thinking_content")) {
                    thinkingContent = msgJson.getString("thinking_content");
                }
                
                if ("assistant".equals(role) && thinkingContent != null && !thinkingContent.trim().isEmpty()) {
                    chatHistory.addMessage(role, content, thinkingContent);} else {
                    chatHistory.addMessage(role, content);}
            }
        } catch (JSONException e) {}
        
        if (isNewSession || historyManager.getHistoryById(currentSessionId) == null) {
            historyManager.addChatHistory(chatHistory);} else {
            historyManager.updateChatHistory(chatHistory);}
    }
    
    private void clearConversationHistory() {
        if (conversationHistory != null) {
            conversationHistory.clear();}
        
        isNewSession = true;
        currentSessionId = null;
        pendingDeleteIndex = -1;  // 重置待删除索引
    }
    
    /**
     * 从历史消息重新发送（点击修改按钮时调用）
     * @param messageIndex 消息在chatMessagesContainer中的索引
     * @param messageContent 消息内容
     */
    private void resendFromMessage(int messageIndex, String messageContent) {
        Log.d("ChatFragment", "resendFromMessage 被调用，索引: " + messageIndex + ", 内容: " + messageContent);
        
        if (isGenerating) {
            Log.d("ChatFragment", "正在生成中，不允许修改");
            safeShowToast("请等待当前回答完成");
            return;
        }
        
        // 将问题填充到输入框
        chatInputEditText.setText(messageContent);
        chatInputEditText.setSelection(messageContent.length());
        
        // 记录待删除的索引，但不立即删除
        pendingDeleteIndex = messageIndex;
        
        Log.d("ChatFragment", "已将问题填充到输入框，等待发送时删除索引 " + messageIndex + " 及之后的消息");
        safeShowToast("已填充到输入框，可以修改后发送");
    }
    
    private void loadHistoryIfNeeded() {
        Bundle args = getArguments();
        if (args != null && args.containsKey("history_id")) {if (getView() != null) {
                View greetingText = getView().findViewById(R.id.greetingText);
                if (greetingText != null && greetingText instanceof TextView) {
                    
                    ((TextView) greetingText).setText("");
                    greetingText.setVisibility(View.GONE);}
                
                if (centerLayout != null) {
                    centerLayout.setVisibility(View.GONE);
                }
            }
            
            String historyId = args.getString("history_id");
            loadHistoryConversation(historyId);
        }
    }
    
    private void safeShowToast(String message) {
        try {
            if (getContext() != null && getActivity() != null && !getActivity().isFinishing()) {
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {}
    }
    
    private void loadHistoryConversation(String historyId) {
        try {
            ChatHistory history = historyManager.getHistoryById(historyId);
            if (history == null) {return;
            }currentSessionId = historyId;
        isNewSession = false;
        
        if (getView() != null) {
            View greetingText = getView().findViewById(R.id.greetingText);
            if (greetingText != null && greetingText instanceof TextView) {
                ((TextView) greetingText).setText("");
                greetingText.setVisibility(View.GONE);}
        }
        
        switchToChatView();
        isFirstMessage = false;
        
        boolean hasThinking = history.hasThinking();
        if (!hasThinking && history.getConversationMessages() != null) {
            
            for (ChatHistory.ConversationMessage msg : history.getConversationMessages()) {
                if (msg.thinkingContent != null && !msg.thinkingContent.trim().isEmpty()) {
                    hasThinking = true;
                    break;
                }
            }
        }
        
        if (deepThinkingSwitch != null) {
            deepThinkingSwitch.setChecked(hasThinking);}
        
        conversationHistory.clear();
        
        if (history.getConversationMessages() != null && !history.getConversationMessages().isEmpty()) {
            for (ChatHistory.ConversationMessage msg : history.getConversationMessages()) {
                try {
                    JSONObject jsonMsg = new JSONObject();
                    jsonMsg.put("role", msg.role);
                    jsonMsg.put("content", msg.content);
                    
                    if (msg.thinkingContent != null && !msg.thinkingContent.trim().isEmpty()) {
                        jsonMsg.put("thinking_content", msg.thinkingContent);}
                    
                    conversationHistory.add(jsonMsg);
                    
                    if ("user".equals(msg.role)) {addUserMessage(msg.content);
                    } else if ("assistant".equals(msg.role)) {if (msg.thinkingContent != null && !msg.thinkingContent.isEmpty()) {
                            addThinkingMessage(msg.thinkingContent);
                        }
                        addBotMessage(msg.content);
                    }
                } catch (JSONException e) {}
            }
        } else {
            
            if (history.getQuestion() != null && !history.getQuestion().isEmpty()) {
                addUserMessage(history.getQuestion());
                
                try {
                    JSONObject userMsg = new JSONObject();
                    userMsg.put("role", "user");
                    userMsg.put("content", history.getQuestion());
                    conversationHistory.add(userMsg);
                } catch (JSONException e) {}
            }
            
            if (history.getAnswer() != null && !history.getAnswer().isEmpty()) {
                if (history.hasThinking() && history.getThinkingContent() != null) {
                    addThinkingMessage(history.getThinkingContent());
                }
                addBotMessage(history.getAnswer());
                
                try {
                    JSONObject assistantMsg = new JSONObject();
                    assistantMsg.put("role", "assistant");
                    assistantMsg.put("content", history.getAnswer());
                    conversationHistory.add(assistantMsg);
                } catch (JSONException e) {}
            }
        }if (getView() != null) {
            getView().post(new Runnable() {
                @Override
                public void run() {if (chatGreetingLayout != null) {
                        chatGreetingLayout.setVisibility(View.GONE);}
                    if (centerLayout != null) {
                        centerLayout.setVisibility(View.GONE);}
                    
                    if (getView() != null) {
                        View greetingText = getView().findViewById(R.id.greetingText);
                        if (greetingText != null && greetingText instanceof TextView) {
                            ((TextView) greetingText).setText("");
                            greetingText.setVisibility(View.GONE);} else {}
                    }
                    
                    if (chatScrollView != null) {
                        chatScrollView.setVisibility(View.VISIBLE);
                        chatScrollView.bringToFront();}
                    if (chatInputContainer != null) {
                        chatInputContainer.setVisibility(View.VISIBLE);}
                    
                    if (getView() != null) {
                        getView().requestLayout();}}
            });
            
            getView().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (getView() != null) {
                        View greetingText = getView().findViewById(R.id.greetingText);
                        if (greetingText != null && greetingText instanceof TextView) {
                            ((TextView) greetingText).setText("");
                            if (greetingText.getVisibility() == View.VISIBLE) {
                                greetingText.setVisibility(View.GONE);}
                        }
                    }
                }
            }, 100); 
        }
        
        } catch (Exception e) {isNewSession = true;
            currentSessionId = null;
            if (conversationHistory != null) {
                conversationHistory.clear();
            }
        }
    }
    
    private void addThinkingMessage(String thinkingContent) {
        
        LinearLayout thinkingContainer = new LinearLayout(getContext());
        thinkingContainer.setOrientation(LinearLayout.VERTICAL);
        
        LinearLayout.LayoutParams containerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        containerParams.setMargins(0, 0, 0, 16);
        thinkingContainer.setLayoutParams(containerParams);
        
        LinearLayout headerLayout = new LinearLayout(getContext());
        headerLayout.setOrientation(LinearLayout.HORIZONTAL);
        headerLayout.setGravity(android.view.Gravity.CENTER_VERTICAL);
        headerLayout.setPadding(32, 16, 32, 16);
        headerLayout.setBackgroundColor(getThinkingBackgroundColor());
        
        ImageView avatarView = new ImageView(getContext());
        avatarView.setImageResource(R.drawable.logo);
        avatarView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        
        LinearLayout.LayoutParams avatarParams = new LinearLayout.LayoutParams(
                (int) (24 * getResources().getDisplayMetrics().density),
                (int) (24 * getResources().getDisplayMetrics().density)
        );
        avatarParams.setMargins(0, 0, 8, 0);
        avatarView.setLayoutParams(avatarParams);
        
        avatarView.setClipToOutline(true);
        avatarView.setOutlineProvider(new android.view.ViewOutlineProvider() {
            @Override
            public void getOutline(android.view.View view, android.graphics.Outline outline) {
                outline.setOval(0, 0, view.getWidth(), view.getHeight());
            }
        });
        
        String language = getArguments() != null ? getArguments().getString("language") : LanguageManager.LANGUAGE_TIBETAN;
        if (language == null) {
            language = LanguageManager.getInstance(getContext()).getLanguage();
        }
        
        String titleTextStr;
        switch (language) {
            case LanguageManager.LANGUAGE_CHINESE:
                titleTextStr = "思考过程";
                break;
            case LanguageManager.LANGUAGE_ENGLISH:
                titleTextStr = "Thinking Process";
                break;
            case LanguageManager.LANGUAGE_TIBETAN:
            default:
                titleTextStr = "བསམ་བློ་གཏོང་རིམ།";
                break;
        }
        
        TextView titleText = new TextView(getContext());
        titleText.setText(titleTextStr);
        titleText.setTextSize(14);
        titleText.setTextColor(0xFF2E7D32);
        titleText.setTypeface(null, android.graphics.Typeface.BOLD);
        
        ImageButton toggleButton = new ImageButton(getContext());
        toggleButton.setImageResource(android.R.drawable.arrow_up_float);
        toggleButton.setBackgroundResource(android.R.color.transparent);
        toggleButton.setScaleType(ImageButton.ScaleType.CENTER);
        LinearLayout.LayoutParams toggleParams = new LinearLayout.LayoutParams(48, 48);
        toggleParams.setMargins(16, 0, 0, 0);
        toggleButton.setLayoutParams(toggleParams);
        
        headerLayout.addView(avatarView);
        headerLayout.addView(titleText);
        headerLayout.addView(toggleButton);
        
        TextView thinkingText = new TextView(getContext());
        markwon.setMarkdown(thinkingText, thinkingContent);
        thinkingText.setTextSize(14);
        thinkingText.setTextColor(getThemeColor(android.R.attr.textColorPrimary));
        thinkingText.setPadding(32, 16, 32, 24);
        thinkingText.setBackgroundColor(getThinkingBackgroundColor());
        thinkingText.setVisibility(View.VISIBLE);
        
        final boolean[] isExpanded = {true};
        headerLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isExpanded[0]) {
                    thinkingText.setVisibility(View.GONE);
                    toggleButton.setImageResource(android.R.drawable.arrow_down_float);
                    isExpanded[0] = false;
                } else {
                    thinkingText.setVisibility(View.VISIBLE);
                    toggleButton.setImageResource(android.R.drawable.arrow_up_float);
                    isExpanded[0] = true;
                }
            }
        });
        
        thinkingContainer.addView(headerLayout);
        thinkingContainer.addView(thinkingText);
        
        chatMessagesContainer.addView(thinkingContainer);
        scrollToBottom();
    }
    
    private void toggleVoiceMode() {
        
        toggleChatVoiceMode();
    }
    
    private void toggleChatVoiceMode() {
        if (!checkAudioPermission()) {
            requestAudioPermission();
            return;
        }
        
        isChatVoiceMode = !isChatVoiceMode;
        updateChatVoiceUI();
        
        if (isChatVoiceMode) {
            startVoiceRecognition(true);
        } else {
            stopVoiceRecognition();
        }
    }
    
    private void updateVoiceUI() {
        
    }
    
    private void updateChatVoiceUI() {
        if (isChatVoiceMode) {
            
            chatInputEditText.setVisibility(View.GONE);
            chatVoiceHintText.setVisibility(View.VISIBLE);
            chatClearButton.setVisibility(View.GONE);
            chatVoiceButton.setImageResource(android.R.drawable.ic_menu_edit);
            chatVoiceButton.setContentDescription("文字输入");
        } else {
            
            chatInputEditText.setVisibility(View.VISIBLE);
            chatVoiceHintText.setVisibility(View.GONE);
            chatVoiceButton.setImageResource(android.R.drawable.ic_btn_speak_now);
            chatVoiceButton.setContentDescription("སྐད་སྒྲ་འཇུག་པ།");
            
            boolean hasText = chatInputEditText.getText().length() > 0;
            chatClearButton.setVisibility(hasText ? View.VISIBLE : View.GONE);
        }
    }
    
    private boolean checkAudioPermission() {
        return ContextCompat.checkSelfPermission(getContext(), Manifest.permission.RECORD_AUDIO) 
                == PackageManager.PERMISSION_GRANTED;
    }
    
    private void requestAudioPermission() {
        ActivityCompat.requestPermissions(getActivity(), 
                new String[]{Manifest.permission.RECORD_AUDIO}, 
                REQUEST_RECORD_AUDIO_PERMISSION);
    }
    
    private void startVoiceRecognition(boolean isChatMode) {
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
        
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(getContext());
        speechRecognizer.setRecognitionListener(new VoiceRecognitionListener(isChatMode));
        
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "bo-CN"); 
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        
        speechRecognizer.startListening(intent);
        
        if (isChatMode) {
            chatVoiceHintText.setText("གསུང་བཞིན་པ...");
        } else {
        
        }
    }
    
    private void stopVoiceRecognition() {
        if (speechRecognizer != null) {
            speechRecognizer.stopListening();
            speechRecognizer.destroy();
            speechRecognizer = null;
        }
        
        if (isChatVoiceMode) {
            chatVoiceHintText.setText("མནན་ནས་གསུང་རོགས།");
        }
        if (isVoiceMode) {
        
        }
    }
    
    private class VoiceRecognitionListener implements android.speech.RecognitionListener {
        private boolean isChatMode;
        
        public VoiceRecognitionListener(boolean isChatMode) {
            this.isChatMode = isChatMode;
        }
        
        @Override
        public void onReadyForSpeech(Bundle params) {}
        
        @Override
        public void onBeginningOfSpeech() {}
        
        @Override
        public void onRmsChanged(float rmsdB) {
            
        }
        
        @Override
        public void onBufferReceived(byte[] buffer) {}
        
        @Override
        public void onEndOfSpeech() {}
        
        @Override
        public void onError(int error) {String errorMessage = getErrorMessage(error);
            Toast.makeText(getContext(), errorMessage, Toast.LENGTH_SHORT).show();
            
            if (isChatMode) {
                isChatVoiceMode = false;
                updateChatVoiceUI();
            } else {
                
                isVoiceMode = false;
                
            }
        }
        
        @Override
        public void onResults(Bundle results) {
            ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            if (matches != null && !matches.isEmpty()) {
                String recognizedText = matches.get(0);if (isChatMode) {
                    chatInputEditText.setText(recognizedText);
                    isChatVoiceMode = false;
                    updateChatVoiceUI();
                } else {
                    
                    chatInputEditText.setText(recognizedText);
                    isVoiceMode = false;
                    
                }
            }
        }
        
        @Override
        public void onPartialResults(Bundle partialResults) {
            ArrayList<String> matches = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            if (matches != null && !matches.isEmpty()) {
                String partialText = matches.get(0);}
        }
        
        @Override
        public void onEvent(int eventType, Bundle params) {}
        
        private String getErrorMessage(int error) {
            switch (error) {
                case SpeechRecognizer.ERROR_AUDIO:
                    return "སྒྲ་ཕབ་ནོར་འཁྲུལ།";
                case SpeechRecognizer.ERROR_CLIENT:
                    return "ལག་ཆ་ནོར་འཁྲུལ།";
                case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                    return "དབང་ཆ་མི་འདང་།";
                case SpeechRecognizer.ERROR_NETWORK:
                    return "དྲ་རྒྱ་ནོར་འཁྲུལ།";
                case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                    return "དྲ་རྒྱ་དུས་ཚོད་ལས་འདས།";
                case SpeechRecognizer.ERROR_NO_MATCH:
                    return "ངོས་འཛིན་མ་ཐུབ།";
                case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                    return "ངོས་འཛིན་ཆས་འཁོར་བཞིན།";
                case SpeechRecognizer.ERROR_SERVER:
                    return "ཞབས་ཞུ་ཆས་ནོར་འཁྲུལ།";
                case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                    return "གསུང་བརྗོད་དུས་ཚོད་ལས་འདས།";
                default:
                    return "མི་ཤེས་པའི་ནོར་འཁྲུལ།";
            }
        }
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
        if (ttsManager != null) {
            ttsManager.release();
        }
        if (realtimeTtsManager != null) {
            realtimeTtsManager.release();
        }
    }
    
    private void setupKeyboardDismissal(View view) {
        
        if (view instanceof ViewGroup) {
            view.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        
                        hideKeyboard();
                    }
                    return false;
                }
            });
        }
        
        if (chatScrollView != null) {
            chatScrollView.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        hideKeyboard();
                    }
                    return false;
                }
            });
        }
    }
    
    private void hideKeyboard() {
        if (getActivity() != null && getView() != null) {
            android.view.inputmethod.InputMethodManager imm = 
                (android.view.inputmethod.InputMethodManager) getActivity().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(getView().getWindowToken(), 0);
            }
        }
    }
    
    private void switchToVoiceMode() {
        isVoiceMode = true;
        
        // 隐藏文本输入框
        if (chatInputEditText != null) {
            chatInputEditText.setVisibility(View.GONE);
        }
        
        // 隐藏框内按钮容器（清除和语音按钮）
        if (textModeButtonsContainer != null) {
            textModeButtonsContainer.setVisibility(View.GONE);
        }
        
        // 显示按住说话按钮
        if (voiceTalkButton != null) {
            voiceTalkButton.setVisibility(View.VISIBLE);
            voiceTalkButton.setBackgroundColor(0xFFFFFFFF);
            voiceTalkButton.setTextColor(0xFF000000);
        }
        
        // 切换左侧按钮：隐藏喇叭，显示键盘
        if (leftVoiceToggleButton != null) {
            leftVoiceToggleButton.setVisibility(View.GONE);
        }
        if (leftKeyboardButton != null) {
            leftKeyboardButton.setVisibility(View.VISIBLE);
        }
    }
    
    private void switchToTextMode() {
        isVoiceMode = false;
        
        // 显示文本输入框
        if (chatInputEditText != null) {
            chatInputEditText.setVisibility(View.VISIBLE);
        }
        
        // 显示框内按钮容器（清除和语音按钮）
        if (textModeButtonsContainer != null) {
            textModeButtonsContainer.setVisibility(View.VISIBLE);
        }
        updateClearButtonVisibility();
        
        // 隐藏按住说话按钮
        if (voiceTalkButton != null) {
            voiceTalkButton.setVisibility(View.GONE);
        }
        
        // 切换左侧按钮：显示喇叭，隐藏键盘
        if (leftVoiceToggleButton != null) {
            leftVoiceToggleButton.setVisibility(View.VISIBLE);
        }
        if (leftKeyboardButton != null) {
            leftKeyboardButton.setVisibility(View.GONE);
        }
    }
    
    private void updateVoiceButtonState(boolean isRecording) {
        if (voiceTalkButton == null) return;
        
        String language = getArguments() != null ? getArguments().getString("language") : LanguageManager.LANGUAGE_TIBETAN;
        if (language == null) {
            language = LanguageManager.getInstance(getContext()).getLanguage();
        }
        
        String pressText, recordingText;
        switch (language) {
            case LanguageManager.LANGUAGE_CHINESE:
                pressText = "按住说话";
                recordingText = "正在说话...";
                break;
            case LanguageManager.LANGUAGE_ENGLISH:
                pressText = "Press to speak";
                recordingText = "Recording...";
                break;
            case LanguageManager.LANGUAGE_TIBETAN:
            default:
                pressText = "མནན་ནས་གསུང་རོགས།";
                recordingText = "གསུང་བཞིན་པ...";
                break;
        }
        
        if (isRecording) {
            voiceTalkButton.setText(recordingText);
            voiceTalkButton.setBackgroundColor(0xFF4CAF50); 
            voiceTalkButton.setTextColor(0xFFFFFFFF); 
        } else {
            voiceTalkButton.setText(pressText);
            voiceTalkButton.setBackgroundColor(0xFFFFFFFF); 
            voiceTalkButton.setTextColor(0xFF000000); 
        }
    }
    
    private boolean checkRecordPermission() {
        if (ContextCompat.checkSelfPermission(getContext(), 
                Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    REQUEST_RECORD_AUDIO_PERMISSION);
            return false;
        }
        return true;
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                safeShowToast("麦克风权限已授予");
                switchToVoiceMode();
            } else {
                safeShowToast("需要麦克风权限才能使用语音输入");
            }
        }
    }
    
    private void updateClearButtonVisibility() {
        if (chatClearButton != null && chatInputEditText != null) {
            chatClearButton.setVisibility(
                chatInputEditText.getText().length() > 0 ? View.VISIBLE : View.GONE
            );
        }
    }
    
    /**
     * 将Markdown格式文本转换为纯文本
     * 移除所有Markdown格式标记，保留纯文本内容
     */
    private String stripMarkdown(String markdown) {
        if (markdown == null || markdown.isEmpty()) {
            return "";
        }
        
        String text = markdown;
        
        // 移除代码块 ```code```
        text = text.replaceAll("```[\\s\\S]*?```", "");
        
        // 移除行内代码 `code`
        text = text.replaceAll("`([^`]+)`", "$1");
        
        // 移除标题标记 # ## ###
        text = text.replaceAll("(?m)^#{1,6}\\s+", "");
        
        // 移除粗体 **text** 或 __text__
        text = text.replaceAll("\\*\\*([^*]+)\\*\\*", "$1");
        text = text.replaceAll("__([^_]+)__", "$1");
        
        // 移除斜体 *text* 或 _text_
        text = text.replaceAll("\\*([^*]+)\\*", "$1");
        text = text.replaceAll("_([^_]+)_", "$1");
        
        // 移除链接，保留链接文本 [text](url) -> text
        text = text.replaceAll("\\[([^\\]]+)\\]\\([^)]+\\)", "$1");
        
        // 移除图片 ![alt](url)
        text = text.replaceAll("!\\[([^\\]]*)\\]\\([^)]+\\)", "");
        
        // 移除无序列表标记 - * +
        text = text.replaceAll("(?m)^[\\s]*[-*+]\\s+", "");
        
        // 移除有序列表标记 1. 2. 3.
        text = text.replaceAll("(?m)^[\\s]*\\d+\\.\\s+", "");
        
        // 移除引用标记 >
        text = text.replaceAll("(?m)^>\\s+", "");
        
        // 移除水平线 --- *** ___
        text = text.replaceAll("(?m)^[-*_]{3,}$", "");
        
        // 移除多余的空行（保留单个换行）
        text = text.replaceAll("\n{3,}", "\n\n");
        
        return text.trim();
    }
    
    /**
     * 更新自动播报按钮图标
     */
    private void updateAutoPlayButtonIcon() {
        if (audioButton == null) {
            return;
        }
        
        if (isAutoPlayEnabled) {
            // 开启状态：显示带声波的喇叭
            audioButton.setImageResource(R.drawable.ic_volume_on);
        } else {
            // 禁用状态：显示带声波的喇叭+斜杠
            audioButton.setImageResource(R.drawable.ic_volume_off);
        }
    }
    
    /**
     * 显示语音波形弹窗
     */
    private void showVoiceWaveformDialog() {
        if (voiceDialog != null && voiceDialog.isShowing()) {
            return;
        }
        
        try {
            View dialogView = LayoutInflater.from(getContext())
                .inflate(R.layout.dialog_voice_waveform, null);
            
            waveformView = dialogView.findViewById(R.id.waveformView);
            volumeText = dialogView.findViewById(R.id.volumeText);
            TextView hintText = dialogView.findViewById(R.id.voiceHintText);
            
            // 根据当前语言设置提示文本
            String language = LanguageManager.getInstance(getContext()).getLanguage();
            switch (language) {
                case LanguageManager.LANGUAGE_CHINESE:
                    hintText.setText("松手转为文字，上滑取消");
                    break;
                case LanguageManager.LANGUAGE_ENGLISH:
                    hintText.setText("Release to convert, slide up to cancel");
                    break;
                case LanguageManager.LANGUAGE_TIBETAN:
                    hintText.setText("ལྷོད་ནས་ཡི་གེར་བསྒྱུར། ཡར་འགྱེད་ནས་མེད་པར་བཟོ།");
                    break;
            }
            
            // 创建透明主题的对话框
            voiceDialog = new android.app.Dialog(getContext(), 
                android.R.style.Theme_Translucent_NoTitleBar);
            voiceDialog.setContentView(dialogView);
            voiceDialog.setCancelable(false);
            
            voiceDialog.show();
            
            // 显示后设置对话框位置在底部
            android.view.Window window = voiceDialog.getWindow();
            if (window != null) {
                android.view.WindowManager.LayoutParams params = window.getAttributes();
                params.gravity = android.view.Gravity.BOTTOM | android.view.Gravity.CENTER_HORIZONTAL;
                params.width = android.view.WindowManager.LayoutParams.WRAP_CONTENT;
                params.height = android.view.WindowManager.LayoutParams.WRAP_CONTENT;
                // 设置底部边距（dp转px）
                int marginBottomPx = (int) (120 * getResources().getDisplayMetrics().density);
                params.y = marginBottomPx;  // 距离底部120dp
                window.setAttributes(params);
            }
            
            // 启动波形动画
            if (waveformView != null) {
                waveformView.startAnimation();
            }
            
            Log.d("ChatFragment", "✅ 语音波形弹窗已显示");
        } catch (Exception e) {
            Log.e("ChatFragment", "❌ 显示语音波形弹窗失败", e);
        }
    }
    
    /**
     * 关闭语音波形弹窗
     */
    private void dismissVoiceWaveformDialog() {
        try {
            if (waveformView != null) {
                waveformView.stopAnimation();
                waveformView.reset();
            }
            
            if (voiceDialog != null && voiceDialog.isShowing()) {
                voiceDialog.dismiss();
            }
            
            waveformView = null;
            volumeText = null;
            
            Log.d("ChatFragment", "✅ 语音波形弹窗已关闭");
        } catch (Exception e) {
            Log.e("ChatFragment", "❌ 关闭语音波形弹窗失败", e);
        }
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        
        // 停止TTS播放
        if (ttsManager != null) {
            ttsManager.stop();
            ttsManager.release();
            ttsManager = null;
        }
        
        // 重置播放按钮状态
        if (currentPlayingButton != null) {
            currentPlayingButton.setImageResource(R.drawable.ic_play);
            currentPlayingButton = null;
        }
        
        if (audioButton != null) {
            updateAutoPlayButtonIcon();
        }
        
        // 清理资源
        dismissVoiceWaveformDialog();
        
        if (speechManager != null) {
            speechManager.release();
        }
    }
}
