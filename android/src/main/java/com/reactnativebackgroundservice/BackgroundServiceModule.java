package com.reactnativebackgroundservice;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.os.PersistableBundle;

import androidx.annotation.NonNull;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Types:
 * Boolean -> Bool
 * Integer -> Number
 * Double -> Number
 * Float -> Number
 * String -> String
 * Callback -> function
 * ReadableMap -> Object
 * ReadableArray -> Array
**/
public class BackgroundServiceModule extends ReactContextBaseJavaModule {
    private static ReactApplicationContext reactContext;

    private static Integer jobId = 0;
    private static final String RNModuleName = "ReactNativeBackgroundService";

    public static final String PROP_TASK_NAME = "PROP_TASK_NAME";
    public static final String PROP_IS_PERIODIC = "PROP_IS_PERIODIC";
    public static final String PROP_PERIOD_SEC = "PROP_PERIOD_SEC";
    public static final String PROP_JOB_ID = "PROP_JOB_ID";
    public static final Integer CHECK_IS_IN_FOREGROUND_DELAY_SEC = 10 * 60;

  BackgroundServiceModule(ReactApplicationContext context) {
        super(context);
        reactContext = context;
    }

    @NonNull
    @Override
    public String getName() {
        return RNModuleName;
    }

    @ReactMethod
    public void scheduleTask(String taskName, boolean isPeriodic, int periodSec, int delaySec, Promise promise) {
        int sheduledId = scheduleBackgroundTask(taskName, isPeriodic, periodSec, delaySec, reactContext);
        if (sheduledId == -1) {
            promise.reject("-1", "Schedule failed. Something went wrong.");
        } else {
            promise.resolve(sheduledId);
        }
    }

    @ReactMethod
    public void cancelAllTasks() {
        JobScheduler jobScheduler = reactContext.getSystemService(JobScheduler.class);
        jobScheduler.cancelAll();
    }

    @ReactMethod
    public void cancelTasksById(int taskId) {
        JobScheduler jobScheduler = reactContext.getSystemService(JobScheduler.class);
        jobScheduler.cancel(taskId);
    }

    public static int scheduleBackgroundTask(String taskName, boolean isPeriodic, int periodSec, int delaySec, Context context) {
        JobScheduler jobScheduler = context.getSystemService(JobScheduler.class);

        int properJobId = getProperTaskId(jobScheduler.getAllPendingJobs(), taskName);

        ComponentName serviceComponent = new ComponentName(context, BackgroundSchedulerService.class);
        JobInfo.Builder builder = new JobInfo.Builder(properJobId, serviceComponent);

        PersistableBundle props = new PersistableBundle();
        props.putString(PROP_TASK_NAME, taskName);
        props.putBoolean(PROP_IS_PERIODIC, isPeriodic);
        props.putInt(PROP_PERIOD_SEC, periodSec);
        builder.setExtras(props);

        builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED);
        builder.setPersisted(true);

        builder.setMinimumLatency(delaySec * 1000);

        int result = jobScheduler.schedule(builder.build());

        System.out.println(
            String.format(
                Locale.ENGLISH,
                "Job %s is scheduled after %d sec",
                taskName,
                delaySec
            )
        );
        printAllPendingTasks(jobScheduler);
        return result == JobScheduler.RESULT_SUCCESS ? properJobId : -1;
    }

    public static void schedulePeriodicBackgroundTask(String taskName, int periodSec, Context context) {
        scheduleBackgroundTask(taskName, true, periodSec, periodSec, context);
    }

    public static void printAllPendingTasks(JobScheduler jobScheduler) {
        System.out.print("Job scheduler queue: ");
        System.out.println(jobScheduler.getAllPendingJobs());
    }

    private static int updateAndGetJobId() {
        int currentJobId = jobId;
        jobId += 1;

        return currentJobId;
    }

    /**
     * @param tasks All pending tasks
     * @param taskName Name far task
     * @return Index of existed task to replace or new incremented id
     */
    private static int getProperTaskId(List<JobInfo> tasks, String taskName) {
        if (tasks.size() == 0) {
            resetJobId();
        } else {
            for (JobInfo task: tasks) {
                if (Objects.equals(task.getExtras().getString(PROP_TASK_NAME), taskName)) {
                    return task.getId();
                }
            }
        }

        return updateAndGetJobId();
    }

    private static void resetJobId() {
        jobId = 0;
    }
}
