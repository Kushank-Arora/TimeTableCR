package arora.kushank.timetablecr;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

/**
 * Created by Password on 27-Feb-17.
 */
public class ChangeBatch extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.changebatch);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar2);
        setSupportActionBar(toolbar);

        final TimeTableDB db = new TimeTableDB(this);
        db.open();
        final String[][] cbranch = db.getCourseBranch();
        db.close();

        final String fileNameSharedPref = "TIMETABLEPREF";
        final SharedPreferences someData = getSharedPreferences(fileNameSharedPref, MODE_PRIVATE);

        final Spinner course, branch, sem;
        Button submit;
        sem = (Spinner) findViewById(R.id.spsem);
        branch = (Spinner) findViewById(R.id.spbranch);
        course = (Spinner) findViewById(R.id.spcourse);
        submit = (Button) findViewById(R.id.bsubmit);
        final String selSem = someData.getString("sem", "6");
        final String selBranch = (someData.getString("branch", "CE"));
        final String selCourse = (someData.getString("course", "BTech"));
        final String uniqueCourse[] = new String[100];
        int count = 0;
        for (String[] aCbranch : cbranch) {
            boolean absent = true;
            for (int j = 0; j < count; j++)
                if (aCbranch[0].equals(uniqueCourse[j])) {
                    absent = false;
                    break;
                }
            if (absent) {
                uniqueCourse[count++] = aCbranch[0];
            }
        }
        final String finalCourse[] = new String[count];
        System.arraycopy(uniqueCourse, 0, finalCourse, 0, count);


        if (course != null) {
            course.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, finalCourse));
            course.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    String[] branches = new String[10];
                    int count = 0;
                    for (String[] aCbranch : cbranch)
                        if (aCbranch[0].equals(uniqueCourse[position]))
                            branches[count++] = aCbranch[1];
                    String[] fBranch = new String[count];
                    System.arraycopy(branches, 0, fBranch, 0, count);

                    if (branch != null) {
                        branch.setAdapter(new ArrayAdapter<>(ChangeBatch.this, android.R.layout.simple_list_item_1, fBranch));
                        int posBranch = findPosition(selBranch, fBranch);
                        branch.setSelection(posBranch);


                        db.open();
                        String[] semString = db.getSemForCourseBranch(uniqueCourse[position], fBranch[branch.getSelectedItemPosition()]);
                        if (sem != null) {
                            sem.setAdapter(new ArrayAdapter<>(ChangeBatch.this, android.R.layout.simple_list_item_1, semString));
                            int posSem = findPosition(selSem, semString);
                            sem.setSelection(posSem);
                        }
                        db.close();
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });
            int posCourse = findPosition(selCourse, finalCourse);
            course.setSelection(posCourse);
        }

        if (branch != null) {
            branch.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    db.open();
                    String[] semString;
                    if (course != null) {
                        semString = db.getSemForCourseBranch(course.getSelectedItem().toString(), branch.getSelectedItem().toString());
                        if (sem != null) {
                            sem.setAdapter(new ArrayAdapter<>(ChangeBatch.this, android.R.layout.simple_list_item_1, semString));
                            int posSem = findPosition(selSem, semString);
                            sem.setSelection(posSem);
                        }
                    }
                    db.close();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });
        }


        assert submit != null;
        submit.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("CommitPrefEdits")
            @Override
            public void onClick(View v) {
                SharedPreferences.Editor editor = someData.edit();
                try {
                    assert sem != null;
                    editor.putString("sem", sem.getSelectedItem().toString());
                    assert branch != null;
                    editor.putString("branch", branch.getSelectedItem().toString());
                    assert course != null;
                    editor.putString("course", course.getSelectedItem().toString());
                    Toast.makeText(ChangeBatch.this, "Updating Successful!", Toast.LENGTH_SHORT).show();
                }catch (Exception e)
                {
                    e.printStackTrace();
                    Toast.makeText(ChangeBatch.this, "Error while updating!", Toast.LENGTH_SHORT).show();
                }
                editor.commit();
                finish();
            }
        });
    }

    int findPosition(String s, String[] arr) {
        for (int i = 0; i < arr.length; i++)
            if (arr[i].equals(s)) {
                return i;
            }
        return 0;
    }

}
