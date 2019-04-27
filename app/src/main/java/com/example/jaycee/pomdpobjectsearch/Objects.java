package com.example.jaycee.pomdpobjectsearch;

public class Objects
{
    public enum Observation
    {
        O_NOTHING (0, "", "Nothing"),
        T_COMPUTER_MONITOR (1, "computer_monitor.txt", "Monitor"),
        T_COMPUTER_KEYBOARD (2, "computer_keyboard.txt", "Keyboard"),
        T_COMPUTER_MOUSE (3, "mouse_0.5.txt", "Mouse"),
        T_DESK (4, "desk.txt", "Desk"),
        T_LAPTOP (5, "laptop.txt", "Laptop"),
        T_MUG (6, "mug.txt", "Mug"),
        T_WINDOW (7, "window.txt", "Window"),
        T_LAMP (8, "lamp.txt", "Lamp"),
        T_BACKPACK (9, "backpack_0.5.txt", "Backpack"),
        T_CHAIR (10, "chair_o.5.txt", "Chair"),
        T_COUCH (11, "couch.txt", "Couch"),
        T_PLANT (12, "plant.txt", "Plant"),
        T_TELEPHONE (13, "telephone_0.5.txt", "Telephone"),
        T_WHITEBOARD (14, "whiteboard_0.5.txt", "Whiteboard"),
        T_DOOR (15, "door.txt", "Door"),
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
