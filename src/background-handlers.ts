import { Platform } from 'react-native';
import Background from './Background';

import type { Task } from 'react-native';
import type { BackgroundTaskProps } from './NativeBackgroundServices';

export type iOSTaskExecutionCallback = (taskName: string) => Promise<void>;
export type AndroidTaskExecutionCallback = (
  taskData: BackgroundTaskProps
) => Promise<void>;

export const getAndroidBackgroundTaskCallback = (
  callback: AndroidTaskExecutionCallback
): Task => callback;

const getIOSTaskExecutionCallback = (callback: iOSTaskExecutionCallback) => {
  return (taskName: string) => {
    callback(taskName)
      .then(() => {
        Background.completeBackgroundProcessingTask(taskName, true);
      })
      .catch((error) => {
        console.error(`Task ${taskName} failed with error: ${error.message}`);
        Background.completeBackgroundProcessingTask(taskName, true);
      });
  };
};

function getPeriodicIOSTaskExecutionCallback(
  periodSec: number,
  callback: (taskName: string) => void
) {
  return (taskName: string) => {
    Background.scheduleBackgroundProcessingTask(
      taskName,
      periodSec,
      getPeriodicIOSTaskExecutionCallback(periodSec, callback),
      (taskNameProp) => {
        console.log(`Task ${taskNameProp} expired`);
        Background.completeBackgroundProcessingTask(taskNameProp, false);
      }
    );

    callback(taskName);
  };
}

export const scheduleBackgroundProcessingIOSTask = async (
  taskName: string,
  timeoutSec: number,
  callback: iOSTaskExecutionCallback
) => {
  if (Platform.OS !== 'ios') {
    return;
  }

  Background.cancelAllBackgroundProcessingTasks();
  await Background.scheduleBackgroundProcessingTask(
    taskName,
    timeoutSec,
    getIOSTaskExecutionCallback(callback),
    (propTaskName) => {
      console.log(`Task ${propTaskName} expired`);
      Background.completeBackgroundProcessingTask(propTaskName, false);
    }
  );
};

export const schedulePeriodicBackgroundProcessingIOSTask = async (
  taskName: string,
  timeoutSec: number,
  periodSec: number,
  callback: iOSTaskExecutionCallback
) => {
  if (Platform.OS !== 'ios') {
    return;
  }

  Background.cancelAllBackgroundProcessingTasks();
  await Background.scheduleBackgroundProcessingTask(
    taskName,
    timeoutSec,
    getPeriodicIOSTaskExecutionCallback(
      periodSec,
      getIOSTaskExecutionCallback(callback)
    ),
    (propTaskName) => {
      console.log(`Task ${propTaskName} expired`);
      Background.completeBackgroundProcessingTask(propTaskName, false);
    }
  );
};
