import { NativeModules } from 'react-native';
type ScheduleTaskType = (
  taskName: string,
  isPeriodic: boolean,
  periodSec: number,
  delaySec: number
) => Promise<number>;

type AndroidBackgroundServiceType = {
  cancelAllTasks: () => void;
  cancelTasksById: (taskId: number) => void;
  scheduleTask: ScheduleTaskType;
};

const AndroidBackgroundService: AndroidBackgroundServiceType =
  NativeModules.AndroidBackgroundService;

export type BackgroundTaskProps = {
  PROP_TASK_NAME: string;
  PROP_IS_PERIODIC: boolean;
  PROP_PERIOD_SEC: number;
  PROP_JOB_ID: number;
};

export const scheduleBackgroundProcessingAndroidTask: ScheduleTaskType =
  AndroidBackgroundService.scheduleTask;

export const cancelAllAndroidTasks = AndroidBackgroundService.cancelAllTasks;
export const cancelAndroidTasksById = AndroidBackgroundService.cancelTasksById;
