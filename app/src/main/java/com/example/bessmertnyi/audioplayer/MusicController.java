package com.example.bessmertnyi.audioplayer;

import android.content.Context;
import android.widget.MediaController;

public class MusicController extends MediaController {

    public MusicController(Context c){
        super(c);
    }

    public void hide(){

    }

    public void forceHide(){
        super.hide();
    }
}
