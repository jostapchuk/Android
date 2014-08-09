// Copyright (c) Jefferey Ostapchuk 2014
package com.nuebkitsune.fuelmeter.model;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;

import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import com.nuebkitsune.fuelmeter.classes.KeyValuePair;
import com.nuebkitsune.fuelmeter.helper.DatabaseHelper;
import com.nuebkitsune.fuelmeter.interfaces.IDatabaseHelperListener;

public abstract class DataAdapter implements IDatabaseHelperListener {
	protected DatabaseHelper db;

	protected String TABLE_NAME = "";
//	protected Map<String, String> TABLE_COLUMNS = new HashMap<String, String>();
	protected List<KeyValuePair> TABLE_COLUMNS = new ArrayList<KeyValuePair>();
	
	public static final String COLUMN_ID = "_id";
	
	public final static String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";
	public final static SimpleDateFormat FormatDateTime = new SimpleDateFormat(DATE_FORMAT);

	public void onCreate(SQLiteDatabase db) {
		ArrayList<String> columns = new ArrayList<String>();
		for (KeyValuePair column : TABLE_COLUMNS) {
			columns.add(column.getKey() + " " + column.getValue());
		}
		this.createTable(db, TABLE_NAME, columns.toArray(new String[] {}));
	}
	public void createTable(SQLiteDatabase db, String tableName, String[] tableColumns) {
		String sql = "CREATE TABLE " + tableName + " (" + TextUtils.join(", ", tableColumns) + ")";
		db.execSQL(sql);
	}
	public void renameTable(SQLiteDatabase db, String tableName) {
		String sql = "ALTER TABLE " + TABLE_NAME + " RENAME TO " + tableName;
		db.execSQL(sql);
	}
	
