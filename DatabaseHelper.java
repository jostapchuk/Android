package com.nuebkitsune.fuelmeter.helper;

import java.util.ArrayList;
import java.util.List;
import com.nuebkitsune.fuelmeter.interfaces.IDatabaseHelperListener;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {
	public static final int DATABASE_VERSION = 2;
	public static final String DATABASE_NAME = "VehiclesDatabase.db";
	
	
	public static List<IDatabaseHelperListener> listeners = new ArrayList<IDatabaseHelperListener>();
	public void addListener(IDatabaseHelperListener l) {
		if(!listeners.contains(l))
			listeners.add(l);
		
	}
	
	public DatabaseHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}
	
	@Override
	public void onCreate(SQLiteDatabase db) {
		// TODO Auto-generated method stub
		for (IDatabaseHelperListener l : listeners)
			l.onCreate(db);
		
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// TODO Auto-generated method stub
		for (IDatabaseHelperListener l : listeners)
			l.onUpgrade(db, oldVersion, newVersion);
	}

}
