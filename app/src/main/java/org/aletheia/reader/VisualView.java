package org.aletheia.reader;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.view.View;

public final class VisualView extends View {
  private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
  private final int mode;
  public int intensity=30;
  public VisualView(Context c,int mode){super(c);this.mode=mode;setBackgroundColor(Color.WHITE);}
  private void ink(int color){p.setColor(color);p.setStyle(Paint.Style.FILL);}
  private void label(Canvas c,String s,float x,float y,int size){
    ink(MainActivity.INK);p.setTypeface(Typeface.DEFAULT_BOLD);p.setTextSize(size);c.drawText(s,x,y,p);
  }
  private void box(Canvas c,String s,float cx,float cy,float w,float h){
    ink(MainActivity.PALE);c.drawRoundRect(cx-w/2,cy-h/2,cx+w/2,cy+h/2,14,14,p);
    p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);p.setColor(MainActivity.BLUE);
    c.drawRoundRect(cx-w/2,cy-h/2,cx+w/2,cy+h/2,14,14,p);
    p.setStyle(Paint.Style.FILL);p.setTypeface(Typeface.DEFAULT_BOLD);p.setTextSize(16);
    c.drawText(s,cx-p.measureText(s)/2,cy+5,p);
  }
  private void link(Canvas c,float x,float y,float x2,float y2){
    p.setColor(MainActivity.MUTED);p.setStrokeWidth(2);c.drawLine(x,y,x2,y2,p);
    c.drawLine(x2,y2,x2-6,y2-9,p);c.drawLine(x2,y2,x2+6,y2-9,p);
  }
  @Override protected void onDraw(Canvas c){
    super.onDraw(c);c.save();
    float referenceHeight=mode==0?405f:mode==1?275f:mode==2?275f:275f;
    c.scale(getWidth()/360f,getHeight()/referenceHeight);
    if(mode==0){
      box(c,"Question of Being",180,39,250,54);
      link(c,180,67,180,105);
      box(c,"Analytic of Dasein",180,135,246,54);
      link(c,180,163,180,200);
      box(c,"Being-in-the-world",180,225,258,51);
      link(c,180,252,180,283);
      box(c,"Care",180,309,170,50);
      link(c,180,336,180,350);
      box(c,"Temporality",180,376,220,52);
    }else if(mode==1){
      label(c,"PRACTICAL INVOLVEMENT",24,35,18);
      ink(Color.rgb(161,115,70));
      c.drawRoundRect(30,155,327,171,2,2,p);
      c.drawRect(53,171,68,251,p);c.drawRect(289,171,304,251,p);
      ink(Color.rgb(72,78,83));c.drawRect(103,101,114,153,p);
      c.drawRoundRect(80,86,144,104,4,4,p);
      ink(Color.rgb(193,149,92));c.drawRect(204,111,270,154,p);
      if(intensity>45){
        p.setStrokeWidth(4);p.setColor(Color.rgb(180,52,51));
        c.drawLine(105,111,122,131,p);c.drawLine(122,131,108,150,p);
      }
      label(c,intensity>45?"Disruption exposes references":"Equipment recedes in use",23,67,15);
    }else if(mode==2){
      label(c,"ANXIETY & SIGNIFICANCE",22,34,17);
      float f=1-intensity/110f;
      for(int i=0;i<6;i++){
        ink(Color.argb(Math.max(15,(int)(255*f)),60+i*10,105+i*9,150));
        c.drawCircle(45+i*53,122+i%2*38,20+i%3*3,p);
      }
      ink(MainActivity.INK);c.drawCircle(179,215,14,p);c.drawRoundRect(167,230,191,255,6,6,p);
      label(c,intensity>55?"The familiar world recedes":"Everyday significance",23,67,15);
    }else if(mode==4){
      label(c,"PHILOSOPHICAL CONFRONTATION",25,32,16);
      ink(Color.rgb(234,244,248));c.drawRoundRect(22,46,338,202,18,18,p);
      ink(Color.rgb(36,87,105));
      c.drawCircle(180,108,49,p);
      ink(Color.WHITE);
      c.drawCircle(180,108,39,p);
      ink(Color.rgb(45,99,142));
      c.drawCircle(180,108,23,p);
      ink(Color.WHITE);
      p.setTypeface(Typeface.DEFAULT_BOLD);p.setTextSize(24);c.drawText("?",173,117,p);
      ink(Color.rgb(164,186,195));c.drawRoundRect(43,175,317,185,5,5,p);
      ink(Color.rgb(45,99,142));c.drawRoundRect(43,175,43+(274*intensity/100f),185,5,5,p);
      for(int i=0;i<4;i++){
        float x=58+i*81;
        ink(intensity>i*25?Color.rgb(45,99,142):Color.rgb(198,210,217));
        c.drawCircle(x,161,8,p);
        if(i<3){ink(Color.rgb(164,186,195));c.drawRect(x+10,159,x+72,162,p);}
      }
      label(c,"Premises assembled: "+(intensity/25)+"/4",80,214,13);
    }else{
      label(c,"ECSTATIC TEMPORALITY",30,31,18);
      ink(MainActivity.BLUE);
      p.setStrokeWidth(4);c.drawLine(82,120,175,172,p);c.drawLine(277,120,190,172,p);
      c.drawLine(181,220,181,177,p);
      box(c,"Future",77,106,107,44);
      box(c,"Having-been",280,106,143,44);
      box(c,"Present",180,237,122,44);
      ink(MainActivity.BLUE);c.drawCircle(181,172,13+intensity/10f,p);
      label(c,"Unity of care",125,268,15);
    }
    c.restore();
  }
}
