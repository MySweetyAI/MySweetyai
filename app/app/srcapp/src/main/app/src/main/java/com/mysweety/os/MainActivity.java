package com.mysweety.os;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends Activity {

    private SpeechRecognizer speechRecognizer;
    private TextView statusText, aiResponseText;
    private Handler handler;
    private boolean waitingForCmd = false;
    private BroadcastReceiver powerReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // बेसिक UI बिना XML फ़ाइल पर निर्भर रहे
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setGravity(android.view.Gravity.CENTER);
        layout.setBackgroundColor(0xFF050814);
        layout.setPadding(40, 40, 40, 40);

        TextView icon = new TextView(this);
        icon.setText("💠");
        icon.setTextSize(80);
        icon.setGravity(android.view.Gravity.CENTER);
        layout.addView(icon);

        TextView title = new TextView(this);
        title.setText("MY SWEETY OS");
        title.setTextColor(0xFF00E5FF);
        title.setTextSize(26);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setGravity(android.view.Gravity.CENTER);
        layout.addView(title);

        statusText = new TextView(this);
        statusText.setText("Say 'Sweety' to activate...");
        statusText.setTextColor(0xB3FFFFFF);
        statusText.setTextSize(15);
        statusText.setGravity(android.view.Gravity.CENTER);
        layout.addView(statusText);

        aiResponseText = new TextView(this);
        aiResponseText.setText("Smart Power & Charging Manager Active");
        aiResponseText.setTextColor(0xFF00E5FF);
        aiResponseText.setTextSize(14);
        aiResponseText.setGravity(android.view.Gravity.CENTER);
        layout.addView(aiResponseText);

        setContentView(layout);

        handler = new Handler(Looper.getMainHandler());

        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED ||
            checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA}, 100);
        } else {
            startVoiceRecognition();
        }

        registerPowerReceiver();
    }

    private void registerPowerReceiver() {
        powerReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (Intent.ACTION_POWER_CONNECTED.equals(intent.getAction())) {
                    aiResponseText.setText("Sweety: Charger connected! Mobile Data is ON, Wi-Fi optimized.");
                    optimizeOnCharging();
                }
            }
        };
        registerReceiver(powerReceiver, new IntentFilter(Intent.ACTION_POWER_CONNECTED));
    }

    private void optimizeOnCharging() {
        try {
            WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wifiManager != null && wifiManager.isWifiEnabled()) {
                wifiManager.setWifiEnabled(false);
            }
        } catch (Exception e) {}
    }

    private void startVoiceRecognition() {
        handler.post(() -> {
            try {
                if (speechRecognizer != null) {
                    speechRecognizer.destroy();
                }
            } catch (Exception e) {}

            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
            speechRecognizer.setRecognitionListener(new RecognitionListener() {
                @Override public void onReadyForSpeech(Bundle params) {
                    statusText.setText(waitingForCmd ? "Listening command..." : "Listening for 'Sweety'...");
                }
                @Override public void onBeginningOfSpeech() {}
                @Override public void onRmsChanged(float rmsdB) {}
                @Override public void onBufferReceived(byte[] buffer) {}
                @Override public void onEndOfSpeech() { statusText.setText("Processing..."); }
                @Override public void onError(int error) {
                    restartListening(1500);
                }
                @Override public void onResults(Bundle results) {
                    ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (matches != null && !matches.isEmpty()) {
                        String text = matches.get(0).toLowerCase();

                        if (!waitingForCmd) {
                            if (text.contains("sweety") || text.contains("स्वीटी")) {
                                waitingForCmd = true;
                                aiResponseText.setText("Yes Boss!");
                                statusText.setText("Activated!");
                                startVoiceRecognition();
                            } else {
                                restartListening(500);
                            }
                        } else {
                            waitingForCmd = false;
                            aiResponseText.setText("You: " + matches.get(0));
                            processCommand(matches.get(0));
                        }
                    } else {
                        restartListening(1000);
                    }
                }
                @Override public void onPartialResults(Bundle partialResults) {}
                @Override public void onEvent(int eventType, Bundle params) {}
            });

            try {
                Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
                speechRecognizer.startListening(intent);
            } catch (Exception e) {}
        });
    }

    private void restartListening(long delay) {
        handler.postDelayed(this::startVoiceRecognition, delay);
    }

    private void processCommand(String query) {
        String lower = query.toLowerCase();

        if (lower.contains("torch on") || lower.contains("flashlight on") || lower.contains("लाइट जलाओ")) {
            setTorch(true);
            aiResponseText.setText("Sweety: Torch ON");
            restartListening(3000);
            return;
        }
        if (lower.contains("torch off") || lower.contains("flashlight off") || lower.contains("लाइट बंद करो")) {
            setTorch(false);
            aiResponseText.setText("Sweety: Torch OFF");
            restartListening(3000);
            return;
        }
        if (lower.contains("youtube") || lower.contains("युट्यूब")) {
            openLink("https://www.youtube.com");
            return;
        }
        if (lower.contains("google") || lower.contains("गूगल")) {
            openLink("https://www.google.com");
            return;
        }

        aiResponseText.setText("Sweety: Command received!");
        restartListening(3000);
    }

    private void setTorch(boolean on) {
        try {
            CameraManager cm = (CameraManager) getSystemService(CAMERA_SERVICE);
            String id = cm.getCameraIdList()[0];
            cm.setTorchMode(id, on);
        } catch (Exception e) {}
    }

    private void openLink(String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
            aiResponseText.setText("Sweety: Opened!");
        } catch (Exception e) {}
        restartListening(3000);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (powerReceiver != null) {
            unregisterReceiver(powerReceiver);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == 100 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startVoiceRecognition();
        }
    }
}
