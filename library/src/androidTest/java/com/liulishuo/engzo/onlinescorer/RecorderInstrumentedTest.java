package com.liulishuo.engzo.onlinescorer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.liulishuo.engzo.lingorecorder.LingoRecorder;
import com.liulishuo.engzo.stat.StatisticManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CountDownLatch;

@RunWith(AndroidJUnit4.class)
public class RecorderInstrumentedTest {

    private ReadLoudExercise readLoudExercise;

    @Before
    public void setup() {
        Config.get().init(InstrumentationRegistry.getTargetContext(), "test2", "test2");
        OnlineScorerProcessor.SERVER = "wss://rating.llsstaging.com/openapi/stream/upload";
        StatisticManager.get().init(InstrumentationRegistry.getTargetContext());

        // 创建 "i will study english very hard" 这句话的练习
        readLoudExercise = new ReadLoudExercise();
        readLoudExercise.setReftext("i will study english very hard");
        readLoudExercise.setTargetAudience(1);
        // 并且指定音频质量为8，该参数只影响传输给服务端的音频质量，不影响本地录音文件
        readLoudExercise.setQuality(8);
    }

    @Test
    public void testCancelAndroidRecorder() throws Exception {
        Context appContext = InstrumentationRegistry.getTargetContext();
        final OnlineScorerRecorder onlineScorerRecorder =
                new OnlineScorerRecorder(readLoudExercise,
                        new File(appContext.getCacheDir(), "temp.wav").getPath());


        final boolean[] recordStopSuccess = new boolean[1];
        final Throwable[] processStopError = new Throwable[1];

        final CountDownLatch countDownLatch = new CountDownLatch(2);

        onlineScorerRecorder.setOnRecordStopListener(new OnlineScorerRecorder.OnRecordListener() {
            @Override
            public void onRecordStop(Throwable error, OnlineScorerRecorder.Result result) {
                if (error == null) {
                    recordStopSuccess[0] = true;
                } else {
                    // if fail check recorder permission
                    throw new RuntimeException(error);
                }
                countDownLatch.countDown();
            }
        });

        onlineScorerRecorder.setOnProcessStopListener(new OnlineScorerRecorder.OnProcessStopListener() {
            @Override
            public void onProcessStop(Throwable error, String filePath, String report) {
                processStopError[0] = error;
                countDownLatch.countDown();
            }
        });

        onlineScorerRecorder.startRecord();

        Thread.sleep(3000);

        onlineScorerRecorder.cancel();

        countDownLatch.await();

        assertEquals(true, recordStopSuccess[0]);
        assertEquals(LingoRecorder.CancelProcessingException.class, processStopError[0].getClass());
    }

    @Test
    public void testCancelWavRecorder() throws Exception {
        Context appContext = InstrumentationRegistry.getTargetContext();

        String inputWavFile = copyAssetToTempDir(appContext, "test.wav", "test.wav");

        final OnlineScorerRecorder onlineScorerRecorder =
                new OnlineScorerRecorder(readLoudExercise,
                        new File(appContext.getCacheDir(), "temp.wav").getPath());


        final boolean[] recordStopSuccess = new boolean[1];
        final Throwable[] processStopError = new Throwable[1];

        final CountDownLatch countDownLatch = new CountDownLatch(2);

        onlineScorerRecorder.setOnRecordStopListener(new OnlineScorerRecorder.OnRecordListener() {
            @Override
            public void onRecordStop(Throwable error, OnlineScorerRecorder.Result result) {
                if (error == null) {
                    recordStopSuccess[0] = true;
                } else {
                    throw new RuntimeException(error);
                }
                countDownLatch.countDown();
            }
        });

        onlineScorerRecorder.setOnProcessStopListener(new OnlineScorerRecorder.OnProcessStopListener() {
            @Override
            public void onProcessStop(Throwable error, String filePath, String report) {
                processStopError[0] = error;
                countDownLatch.countDown();
            }
        });

        onlineScorerRecorder.startRecord(inputWavFile);

        onlineScorerRecorder.cancel();

        countDownLatch.await();

        assertEquals(true, recordStopSuccess[0]);
        assertEquals(LingoRecorder.CancelProcessingException.class, processStopError[0].getClass());
    }

    @Test
    public void testRecorderThreadSafe() throws Exception {
        Context appContext = InstrumentationRegistry.getTargetContext();

        String inputWavFile = copyAssetToTempDir(appContext, "test.wav", "test.wav");

        OnlineScorerRecorder onlineScorerRecorderA =
                new OnlineScorerRecorder(readLoudExercise,
                        new File(appContext.getCacheDir(), "temp.wav").getPath());
        OnlineScorerRecorder onlineScorerRecorderB =
                new OnlineScorerRecorder(readLoudExercise,
                        new File(appContext.getCacheDir(), "temp2.wav").getPath());

        onlineScorerRecorderA.startRecord(inputWavFile);

        final CountDownLatch countDownLatch = new CountDownLatch(2);

        final String[] reports = new String[2];

        onlineScorerRecorderA.setOnProcessStopListener(new OnlineScorerRecorder.OnProcessStopListener() {
            @Override
            public void onProcessStop(Throwable error, String filePath, String report) {
                reports[0] = report;
                countDownLatch.countDown();
            }
        });
        onlineScorerRecorderB.startRecord(inputWavFile);
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

    private String copyAssetToTempDir(Context context, String fromFileName, String toFileName) throws IOException {
        InputStream is = context.getResources().getAssets().open(fromFileName);

        File testFile = new File(context.getCacheDir(), toFileName);
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

        return testFilePath;
    }
}
