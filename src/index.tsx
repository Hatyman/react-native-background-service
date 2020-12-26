import { NativeModules } from 'react-native';

type BackgroundServiceType = {
  multiply(a: number, b: number): Promise<number>;
};

const { BackgroundService } = NativeModules;

export default BackgroundService as BackgroundServiceType;
