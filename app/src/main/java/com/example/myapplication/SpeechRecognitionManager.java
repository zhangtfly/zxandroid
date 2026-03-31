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

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SpeechRecognitionManager implements OnHttpListener {
    private static final String TAG = "SpeechRecognition";

    private static final String API_URL = BaseUrl.BaseUrl+"api/v1";
    private LifecycleOwner lifecycleOwner; // 新增：正确持有 LifecycleOwner
    private static final String LANG_TYPE = "bo-CN";

    private static final int SAMPLE_RATE = 16000;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;

    private Context context;
    private OkHttpClient client;
    private AudioRecord audioRecord;
    private volatile boolean isRecording = false; // 修复：volatile 线程安全
    private Thread recordingThread;
    private ByteArrayOutputStream audioData;

    private static final int SILENCE_THRESHOLD = 500;
    private static final int PRE_BUFFER_SIZE = 8000;
    private byte[] preBuffer = new byte[PRE_BUFFER_SIZE];
    private int preBufferPos = 0;
    private boolean hasStartedSaving = false;

    private RecognitionCallback callback;

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
    }

    public void setCallback(RecognitionCallback callback) {
        this.callback = callback;
    }

    public void startRecording() {
        if (isRecording) {
            Log.w(TAG, "已经在录音中");
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

            Log.d(TAG, "🎤 开始录音（智能缓冲模式）");

            recordingThread = new Thread(() -> {
                byte[] buffer = new byte[bufferSize];
                while (isRecording) {
                    try {
                        AudioRecord record = audioRecord;
                        if (record == null || record.getRecordingState() != AudioRecord.RECORDSTATE_RECORDING) {
                            break;
                        }

                        int read = record.read(buffer, 0, buffer.length);
                        if (read > 0) {
                            final float amplitude = calculateAmplitude(buffer, read);

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
            try {
                // 修复：安全捕获尾音，防止崩溃
                if (audioRecord != null && audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING && hasStartedSaving) {
                    int extraSamples = SAMPLE_RATE * 2 / 10;
                    byte[] tailBuffer = new byte[extraSamples * 2];
                    int read = audioRecord.read(tailBuffer, 0, tailBuffer.length);
                    if (read > 0) {
                        audioData.write(tailBuffer, 0, read);
                        Log.d(TAG, "捕获尾音: " + read + " 字节");
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "捕获尾音失败", e);
            }

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

            if (audioData != null && audioData.size() > 0) {
                sendRecognitionRequest(audioData.toByteArray());
            } else {
                Log.w(TAG, "没有录制到有效音频");
                if (callback != null) {
                    new Handler(Looper.getMainLooper()).post(() ->
                            callback.onRecognitionError("没有检测到声音")
                    );
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

    private void sendRecognitionRequest(byte[] audioBytes) {
        byte[] wavData = convertToWav(audioBytes);
        Log.d(TAG, "📤 发送语音识别请求，音频大小: " + wavData.length + " bytes");

        String url =
                "?lang_type=" + LANG_TYPE +
                "&format=wav" +
                "&sample_rate=" + SAMPLE_RATE +
                "&bit_depth=16" +
                "&enable_punctuation_prediction=true" +
                "&enable_inverse_text_normalization=true";
        Log.e("voice1",url);
        RequestBody body = RequestBody.create(wavData, MediaType.parse("application/octet-stream"));
        TokenManager tokenManager = new TokenManager(context);
//        Request request = new Request.Builder()
//                .url(url)
//                .post(body)
//                .addHeader("Content-Type", "application/octet-stream")
//                .addHeader("Authorization",  "Bearer " + tokenManager.getToken())
//                .build();
        EasyHttp.post(lifecycleOwner)
                .api("api/v1/asr"+url)
                .body(body)
                .request(new HttpCallbackProxy<Response>(this) {

                    @Override
                    public void onHttpSuccess(Response response) {
                        if (!response.isSuccessful() || response.body() == null) {
                            new Handler(Looper.getMainLooper()).post(() -> {
                                if (callback != null) callback.onRecognitionError("服务器错误: " + response.code());
                            });
                            return;
                        }

                        String responseBody = null;
                        try {
                            responseBody = response.body().string();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        Log.d(TAG, "📥 收到识别结果: " + responseBody);

                        try {
                            JSONObject json = new JSONObject(responseBody);
                            if ("00000".equals(json.optString("status"))) {
                                String result = json.optString("result");
                                new Handler(Looper.getMainLooper()).post(() -> {
                                    if (callback != null) callback.onRecognitionSuccess(result);
                                });
                            } else {
                                String msg = json.optString("message", "未知错误");
                                new Handler(Looper.getMainLooper()).post(() -> {
                                    if (callback != null) callback.onRecognitionError("识别失败: " + msg);
                                });
                            }
                        } catch (JSONException e) {
                            new Handler(Looper.getMainLooper()).post(() -> {
                                if (callback != null) callback.onRecognitionError("解析结果失败");
                            });
                        }
                    }

                    @Override
                    public void onHttpFail(@NonNull Throwable throwable) {
                        super.onHttpFail(throwable);
                        Log.e(TAG, "❌ 请求失败", throwable);
                        new Handler(Looper.getMainLooper()).post(() -> {
                            if (callback != null) callback.onRecognitionError("识别失败: " + throwable.getMessage());
                        });
                    }
                });
//        client.newCall(request).enqueue(new Callback() {
//            @Override
//            public void onFailure(@NonNull Call call, @NonNull IOException e) {
//                Log.e(TAG, "❌ 请求失败", e);
//                new Handler(Looper.getMainLooper()).post(() -> {
//                    if (callback != null) callback.onRecognitionError("识别失败: " + e.getMessage());
//                });
//            }
//
//            @Override
//            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
//                if (!response.isSuccessful() || response.body() == null) {
//                    new Handler(Looper.getMainLooper()).post(() -> {
//                        if (callback != null) callback.onRecognitionError("服务器错误: " + response.code());
//                    });
//                    return;
//                }
//
//                String responseBody = response.body().string();
//                Log.d(TAG, "📥 收到识别结果: " + responseBody);
//
//                try {
//                    JSONObject json = new JSONObject(responseBody);
//                    if ("00000".equals(json.optString("status"))) {
//                        String result = json.optString("result");
//                        new Handler(Looper.getMainLooper()).post(() -> {
//                            if (callback != null) callback.onRecognitionSuccess(result);
//                        });
//                    } else {
//                        String msg = json.optString("message", "未知错误");
//                        new Handler(Looper.getMainLooper()).post(() -> {
//                            if (callback != null) callback.onRecognitionError("识别失败: " + msg);
//                        });
//                    }
//                } catch (JSONException e) {
//                    new Handler(Looper.getMainLooper()).post(() -> {
//                        if (callback != null) callback.onRecognitionError("解析结果失败");
//                    });
//                }
//            }
//        });
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

    public void testWithAudioFile(String fileName) {
        try (InputStream is = context.getAssets().open(fileName);
             ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {

            byte[] data = new byte[1024];
            int nRead;
            while ((nRead = is.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            buffer.flush();
            sendRecognitionRequest(buffer.toByteArray());

        } catch (IOException e) {
            Log.e(TAG, "❌ 读取测试音频文件失败", e);
            if (callback != null) {
                new Handler(Looper.getMainLooper()).post(() ->
                        callback.onRecognitionError("读取测试文件失败: " + e.getMessage())
                );
            }
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

    public void preconnect() {
        Log.d(TAG, "🔌 预连接语音识别接口");
        byte[] emptyWav = convertToWav(new byte[0]);

        String url = "?lang_type=" + LANG_TYPE + "&format=wav&sample_rate=" + SAMPLE_RATE + "&bit_depth=16";
        Log.e("voice2",url);
        TokenManager tokenManager = new TokenManager(context);
        RequestBody body = RequestBody.create(emptyWav, MediaType.parse("application/octet-stream"));
        EasyHttp.post(lifecycleOwner)
                .api("api/v1/asr"+url)
                .body(body)
                .request(new HttpCallbackProxy<HttpData<LoginV2Bean>>(this) {

                    @Override
                    public void onHttpSuccess(HttpData<LoginV2Bean> result) {

                    }
                });
//        Request request = new Request.Builder().url(url).post(body).addHeader("Content-Type", "application/octet-stream")
//                .addHeader("Authorization",  "Bearer " + tokenManager.getToken())
//                .build();
//
//        client.newCall(request).enqueue(new Callback() {
//            @Override public void onFailure(@NonNull Call call, @NonNull IOException e) {}
//            @Override public void onResponse(@NonNull Call call, @NonNull Response response) {
//                if (response.body() != null) response.body().close();
//            }
//        });
    }

    public void release() {
        stopRecording();
        releaseRecordResources();
    }
}