package com.reactnativebackgroundservice;

import android.content.Intent;
import android.os.Bundle;

import com.facebook.react.HeadlessJsTaskService;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.jstasks.HeadlessJsTaskConfig;
import com.facebook.react.jstasks.HeadlessJsTaskRetryPolicy;
import com.facebook.react.jstasks.LinearCountingRetryPolicy;

import javax.annotation.Nullable;

public class BackgroundService extends HeadlessJsTaskService {

  @Override
  protected @Nullable HeadlessJsTaskConfig getTaskConfig(Intent intent) {
    Bundle props = intent.getExtras();

    if (props != null && props.containsKey(ReactNativeBackgroundServiceModule.PROP_TASK_NAME)) {
      HeadlessJsTaskRetryPolicy retryPolicy = new LinearCountingRetryPolicy(
              1, // Max number of retry attempts
              1000 // Delay between each retry attempt
      );

      return new HeadlessJsTaskConfig(
          props.getString(ReactNativeBackgroundServiceModule.PROP_TASK_NAME),
          Arguments.fromBundle(props),
          0, // timeout for the task
          true, // optional: defines whether or not  the task is allowed in foreground. Default is false
          retryPolicy
        );
    }
    return null;
  }
}
