package com.example.jaycee.pomdpobjectsearch;

public class Objects
{
    public enum Observation
    {
        O_NOTHING (0, "", "Nothing"),
        T_COMPUTER_MONITOR (1, "monitor_0.5.txt", "Monitor"),
        T_COMPUTER_KEYBOARD (2, "keyboard_0.5.txt", "Keyboard"),
        T_COMPUTER_MOUSE (3, "mouse_0.5.txt", "Mouse"),
        T_DESK (4, "desk_0.5.txt", "Desk"),
        T_LAPTOP (5, "laptop_0.5.txt", "Laptop"),
        T_MUG (6, "mug_0.5.txt", "Mug"),
        T_WINDOW (7, "window_0.5.txt", "Window"),
        T_LAMP (8, "lamp_0.5.txt", "Lamp"),
        T_BACKPACK (9, "backpack_0.5.txt", "Backpack"),
        T_CHAIR (10, "chair_0.5.txt", "Chair"),
        T_COUCH (11, "couch_0.5.txt", "Couch"),
        T_PLANT (12, "plant_0.5.txt", "Plant"),
        T_TELEPHONE (13, "telephone_0.5.txt", "Telephone"),
        T_WHITEBOARD (14, "whiteboard_0.5.txt", "Whiteboard"),
        T_DOOR (15, "door_0.5.txt", "Door"),
        T_TIGER (16, "tiger.txt", "Tiger");

        private final int obsCode;
        private final String fileName;
        private final String friendlyName;

        Observation(int obsCode, String fileName, String friendlyName)
        {
            this.obsCode = obsCode;
            this.fileName = fileName;
            this.friendlyName = friendlyName;
        }

        public int getCode() { return this.obsCode; }
        public String getFileName() { return this.fileName; }
        public String getFriendlyName() { return this.friendlyName; }
    }
}
