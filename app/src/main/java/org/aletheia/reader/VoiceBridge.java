package org.aletheia.reader;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.widget.Toast;
import java.util.List;
import java.util.Collections;
import java.util.Locale;

/** Explicit Android TTS lifecycle plus external reader handoff. */
public final class VoiceBridge {
  private final Activity context;
  private TextToSpeech service;
  private volatile boolean ready=false;
  private volatile String status="Speech engine initializing";
  private int generation=0;
  private String preferred=null;
  private float speed=.95f;
  private boolean leftForSettings=false;
  public VoiceBridge(Activity a){context=a;}
  public void init(String engine){
    generation++;
    final int gen=generation;
    TextToSpeech old=service;
    service=null;
    ready=false;
    status="Speech engine initializing…";
    preferred=engine;
    if(old!=null){old.stop();old.shutdown();}
    try{
      TextToSpeech.OnInitListener callback=result->context.runOnUiThread(()->{
        if(gen!=generation)return;
        if(result!=TextToSpeech.SUCCESS||service==null){
          ready=false;status="Engine unavailable. Try Android default or the ElevenReader handoff.";
          return;
        }
        try{
          int language=service.setLanguage(Locale.ENGLISH);
          service.setSpeechRate(speed);
          ready=language!=TextToSpeech.LANG_MISSING_DATA&&language!=TextToSpeech.LANG_NOT_SUPPORTED;
          status=ready?"Ready · "+service.getCurrentEngine():"English voice data missing for this engine";
          service.setOnUtteranceProgressListener(new UtteranceProgressListener(){
            @Override public void onStart(String id){}
            @Override public void onDone(String id){}
            @Override public void onError(String id){
              context.runOnUiThread(()->{
                status="Speech failed in the installed TTS engine. Try another engine.";
                Toast.makeText(context,status,Toast.LENGTH_LONG).show();
              });
            }
          });
        }catch(Exception e){ready=false;status="Speech engine error: "+e.getClass().getSimpleName();}
      });
      service=engine==null?new TextToSpeech(context,callback):new TextToSpeech(context,callback,engine);
    }catch(Exception e){ready=false;status="Cannot create Android speech service: "+e.getClass().getSimpleName();}
  }
  public boolean ready(){return ready;}
  public String status(){return status;}
  public String engine(){return preferred==null?"Android system default":preferred;}
  public List<TextToSpeech.EngineInfo> engines(){
    try{return service!=null?service.getEngines():Collections.emptyList();}
    catch(Exception e){return Collections.emptyList();}
  }
  public void speed(float value){
    speed=Math.max(.55f,Math.min(1.9f,value));
    if(ready&&service!=null)service.setSpeechRate(speed);
  }
  public boolean speak(String text){
    if(text==null||text.trim().isEmpty()){
      Toast.makeText(context,"Open text in the Reader first, or narrate a study section.",Toast.LENGTH_LONG).show();
      return false;
    }
    if(!ready||service==null){
      Toast.makeText(context,status,Toast.LENGTH_LONG).show();
      return false;
    }
    try{
      service.stop();
      int total=0,pos=0;
      while(pos<text.length()){
        int end=Math.min(pos+2600,text.length());
        if(end<text.length()){
          int point=text.lastIndexOf(' ',end);
          if(point>pos+900)end=point;
        }
        String chunk=text.substring(pos,end).trim();
        if(chunk.length()>0){
          int rc=service.speak(chunk,total==0?TextToSpeech.QUEUE_FLUSH:TextToSpeech.QUEUE_ADD,
             null,"aletheia-"+generation+"-"+total);
          if(rc==TextToSpeech.ERROR){
            status="Speech engine rejected the narration request.";
            Toast.makeText(context,status,Toast.LENGTH_LONG).show();
            return false;
          }
          total++;
        }
        pos=end;
      }
      return true;
    }catch(Exception e){
      status="Audio error: "+e.getClass().getSimpleName();
      Toast.makeText(context,status,Toast.LENGTH_LONG).show();
      return false;
    }
  }
  public void stop(){if(service!=null)service.stop();}
  public void settings(){
    leftForSettings=true;
    try{context.startActivity(new Intent("com.android.settings.TTS_SETTINGS"));}
    catch(Exception e){
      try{context.startActivity(new Intent(Settings.ACTION_SETTINGS));}
      catch(Exception x){Toast.makeText(context,"Open Android Settings → Text-to-speech output.",Toast.LENGTH_LONG).show();}
    }
  }
  public boolean returnedFromSettings(){
    if(!leftForSettings)return false;
    leftForSettings=false;
    return true;
  }
  public void share(String text){
    if(text==null||text.trim().isEmpty()){
      Toast.makeText(context,"Nothing to send. Open a passage or study section.",Toast.LENGTH_LONG).show();
      return;
    }
    Intent i=new Intent(Intent.ACTION_SEND);
    i.setType("text/plain");
    i.putExtra(Intent.EXTRA_TEXT,text);
    i.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
    try{context.startActivity(Intent.createChooser(i,"Open passage in a compatible reader"));}
    catch(Exception e){Toast.makeText(context,"No compatible sharing application found.",Toast.LENGTH_LONG).show();}
  }
  public void handoffToElevenReader(String text){
    if(text==null||text.trim().isEmpty()){
      Toast.makeText(context,"Choose a passage or study section first.",Toast.LENGTH_LONG).show();
      return;
    }
    // An installed app may accept TEXT sharing without exposing a system TTS service.
    Intent target=new Intent(Intent.ACTION_SEND);
    target.setType("text/plain");
    target.putExtra(Intent.EXTRA_TEXT,text);
    target.setPackage("io.elevenlabs.readerapp");
    if(target.resolveActivity(context.getPackageManager())!=null){
      try{context.startActivity(target);return;}
      catch(Exception ignored){}
    }
    Intent app=context.getPackageManager().getLaunchIntentForPackage("io.elevenlabs.readerapp");
    if(app!=null){
      // Launch application and put selected text on clipboard for its paste/import flow.
      android.content.ClipboardManager clipboard=(android.content.ClipboardManager)
         context.getSystemService(Activity.CLIPBOARD_SERVICE);
      clipboard.setPrimaryClip(android.content.ClipData.newPlainText("ALETHEIA passage",text));
      context.startActivity(app);
      Toast.makeText(context,"Passage copied. Use Paste or Import in ElevenReader.",Toast.LENGTH_LONG).show();
    }else{
      share(text);
    }
  }
  public void destroy(){
    generation++;
    if(service!=null){service.stop();service.shutdown();service=null;}
    ready=false;
  }
}
