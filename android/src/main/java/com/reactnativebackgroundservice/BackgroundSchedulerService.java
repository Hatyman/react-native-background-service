package com.reactnativebackgroundservice;

import android.app.ActivityManager;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.PersistableBundle;

import java.util.List;
import java.util.Locale;

public class BackgroundSchedulerService extends JobService {

    @Override
    public boolean onStartJob(JobParameters parameters) {
        PersistableBundle props = parameters.getExtras();
        Context context = getApplicationContext();

        System.out.println(
            String.format(
                Locale.ENGLISH,
                "Job %s is going to start",
                props.getString(ReactNativeBackgroundServiceModule.PROP_TASK_NAME)
            )
        );

        if (isAppOnForeground(context)) {
//         We should check: Is there all the data we need (isPeriodic, taskName, periodSec)
            if (props.containsKey(ReactNativeBackgroundServiceModule.PROP_IS_PERIODIC)
                    && props.containsKey(ReactNativeBackgroundServiceModule.PROP_TASK_NAME)
                    && props.containsKey(ReactNativeBackgroundServiceModule.PROP_PERIOD_SEC)) {
//                We need to reschedule this task after 10 minutes, because we don't want to execute it in foreground
                System.out.println(
                    String.format(
                        Locale.ENGLISH,
                        "App is running on foreground, %s is rescheduled after 10 min",
                        props.getString(ReactNativeBackgroundServiceModule.PROP_TASK_NAME)
                    )
                );
                ReactNativeBackgroundServiceModule.scheduleBackgroundTask(
                    props.getString(ReactNativeBackgroundServiceModule.PROP_TASK_NAME),
                    props.getBoolean(ReactNativeBackgroundServiceModule.PROP_IS_PERIODIC),
                    props.getInt(ReactNativeBackgroundServiceModule.PROP_PERIOD_SEC),
                    ReactNativeBackgroundServiceModule.CHECK_IS_IN_FOREGROUND_DELAY_SEC,
                    context
                );
            }

            return false;
        }

        props.putInt(ReactNativeBackgroundServiceModule.PROP_JOB_ID, parameters.getJobId());
        Bundle serviceProps = new Bundle(props);

        Intent service = new Intent(getApplicationContext(), BackgroundService.class);
        service.putExtras(serviceProps);

        getApplicationContext().startService(service);

        if (props.getBoolean(ReactNativeBackgroundServiceModule.PROP_IS_PERIODIC, false)
                && props.containsKey(ReactNativeBackgroundServiceModule.PROP_TASK_NAME)
                && props.containsKey(ReactNativeBackgroundServiceModule.PROP_PERIOD_SEC)) {
            System.out.println(
                String.format(
                    Locale.ENGLISH,
                    "Job is periodic, %s is scheduled after %d sec",
                    props.getString(ReactNativeBackgroundServiceModule.PROP_TASK_NAME),
                    props.getInt(ReactNativeBackgroundServiceModule.PROP_PERIOD_SEC)
                )
            );
            ReactNativeBackgroundServiceModule.schedulePeriodicBackgroundTask(
                props.getString(ReactNativeBackgroundServiceModule.PROP_TASK_NAME),
                props.getInt(ReactNativeBackgroundServiceModule.PROP_PERIOD_SEC),
                context
            );

        }

        return true;
    }

    @Override
    public boolean onStopJob(JobParameters props) {
        return false;
    }

    private boolean isAppOnForeground(Context context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> appProcesses =
                activityManager.getRunningAppProcesses();
        if (appProcesses == null) {
            return false;
        }
        final String packageName = context.getPackageName();
        for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
            if (appProcess.importance ==
                    ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND &&
                    appProcess.processName.equals(packageName)) {
                return true;
            }
        }
        return false;
    }
}
