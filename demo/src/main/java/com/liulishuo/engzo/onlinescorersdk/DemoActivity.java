package com.liulishuo.engzo.onlinescorersdk;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.PermissionChecker;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.liulishuo.engzo.onlinescorer.OnlineScorer;
import com.liulishuo.engzo.onlinescorer.OnlineScorerRecorder;
import com.liulishuo.engzo.onlinescorer.ReadLoudExercise;
import com.liulishuo.engzo.onlinescorer.RequestLogCallback;

import java.io.File;

/**
 * Created by wcw on 4/11/17.
 */

public class DemoActivity extends AppCompatActivity {

    private String errorFilePath;
    private OnlineScorerRecorder onlineScorerRecorder;
    private Button recordBtn;

    private final static int REQUEST_CODE_PERMISSION = 1000;

    @Override
    protected void onPause() {
        super.onPause();
        onlineScorerRecorder.stopRecord();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_demo);

        final TextView resultView = (TextView) findViewById(R.id.resultView);
        TextView titleView = (TextView) findViewById(R.id.titleView);
        recordBtn = (Button) findViewById(R.id.recordBtn);

        recordBtn.setText("start");
        titleView.setText("请说 i will study english very hard");

        // 创建 "i will study english very hard" 这句话的练习
        ReadLoudExercise readLoudExercise = new ReadLoudExercise();
        readLoudExercise.setReftext("i will study english very hard");
        // 并且指定音频质量为8，该参数只影响传输给服务端的音频质量，不影响本地录音文件
        readLoudExercise.setQuality(8);

        // 创建打分录音器
        onlineScorerRecorder = new OnlineScorerRecorder(readLoudExercise, "/sdcard/test.wav");

        // 录音完成的回调，开发者可以用这个监听录音的完成，在里头一般做更新录音按钮状态的操作
        onlineScorerRecorder.setOnRecordStopListener(new OnlineScorerRecorder.OnRecordListener() {
            @Override
            public void onRecordStop(Throwable error) {
                if (error != null) {
                    Toast.makeText(DemoActivity.this, "录音出错\n" + Log.getStackTraceString(error),
                            Toast.LENGTH_SHORT).show();
                }
                if (errorFilePath != null) {
                    recordBtn.setText("retry");
                } else {
                    recordBtn.setText("start");
                }
            }
        });

        // 录音处理完成的回调，开发者可以监听该回调，获取打分报告与录音文件
        onlineScorerRecorder.setOnProcessStopListener(new OnlineScorerRecorder.OnProcessStopListener() {

            @Override
            public void onProcessStop(Throwable error, String filePath, String report) {
                if (error != null) {
                    // 当异常为 ScorerException 可以把 filePath 存下来拿来 retry
                    if (error instanceof OnlineScorerRecorder.ScorerException) {
                        errorFilePath = "/sdcard/retry.wav";
                        boolean renameSuccess = new File(filePath).renameTo(new File(errorFilePath));
                        if (renameSuccess) {
                            resultView.setText(error.getMessage());
                            recordBtn.setText("retry");
                            fetchLogFile();
                            return;
                        }
                    }
                    errorFilePath = null;
                    recordBtn.setText("start");
                    resultView.setText(error.getMessage());
                } else {
                    errorFilePath = null;

                    resultView.setText(String.format("filePath = %s\n report = %s", filePath, report));
                    recordBtn.setText("start");
                }
                fetchLogFile();
            }

            private void fetchLogFile() {
                //拿出日志文件
                OnlineScorer.requestLogDir(
                        new RequestLogCallback() {
                            @Override
                            public void onDirResponse(File logDir) {
                                if (logDir != null) {
                                    final String result = String.format("%s\n logDir is %s",
                                            resultView.getText(),
                                            logDir.getAbsoluteFile());
                                    resultView.setText(result);
                                }
                            }
                        });
            }
        });

        recordBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // isAvailable 为 false 为录音正在处理时，需要保护避免在这个时候操作录音器
                if (onlineScorerRecorder.isAvailable()) {
                    if (onlineScorerRecorder.isRecording()) {
                        onlineScorerRecorder.stopRecord();
                    } else {
                        if (!checkRecordPermission()) return;
                        // 当 errorFilePath 不为空时，可以将该文件作为录音文件传入
                        if (errorFilePath != null) {
                            onlineScorerRecorder.startRecord(errorFilePath);
                        } else {
                            // need get record permission
                            onlineScorerRecorder.startRecord();
                        }
                        resultView.setText("");
                        recordBtn.setText("stop");
                    }
                }
            }
        });
    }

    private boolean checkRecordPermission() {
        if (PermissionChecker.checkSelfPermission(DemoActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || PermissionChecker.checkSelfPermission(DemoActivity.this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE) ||
                        shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)) {
                    new AlertDialog.Builder(DemoActivity.this)
                            .setTitle(R.string.check_permission_title)
                            .setMessage(R.string.check_permission_content)
                            .setCancelable(false)
                            .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                        requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO}, REQUEST_CODE_PERMISSION);
                                    }
                                }
                            }).show();
                } else {
                    requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO}, REQUEST_CODE_PERMISSION);
                }
            }
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSION) {
            for (int grantResult : grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, R.string.check_permission_fail, Toast.LENGTH_LONG).show();
                    return;
                }
            }
            recordBtn.performClick();
        }
    }
}
