# react-native-background-service

React native service to do background tasks on Android and iOS

## Installation

```sh
npm install @hatyman/react-native-background-service
```

_iOS:_
1. Add background capabilities for `Background Modes` in XCode. (Don't forget re-release provision profile)
  - Background fetch;
  - Background processing.
2. Add an empty header file to the project root (e.g. ProjectName-Bridging-Header.h)

_Android:_
1. Register package in `MainApplication.java`
```java
import com.reactnativebackgroundservice.BackgroundServicePackage;

...

@Override
protected List<ReactPackage> getPackages() {
    List<ReactPackage> packages = new PackageList(this).getPackages();
    // Packages that cannot be autolinked yet can be added manually here:
    packages.add(new BackgroundServicePackage());
    return packages;
}
```
2. Register services in `AndroidManifest.xml`
```xml
<manifest ...>
    ...
    <!-- Add this line to keep scheduled job after reboot -->
    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
    ...

    <application ...>
        ...
        <service android:name="com.reactnativebackgroundservice.BackgroundService" />
        <service
            android:name="com.reactnativebackgroundservice.BackgroundSchedulerService"
            android:permission="android.permission.BIND_JOB_SERVICE" />
        ...
    </application>
</manifest>
```
3. Increase `minSdkVersion` to 24 or higher in `build.gradle`.

## Usage

To add new scheduled task you need:
_iOS:_
1. Register task in AppDelegate.m with some identifier `taskName` (e.g MyLongRunningTask)
```obj-c
[[BGTaskScheduler sharedScheduler] registerForTaskWithIdentifier:@"MyLongRunningTask" usingQueue:dispatch_get_main_queue() launchHandler:^(__kindof BGTask * _Nonnull task) {
    id backgroundService = [bridge moduleForName:@"ReactNativeBackgroundService"];
    if (backgroundService != nil &&
       [backgroundService isKindOfClass:[ReactNativeBackgroundService class]]) {
      NSLog(@"Propagating MyLongRunningTask to a module...");
      [((ReactNativeBackgroundService*)backgroundService) backgroundTaskExecuting:task];
    } else {
     NSLog(@"Cannot propagate MyLongRunningTask to a module, aborting...");
     [task setTaskCompletedWithSuccess:true];
    }
  }];
```
2. Add this identifier `taskName` into Info.plist
```plist
<key>BGTaskSchedulerPermittedIdentifiers</key>
<array>
	<string>MyLongRunningTask</string>
	<string>$(PRODUCT_BUNDLE_IDENTIFIER)</string>
</array>
```
3. Call function scheduleBackgroundProcessingIOSTask (schedulePeriodicBackgroundProcessingIOSTask), define delay (sec) and period (sec) if needed and execution callback.
```
Scheduled   Task is running             Task is running             Task is running             Task is running
|-----------------||--------------------------||--------------------------||--------------------------||----->
<-----timeout-----><----------period----------><----------period----------><----------period---------->
```
**Note:** it is an ideal case, in reality, dasd can hold task for longer. (That is dasd see below)

```ts
// Types
iOSTaskExecutionCallback: (taskName: string) => Promise<void>;

scheduleBackgroundProcessingIOSTask: (
  taskName: string,
  timeoutSec: number,
  callback: iOSTaskExecutionCallback
) => Promise<void>;

schedulePeriodicBackgroundProcessingIOSTask: (
  taskName: string,
  timeoutSec: number,
  periodSec: number,
  callback: iOSTaskExecutionCallback
) => Promise<void>;
```

```ts
import { scheduleBackgroundProcessingIOSTask } from "@hatyman/react-native-background-service";

// ...

await scheduleBackgroundProcessingIOSTask(
  'MyLongRunningTask',
  15 * 60,
  executionCallback
);
```

_Android:_
1. Register HeadlessJsTask via `AppRegistry` from `react-native` with some task identifier `myBackgroundTask` and callback with `Task` type. (I did it after `registerComponent` execution).
```ts
// Types
getAndroidBackgroundTaskCallback: (
  callback: AndroidTaskExecutionCallback
) => Task;
```
```ts
// index.js
import { AppRegistry } from "react-native"

// ...

AppRegistry.registerHeadlessTask('myBackgroundTask', () => getAndroidBackgroundTaskCallback(yourFunction));
```
2. Implement background task callback `yourFunction`.
```ts
// Types
type BackgroundTaskProps = {
  PROP_TASK_NAME: string;
  PROP_IS_PERIODIC: boolean;
  PROP_PERIOD_SEC: number;
  PROP_JOB_ID: number;
};

type AndroidTaskExecutionCallback = (
  taskData: BackgroundTaskProps
) => Promise<void>;
```

```ts
import { AndroidTaskExecutionCallback } from "@hatyman/react-native-background-service";

export const yourFunction: AndroidTaskExecutionCallback = (taskData) => {
  // do something
};
```
3. Schedule your task. This promise returns jobId, JobScheduler register task with this identifier `myBackgroundTask`. There in `taskName` you have to place identifier which was used in `registerHeadlessTask`.
```ts
// Types
scheduleBackgroundProcessingAndroidTask: (
    taskName: string,
    isPeriodic: boolean,
    periodSec: number,
    delaySec: number
  ) => Promise<number>;
```

```ts
import { scheduleBackgroundProcessingAndroidTask } from "@hatyman/react-native-background-service";

// ...

scheduleBackgroundProcessingAndroidTask('myBackgroundTask', true, 3600, 15 * 60);
```

##Debug
_iOS:_
1. open "Console" on your macOS
2. Start streaming
3. Text a task identifier into search field
4. Schedule the task and check logs. When timeout is expired there would be dasd logs about scoring with decision to run (1 - run, 0 - wait)
5. Monitor logs there
   Also we can monitor app state via XCode.
6. Attach to the process (if you aren't already)
7. Open energy status listener
   There we can see app is which state (foreground, suspended, background)
   Also we can gather information by common way - Metro server console
8. Monitor app's logs in metro server console.

_Android:_ Monitor logs via adb logcat (in android studio) and via Metro server console.

##Restrictions
_iOS:_
- If app goes to killed state by user's swipe, **scheduled task will be restricted and cancelled** (when dasd try to run it). When app is minimized, iOS can kill it to remain some resources, in that case, our job will run as soon as dasd decides to run it.
- For a task, we can specify delay. In doesn't mean that task will exactly executes after this delay, it only guarantees that task won't run at this period. Dasd sometimes (I don't know what triggers it) recalculates tasks' scores, after that, it decides what tasks should be run. Task will be run after delay + 1h or + 5min, it depends on magic and phone's mood. Also, if you schedule your task after about 15min or more, it will have greater chances to run at appointed time (if phone is not on active use).
- Task will not run in the foreground, it will wait until you minimize the app.
- Task won't run after reboot.

_Android:_
- Background task is run if app wasn't killed by the user (kill = swipe the app away, or using some process manager)
- Background task isn't run after phone restart (unless an app is started).
- To overcome two restrictions above you need to enable auto-start of the app in Android Settings
- Other interesting configuration options:
  - If app is in the foreground, job is not executed, but rescheduled after 10 minutes (we don't want to decrease UI performance).
  - Job is configured to run only if **UNMETERED** network connection is present. It means, that JobScheduler will wait to run task until there is connection like Wi-Fi or without traffic control.


##Technical introduction
Both platforms have 4 app states:
1. Active - app is running in foreground
2. Background - app is running without a UI (there aren't component rendering -> no useEffects and no any hooks, only callbacks and plain functions)
3. Suspended - app is not running, but app process still there
4. Killed - app is not running and there is not app process
```
                        +------------+
                        | Foreground |
                        +---+----^---+
                            |    |
           User changed app |    | User went back
                            |    |
                        +---v----+---+
                  +---->+ Background <------+
   Scheduled task |     +-------+----+      |
   or BLE event   |             |           |
                  |             |           |
                  |             |           |
            +-----+-----+       |           |
            | Suspended <-------+           | Scheduled task
            +-----+-----+  Run out of       | (only if killed by system, not user)
                  |        computing time   |
                  |                         |
Lack of resources |                         |
                  |                         |
             +----v---+                     |
             | Killed +---------------------+
             +--------+
```

Our target - execute a task in background.

_iOS:_
There is some activity control 'intelligent' system (named **dasd**), which decides run your task or not. Its decision depends on several points:
- Critical low battery: When the phone is about to run out of battery (< 20%), background execution will be paused by the system to avoid battery usage.
- Low power mode: When users change to phone to low power mode, the user explicitly indicates that the system should preserve battery for critical tasks only.
- Background App refresh setting: The user can toggle the setting to allow or not a specified app can run background tasks.
- App usage: There is a limit of resources on the phone so that the system must priorities which apps it should allocate resources for. Typically, apps that the user uses the most. Apple also mentioned to “On-device predictive engine” that learns which apps the user often uses and when. The on-device predictive engine will rely on this information to prioritize background execution.
- App switcher: Only apps are visible in App Switcher have opportunities to run background tasks.
- System budget: Ensure background activities do not drain battery and data plans, there is a limit of battery and data of background execution throughout the day.
- Rate limit: The system performs sone rate-limiting per launch.
  <IMG  src="https://uynguyen.github.io/Post-Resources/RefreshInBg/factors.png"/>

This service is used to prevent to fall down performance, privacy, etc.

Each task has **score**, which calculates by some mysterious variables (each scoring has logged in a console with summary and dependencies with their gain). Also, dasd (or iOS) has **threshold**, which depends on a phone's state and app's behavior, if there is running game with GPU and some other hard activities, threshold will be highest (max 1.0 for all scoring). **Dasd decides to run our task if task's score is bigger than threshold**, so in case when there is running game and threshold is about 1, task will not run, it will wait until mobile is back to normal state.

And if we want to run background task, our app should be **GOOD** citizen, else dasd will never let it go.

## License

MIT
