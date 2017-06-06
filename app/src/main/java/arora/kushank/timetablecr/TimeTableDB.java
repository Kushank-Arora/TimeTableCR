package arora.kushank.timetablecr;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.Vector;

import static java.util.Arrays.sort;

/**
 * Created by Kushank on 24-Feb-17.
 */

public class TimeTableDB {
    public static final String KEY_P_BRANCH = "branch";
    public static final String KEY_P_SEM = "sem";
    public static final String KEY_P_GROUP = "cgroup";
    public static final String KEY_P_DAY = "day";
    public static final String KEY_P_PER_NO = "per_no";
    public static final String KEY_P_SUB_ID = "sub_id";
    public static final String KEY_P_ACTIVE = "active";
    public static final String KEY_P_COURSE = "course";

    public static final String KEY_S_SUB_ID = "sub_id";
    public static final String KEY_S_SUB_NAME = "sub_name";
    public static final String KEY_S_SUB_TEACHER = "sub_teacher";
    public static final String KEY_S_SUB_DESC = "sub_desc";

    private static final String DATABASE_NAME = "det";
    private static final String DATABASE_TABLE_P = "periods";
    private static final String DATABASE_TABLE_S = "sub_details";
    private static final int DATABASE_VERSION = 1;

    private DbHelper ourHelper;
    private final Context ourContext;
    private SQLiteDatabase ourDatabase;

    private class DbHelper extends SQLiteOpenHelper {

