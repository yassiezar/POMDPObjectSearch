package com.example.jaycee.pomdpobjectsearch;

public class Objects
{
    public enum Observation
    {
        O_NOTHING (0, ""),
        T_COMPUTER_MONITOR (1, "computer_monitor.txt"),
        T_COMPUTER_KEYBOARD (2, "computer_keyboard.txt"),
        T_COMPUTER_MOUSE (3, "computer_mouse.txt"),
        T_DESK (4, "desk.txt"),
        T_LAPTOP (5, "laptop.txt"),
        T_MUG (6, "mug.txt"),
        T_WINDOW (7, "window.txt"),
        T_LAMP (8, "lamp.txt"),
        T_BACKPACK (9, "backpack.txt"),
        T_CHAIR (10, "chair.txt"),
        T_COUCH (11, "couch.txt"),
        T_PLANT (12, "plant.txt"),
        T_TELEPHONE (13, "telephone.txt"),
        T_WHITEBOARD (14, "whiteboard.txt"),
        T_DOOR (15, "door.txt"),
        T_TIGER (16, "tiger.txt");

        private final int obsCode;
        private final String fileName;

        Observation(int obsCode, String fileName)
        {
            this.obsCode = obsCode;
            this.fileName = fileName;
        }

        public int getCode() { return this.obsCode; }
        public String getFileName() { return this.fileName; }
    }
}
