package com.android.systemui.analytics;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.util.Log;
import java.util.List;

public class JobHelper {
    public void startJob(Context context) {
        JobScheduler jobSchedule = (JobScheduler) context.getSystemService("jobscheduler");
        JobInfo.Builder builder = new JobInfo.Builder(300002, new ComponentName(context, SettingsJobSchedulerService.class));
        builder.setPeriodic(86400000);
        builder.setPersisted(true);
        List<JobInfo> jobs = jobSchedule.getAllPendingJobs();
        if (jobs != null) {
            boolean jobAdded = false;
            for (JobInfo jobInfo : jobs) {
                if (jobInfo.getId() == 300002) {
                    jobAdded = true;
                }
            }
            if (!jobAdded && jobSchedule.schedule(builder.build()) == 0) {
                Log.e("JobHelper", "SettingsJobSchedulerService schedule failed");
            }
        }
    }
}
