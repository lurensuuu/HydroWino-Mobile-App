package com.example.hydrowino;

public class IoT {
    public int Button1;
    public float humidity;
    public float phLevel;
    public float airTemperature;
    public float waterLevel;
    public float TDS;
    public float waterTemperature;


    public boolean motor1;
    public boolean motor2;
    public boolean motor3;
    public boolean waterPump;



    public IoT()
    {
        Button1 = 0;
        humidity = 0;
        phLevel = 0;
        airTemperature = 0;
        waterLevel = 0;
        TDS = 0;
        waterTemperature = 0;

        motor1 = false;
        motor2 = false;
        motor3 = false;
        waterPump = false;

    }
}