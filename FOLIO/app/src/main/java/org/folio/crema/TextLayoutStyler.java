package org.folio.crema;

import android.graphics.Paint;
import android.text.*;
import android.text.style.*;

/** Optional paragraph indentation/spacing while retaining EPUB inline formatting spans. */
final class TextLayoutStyler {
    private TextLayoutStyler(){}
    static CharSequence style(CharSequence source,int indentPx,int gapPx,boolean txt){
        if(source==null||source.length()==0)return "";
        SpannableStringBuilder text=new SpannableStringBuilder(source);
        // Html.fromHtml puts blank lines between block paragraphs. Replace duplicate breaks
        // with a single paragraph break; precise additional space is supplied by a span.
        // Preserve soft line breaks inside blocks.
        for(int i=text.length()-1;i>0;i--){
            if(text.charAt(i)=='\n'&&text.charAt(i-1)=='\n')text.delete(i,i+1);
        }
        int start=0,lim=text.length();
        while(start<lim){
            int end=start;
            while(end<lim&&text.charAt(end)!='\n')end++;
            int to=end<lim?end+1:lim;
            // Ignore empty paragraph markers and headings styled by Android's HTML parser.
            if(end>start&&hasVisibleText(text,start,end)){
                boolean heading=false;
                RelativeSizeSpan[] sizes=text.getSpans(start,Math.min(end,start+1),RelativeSizeSpan.class);
                for(RelativeSizeSpan span:sizes){if(span.getSizeChange()>=1.14f){heading=true;break;}}
                if(indentPx>0&&!heading)
                    text.setSpan(new LeadingMarginSpan.Standard(indentPx,0),start,to,Spanned.SPAN_PARAGRAPH);
                if(gapPx>0&&to<lim)
                    text.setSpan(new Gap(gapPx,to),start,to,Spanned.SPAN_PARAGRAPH);
            }
            if(to>=lim)break;
            start=to;
        }
        return text;
    }
    private static boolean hasVisibleText(CharSequence t,int start,int end){
        for(int i=start;i<end;i++)if(!Character.isWhitespace(t.charAt(i)))return true;
        return false;
    }
    private static final class Gap implements LineHeightSpan {
        private final int pixels,end;
        Gap(int gap,int end){this.pixels=gap;this.end=end;}
        @Override public void chooseHeight(CharSequence text,int start,int lineEnd,int spanstartv,int v,Paint.FontMetricsInt fm){
            if(lineEnd>=end && start<end){fm.descent+=pixels;fm.bottom+=pixels;}
        }
    }
}
