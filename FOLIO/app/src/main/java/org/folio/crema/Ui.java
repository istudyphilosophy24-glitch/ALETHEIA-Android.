package org.folio.crema;
import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
public final class Ui {
    static final int BLACK=Color.rgb(24,24,24),GRAY=Color.rgb(85,85,85),WHITE=Color.WHITE;
    private Ui(){}
    static int dp(Activity a,float n){return (int)(a.getResources().getDisplayMetrics().density*n+.5f);}
    static TextView text(Activity a,String s,int sp,boolean bold){
        TextView t=new TextView(a);t.setText(s);t.setTextColor(BLACK);t.setTextSize(sp);
        if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);t.setGravity(android.view.Gravity.CENTER_VERTICAL);return t;
    }
    static TextView action(Activity a,String label){
        TextView t=text(a,label,17,true);t.setGravity(android.view.Gravity.CENTER);t.setPadding(dp(a,12),dp(a,12),dp(a,12),dp(a,12));
        GradientDrawable d=new GradientDrawable();d.setColor(WHITE);d.setStroke(dp(a,1),GRAY);t.setBackground(d);return t;
    }
    static LinearLayout column(Activity a){LinearLayout l=new LinearLayout(a);l.setOrientation(LinearLayout.VERTICAL);l.setBackgroundColor(WHITE);return l;}
    static LinearLayout row(Activity a){LinearLayout l=new LinearLayout(a);l.setOrientation(LinearLayout.HORIZONTAL);return l;}
    static void separator(Activity a,LinearLayout l){View v=new View(a);v.setBackgroundColor(Color.rgb(210,210,210));l.addView(v,new LinearLayout.LayoutParams(-1,dp(a,1)));}
    static void margins(LinearLayout.LayoutParams p,Activity a,int l,int t,int r,int b){p.setMargins(dp(a,l),dp(a,t),dp(a,r),dp(a,b));}
}
