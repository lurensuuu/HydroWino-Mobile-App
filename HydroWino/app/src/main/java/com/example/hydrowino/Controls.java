package com.example.hydrowino;

import com.google.firebase.database.PropertyName;

public class Controls {
    public Motor motor1;
    public Motor motor2;
    public Motor motor3;
    public boolean waterPump;

    public Controls() {
        // Required for Firebase
    }

    public static class Motor {
        public boolean enabled;

        public Motor() {
        }
    }
}
