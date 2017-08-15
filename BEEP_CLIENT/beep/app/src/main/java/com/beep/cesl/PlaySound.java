package com.beep.cesl;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

public class PlaySound {

    private static final int HEIGHT = 127; //正弦波的幅度
    private static final double TWOPI = 2 * 3.1415;// 2 * PI
    private static final int RATE = 192000; // 正弦波序列的长度 参考44100
    private  static final int HERTZ = 1000; //正弦波的频率

    private AudioTrack audioTrack = null; //播放声音类

    private byte[] wave = null; //播放的正弦波声音序列

    //生成正弦波的序列
    private byte[] sinWave(byte[] wave, int waveLen, int length) {
        for (int i = 0; i < length; i++) {
            wave[i] = (byte) (HEIGHT * (1 - Math.sin(TWOPI * ((i % waveLen) * 1.00 / waveLen))));
        }
        Log.v("##SOUND##","sinWave");
        return wave;
    }

    //计算正弦波声音序列的各参数
    public void start() {
        stop();
        int waveLen = RATE / HERTZ; //waveLen：每段正弦波的长度
        int length = waveLen * HERTZ; //length：总长度
            audioTrack = new AudioTrack( AudioManager.STREAM_MUSIC, RATE,
                    AudioFormat.CHANNEL_CONFIGURATION_STEREO, // CHANNEL_CONFIGURATION_MONO,
                    AudioFormat.ENCODING_PCM_8BIT, length, AudioTrack.MODE_STREAM);
        Log.v("##SOUND##","Ready");
            //生成正弦波
            wave = sinWave(wave, waveLen, length);
            if(audioTrack != null){
                audioTrack.play();
            }
        Log.v("##SOUND##","audioTrack");
        }

    //写入数据
    public void play(){
        if(audioTrack != null){
            audioTrack.write(wave, 0, wave.length);
            Log.v("##SOUND##","Play");
        }
    }

    //停止播放
    public void stop(){
        if(audioTrack != null){
            audioTrack.stop();
            audioTrack.release();
            audioTrack = null;
        }
    }
}
