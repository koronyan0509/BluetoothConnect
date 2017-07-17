package com.example.mizun.kitaharasystem;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.opengl.Matrix;
import android.util.Log;

/**
 * Created by korona on 2016/09/20.
 */

    public class OrientationEstimater  {
        private float currentPosition;
        public float Gx;
        public float Gy;
        public float Gz;
        public float v2;

        private final static float PI = (float) Math.PI;

        private final float[] outputRotationMatrix = new float[16];
        public float[] rotationMatrix = new float[16];
        public final float[] rotationMatrix_t1 = new float[16];
        public float[] rotationMatrix_t2 = new float[16];

        public float[] rotationMatrix_d = new float[16];

        // configurations
        private boolean landscape = true; // swapXY
        private boolean zeroSnap = true;
        private boolean applyPressureHeight = false;


        private float[] mag = new float[3];
        private long lastGyroTime = 0;
        private long lastAccelTime = 0;
        private long resetTime = 0;
        private final Vector3f gravityVecI = new Vector3f(0, 1, 0);
        public final Vector3f vVec2 = new Vector3f();
        private final Vector3f tmpVec = new Vector3f();
        public final Vector3f accVec = new Vector3f();
        private final Vector3f accVecN = new Vector3f();
        public final Vector3f vVec = new Vector3f();
        public final Vector3f posVec = new Vector3f();
        private final Vector3f gyroVec = new Vector3f();
        private final Vector3f gravity = new Vector3f();

        public float posIntegretedError = 0;

        private float[] outputPosition = new float[3];
        private float[] orientation = new float[3]; // [yaw roll pitch] (rad)
        private float[] position = new float[3]; // beta
        public long startTime = 0;
        private long currentTime = 0;
        private long secondTime = 0;



        private final float[] accHistory = new float[8];
        private int accHistoryCount = 0;



        public OrientationEstimater() {
            reset();
        }
        public void reset() { /*値の初期化*/
            Log.d("OrientationEstimater", "reset");
            resetTime = System.currentTimeMillis(); /*現在の時間を返す*/
            posIntegretedError = 0;
            Matrix.setIdentityM(rotationMatrix, 0);
            Matrix.setIdentityM(rotationMatrix_d, 0);
            Matrix.setIdentityM(outputRotationMatrix, 0);
            position[0] = 0;
            position[1] = 0;
            position[2] = 0;
            posVec.set(0, 0, 0);
            vVec.set(0, 0, 0);
            vVec2.set(0, 0, 0);
            accVec.set(0, 0, 0);
            currentPosition = 0;

        }



        public float getPosition() {
            outputPosition[0] = position[0] + posVec.values[0];
            outputPosition[1] = position[1] + posVec.values[1];
            outputPosition[2] = position[2] + posVec.values[2];

            currentPosition = outputPosition[1];


            return currentPosition;
        }


        public void onSensorEvent(SensorEvent event) {

            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                accVec.values[0] = event.values[0] - Gx;
                accVec.values[1] = event.values[1] - Gy;
                accVec.values[2] = event.values[2] - Gz;

                float dt = (event.timestamp - lastAccelTime) * 0.000000001f; // dt(sec)
                if (lastAccelTime > 0 && dt < 0.5f && System.currentTimeMillis() - resetTime > 500) {
                    // m/s^2　大きさ0.5以下の場合は0と仮定することで細かいブレを補正
                    if (accVec.values[0] <= 0.5f && accVec.values[0] >= -0.5f)
                        accVec.values[0] = 0;
                    if (accVec.values[1] <= 0.5f && accVec.values[1] >= -0.5f)
                        accVec.values[1] = 0;
                    if (accVec.values[2] <= 0.5f && accVec.values[2] >= -0.5f)
                        accVec.values[2] = 0;



                    vVec.values[0] += accVec.values[0] * dt * 100;
                    vVec.values[1] += accVec.values[1] * dt * 100;
                    vVec.values[2] += accVec.values[2] * dt * 100;
                    // velocity limit
                    /*
                    if (vVec.length() > 5000) {
                        vVec.scale(0.95f);
                    }
                       */
                    boolean resting = false;
                    //一定回数の加速度が一定以下の値ならば速度を0とみなす
                    accHistory[(accHistoryCount++) % accHistory.length] = accVec.length();
                    if (accHistoryCount > accHistory.length) {
                        final float l = accVec.length();
                        float min = l, max = l, sum = 0;
                        for (float a : accHistory) {
                            sum += a;
                            if (a > max)
                                max = a;
                            if (a < min)
                                min = a;
                        }
                        if (sum < 2.5f && max - min < 0.2f) {
                            resting = true;
                            vVec.scale(0.9f);
                            if (max - min < 0.1f) {
                                vVec.set(0, 0, 0);
                            }
                        }
                    }

                    // position(cm)
                    if (vVec.length() > 0.5f) {
                        posVec.values[0] += vVec.values[0] * dt;
                        posVec.values[1] += vVec.values[1] * dt;
                        posVec.values[2] += vVec.values[2] * dt;
                    }
                    posIntegretedError += vVec.length() * 0.0001f + accVec.length() * 0.1f;

                    // position limit

                    if (posVec.values[0] > 100) {
                        posVec.values[0] *= 0.9f;
                    } else if (posVec.values[0] < -100) {
                        posVec.values[0] *= 0.9f;
                    }

                    if (posVec.values[2] > 100) {
                        posVec.values[2] *= 0.9f;
                    } else if (posVec.values[2] < -100) {
                        posVec.values[2] *= 0.9f;
                    }

                    if (posVec.values[1] < -180) {
                        posVec.values[1] *= 0.8f;
                    } else if (posVec.values[1] > 180) {
                        posVec.values[1] *= 0.8f;
                    }

                    // snap to 0
                    if (resting && zeroSnap && posIntegretedError > 0) {
                        if (posIntegretedError > 0) {
                            tmpVec.set(posVec.array());
                            posVec.scale(0.995f);
                            posIntegretedError -= tmpVec.sub(posVec).length();
                        }
                    }
                    v2 = vVec.values[1] - vVec2.values[1];
                    currentTime = System.currentTimeMillis() - startTime;
                    secondTime = currentTime;


                        vVec2.values[0] = vVec.values[0];
                        vVec2.values[1] = vVec.values[1];
                        vVec2.values[2] = vVec.values[2];
                    //Log.d("OrientationEstimater", "" + posVec.values[1]);
                    /*
                    }
                       */


                }

                lastAccelTime = event.timestamp;

            } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                //Log.d("Sensor","TYPE_MAGNETIC_FIELD " + event.values[0] + "," + event.values[1] + "," + event.values[2]+ " ("+  event.timestamp);
                if (event.accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE) {
                    //return;
                }
                System.arraycopy(event.values, 0, mag, 0, 3);
                if (landscape) {
                    mag[0] = -event.values[1];
                    mag[1] = event.values[0];
                }

            }/* else if (event.sensor.getType() == Sensor.TYPE_PROXIMITY) {
                proximate = event.values[0];

                Log.d("Sensor", "proximity=" + proximate);

            } */
             else if (event.sensor.getType() == Sensor.TYPE_GRAVITY) {
                Gx = event.values[0];
                Gy = event.values[1];
                Gz = event.values[2];
                gravity.set(Gx, Gy, Gz);
            /*
            Log.d("Sensor", "gravity.length="+gravity.length());*/
            } else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
                if (lastGyroTime > 0) {
                    float dt = (event.timestamp - lastGyroTime) * 0.000000001f;
                    if (landscape) {
                        gyroVec.set(event.values[1], -event.values[0], event.values[2]);
                    } else {
                        gyroVec.set(event.values[0], event.values[1], event.values[2]);
                    }

                    Matrix.rotateM(rotationMatrix, 0, gyroVec.length() * dt * 180 / PI, gyroVec.array()[0], gyroVec.array()[1], gyroVec.array()[2]);
                    posIntegretedError += gyroVec.length() * dt * 5.0f; // TODO: error ratio control.

                }
                lastGyroTime = event.timestamp;
            }


            // adjust ground vector.
            if (gyroVec.length() < 0.3f && Math.abs(accVecN.length() - Gy) < 0.5f) {
                // estimated ground vec.
                Matrix.multiplyMV(tmpVec.array(), 0, rotationMatrix, 0, accVec.values, 0);
                float theta = (float) Math.acos(tmpVec.dot(gravityVecI));
                if (theta > 0) {
                    float[] cross = tmpVec.cross(gravityVecI).normalize().array();
                    float factor = (System.currentTimeMillis() - resetTime < 500) ? 0.9f : 0.0005f;

                    //Matrix.rotateM(rotationMatrix, 0, theta * 180 / PI * factor, cross[0], cross[1], cross[2]);
                    Matrix.setRotateM(rotationMatrix_t1, 0, theta * 180 / PI * factor, cross[0], cross[1], cross[2]);
                    Matrix.multiplyMM(rotationMatrix_t2, 0, rotationMatrix_t1, 0, rotationMatrix, 0);
                    float tm[] = rotationMatrix_t2;
                    rotationMatrix_t2 = rotationMatrix;
                    rotationMatrix = tm;
                }
            }
        }
    }