	public void renameColumns(SQLiteDatabase db, Hashtable<String,String> oldTable, String[] oldColumnNames, String[] newColumnName[]) {
		
	}
	public void modifyColumn(SQLiteDatabase db, String oldColumnName, String newColumnName, String[] newColumnConstraints) {
		
		// get current table columns
		String sql = String.format("PRAGMA table_info('%s')", TABLE_NAME);
		Cursor cursor = db.rawQuery(sql, new String[] {});
		
		int length = cursor.getCount();
		String[] oC = new String[length], nC = new String[length], c = new String[length];
		
		while(cursor.moveToNext()){
			int cid = cursor.getInt(cursor.getColumnIndex("cid"));
			String name = cursor.getString(cursor.getColumnIndex("name"));
			String type = cursor.getString(cursor.getColumnIndex("type"));
			int notnull = cursor.getInt(cursor.getColumnIndex("notnull"));
			String dflt = cursor.getString(cursor.getColumnIndex("dflt_value"));
			int pk = cursor.getInt(cursor.getColumnIndex("pk"));
			
			Vector<String> constraints = new Vector<String>();
			
			// add data type to field
			constraints.add(type);
			
			// add primary key option to field
			if(pk > 0)
				constraints.add("PRIMARY KEY");
			
			// add not null option to field
			if(notnull > 0)
				constraints.add("NOT NULL");
			
			// add default value to field
			if(dflt != null)
				constraints.add("DEFAULT " + dflt);
			
			String cName = name;
			String cType = TextUtils.join(" ", constraints.toArray());
			
			oC[cid] = cName;
			cName = (cName.equals(oldColumnName))? newColumnName : cName;
			nC[cid] = cName;
			
			if(newColumnConstraints != null)
				cType = TextUtils.join(" ", newColumnConstraints);
			
			c[cid] = cName + " " + cType;
		}
		cursor.close();
		
		// rename old table
		this.renameTable(db, "tmp_" + TABLE_NAME);
		
		// create new table with modified column
		this.createTable(db, TABLE_NAME, c);

		// copy data from temporary table to new table
		sql = "INSERT INTO " + TABLE_NAME + " (" + TextUtils.join(", ", nC) + 
				") SELECT " + TextUtils.join(", ", oC) + " FROM tmp_" + TABLE_NAME;
		db.execSQL(sql);
		
		// drop temporary table
		db.execSQL("DROP TABLE tmp_" + TABLE_NAME);
	}
	
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// if table not exist, create it
		if(!exists(db))
			onCreate(db);
	}
	
	public boolean exists(SQLiteDatabase db) {
		String sql = "SELECT name FROM sqlite_master WHERE type=? AND name=?";
		Cursor cursor = db.rawQuery(sql, new String[] {"table", TABLE_NAME});
		return cursor.getCount() > 0;
	}
	public boolean exists(SQLiteDatabase db, String tableName) {
		String sql = "SELECT name FROM sqlite_master WHERE type=? AND name=?";
		Cursor cursor = db.rawQuery(sql, new String[] {"table", tableName});
		return cursor.getCount() > 0;
	}
	
	public Cursor getCursor(Map<String, String> selectionArgs) {
		
		String sql = "SELECT * FROM " + TABLE_NAME;
		
		if(selectionArgs.size() > 0) {
			List<String> whereFields = new ArrayList<String>();
			for (String iterable_element : selectionArgs.keySet()) {
				whereFields.add(iterable_element + " = ?"); 
			}
			sql += " WHERE " + TextUtils.join(", ", whereFields.toArray(new String[] {}));
		}
		
		return db.getReadableDatabase().rawQuery(sql, selectionArgs.values().toArray(new String[] {}));
	}
	public Cursor getCursor(Map<String,String> queryArgs, Map<String,String> selectionArgs, Map<String,String> groupArgs) {
		return null;
	}
	public Cursor getCursor(Map<String,String> queryArgs, Map<String,String> selectionArgs, boolean includeDistinct, boolean includeIdColumn, boolean includeAsterisk) {
		
		String sql = "SELECT ";
		
		if(includeDistinct)
			sql += "DISTINCT ";
		else if(includeIdColumn)
			sql += COLUMN_ID + ", ";
		
		if(queryArgs.size() > 0) {
			List<String> queryFields = new ArrayList<String>();
			for (Entry<String, String> iterable_element : queryArgs.entrySet()) {
				String key = iterable_element.getKey(), val = iterable_element.getValue();
				queryFields.add(((key == val)|(val.length() == 0))? key : val + " AS " + key); 
			}
			sql += TextUtils.join(", ", queryFields.toArray(new String[] {}));
		}
		
		if(includeAsterisk)
			sql += ", *";
		
		sql += " FROM " + TABLE_NAME;
		
		if(selectionArgs.size() > 0) {
			List<String> whereFields = new ArrayList<String>();
			for (String iterable_element : selectionArgs.keySet()) {
				whereFields.add(iterable_element + " = ?"); 
			}
			sql += " WHERE " + TextUtils.join(", ", whereFields.toArray(new String[] {}));
		}
		
		return db.getReadableDatabase().rawQuery(sql, selectionArgs.values().toArray(new String[] {}));
	}
	
	public static String getColumnString(Cursor cursor, String column) {
		return cursor.getString(cursor.getColumnIndex(column));
	}
	public static int getColumnInteger(Cursor cursor, String column) {
		return cursor.getInt(cursor.getColumnIndex(column));
	}
	
	public long getCount() {
		return DatabaseUtils.queryNumEntries(db.getReadableDatabase(), TABLE_NAME);
	}
	
	public void getIds() {
		
	}
	
	public void delete(DataModel m) {
		delete(String.format("%s = ?", COLUMN_ID), new String[] {String.valueOf(m.id)});
	}
	public void delete(String whereClause, String[] whereArgs) {
		db.getWritableDatabase().delete(TABLE_NAME, whereClause, whereArgs);
	}
	
	public DataAdapter(DatabaseHelper dbh) {
		this.db = dbh;
		this.db.addListener(this);
	}
//	public String[] getColumnNames() {
//		List<String> names = new ArrayList<String>();
//		
//		for (Map.Entry<String, String> column : TABLE_COLUMS.entrySet())
//			names.add(column.getKey());
//					
//		
//		return (String[])names.toArray();
//	}
//	
//	public String getColumnName(Integer columnIndex) {
//		return getColumnNames()[columnIndex];
//	}
//	
//	public Integer getColumnIndex(String columnName) {
//		String[] columns = getColumnNames();
//		for (int x = 0; x < columns.length; x++)
//			if (columns[x] == columnName)
//				return x;
//		
//		return -1;
//	}
	
	public String getTableName() {
		return TABLE_NAME;
	}
	
}
