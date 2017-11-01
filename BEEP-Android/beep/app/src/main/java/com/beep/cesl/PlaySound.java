package com.beep.cesl;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

public class PlaySound {

    private static final int HEIGHT = 127; //正弦波的幅度
    private static final double TWOPI = 2 * 3.1415927;// 2 * PI
    private static final int RATE = 44000; // 声音信号采样率，单位Hz
    private AudioTrack audioTrack; //声音播放类

    //产生占空比正弦波序列
    private static byte[] waveGen(int waveLen, int length, int duty, int period) {
            byte[] wave = new byte[length];
            for (int i = 0; i < length; i++) {
                if( waveLen / (i % (waveLen) * 1.00) > duty ) {
                    wave[i] = (byte) (HEIGHT * (1 - Math.sin(TWOPI * ((i % (waveLen / period)) * 1.00 / (waveLen / period)))));
                }
                else
                    wave[i] = 0;
            }
            /* 产生占空比三角波序列
             for (int i = 0; i < length; i++) {
                if( waveLen / (i % (waveLen) * 1.00) > (2*DUTY) ) {
                    wave[i] = (byte)(HEIGHT * (i % (waveLen / PERIOD / 2) * 1.00 / (waveLen / PERIOD / 2)));
                }else if (waveLen / (i % (waveLen) * 1.00) > DUTY){
                    wave[i] = (byte)(HEIGHT * (1 - (i % (waveLen / PERIOD / 2) * 1.00 / (waveLen / PERIOD / 2))));
                }
                else
                    wave[i] = 0;
            }
            */
            return wave;
    }

    //计算正弦波声音序列的各参数
    public byte[] start(int hertz, int time, int duty, int period) {
        stop();
        int waveLen = RATE / (hertz / (duty * period)); //waveLen：正弦波每个周期的采样点数
        int length =  waveLen * (hertz / (duty * period)) / 1000 * time; //length：声音信号总的采样点数
        audioTrack = new AudioTrack( AudioManager.STREAM_MUSIC, RATE,
                    AudioFormat.CHANNEL_CONFIGURATION_MONO, // CHANNEL_CONFIGURATION_STEREO,
                    AudioFormat.ENCODING_PCM_8BIT, length, AudioTrack.MODE_STREAM);
        audioTrack.play();
        //生成正弦波
        return waveGen(waveLen, length, duty, duty * period);
        }

    //写入数据
    public void play(byte[] wave){
        if(audioTrack != null){
            audioTrack.write(wave, 0, wave.length);
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
