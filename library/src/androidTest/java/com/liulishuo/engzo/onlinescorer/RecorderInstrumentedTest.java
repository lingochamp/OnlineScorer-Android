package com.liulishuo.engzo.onlinescorer;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertNotNull;

@RunWith(AndroidJUnit4.class)
public class RecorderInstrumentedTest {

    private String appId;
    private String appSecret;
    private ReadLoudExercise readLoudExercise;

    @Before
    public void setup() {
        // 仅供测试用的 appId 和 appSecret
        appId = "test2";
        appSecret = "test2";

        // 创建 "i will study english very hard" 这句话的练习
        readLoudExercise = new ReadLoudExercise();
        readLoudExercise.setReftext("i will study english very hard");
        // 并且指定音频质量为8，该参数只影响传输给服务端的音频质量，不影响本地录音文件
        readLoudExercise.setQuality(8);
    }

    @Test
    public void testRecorderThreadSafe() throws Exception {
        Context appContext = InstrumentationRegistry.getTargetContext();

        InputStream is = appContext.getResources().getAssets().open("test.wav");


        File testFile = new File(appContext.getCacheDir(), "test.wav");
        String testFilePath = testFile.getPath();

        FileOutputStream fos = new FileOutputStream(testFilePath);
        BufferedOutputStream bos = new BufferedOutputStream(fos);

        byte[] buffer = new byte[8096];

        int read = 0;
        while ((read = is.read(buffer)) > 0) {
            bos.write(buffer, 0, read);
        }
        is.close();
        bos.flush();
        bos.close();


        OnlineScorerRecorder onlineScorerRecorderA =
                new OnlineScorerRecorder(appId, appSecret, readLoudExercise,
                        new File(appContext.getCacheDir(), "temp.wav").getPath());
        OnlineScorerRecorder onlineScorerRecorderB =
                new OnlineScorerRecorder(appId, appSecret, readLoudExercise,
                        new File(appContext.getCacheDir(), "temp2.wav").getPath());

        onlineScorerRecorderA.startRecord(testFilePath);

        final CountDownLatch countDownLatch = new CountDownLatch(2);

        final String[] reports = new String[2];

        onlineScorerRecorderA.setOnProcessStopListener(new OnlineScorerRecorder.OnProcessStopListener() {
            @Override
            public void onProcessStop(Throwable error, String filePath, String report) {
                reports[0] = report;
                countDownLatch.countDown();
            }
        });
        onlineScorerRecorderB.startRecord(testFilePath);
        onlineScorerRecorderB.setOnProcessStopListener(new OnlineScorerRecorder.OnProcessStopListener() {
            @Override
            public void onProcessStop(Throwable error, String filePath, String report) {
                reports[1] = report;
                countDownLatch.countDown();
            }
        });

        countDownLatch.await();

        assertNotNull(reports[0]);
        assertNotNull(reports[1]);
    }
}
