package com.example.jaycee.pomdpobjectsearch;

public class Objects
{
    public enum Observation
    {
        O_NOTHING (0),
        T_COMPUTER_MONITOR (1),
        T_COMPUTER_KEYBOARD (2),
        T_COMPUTER_MOUSE (3),
        T_DESK (4),
        T_LAPTOP (5),
        T_MUG (6),
        T_WINDOW (7),
        T_LAMP (8),
        T_BACKPACK (9),
        T_CHAIR (10),
        T_COUCH (11),
        T_PLANT (12),
        T_TELEPHONE (13),
        T_WHITEBOARD (14),
        T_DOOR (15);

        private final int obsCode;
        Observation(int obsCode) { this.obsCode = obsCode; }

        public int getCode() { return this.obsCode; }
    }
}
