package com.liulishuo.engzo.onlinescorersdk;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.liulishuo.engzo.onlinescorer.OnlineScorerRecorder;
import com.liulishuo.engzo.onlinescorer.ReadLoudExercise;

import java.io.File;

/**
 * Created by wcw on 4/11/17.
 */

public class DemoActivity extends AppCompatActivity {

    private String errorFilePath;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_demo);

        final TextView resultView = (TextView) findViewById(R.id.resultView);
        TextView titleView = (TextView) findViewById(R.id.titleView);
        final Button recordBtn = (Button) findViewById(R.id.recordBtn);

        recordBtn.setText("start");
        titleView.setText("请说 i will study english very hard");

        // 仅供测试用的 appId 和 appSecret
        String appId = "test2";
        String appSecret = "test2";

        // 创建 "i will study english very hard" 这句话的练习
        ReadLoudExercise readLoudExercise = new ReadLoudExercise();
        readLoudExercise.setReftext("i will study english very hard");
        // 并且指定音频质量为8，该参数只影响传输给服务端的音频质量，不影响本地录音文件
        readLoudExercise.setQuality(8);

        // 创建打分录音器
        final OnlineScorerRecorder onlineScorerRecorder = new OnlineScorerRecorder(appId, appSecret, readLoudExercise, "/sdcard/test.wav");

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
                            recordBtn.setText("retry");
                            return;
                        }
                    }
                    errorFilePath = null;
                    recordBtn.setText("start");
                    resultView.setText(Log.getStackTraceString(error));
                } else {
                    errorFilePath = null;
                    resultView.setText(String.format("filePath = %s\n report = %s", filePath, report));
                    recordBtn.setText("start");
                }
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
}
