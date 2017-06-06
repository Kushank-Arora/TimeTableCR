package arora.kushank.timetablecr;


import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.TimeZone;

public class MainActivity extends AppCompatActivity {

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;
    private String fileNameSharedPref;
    static SharedPreferences someData;
    static Period[][] period;
    static Batch batch;
    final static String url="http://ktimetable.tk/firstUpdate.php";
    final static int requestIdChangePeriod=1024;
    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;
    private SwipeRefreshLayout refreshLayout;

    protected void instantiateViews() {
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        if (mViewPager != null) {
            mViewPager.setAdapter(mSectionsPagerAdapter);
        }

    }

    void bringToday(){
        Calendar cal = Calendar.getInstance();
        cal.setTimeZone(TimeZone.getDefault());
        //One Based Indexing
        int day = (cal.get(Calendar.DAY_OF_WEEK) - 1 + 6) % 7 + 1;
        int hour = cal.get(Calendar.HOUR_OF_DAY);

        if (hour >= 18)
            day += 1;

        if (day <= 5)
            mViewPager.setCurrentItem(day - 1, true);
        else
            mViewPager.setCurrentItem(0, true);
    }

    void loadDatabaseToArray(Context con) {
        period = new Period[5][9];
        Batch temp=new Batch(batch.branch,batch.semester,"0",batch.course);

        try {
            TimeTableDB db = new TimeTableDB(con);
            db.open();
            for (int week = 0; week < 5; week++)
                for (int p = 0; p < 9; p++)
                {
                    try {
                        if (db.isPeriodGroup(p, week, batch)) {
                            String s_id = db.getSubIdForPeriod(p, week, batch);
                            period[week][p] = new Period();
                            period[week][p].subject = db.getSubFromId(s_id);
                            period[week][p].active = db.getActive(p, week, batch);
                        } else {
                            String s_id = db.getSubIdForPeriod(p, week, temp);
                            period[week][p] = new Period();
                            period[week][p].subject = db.getSubFromId(s_id);
                            period[week][p].active = db.getActive(p, week, temp);
                        }
                    }catch (Exception e)
                    {
                        e.printStackTrace();
                        period[week][p] = new Period();
                    }
                }

        } catch (Exception e) {
            System.out.println(e);
            Toast.makeText(getApplicationContext(),e.toString()+"",Toast.LENGTH_SHORT).show();
            for (int week = 0; week < 5; week++)
                for (int p = 0; p < 9; p++) {
                    period[week][p]=new Period();
                }
        }
        mViewPager = (ViewPager) findViewById(R.id.container);
        if (mViewPager != null) {
            mViewPager.setAdapter(mSectionsPagerAdapter);
        }
    }

    void initBatch()
    {
        boolean isGroup1=someData.getBoolean("group1", true);
        batch=new Batch();
        batch.branch=someData.getString("branch","CE");
        if(isGroup1)
            batch.group="1";
        else
            batch.group="2";
        batch.semester=someData.getString("sem","6");
        batch.course=someData.getString("course","BTech");
    }

