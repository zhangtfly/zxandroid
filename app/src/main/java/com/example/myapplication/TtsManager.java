package com.example.myapplication;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.util.Base64;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public final class TtsManager {
    private static final String TAG = "TtsManager";
    private static final String TTS_WS_URL = "ws://117.68.88.175:38086/tts/v1/streaming?app_id=VXBpX2tleT@ia2VSeHh4eHh";
    private static final int SAMPLE_RATE = 24000;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_OUT_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int MAX_CHUNK_SIZE = 500;

    private final Context context;
    private final OkHttpClient client;
    private WebSocket currentWebSocket;
    private AudioTrack audioTrack;
    private TtsCallback callback;

    private volatile boolean isPlaying = false;
    private volatile boolean isStopped = false;
    private List<String> textChunks;
    private int currentChunkIndex = 0;
    private boolean isFirstChunk = true;

    private final LinkedBlockingQueue<byte[]> audioQueue;
    private Thread playbackThread;
    private Thread synthesisThread;
    private int completedChunks = 0;

    public interface TtsCallback {
        void onStart();
        void onComplete();
        void onError(String error);
        void onProgress(int current, int total);
    }

    public TtsManager(Context context) {
        this.context = context.getApplicationContext();
        this.client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .build();
        this.audioQueue = new LinkedBlockingQueue<>();
    }

    public void setCallback(TtsCallback callback) {
        this.callback = callback;
    }

    public void synthesize(String text) {
        stop();

        if (text == null || text.trim().isEmpty()) {
            if (callback != null) callback.onError("文本为空");
            return;
        }

        text = text.replaceAll("\\s+", " ").trim();
        textChunks = splitText(text);
        currentChunkIndex = 0;
        completedChunks = 0;
        isFirstChunk = true;
        isStopped = false;
        isPlaying = false;

        audioQueue.clear();
        initAudioTrack();
        startPlaybackThread();
        startSynthesisThread();
    }

    private List<String> splitText(String text) {
        List<String> chunks = new ArrayList<>();
        if (text.length() <= MAX_CHUNK_SIZE) {
            chunks.add(text);
            return chunks;
        }

        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + MAX_CHUNK_SIZE, text.length());
            if (end < text.length()) {
                int lastPeriod = text.lastIndexOf('།', end);
                if (lastPeriod > start && lastPeriod < end) end = lastPeriod + 1;
            }
            chunks.add(text.substring(start, end).trim());
            start = end;
        }
        return chunks;
    }

    private void startSynthesisThread() {
        if (synthesisThread != null && synthesisThread.isAlive()) return;

        synthesisThread = new Thread(() -> {
            for (int i = 0; i < textChunks.size() && !isStopped; i++) {
                synthesizeChunkSync(textChunks.get(i), i);
            }
        });
        synthesisThread.start();
    }

    private void synthesizeChunkSync(final String text, final int chunkIndex) {
        if (isStopped) return;

        final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
        Request request = new Request.Builder().url(TTS_WS_URL).build();

        currentWebSocket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                sendSynthesisRequest(webSocket, text);
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                try {
                    JSONObject json = new JSONObject(text);
                    int code = json.getInt("code");

                    if (code == 101 || (code == 10000 && !json.has("result"))) {
                        if (isFirstChunk && callback != null) {
                            callback.onStart();
                            isFirstChunk = false;
                        }
                        isPlaying = true;
                    } else if (code == 10000 && json.has("result")) {
                        JSONObject result = json.getJSONObject("result");
                        String audioBase64 = result.getString("audio");
                        boolean isEnd = result.getBoolean("is_end");
                        byte[] audioData = Base64.decode(audioBase64, Base64.DEFAULT);

                        if (!isStopped) audioQueue.offer(audioData);
                        if (isEnd) {
                            onChunkComplete(chunkIndex);
                            latch.countDown();
                        }
                    }
                } catch (Exception e) {
                    latch.countDown();
                }
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                if (isStopped) {
                    latch.countDown();
                    return;
                }
                if (callback != null) callback.onError("语音服务连接失败");
                latch.countDown();
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                latch.countDown();
            }
        });

        try {
            latch.await();
        } catch (Exception ignored) {}
    }

    private void startPlaybackThread() {
        if (playbackThread != null && playbackThread.isAlive()) return;

        playbackThread = new Thread(() -> {
            while (!isStopped) {
                if (completedChunks >= textChunks.size() && audioQueue.isEmpty()) {
                    break;
                }

                byte[] audioData;
                try {
                    audioData = audioQueue.poll(40, TimeUnit.MILLISECONDS);
                } catch (Exception e) {
                    if (isStopped) break;
                    continue;
                }

                if (audioData == null) continue;

                // ==========================
                // 【绝杀防崩溃】
                // ==========================
                if (isStopped || audioTrack == null) break;

                try {
                    if (audioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
                        audioTrack.write(audioData, 0, audioData.length);
                    }
                } catch (Exception ignored) {
                    break;
                }
            }
            finishPlayback();
        });
        playbackThread.start();
    }

    private void onChunkComplete(int chunkIndex) {
        synchronized (this) {
            completedChunks++;
            if (callback != null) callback.onProgress(completedChunks, textChunks.size());
        }
    }

    private void sendSynthesisRequest(WebSocket webSocket, String text) {
        try {
            JSONObject request = new JSONObject();
            request.put("req_id", UUID.randomUUID().toString());
            request.put("text", prepareTextForTTS(text));
            webSocket.send(request.toString());
        } catch (Exception ignored) {}
    }

    private String prepareTextForTTS(String text) {
        if (text == null) return "";
        text = text.replace('\u3000', ' ').replace('\u00A0', ' ');
        text = text.replaceAll("[ \\t]+", " ");
        text = text.replaceAll("^\\s+", "");
        return text;
    }

    private void initAudioTrack() {
        if (audioTrack != null) return;

        int bufferSize = AudioTrack.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
        if (bufferSize <= 0) return;
        bufferSize *= 4;

        try {
            android.media.AudioAttributes attr = new android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build();

            android.media.AudioFormat format = new android.media.AudioFormat.Builder()
                    .setSampleRate(SAMPLE_RATE)
                    .setEncoding(AUDIO_FORMAT)
                    .setChannelMask(CHANNEL_CONFIG)
                    .build();

            audioTrack = new AudioTrack(attr, format, bufferSize, AudioTrack.MODE_STREAM, 0);
            if (audioTrack.getState() == AudioTrack.STATE_INITIALIZED) {
                audioTrack.play();
            } else {
                audioTrack = null;
            }
        } catch (Exception e) {
            audioTrack = null;
        }
    }

    private void finishPlayback() {
        try {
            Thread.sleep(300);
            if (callback != null && !isStopped) callback.onComplete();
        } catch (Exception ignored) {}
        stop();
    }

    // ==========================
    // 【终极安全停止】
    // ==========================
    public void stop() {
        isStopped = true;
        isPlaying = false;

        // 立刻清空队列，杜绝最后一次写入
        audioQueue.clear();

        // 中断线程
        if (synthesisThread != null) synthesisThread.interrupt();
        if (playbackThread != null) playbackThread.interrupt();

        // 强制等待线程死亡
        try { if (playbackThread != null) playbackThread.join(1000); } catch (Exception ignored) {}
        try { if (synthesisThread != null) synthesisThread.join(1000); } catch (Exception ignored) {}

        // 线程死光 → 再释放
        cleanup();
    }

    private void cleanup() {
        if (currentWebSocket != null) {
            try { currentWebSocket.close(1000, "close"); } catch (Exception ignored) {}
            currentWebSocket = null;
        }

        if (audioTrack != null) {
            try {
                audioTrack.pause();
                audioTrack.flush();
                audioTrack.stop();
                audioTrack.release();
            } catch (Exception ignored) {}
            audioTrack = null;
        }
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public void release() {
        stop();
        client.dispatcher().executorService().shutdown();
    }
}