package com.yusuf.sarjkilidi.v5;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Movie;
import android.os.SystemClock;
import android.view.View;
import java.io.InputStream;

public class GifView extends View {
    private Movie movie;
    private long start=SystemClock.uptimeMillis();

    public GifView(Context c,int resId){
        super(c);
        try{
            InputStream in=getResources().openRawResource(resId);
            movie=Movie.decodeStream(in);
            in.close();
        }catch(Exception ignored){}
    }

    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);
        if(movie==null)return;
        int d=movie.duration(); if(d<=0)d=1000;
        movie.setTime((int)((SystemClock.uptimeMillis()-start)%d));
        float sx=getWidth()/(float)Math.max(1,movie.width());
        float sy=getHeight()/(float)Math.max(1,movie.height());
        float sc=Math.min(sx,sy);
        float dx=(getWidth()-movie.width()*sc)/2f;
        float dy=(getHeight()-movie.height()*sc)/2f;
        canvas.save(); canvas.translate(dx,dy); canvas.scale(sc,sc);
        movie.draw(canvas,0,0); canvas.restore();
        invalidate();
    }
}

