package arora.kushank.timetablecr;

/**
 * Created by Password on 24-Feb-17.
 */
public class Batch {
    public String branch;
    public String semester;
    public String group;
    public String course;
    Batch(){
        branch="-";
        semester="-";
        group="-";
        course="-";
    }
    Batch(String branch,String sem,String group,String course){
        this.branch=branch;
        this.semester=sem;
        this.group=group;
        this.course=course;
    }
}
