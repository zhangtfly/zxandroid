package com.example.myapplication;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;

import com.example.myapplication.http.BaseUrl;
import com.example.myapplication.http.api.LoginVisitorV2Api;
import com.example.myapplication.http.model.HttpData;
import com.example.myapplication.login.bean.LoginV2Bean;
import com.example.myapplication.utils.TokenManager;
import com.hjq.http.EasyHttp;
import com.hjq.http.listener.HttpCallbackProxy;
import com.hjq.http.listener.OnHttpListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public class SpeechRecognitionManager implements OnHttpListener {
    private static final String TAG = "SpeechRecognition";

    // WebSocket URL
    private static final String WSS_URL = "wss://derisively-nonchargeable-lailah.ngrok-free.dev/api/ws/asr";
    private LifecycleOwner lifecycleOwner;
    private static final String LANG_TYPE = "bo-CN";

    private static final int SAMPLE_RATE = 16000;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;

    private Context context;
    private OkHttpClient client;
    private AudioRecord audioRecord;
    private volatile boolean isRecording = false;
    private Thread recordingThread;
    private ByteArrayOutputStream audioData;

    private static final int SILENCE_THRESHOLD = 500;
    private static final int PRE_BUFFER_SIZE = 8000;
    private byte[] preBuffer = new byte[PRE_BUFFER_SIZE];
    private int preBufferPos = 0;
    private boolean hasStartedSaving = false;

    private RecognitionCallback callback;
    
    // WebSocket 相关
    private WebSocket webSocket;
    private StringBuilder recognitionResult = new StringBuilder();
    private boolean isWebSocketConnected = false;
    private boolean isWebSocketReady = false; // 是否收到started消息

    @Override
    public void onHttpSuccess(@NonNull Object result) {

    }

    @Override
    public void onHttpFail(@NonNull Throwable throwable) {

    }

    public interface RecognitionCallback {
        void onRecordingStart();
        void onRecordingStop();
        void onRecognitionSuccess(String result);
        void onRecognitionError(String error);
        void onAmplitudeChanged(float amplitude);
    }

    public SpeechRecognitionManager(Context context,LifecycleOwner lifecycleOwner, OkHttpClient client) {
        this.context = context.getApplicationContext(); // 修复：避免内存泄漏
        this.lifecycleOwner = lifecycleOwner;
        this.client = client;
        
        // 立即建立WebSocket连接
        connectWebSocket();
    }

    /**
     * 建立持久的WebSocket连接
     */
    private void connectWebSocket() {
        TokenManager tokenManager = new TokenManager(context);
        String token = tokenManager.getToken();
        String wsUrl = WSS_URL + "?token=" + token;
        
        Log.d(TAG, "📤 建立持久 WebSocket 连接: " + wsUrl);
        
        OkHttpClient wsClient = createWebSocketClient();
        
        Request request = new Request.Builder()
                .url(wsUrl)
                .addHeader("Authorization", "Bearer " + token)
                .build();
        
        webSocket = wsClient.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(@NonNull WebSocket webSocket, @NonNull Response response) {
                Log.d(TAG, "✅ WebSocket 连接已建立 - HTTP状态码: " + response.code());
                isWebSocketConnected = true;
            }
            
            @Override
            public void onMessage(@NonNull WebSocket webSocket, @NonNull String text) {
                Log.d(TAG, "📥 收到文本消息: " + text);
                
                try {
                    JSONObject json = new JSONObject(text);
                    
                    // 检查是否是连接成功消息
                    if (json.has("type") && "connected".equals(json.optString("type"))) {
                        Log.d(TAG, "连接成功: " + json.optString("message", ""));
                        return;
                    }
                    
                    // 检查是否是started消息
                    if (json.has("type") && "started".equals(json.optString("type"))) {
                        Log.d(TAG, "识别已开始: " + json.optString("message", ""));
                        isWebSocketReady = true;
                        return;
                    }
                    
                    // 检查是否是stopped消息
                    if (json.has("type") && "stopped".equals(json.optString("type"))) {
                        Log.d(TAG, "识别已停止: " + json.optString("message", ""));
                        isWebSocketReady = false;
                        
                        // 返回最终结果
                        String finalResult = recognitionResult.toString().trim();
                        Log.d(TAG, "📝 最终识别结果: [" + finalResult + "], 长度: " + finalResult.length());
                        
                        if (!finalResult.isEmpty()) {
                            new Handler(Looper.getMainLooper()).post(() -> {
                                if (callback != null) callback.onRecognitionSuccess(finalResult);
                            });
                        } else {
                            Log.w(TAG, "⚠️ 识别结果为空");
                            new Handler(Looper.getMainLooper()).post(() -> {
                                if (callback != null) callback.onRecognitionError("没有识别到内容");
                            });
                        }
                        
                        // 清空结果，准备下次识别
                        recognitionResult.setLength(0);
                        return;
                    }
                    
                    // 检查是否是错误消息
                    if (json.has("type") && "error".equals(json.optString("type"))) {
                        String errorCode = json.optString("errorCode", "UNKNOWN");
                        String message = json.optString("message", "未知错误");
                        
                        String errorMsg = "后端错误 - " + errorCode + ": " + message;
                        Log.e(TAG, errorMsg);
                        
                        new Handler(Looper.getMainLooper()).post(() -> {
                            if (callback != null) callback.onRecognitionError(errorMsg);
                        });
                        return;
                    }
                    
                    // 处理识别结果消息
                    JSONObject header = json.optJSONObject("header");
                    JSONObject payload = json.optJSONObject("payload");
                    
                    if (header != null && payload != null) {
                        String status = header.optString("status");
                        String name = header.optString("name");
                        String message = header.optString("message", "");
                        
                        Log.d(TAG, "📊 消息详情 - status: " + status + ", name: " + name + ", message: " + message);
                        
                        if ("00000".equals(status)) {
                            String result = payload.optString("result", "");
                            
                            if ("SentenceBegin".equals(name)) {
                                Log.d(TAG, "句子开始");
                            } else if ("SentenceEnd".equals(name)) {
                                if (!result.isEmpty()) {
                                    recognitionResult.append(result);
                                }
                                Log.d(TAG, "句子结束，当前结果: " + recognitionResult.toString());
                            } else if ("TranscriptionResultChanged".equals(name)) {
                                if (!result.isEmpty()) {
                                    recognitionResult.append(result);
                                }
                                Log.d(TAG, "中间结果: " + result);
                            }
                        } else {
                            String errorMsg = "识别失败 - status: " + status + ", message: " + message;
                            Log.e(TAG, errorMsg);
                            new Handler(Looper.getMainLooper()).post(() -> {
                                if (callback != null) callback.onRecognitionError(errorMsg);
                            });
                        }
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "解析消息失败", e);
                    new Handler(Looper.getMainLooper()).post(() -> {
                        if (callback != null) callback.onRecognitionError("解析后端消息失败: " + e.getMessage());
                    });
                }
            }
            
            @Override
            public void onMessage(@NonNull WebSocket webSocket, @NonNull ByteString bytes) {
                Log.d(TAG, "📥 收到二进制消息，大小: " + bytes.size());
            }
            
            @Override
            public void onClosing(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
                Log.d(TAG, "🔌 WebSocket 正在关闭: code=" + code + ", reason=" + reason);
                isWebSocketConnected = false;
                isWebSocketReady = false;
                webSocket.close(1000, null);
            }
            
            @Override
            public void onFailure(@NonNull WebSocket webSocket, @NonNull Throwable t, Response response) {
                String errorDetails = "连接失败: " + t.getMessage();
                if (response != null) {
                    errorDetails += "\nHTTP状态码: " + response.code();
                    errorDetails += "\n响应消息: " + response.message();
                }
                Log.e(TAG, "❌ " + errorDetails, t);
                
                isWebSocketConnected = false;
                isWebSocketReady = false;
                
                final String finalError = errorDetails;
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (callback != null) callback.onRecognitionError(finalError);
                });
            }
        });
    }

    public void setCallback(RecognitionCallback callback) {
        this.callback = callback;
    }

    public void startRecording() {
        if (isRecording) {
            Log.w(TAG, "已经在录音中");
            return;
        }

        if (!isWebSocketConnected) {
            Log.e(TAG, "WebSocket未连接");
            if (callback != null) {
                new Handler(Looper.getMainLooper()).post(() ->
                        callback.onRecognitionError("WebSocket未连接，请稍后重试")
                );
            }
            return;
        }

        // 发送start指令
        try {
            JSONObject startCommand = new JSONObject();
            startCommand.put("action", "start");
            
            JSONObject params = new JSONObject();
            params.put("langType", LANG_TYPE);
            params.put("format", "pcm"); // 实时发送PCM数据
            params.put("sampleRate", SAMPLE_RATE);
            params.put("enableIntermediateResult", true);
            params.put("enablePunctuationPrediction", true);
            params.put("enableInverseTextNormalization", true);
            
            startCommand.put("params", params);
            
            boolean startSuccess = webSocket.send(startCommand.toString());
            Log.d(TAG, "📤 发送开始指令" + (startSuccess ? "成功" : "失败"));
            
        } catch (JSONException e) {
            Log.e(TAG, "❌ 构建开始指令失败", e);
            if (callback != null) {
                new Handler(Looper.getMainLooper()).post(() ->
                        callback.onRecognitionError("构建开始指令失败")
                );
            }
            return;
        }

        try {
            int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
            if (bufferSize <= 0) {
                throw new IllegalStateException("AudioRecord 缓冲区无效");
            }

            audioRecord = new AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT,
                    bufferSize
            );

            // 修复：必须校验初始化状态
            if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                throw new IllegalStateException("AudioRecord 初始化失败");
            }

            audioData = new ByteArrayOutputStream();
            preBufferPos = 0;
            hasStartedSaving = false;

            audioRecord.startRecording();
            isRecording = true;

            if (callback != null) {
                callback.onRecordingStart();
            }

            Log.d(TAG, "🎤 开始录音（实时流式模式）");

            recordingThread = new Thread(() -> {
                byte[] buffer = new byte[bufferSize];
                
                // 等待started消息
                int waitCount = 0;
                while (!isWebSocketReady && waitCount < 50) { // 最多等待5秒
                    try {
                        Thread.sleep(100);
                        waitCount++;
                    } catch (InterruptedException e) {
                        break;
                    }
                }
                
                if (!isWebSocketReady) {
                    Log.e(TAG, "等待started消息超时");
                    return;
                }
                
                Log.d(TAG, "开始实时发送音频流");
                
                while (isRecording) {
                    try {
                        AudioRecord record = audioRecord;
                        if (record == null || record.getRecordingState() != AudioRecord.RECORDSTATE_RECORDING) {
                            break;
                        }

                        int read = record.read(buffer, 0, buffer.length);
                        if (read > 0) {
                            final float amplitude = calculateAmplitude(buffer, read);

                            // 保存到本地（用于最后的备份）
                            if (!hasStartedSaving) {
                                addToPreBuffer(buffer, read);
                                if (amplitude > 0.05f) {
                                    hasStartedSaving = true;
                                    flushPreBufferToMain();
                                    Log.d(TAG, "检测到声音，开始保存音频");
                                }
                            } else {
                                audioData.write(buffer, 0, read);
                            }

                            // 实时发送音频数据到WebSocket
                            if (isWebSocketReady && webSocket != null) {
                                byte[] chunk = new byte[read];
                                System.arraycopy(buffer, 0, chunk, 0, read);
                                webSocket.send(ByteString.of(chunk));
                            }

                            if (callback != null) {
                                new Handler(Looper.getMainLooper()).post(() ->
                                        callback.onAmplitudeChanged(amplitude)
                                );
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "录音读取异常", e);
                        break;
                    }
                }
            });
            recordingThread.start();

        } catch (Exception e) {
            Log.e(TAG, "❌ 录音失败", e);
            releaseRecordResources(); // 修复：异常时释放资源
            if (callback != null) {
                callback.onRecognitionError("录音失败: " + e.getMessage());
            }
        }
    }

    private void addToPreBuffer(byte[] data, int length) {
        for (int i = 0; i < length; i++) {
            preBuffer[preBufferPos] = data[i];
            preBufferPos = (preBufferPos + 1) % PRE_BUFFER_SIZE;
        }
    }

    private void flushPreBufferToMain() {
        try {
            if (preBufferPos < PRE_BUFFER_SIZE) {
                audioData.write(preBuffer, preBufferPos, PRE_BUFFER_SIZE - preBufferPos);
            }
            if (preBufferPos > 0) {
                audioData.write(preBuffer, 0, preBufferPos);
            }
        } catch (Exception e) {
            Log.e(TAG, "刷新预缓冲区失败", e);
        }
    }

    public void stopRecording() {
        if (!isRecording) return;

        isRecording = false;

        new Thread(() -> {
            // 修复：统一安全释放 AudioRecord
            releaseRecordResources();

            try {
                if (recordingThread != null) {
                    recordingThread.join(300);
                }
            } catch (InterruptedException e) {
                Log.e(TAG, "线程等待异常", e);
            }

            Log.d(TAG, "🛑 停止录音");

            if (callback != null) {
                new Handler(Looper.getMainLooper()).post(() ->
                        callback.onRecordingStop()
                );
            }

            // 发送stop指令
            if (isWebSocketConnected && webSocket != null) {
                try {
                    JSONObject stopCommand = new JSONObject();
                    stopCommand.put("action", "stop");
                    stopCommand.put("params", new JSONObject());
                    
                    boolean stopSuccess = webSocket.send(stopCommand.toString());
                    Log.d(TAG, "📤 发送停止指令" + (stopSuccess ? "成功" : "失败"));
                    
                } catch (JSONException e) {
                    Log.e(TAG, "❌ 构建停止指令失败", e);
                }
            }
        }).start();
    }

    public void cancelRecording() {
        if (!isRecording) return;

        isRecording = false;
        releaseRecordResources();

        try {
            if (recordingThread != null) {
                recordingThread.join(300);
            }
        } catch (InterruptedException e) {
            Log.e(TAG, "取消录音线程异常", e);
        }

        if (audioData != null) {
            audioData.reset();
        }

        if (callback != null) {
            new Handler(Looper.getMainLooper()).post(() ->
                    callback.onRecordingStop()
            );
        }

        Log.d(TAG, "❌ 取消录音");
    }

    // 修复：统一安全释放 AudioRecord（核心修复）
    private void releaseRecordResources() {
        try {
            if (audioRecord != null) {
                audioRecord.stop();
                audioRecord.release();
                audioRecord = null;
            }
        } catch (Exception e) {
            Log.e(TAG, "释放录音资源异常", e);
        }
    }

    private float calculateAmplitude(byte[] buffer, int length) {
        if (buffer == null || length <= 1) return 0f;
        long sum = 0;
        int count = 0;

        for (int i = 0; i < length - 1; i += 2) {
            short sample = (short) ((buffer[i + 1] << 8) | (buffer[i] & 0xFF));
            sum += Math.abs(sample);
            count++;
        }

        if (count == 0) return 0f;
        float avg = sum / (float) count;
        float norm = avg / 32768f;
        return (float) Math.log10(1 + norm * 9) / (float) Math.log10(10);
    }

    /**
     * 创建支持 ngrok 证书的 WebSocket 客户端
     * ngrok 使用的是公共 CA 签发的证书，需要信任系统默认证书
     */
    private OkHttpClient createWebSocketClient() {
        try {
            // 创建信任所有证书的 TrustManager（仅用于 ngrok 开发环境）
            javax.net.ssl.TrustManager[] trustAllCerts = new javax.net.ssl.TrustManager[]{
                new javax.net.ssl.X509TrustManager() {
                    @Override
                    public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                    }

                    @Override
                    public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                    }

                    @Override
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return new java.security.cert.X509Certificate[]{};
                    }
                }
            };

            // 安装信任所有证书的 TrustManager
            javax.net.ssl.SSLContext sslContext = javax.net.ssl.SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());

            // 创建不验证主机名的 HostnameVerifier
            javax.net.ssl.HostnameVerifier hostnameVerifier = new javax.net.ssl.HostnameVerifier() {
                @Override
                public boolean verify(String hostname, javax.net.ssl.SSLSession session) {
                    return true; // 接受所有主机名
                }
            };

            return new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(300, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .sslSocketFactory(sslContext.getSocketFactory(), (javax.net.ssl.X509TrustManager) trustAllCerts[0])
                    .hostnameVerifier(hostnameVerifier)
                    .retryOnConnectionFailure(true)
                    // 增大WebSocket消息缓冲区，支持大消息
                    .build();
        } catch (Exception e) {
            Log.e(TAG, "创建 WebSocket 客户端失败，使用默认配置", e);
            return client; // 失败时使用原始 client
        }
    }

    public void preconnect() {
        // 预连接方法：只建立WebSocket连接，不发送音频数据
        // 这个方法在Fragment创建时调用，用于提前建立连接，减少首次使用延迟
        Log.d(TAG, "📡 预连接：WebSocket连接已在构造函数中建立");
        // 实际的连接已经在构造函数的 connectWebSocket() 中完成
        // 这里不需要额外操作
    }

    private byte[] convertToWav(byte[] pcmData) {
        int totalDataLen = pcmData.length + 36;
        int totalAudioLen = pcmData.length;
        int channels = 1;
        int byteRate = SAMPLE_RATE * channels * 2;

        byte[] header = new byte[44];
        header[0] = 'R'; header[1] = 'I'; header[2] = 'F'; header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W'; header[9] = 'A'; header[10] = 'V'; header[11] = 'E';
        header[12] = 'f'; header[13] = 'm'; header[14] = 't'; header[15] = ' ';
        header[16] = 16; header[20] = 1;
        header[22] = (byte) channels;
        header[24] = (byte) (SAMPLE_RATE & 0xff);
        header[25] = (byte) ((SAMPLE_RATE >> 8) & 0xff);
        header[26] = (byte) ((SAMPLE_RATE >> 16) & 0xff);
        header[27] = (byte) ((SAMPLE_RATE >> 24) & 0xff);
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        header[32] = (byte) (channels * 2);
        header[34] = 16;
        header[36] = 'd'; header[37] = 'a'; header[38] = 't'; header[39] = 'a';
        header[40] = (byte) (totalAudioLen & 0xff);
        header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
        header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
        header[43] = (byte) ((totalAudioLen >> 24) & 0xff);

        byte[] wavData = new byte[header.length + pcmData.length];
        System.arraycopy(header, 0, wavData, 0, header.length);
        System.arraycopy(pcmData, 0, wavData, header.length, pcmData.length);
        return wavData;
    }

    public boolean isRecording() {
        return isRecording;
    }

    public void release() {
        stopRecording();
        releaseRecordResources();
        
        // 关闭 WebSocket 连接
        if (webSocket != null) {
            webSocket.close(1000, "释放资源");
            webSocket = null;
        }
        
        isWebSocketConnected = false;
        isWebSocketReady = false;
    }
}