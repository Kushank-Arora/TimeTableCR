package arora.kushank.timetablecr;

import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

import static java.util.Arrays.sort;

/**
 * Created by Password on 04-Mar-17.
 */

public class ChangePeriod extends AppCompatActivity {
    Batch batch;
    TextView tv[];
    EditText et[];
    Switch switchActive;

    enum Sending {
        SendPeriod, SendSubject;
    }

    void setDetailVisibility(boolean vis) {
        if(vis){
            for(int i=0;i<3;i++) {
                tv[i].setVisibility(View.VISIBLE);
                et[i].setVisibility(View.VISIBLE);
            }
        }else{
            for(int i=0;i<3;i++) {
                tv[i].setVisibility(View.INVISIBLE);
                et[i].setVisibility(View.INVISIBLE);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Bundle myBasket = getIntent().getExtras();
        setContentView(R.layout.changeperiod);

        Button b = (Button) findViewById(R.id.bChangePeriod);
        final Spinner spinner = (Spinner) findViewById(R.id.spSub);
        switchActive=(Switch)findViewById(R.id.sw_cp_active);

        et = new EditText[3];
        et[0] = (EditText) findViewById(R.id.et_cp_sname);
        et[1] = (EditText) findViewById(R.id.et_cp_tname);
        et[2] = (EditText) findViewById(R.id.et_cp_desc);

        tv = new TextView[3];
        tv[0] = (TextView) findViewById(R.id.tv_cp_sname);
        tv[1] = (TextView) findViewById(R.id.tv_cp_tname);
        tv[2] = (TextView) findViewById(R.id.tv_cp_desc);

        final String fileNameSharedPref = "TIMETABLEPREF";
        SharedPreferences someData = getSharedPreferences(fileNameSharedPref, MODE_PRIVATE);

        batch = new Batch();
        batch.branch = someData.getString("branch", "CE");
        batch.group = myBasket.getString("cgroup");
        batch.semester = someData.getString("sem", "6");
        batch.course = someData.getString("course", "BTech");

        String curPeriod=myBasket.getString("s_id");

        //assert et != null;
        //et.setText(String.valueOf(myBasket.getInt("per_no")));

        final TimeTableDB db = new TimeTableDB(this);
        db.open();

        String[] out = db.getSubjects(batch);
        final String[] subjectCode = new String[out.length ];
        System.arraycopy(out, 0, subjectCode, 0, out.length);

        sort(subjectCode);

        int curPos=0;
        for(int i=0;i<out.length;i++)
            if(subjectCode[i]!=null && subjectCode[i].equals(curPeriod)) {
                curPos = i;
                break;
            }

        final String[] subjects=new String[out.length+1];
        for(int i=0;i<out.length;i++)
            subjects[i]=db.getSubFromId(subjectCode[i]).name;
        subjects[out.length] = "Other";

        db.close();
        assert spinner != null;
        spinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, subjects));
        spinner.setSelection(curPos);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == subjects.length - 1) {
                    setDetailVisibility(true);
                } else {
                    setDetailVisibility(false);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        et[0].setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(!hasFocus){

                    String sName=et[0].getText().toString().toUpperCase().trim();
                    et[0].setText(sName);

                    for(int i=0;i<subjects.length-1;i++)
                        if(sName.equals(subjects[i]))
                        {
                            db.open();
                            String teacher=db.getTeacherForSubject(sName).trim().toUpperCase();
                            if(teacher!=null)
                                et[1].setText(teacher);
                            db.close();
                            break;
                        }
                }
            }
        });

        et[1].setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(!hasFocus){
                    try {
                        et[1].setText(et[1].getText().toString().toUpperCase().trim());
                    }catch(Exception e){
                        System.out.println("Teacher Name field is empty");
                        e.printStackTrace();
                    }
                }
            }
        });

        assert b != null;
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                int pos = spinner.getSelectedItemPosition();
                if (pos == subjects.length - 1) {
                    if(et[2].getText()==null || et[2].getText().toString().trim().equals(""))
                        et[2].setText("-");
                    changeSubject(myBasket);
                } else {
                    changePeriod(subjectCode[spinner.getSelectedItemPosition()], myBasket, switchActive.isChecked()?"1":"0");
                }
            }
        });

        setDetailVisibility(false);
    }

    private void changePeriod(String subId, Bundle myBasket, String active) {
        JSONObject jsonParam = new JSONObject();
        try {
            jsonParam.put("branch", batch.branch);
            jsonParam.put("sem", batch.semester);
            jsonParam.put("cgroup", batch.group);
            jsonParam.put("day", myBasket.getString("day"));
            jsonParam.put("per_no", myBasket.getString("per_no"));
            jsonParam.put("sub_id", subId);
            jsonParam.put("active", active);
            jsonParam.put("course", batch.course);
            ProgressTask p = new ProgressTask("http://ktimetable.tk/updatePeriodInDB.php", jsonParam, Sending.SendPeriod,null,null);
            p.execute();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, e.toString(), Toast.LENGTH_SHORT).show();
        }
    }

    private void changeSubject(Bundle myBasket) {
        String sName = et[0].getText().toString().trim().toUpperCase();
        String tName = et[1].getText().toString().trim().toUpperCase();
        String active = switchActive.isChecked()?"1":"0";
        String desc = et[2].getText().toString().trim();
        String course = batch.course;
        String branch = batch.branch;
        int sem = Integer.parseInt(batch.semester);
        String sId = getSubId(sName, course, branch, sem);
        JSONObject jsonParam = new JSONObject();
        try {
            jsonParam.put("sub_id", sId);
            jsonParam.put("sub_teacher", tName);
            jsonParam.put("sub_name", sName);
            jsonParam.put("sub_desc", desc);
            ProgressTask p = new ProgressTask("http://ktimetable.tk/updateSubjectInDB.php", jsonParam, Sending.SendSubject,myBasket,active);
            p.execute();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, e.toString(), Toast.LENGTH_SHORT).show();
        }
    }

    /*
    Copied from ExtractTableInfo NetBeans Project
     */
    String getSubId(String sName, String course, String branch, int sem) {
        if (sName.equals("-")) {
            return "-";
        }
        String ans = "";
        if (course.equals("MTech")) {
            ans = "m";
        } else if (course.equals("MCA")) {
            ans = "mca";
        }
        if (!branch.equals("-")) {
            ans += branch.toLowerCase();
        }
        ans += sem + "";
        if (sName.contains("/")) {
            String list[] = sName.split("/");
            String cur = ans;
            ans += list[0].split(" ")[0].trim().toLowerCase();

            if (list[0].contains("G1"))
                ans += "1";
            else if (list[0].contains("G2"))
                ans += "2";

            if (sName.toLowerCase().contains("lab")) {
                ans += "lab";
            }
            ans += "/";
            ans += cur + list[1].split(" ")[0].trim().toLowerCase();

            if (list[1].contains("G1"))
                ans += "1";
            else if (list[1].contains("G2"))
                ans += "2";

            if (sName.toLowerCase().contains("lab")) {
                ans += "lab";
            }
        } else if (sName.contains("G1")) {
            ans += sName.split(" ")[0].trim().toLowerCase();
            ans += "1";
            ans += "lab";

        } else if (sName.contains("G2")) {
            ans += sName.split(" ")[0].trim().toLowerCase();
            ans += "2";
            ans += "lab";

        } else {
            ans += sName.split(" ")[0].trim().toLowerCase();
            if (sName.toLowerCase().contains("lab")) {
                ans += "lab";
            }
        }
        return ans;
    }

    private class ProgressTask extends AsyncTask<String, Void, Boolean> {

        String url;
        JSONObject json;
        boolean done;
        String error;
        ProgressDialog dialog;
        Sending sendingWhat;
        Bundle basket;
        String active;

        public ProgressTask(String url, JSONObject json, Sending s, Bundle myBasket, String active) {
            this.url = url;
            this.json = json;
            sendingWhat = s;
            basket=myBasket;
            this.active=active;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            done = true;
            try {
                dialog = new ProgressDialog(ChangePeriod.this);
                dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                dialog.setTitle("Uploading Data...");
                dialog.show();
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);
            try {
                dialog.dismiss();
            }catch(Exception e){
                e.printStackTrace();
            }

            if (done) {
                Batch b = new Batch();
                try {
                    if (sendingWhat == Sending.SendPeriod) {
                        b.group = json.getString("cgroup");
                        b.branch = json.getString("branch");
                        b.semester = json.getString("sem");
                        b.course = json.getString("course");
                        TimeTableDB db = new TimeTableDB(ChangePeriod.this);
                        db.open();
                        db.modifyPeriod(json.getString("per_no"), json.getString("day"), b, json.getString("sub_id"), json.getString("active"));
                        db.close();
                        setResult(RESULT_OK);
                        finish();
                    } else if (sendingWhat == Sending.SendSubject) {
                        TimeTableDB db = new TimeTableDB(ChangePeriod.this);
                        db.open();
                        db.modifySubject(json.getString("sub_id"), json.getString("sub_teacher"), json.getString("sub_name"), json.getString("sub_desc"));
                        db.close();
                        changePeriod(json.getString("sub_id"), basket, active);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                setResult(RESULT_FIRST_USER);
                finish();
            }
        }

        @Override
        protected Boolean doInBackground(String... params) {
            try {
                StringBuilder sb = new StringBuilder();
                URL timetable = new URL(url);
                HttpURLConnection conn = (HttpURLConnection) timetable.openConnection();
                conn.setDoOutput(true);
                conn.setDoInput(true);
                conn.setInstanceFollowRedirects(false);
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setUseCaches(false);
                conn.connect();


                OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
                wr.write(json.toString());
                wr.flush();
                wr.close();

                int HttpResult = conn.getResponseCode();
                if (HttpResult == HttpURLConnection.HTTP_OK) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));

                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line).append("\n");
                    }
                    br.close();

                    System.out.println("" + sb.toString());
                } else {
                    System.out.println(conn.getResponseMessage());
                }
            } catch (IOException e) {
                e.printStackTrace();
                error = e.toString();
                done = false;
            }
            return null;
        }
    }
}
