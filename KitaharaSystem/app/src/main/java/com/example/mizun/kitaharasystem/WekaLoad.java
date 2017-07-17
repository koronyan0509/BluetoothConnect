package com.example.mizun.kitaharasystem;

import android.os.Environment;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.ObjectInputStream;

import weka.classifiers.Classifier;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;

/**
 * Created by korona on 2016/06/27.
 */
public class WekaLoad {

    public Instances instances;
    public Classifier classifier;
    public float Position_y = 2;

    Attribute tracking_posiy;
    Attribute tracking_veloy;
    Attribute tracking_velo2y;
    Attribute tracking_accy;
    Attribute tracking_grav;
    Attribute tracking_result;


    Instance instance;

    private OrientationEstimater orientationEstimater = new OrientationEstimater();
    {
        try {

            String path = Environment.getExternalStorageDirectory().getPath() + "/Android/tracking.arff";
            BufferedReader source = new BufferedReader(new FileReader(path));
            instances = new Instances(source);
            instances.setClassIndex(4);
            String path2 = Environment.getExternalStorageDirectory().getPath() + "/Android/tracking.model";
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(path2));
            classifier = (Classifier) ois.readObject();
            ois.close();
            source.close();




            //tracking_posiy = new Attribute("tracking_posiy", 0);
            tracking_veloy = new Attribute("tracking_veloy", 0);
            tracking_velo2y = new Attribute("tracking_velo2y", 1);
            tracking_accy = new Attribute("tracking_accy", 2);
            tracking_grav = new Attribute("tracking_grav", 3);

            FastVector ud = new FastVector(5);
            ud.addElement("up_sharp");
            ud.addElement("up");
            ud.addElement("keep");
            ud.addElement("down");
            ud.addElement("down_sharp");

            tracking_result = new Attribute("tracking_result",ud,4);
            /*
            NumericToNominal convert= new NumericToNominal();
            String[] options= new String[2];
            options[0]="-R";
            options[1]="5-5";  //range of variables to make numeric

            convert.setOptions(options);
            convert.setInputFormat(instances);

            instances = Filter.useFilter(instances, convert);
            */
            instance = new Instance(5);
            instance.setDataset(instances);




            /*
            FastVector updown = new FastVector(3);
            updown.addElement("0");
            updown.addElement("1");
            updown.addElement("2");
            */
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void tracking(float accel_y,float v_y,float v2_y,float posi_y,float G){
        try {
            //instance.setValue(tracking_posiy, posi_y);
            instance.setValue(tracking_veloy, v_y);
            instance.setValue(tracking_velo2y, v2_y);
            instance.setValue(tracking_accy, accel_y);
            instance.setValue(tracking_grav, G);

            instance.setDataset(instances);
            Position_y = (float) classifier.classifyInstance(instance);
            Log.d("OrientationEstimater", "" +Position_y );
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

}