    void loadDBFromSite(ArrayList<Subject> listSubject, ArrayList<DBPeriod> listPeriod)
    {

        TimeTableDB db=new TimeTableDB(this);
        db.open();
        try {
            db.delTables();
        }catch (Exception e)
        {
            e.printStackTrace();
            Toast.makeText(this,"Can't Delete earlier version of Database",Toast.LENGTH_SHORT).show();
        }

        for(Subject s:listSubject){
            System.out.println(s);
            try {
                db.createEntrySub(s.id, s.name, s.teacher, s.description);
            }catch(Exception e){
                e.printStackTrace();
                Toast.makeText(this,e.toString(),Toast.LENGTH_SHORT).show();
            }
        }
        for(DBPeriod p:listPeriod){
            try {
                db.createEntryPer(p.branch, p.sem, p.group, p.day, p.per_no, p.sub_id, p.active,p.course);
            }catch(Exception e){
                e.printStackTrace();
                Toast.makeText(this,e.toString(),Toast.LENGTH_SHORT).show();
            }
        }
        db.close();
        Toast.makeText(this,"Sync Complete",Toast.LENGTH_SHORT).show();
        loadDatabaseToArray(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Set file name for the preferences which currently stores:
        //1. The Group

        fileNameSharedPref = "TIMETABLEPREF";
        someData = getSharedPreferences(fileNameSharedPref, MODE_PRIVATE);

        //Instantiate Batch uses preferences
        initBatch();

        //Load Database to the period Array for Display
        loadDatabaseToArray(this);

        //Instantiate the Fragments and the ViewPagers(ListAdapters,etc).
        //Set the Current Tab based upon the current day and time.
        instantiateViews();
        bringToday();

        //Link the Toolbar with the concerned resource file.
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //Set the Floating Action Button and Its Task.
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        assert fab != null;
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Calendar cal = Calendar.getInstance();
                cal.setTimeZone(TimeZone.getDefault());
                //One Based Indexing
                int day = (cal.get(Calendar.DAY_OF_WEEK) - 1 + 6) % 7 + 1;
                int hour = cal.get(Calendar.HOUR_OF_DAY);

                if (hour >= 18)
                    day += 1;

                //Toast.makeText(getApplicationContext(),day+"",Toast.LENGTH_SHORT).show();
                if (day <= 5)
                    mViewPager.setCurrentItem(day - 1, true);
                else
                    mViewPager.setCurrentItem(0, true);

                initBatch();
                loadDatabaseToArray(MainActivity.this);
                instantiateViews();
                bringToday();
                //Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG).setAction("Action", null).show();
            }
        });
        refreshLayout= (SwipeRefreshLayout) findViewById(R.id.swipeRefresh);
        refreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                Log.i("Refresh","Now Refreshing Closed");
                new ProgressTask().execute();
            }
        });
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {

            }

            @Override
            public void onPageScrollStateChanged(int state) {
                enableDisableSwipeRefresh(state==ViewPager.SCROLL_STATE_IDLE);
            }
        });
    }
    private void enableDisableSwipeRefresh(boolean b) {
        if(refreshLayout!=null)
            refreshLayout.setEnabled(b);
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);

        boolean group1 = someData.getBoolean("group1", true);
        if (group1)
            menu.getItem(0).setTitle("Swap to Group 2");
        else
            menu.getItem(0).setTitle("Swap to Group 1");
        //View v=findViewById(R.id.action_settings);
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        initBatch();
        loadDatabaseToArray(this);
        instantiateViews();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Boolean prev = someData.getBoolean("group1", true);
            SharedPreferences.Editor editor = someData.edit();
            editor.putBoolean("group1", !prev);
            editor.commit();

            if (!prev)
                item.setTitle("Swap to Group 2");
            else
                item.setTitle("Swap to Group 1");
            initBatch();
            loadDatabaseToArray(this);
            instantiateViews();
            return true;
        }else if(id==R.id.action_update){
            refreshLayout.setRefreshing(true);
            ProgressTask p=new ProgressTask();
            p.execute();
            return true;
        }else if(id==R.id.action_batch){
            startActivity(new Intent(this,ChangeBatch.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode==RESULT_OK) {
            Toast.makeText(this, "Updating Successful!", Toast.LENGTH_SHORT).show();
            loadDatabaseToArray(this);
            instantiateViews();

        }
        else if(resultCode==RESULT_FIRST_USER){
            Snackbar.make(MainActivity.this.findViewById(R.id.listView), "Internet Connection Error", Snackbar.LENGTH_LONG).setAction("Action", null).show();
        }

    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";
        private ListView listView;
        TextView textView;

        public PlaceholderFragment() {
        }

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public void onResume() {
            super.onResume();
            doSomething();
        }

        @Override
        public void onPause() {
            super.onPause();
        }

        void doSomething() {
            String[] items;
            String head = "";
            final int sectionId = getArguments().getInt(ARG_SECTION_NUMBER);
            String days[]={"Monday","Tuesday","Wednesday","Thursday","Friday"};
            if(sectionId>=6){
                items = new String[]{"-", "-", "-", "-", "-", "-", "-", "-", "-"};
            }else{
                head = days[sectionId-1];
                items = new String[9];
                for(int i=0;i<9;i++){
                    items[i]=period[sectionId-1][i].subject.name;
                }
            }

            items[0] = "09:15   |   " + items[0];
            items[1] = "10:10   |   " + items[1];
            items[2] = "11:05   |   " + items[2];
            items[3] = "12:00   |   " + items[3];
            items[4] = "12:55   |   " + items[4];
            items[5] = "01:50   |   " + items[5];
            items[6] = "02:45   |   " + items[6];
            items[7] = "03:40   |   " + items[7];
            items[8] = "04:35   |   " + items[8];

            textView.setText(head);
            Calendar cal = Calendar.getInstance();
            cal.setTimeZone(TimeZone.getDefault());
            final int day = (cal.get(Calendar.DAY_OF_WEEK) - 1 + 6) % 7 + 1;
            final int hour = cal.get(Calendar.HOUR_OF_DAY);
            final int min = cal.get(Calendar.MINUTE);

            final int per_no = (hour * 60 + min - 9 * 60 - 15) / 55;

            listView.setAdapter(new ArrayAdapter<String>(getContext(), android.R.layout.simple_list_item_1, items) {
                @Override
                public View getView(int position, View convertView, ViewGroup parent) {
                    final View rendered = super.getView(position, convertView, parent);
                    if (position == per_no && day == sectionId && convertView == null) {
                        rendered.setBackgroundColor(Color.GRAY);
                        //Toast.makeText(getContext(),position+" "+convertView+" "+parent+" ",Toast.LENGTH_SHORT).show();
                    }else if(!(period[sectionId-1][position].active) && convertView==null){
                        rendered.setBackgroundColor(Color.LTGRAY);
                    }

                    return rendered;
                }
            });
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    TimeTableDB db=new TimeTableDB(getContext());
                    //db.modifyActive(position,sectionId-1,batch,false);
                    Intent i=new Intent(getContext(),ChangePeriod.class);
                    Bundle b=new Bundle();
                    b.putString("per_no",position+"");
                    b.putString("day",(sectionId-1)+"");
                    b.putString("s_id",period[sectionId-1][position].subject.id);
                    db.open();
                    if(db.isPeriodGroup(position,sectionId-1,batch))
                    {
                        b.putString("cgroup",batch.group);
                    }else{
                        b.putString("cgroup","0");
                    }
                    db.close();

                    i.putExtras(b);
                    startActivityForResult(i,requestIdChangePeriod);

                    //Toast.makeText(getContext(),period[sectionId-1][position].subject.teacher,Toast.LENGTH_SHORT).show();
                }
            });

        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            textView = (TextView) rootView.findViewById(R.id.section_label);
            listView = (ListView) rootView.findViewById(R.id.listView);
            doSomething();
            return rootView;
        }
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {
        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            return PlaceholderFragment.newInstance(position + 1);
        }

        @Override
        public int getCount() {
            // Show 5 total pages.
            return 5;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "Monday";
                case 1:
                    return "Tuesday";
                case 2:
                    return "Wednesday";
                case 3:
                    return "Thursday";
                case 4:
                    return "Friday";
            }
            return null;
        }
    }
    private class ProgressTask extends AsyncTask<String,Void,Boolean> {

        boolean done;
        String error;
        ArrayList<DBPeriod> listDBPeriod;
        ArrayList<Subject> listSubject;
        ProgressDialog dialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            listDBPeriod=new ArrayList<>();
            listSubject=new ArrayList<>();
            done=true;
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);
            if(done) {
                loadDBFromSite(listSubject,listDBPeriod);
            }
            else
                Snackbar.make(MainActivity.this.findViewById(R.id.listView), "Internet Connection Error", Snackbar.LENGTH_LONG).setAction("Action", null).show();

            refreshLayout.setRefreshing(false);
        }

        @Override
        protected Boolean doInBackground(String... params) {
            try {
                URL timetable = new URL(url);
                URLConnection tc=timetable.openConnection();
                BufferedReader in = new BufferedReader(new InputStreamReader(tc.getInputStream()));

                String line,line2;
                line=in.readLine();
                JSONArray ja=new JSONArray(line);
                line2=in.readLine();
                JSONArray ja2=new JSONArray(line2);

                {
                    for(int i=0;i<ja.length();i++){
                        JSONObject jo= (JSONObject) ja.get(i);
                        Subject s=new Subject();
                        s.description=jo.getString(TimeTableDB.KEY_S_SUB_DESC);
                        s.name=jo.getString(TimeTableDB.KEY_S_SUB_NAME);
                        s.teacher=jo.getString(TimeTableDB.KEY_S_SUB_TEACHER);
                        s.id=jo.getString(TimeTableDB.KEY_P_SUB_ID);
                        listSubject.add(s);
                    }
                }
                //Get Periods
                {
                    for(int i=0;i<ja2.length();i++){
                        JSONObject jo= (JSONObject) ja2.get(i);
                        DBPeriod p=new DBPeriod();
                        p.active=jo.getString(TimeTableDB.KEY_P_ACTIVE);
                        p.branch=jo.getString(TimeTableDB.KEY_P_BRANCH);
                        p.day=jo.getString(TimeTableDB.KEY_P_DAY);
                        p.group=jo.getString(TimeTableDB.KEY_P_GROUP);
                        p.per_no=jo.getString(TimeTableDB.KEY_P_PER_NO);
                        p.sem=jo.getString(TimeTableDB.KEY_P_SEM);
                        p.sub_id=jo.getString(TimeTableDB.KEY_P_SUB_ID);
                        p.course=jo.getString(TimeTableDB.KEY_P_COURSE);
                        listDBPeriod.add(p);
                    }
                }
            } catch (IOException | JSONException e) {
                e.printStackTrace();
                error=e.toString();
                done=false;
            }
            return null;
        }
    }

}