        public DbHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE " + DATABASE_TABLE_P + " ("
                    + KEY_P_BRANCH + " VARCHAR(10), "
                    + KEY_P_SEM + " VARCHAR(10), "
                    + KEY_P_GROUP + " VARCHAR(10), "
                    + KEY_P_DAY + " VARCHAR(10), "
                    + KEY_P_PER_NO + " VARCHAR(10), "
                    + KEY_P_SUB_ID + " VARCHAR(10), "
                    + KEY_P_ACTIVE + " VARCHAR(10), "
                    + KEY_P_COURSE + " VARCHAR(10) "
                    + ");");
            db.execSQL("CREATE TABLE " + DATABASE_TABLE_S + " ("
                    + KEY_S_SUB_ID + " VARCHAR(10), "
                    + KEY_S_SUB_NAME + " VARCHAR(10), "
                    + KEY_S_SUB_TEACHER + " VARCHAR(10), "
                    + KEY_S_SUB_DESC + " VARCHAR(10) "
                    + ");");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE_P);
            db.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE_S);
            onCreate(db);
        }
    }

    public TimeTableDB(Context c) {
        ourContext = c;
    }

    public TimeTableDB open() {
        ourHelper = new DbHelper(ourContext);
        ourDatabase = ourHelper.getWritableDatabase();
        return this;
    }

    public void delTables() {
        ourDatabase.delete(DATABASE_TABLE_P, null, null);
        ourDatabase.delete(DATABASE_TABLE_S, null, null);
    }

    public void close() {
        ourHelper.close();
    }

    public long createEntryPer(String branch, String sem, String group, String day, String per_no, String sub_id, String active, String course) {
        ContentValues cv = new ContentValues();
        cv.put(KEY_P_BRANCH, branch);
        cv.put(KEY_P_SEM, sem);
        cv.put(KEY_P_GROUP, group);
        cv.put(KEY_P_DAY, day);
        cv.put(KEY_P_PER_NO, per_no);
        cv.put(KEY_P_SUB_ID, sub_id);
        cv.put(KEY_P_ACTIVE, active);
        cv.put(KEY_P_COURSE, course);
        return ourDatabase.insert(DATABASE_TABLE_P, null, cv);
    }

    public long createEntrySub(String sub_id, String sub_name, String sub_teacher, String sub_desc) {
        ContentValues cv = new ContentValues();
        cv.put(KEY_S_SUB_ID, sub_id);
        cv.put(KEY_S_SUB_NAME, sub_name);
        cv.put(KEY_S_SUB_TEACHER, sub_teacher);
        cv.put(KEY_S_SUB_DESC, sub_desc);
        return ourDatabase.insert(DATABASE_TABLE_S, null, cv);
    }

    public boolean isPeriodGroup(int per_no, int weekday, Batch b) {
        Cursor c = ourDatabase.rawQuery("SELECT DISTINCT COUNT(*) as NEW_COL FROM " + DATABASE_TABLE_P + " WHERE " + KEY_P_BRANCH + "='" + b.branch + "' and " +
                KEY_P_COURSE + "='" + b.course + "' and " +
                KEY_P_SEM + "='" + b.semester + "' and " + KEY_P_GROUP + "!='0' and " + KEY_P_DAY + "='" + weekday + "' and " + KEY_P_PER_NO + "='" + per_no + "'", null);
        c.moveToFirst();
        String result = c.getString(c.getColumnIndex("NEW_COL"));
        c.close();
        return !result.equals("0");
    }

    public String getSubIdForPeriod(int per_no, int weekday, Batch b) {
        Cursor c = ourDatabase.rawQuery("SELECT DISTINCT " + DATABASE_TABLE_P + "." + KEY_P_SUB_ID + " FROM " + DATABASE_TABLE_P + " WHERE " + KEY_P_BRANCH + "='" + b.branch + "' and " +
                KEY_P_SEM + "='" + b.semester + "' and " +
                KEY_P_COURSE + "='" + b.course + "' and " + KEY_P_GROUP + "='" + b.group + "' and " + KEY_P_DAY + "='" + weekday + "' and " + KEY_P_PER_NO + "='" + per_no + "'", null);
        //Cursor c = ourDatabase.query(DATABASE_TABLE_S, columns, null, null, null, null, null);
        String result = null;

        int iSubId = c.getColumnIndex(KEY_S_SUB_ID);


        for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {

            if (result == null)
                result = c.getString(iSubId);
            else
                result += "/" + c.getString(iSubId);
        }

        c.close();

        return result;
    }

    public Subject getSubFromId(String sub_id) {


        String[] columns = new String[]{KEY_S_SUB_NAME, KEY_S_SUB_TEACHER, KEY_S_SUB_DESC};
        Cursor c;
        if (sub_id.contains("/")) {
            String list[] = sub_id.split("/");
            c = ourDatabase.query(true, DATABASE_TABLE_S, columns, KEY_S_SUB_ID + "='" + list[0] + "' or " + KEY_S_SUB_ID + "='" + list[1] + "'", null, null, null, null, null);

        } else {
            c = ourDatabase.query(true, DATABASE_TABLE_S, columns, KEY_S_SUB_ID + "='" + sub_id + "'", null, null, null, null, null);
        }
        int iSubName = c.getColumnIndex(KEY_S_SUB_NAME);
        int iSubTeacher = c.getColumnIndex(KEY_S_SUB_TEACHER);
        int iSubDesc = c.getColumnIndex(KEY_S_SUB_DESC);

        Subject result = new Subject();

        result.description = null;
        result.name = null;
        result.teacher = null;
        result.id = null;

        for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
            if (result.description == null)
                result.description = c.getString(iSubDesc);
            else
                result.description += "/" + c.getString(iSubDesc);

            if (result.name == null)
                result.name = c.getString(iSubName);
            else
                result.name += "/" + c.getString(iSubName);

            if (result.teacher == null)
                result.teacher = c.getString(iSubTeacher);
            else
                result.teacher += "/" + c.getString(iSubTeacher);

            if (result.id == null)
                result.id = sub_id;
            else
                result.id += "/" + sub_id;

        }
        c.close();
        return result;
    }

    public Boolean getActive(int per_no, int weekday, Batch b) {

        String[] columns = new String[]{KEY_P_ACTIVE};
        Cursor c = ourDatabase.query(true, DATABASE_TABLE_P, columns, KEY_P_BRANCH + "='" + b.branch
                + "' and " + KEY_P_SEM + "='" + b.semester
                + "' and " + KEY_P_COURSE + "='" + b.course
                + "' and " + KEY_P_GROUP + "='" + b.group
                + "' and " + KEY_P_DAY + "='" + weekday
                + "' and " + KEY_P_PER_NO + "='" +
                per_no + "'", null, null, null, null, null);
        if (c != null) {
            String result;
            c.moveToFirst();
            result = c.getString(c.getColumnIndex(KEY_P_ACTIVE));
            c.close();
            return result.equals("1");
        }
        return null;
    }

    public void modifyPeriod(String per_no, String weekday, Batch b, String sub_id, String active) {
        ContentValues cv = new ContentValues();
        cv.put(KEY_P_SUB_ID, sub_id);
        cv.put(KEY_P_ACTIVE, active);
        ourDatabase.update(DATABASE_TABLE_P, cv, KEY_P_BRANCH + "='" + b.branch
                + "' and " + KEY_P_SEM + "='" + b.semester
                + "' and " + KEY_P_COURSE + "='" + b.course
                + "' and " + KEY_P_GROUP + "='" + b.group
                + "' and " + KEY_P_DAY + "='" + weekday
                + "' and " + KEY_P_PER_NO + "='" +
                per_no + "'", null);
    }

    public void modifySubject(String sub_id, String sub_teacher, String sub_name, String desc) {
        Subject res = getSubFromId(sub_id);
        if (res.name == null) {
            ContentValues cv = new ContentValues();
            cv.put(KEY_S_SUB_ID, sub_id);
            cv.put(KEY_S_SUB_TEACHER, sub_teacher);
            cv.put(KEY_S_SUB_NAME, sub_name);
            cv.put(KEY_S_SUB_DESC, desc);
            ourDatabase.insert(DATABASE_TABLE_S, null, cv);

        } else {
            ContentValues cv = new ContentValues();
            cv.put(KEY_S_SUB_TEACHER, sub_teacher);
            cv.put(KEY_S_SUB_NAME, sub_name);
            cv.put(KEY_S_SUB_DESC, desc);

            ourDatabase.update(DATABASE_TABLE_S, cv, KEY_S_SUB_ID + "='" + sub_id + "'", null);
        }

    }

    public void deleteSubject(String sub_id) {
        ourDatabase.delete(DATABASE_TABLE_S, KEY_S_SUB_ID + "=" + sub_id, null);
    }

    public String[][] getCourseBranch() {
        String[] columns = new String[]{KEY_P_COURSE, KEY_P_BRANCH};
        Cursor c = ourDatabase.query(true, DATABASE_TABLE_P, columns, null, null, null, null, null, null);
        String result[][] = new String[50][2];

        int iCourse = c.getColumnIndex(KEY_P_COURSE);
        int iBranch = c.getColumnIndex(KEY_P_BRANCH);

        int count = 0;

        for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
            result[count][0] = c.getString(iCourse);
            result[count][1] = c.getString(iBranch);
            count++;
        }
        c.close();

        String finalResult[][] = new String[count][2];
        System.arraycopy(result, 0, finalResult, 0, count);

        return finalResult;
    }

    public String[] getSubjects(Batch b) {

        String[] columns = new String[]{KEY_P_SUB_ID};
        Cursor c = ourDatabase.query(true, DATABASE_TABLE_P, columns, KEY_P_BRANCH + "='" + b.branch + "' and " +
                KEY_P_COURSE + "='" + b.course + "' and " + KEY_P_SEM + "='" + b.semester + "'", null, null, null, null, null);
        String result[] = new String[50];

        int iSubId = c.getColumnIndex(KEY_P_SUB_ID);

        int count = 0;

        for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
            result[count] = c.getString(iSubId);
            count++;
        }
        c.close();

        String finalResult[] = new String[count];
        System.arraycopy(result, 0, finalResult, 0, count);

        return finalResult;
    }

    public String[] getSemForCourseBranch(String course, String branch) {
        String[] columns = new String[]{KEY_P_SEM};
        Cursor c = ourDatabase.query(true, DATABASE_TABLE_P, columns, KEY_P_COURSE + "='" + course + "' and " + KEY_P_BRANCH + "='" + branch + "'", null, null, null, null, null);
        String result[] = new String[50];

        int iSem = c.getColumnIndex(KEY_P_SEM);

        int count = 0;

        for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
            result[count] = c.getString(iSem);
            count++;
        }
        c.close();

        String finalResult[] = new String[count];
        System.arraycopy(result, 0, finalResult, 0, count);
        sort(finalResult);
        return finalResult;
    }

    public String getTeacherForSubject(String sub) {
        String[] columns = new String[]{KEY_S_SUB_TEACHER};
        Cursor c;
        c = ourDatabase.query(true, DATABASE_TABLE_S, columns, KEY_S_SUB_NAME + "='" + sub + "'", null, null, null, null, null);

        int iSubTeacher = c.getColumnIndex(KEY_S_SUB_TEACHER);

        String result=null;

        if (c != null) {
            c.moveToFirst();
            result = c.getString(iSubTeacher);
            c.close();
        }
        return result;
    }
}
