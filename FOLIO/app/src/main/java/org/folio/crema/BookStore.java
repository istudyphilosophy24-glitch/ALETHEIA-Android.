package org.folio.crema;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;

/** Private, offline SQLite library. Books are copied into the application's own files area. */
public final class BookStore extends SQLiteOpenHelper {
    public static final class Book {
        public String id, title, author, path, format;
        public int chapter, offset;
        public long opened;
    }
    public static final class Mark {
        public int id, chapter, offset;
        public String label;
    }
    public BookStore(Context c) { super(c, "folio.db", null, 1); }
    @Override public void onCreate(SQLiteDatabase d) {
        d.execSQL("CREATE TABLE books(id TEXT PRIMARY KEY,title TEXT NOT NULL,author TEXT NOT NULL,path TEXT NOT NULL,format TEXT NOT NULL,chapter INTEGER DEFAULT 0,offset INTEGER DEFAULT 0,opened INTEGER DEFAULT 0)");
        d.execSQL("CREATE TABLE marks(id INTEGER PRIMARY KEY AUTOINCREMENT,book_id TEXT NOT NULL,chapter INTEGER NOT NULL,offset INTEGER NOT NULL,label TEXT NOT NULL,created INTEGER NOT NULL)");
        d.execSQL("CREATE INDEX marks_book ON marks(book_id)");
    }
    @Override public void onUpgrade(SQLiteDatabase d,int oldV,int newV) { }
    private static Book read(Cursor c) {
        Book b=new Book();b.id=c.getString(0);b.title=c.getString(1);b.author=c.getString(2);b.path=c.getString(3);
        b.format=c.getString(4);b.chapter=c.getInt(5);b.offset=c.getInt(6);b.opened=c.getLong(7);return b;
    }
    public ArrayList<Book> books() {
        ArrayList<Book> list=new ArrayList<>();Cursor c=getReadableDatabase().rawQuery("SELECT id,title,author,path,format,chapter,offset,opened FROM books ORDER BY opened DESC,title COLLATE NOCASE",null);
        try {while(c.moveToNext())list.add(read(c));}finally{c.close();}return list;
    }
    public Book byId(String id) {
        Cursor c=getReadableDatabase().rawQuery("SELECT id,title,author,path,format,chapter,offset,opened FROM books WHERE id=?",new String[]{id});
        try{return c.moveToFirst()?read(c):null;}finally{c.close();}
    }
    public void upsert(Book b){
        SQLiteDatabase d=getWritableDatabase();ContentValues v=new ContentValues();
        v.put("id",b.id);v.put("title",b.title);v.put("author",b.author);v.put("path",b.path);v.put("format",b.format);
        d.insertWithOnConflict("books",null,v,SQLiteDatabase.CONFLICT_IGNORE);
    }
    public void position(String id,int chapter,int offset) {
        ContentValues v=new ContentValues();v.put("chapter",chapter);v.put("offset",offset);v.put("opened",System.currentTimeMillis());
        getWritableDatabase().update("books",v,"id=?",new String[]{id});
    }
    public void addMark(String id,int chapter,int offset,String label) {
        ContentValues v=new ContentValues();v.put("book_id",id);v.put("chapter",chapter);v.put("offset",offset);
        v.put("label",label);v.put("created",System.currentTimeMillis());getWritableDatabase().insert("marks",null,v);
    }
    public ArrayList<Mark> marks(String id) {
        ArrayList<Mark> result=new ArrayList<>();Cursor c=getReadableDatabase().rawQuery("SELECT id,chapter,offset,label FROM marks WHERE book_id=? ORDER BY created DESC",new String[]{id});
        try {while(c.moveToNext()){Mark m=new Mark();m.id=c.getInt(0);m.chapter=c.getInt(1);m.offset=c.getInt(2);m.label=c.getString(3);result.add(m);}}finally{c.close();}
        return result;
    }
    public void deleteBook(Book b) {
        SQLiteDatabase d=getWritableDatabase();d.beginTransaction();try {d.delete("marks","book_id=?",new String[]{b.id});d.delete("books","id=?",new String[]{b.id});d.setTransactionSuccessful();}finally{d.endTransaction();}
        new java.io.File(b.path).delete();
    }
}
