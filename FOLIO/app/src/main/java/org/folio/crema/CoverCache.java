package org.folio.crema;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import java.io.*;

/** Tiny, grayscale-friendly cover thumbnails. Never decode full-sized cover into the shelf. */
public final class CoverCache {
    private CoverCache(){}
    public static File target(Context context,String id){
        File dir=new File(context.getFilesDir(),"covers");
        if(!dir.exists())dir.mkdirs();
        return new File(dir,id+".jpg");
    }
    public static void create(Context context,File epubFile,String id){
        File output=target(context,id);
        if(output.isFile()&&output.length()>100)return;
        try(EpubBook book=new EpubBook(epubFile)){
            byte[] bytes=book.coverBytes();if(bytes==null||bytes.length==0)return;
            BitmapFactory.Options bounds=new BitmapFactory.Options();bounds.inJustDecodeBounds=true;
            BitmapFactory.decodeByteArray(bytes,0,bytes.length,bounds);
            if(bounds.outWidth<=0||bounds.outHeight<=0||bounds.outWidth>18000||bounds.outHeight>18000)return;
            int sample=1;
            while(bounds.outWidth/sample>260||bounds.outHeight/sample>390)sample*=2;
            BitmapFactory.Options opt=new BitmapFactory.Options();opt.inSampleSize=sample;opt.inPreferredConfig=Bitmap.Config.RGB_565;
            Bitmap bitmap=BitmapFactory.decodeByteArray(bytes,0,bytes.length,opt);
            if(bitmap==null)return;
            try(OutputStream out=new FileOutputStream(output)){
                bitmap.compress(Bitmap.CompressFormat.JPEG,78,out);
            } finally {bitmap.recycle();}
        }catch(Exception ignored){if(output.exists())output.delete();}
    }
    public static Bitmap decode(Context context,String id){
        File file=target(context,id);
        if(!file.isFile())return null;
        BitmapFactory.Options opt=new BitmapFactory.Options();opt.inPreferredConfig=Bitmap.Config.RGB_565;
        return BitmapFactory.decodeFile(file.getAbsolutePath(),opt);
    }
    public static void remove(Context context,String id){File f=target(context,id);if(f.exists())f.delete();}
}
