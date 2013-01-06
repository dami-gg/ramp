/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package it.unibo.deis.lia.ramp.service.application;

/**
 *
 * @author Dami
 */
public class Communication implements Cloneable {

    private int hour = -1;
    private int minute = -1;
    private int second = -1;

    private int source = -1;
    private String service = "";
    private int dest = -1;

    private String action = "";
    private String behaviour = "";

    private String fileName = "";
    private int clientPort = -1;

    private int serverPort = -1;
    private double size = -1; // Kb

    private int parameters = 0;

    public Communication (int h, int m, int sec, int s, String serv) {

        hour = h;
        minute = m;
        second = sec;
        source = s;
        service = serv;

        parameters = 5;
    }

    public Communication (int s, String serv) {

        source = s;
        service = serv;

        parameters = 2;
    }

    public Communication (int s, String serv, int d) {

        source = s;
        service = serv;
        dest = d;

        parameters = 3;
    }

    public Communication (int s, int d, int p, String whichPort) {

        source = s;
        dest = d;

        if (whichPort.equals("client"))
        
            clientPort = p;

        else if (whichPort.equals("server"))

            serverPort = p;

        parameters = 3;
    }

    public Communication (int s, int d, String getList) {

        source = s;
        dest = d;
        action = getList;

        parameters = 3;
    }

    public Communication (int s, int d, String getStream, int cPort) {

        source = s;
        dest = d;
        action = getStream;
        clientPort = cPort;

        parameters = 4;
    }

    public Communication (int s, int d, int p, boolean flag) {

        source = s;
        dest = d;

        if (!flag) { // Getting a file --> Client port

            clientPort = p;
        }

        else { // Putting a file --> Server port

            serverPort = p;
        }

        parameters = 4;
    }
    
    public int howManyParameters () {

        return parameters;
    }

    public int getHour () {

        return hour;
    }

    public void setHour (int h) {

        if (hour == -1)
            
            parameters++;

        hour = h;
    }

    public int getMinute () {

        return minute;
    }

    public void setMinute (int m) {

        if (minute == -1)

            parameters++;

        minute = m;
    }

    public int getSecond () {

        return second;
    }

    public void setSecond (int s) {

        if (second == -1)

            parameters++;

        second = s;
    }

    public int getSource () {

        return source;
    }

    public void setSource (int s) {

        if (source == -1)

            parameters++;

        source = s;
    }

    public String getService () {

        return service;
    }

    public void setService (String s) {

        if (service.equals(""))

            parameters++;

        service = s;
    }

    public int getDest () {

        return dest;
    }

    public void setDest (int d) {

        if (dest == -1)

            parameters++;

        dest = d;
    }

    public String getAction () {

        return action;
    }

    public void setAction (String a) {

        if (action.equals(""))

            parameters++;

        action = a;
    }

    public String getBehaviour () {

        return behaviour;
    }

    public void setBehaviour (String b) {

        if (behaviour.equals(""))

            parameters++;

        behaviour = b;
    }

    public String getFileName () {

        return fileName;
    }

    public void setFileName (String f) {

        if (fileName.equals(""))

            parameters++;

        fileName = f;
    }

    public int getClientPort () {

        return clientPort;
    }

    public void setClientPort (int p) {

        if (clientPort == -1)

            parameters++;

        clientPort = p;
    }

    public double getSize () {

        return size;
    }

    public void setSize (double s) {

        if (size == -1)

            parameters++;

        size = s;
    }

    public int getServerPort () {

        return serverPort;
    }

    public void setServerPort (int p) {

        if (serverPort == -1)

            parameters++;

        serverPort = p;
    }

    public boolean equals (Communication c) {

        if ((c.source == -1 || this.source == c.source) && (c.service.equals("") || this.service.equals(c.service)) &&
                (c.dest == -1 || this.dest == c.dest) && (c.action.equals("") ||this.action.equals(c.action)) &&
                (c.behaviour.equals("") || this.behaviour.equals(c.behaviour)) && (c.fileName.equals("") || this.fileName.equals(c.fileName))
                && (c.clientPort == -1 || this.clientPort == c.clientPort) && (c.serverPort == -1 || this.serverPort == c.serverPort) &&
                (c.size == -1 || this.size == c.size))

            // Ignore if the time is different

            return true;

        return false;
    }

    public Object clone() {

        Object clone = null;

        try {

            clone = super.clone();
        }

        catch (CloneNotSupportedException e) {

        }

        return clone;
    }
}
