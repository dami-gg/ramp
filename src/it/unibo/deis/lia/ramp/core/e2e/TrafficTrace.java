 /*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package it.unibo.deis.lia.ramp.core.e2e;

import java.io.*;
import java.util.*;

/**
 *
 * @author Dami
 */
public class TrafficTrace {

    private static TrafficTrace instance = null;

    public static final int HOURS_TO_SAVE = 0;
    public static final int MINUTES_TO_SAVE = 19;
    public static final int SECONDS_TO_SAVE = 59; 

    public static final int RANGE = 4000; // KB

    private static ArrayList <ArrayList> trace;

    private static ArrayList <Integer> nodes;
    private static ArrayList <Double> rates;
    private static ArrayList <Integer> counters;


    protected TrafficTrace () {

        trace = new ArrayList <ArrayList> ();

        nodes = new ArrayList();
        rates = new ArrayList();
        counters = new ArrayList();
    }

    public static TrafficTrace getInstance() {

        if (instance == null) {

            instance = new TrafficTrace();
        }

        return instance;
    }

    public static void register (int hour, int min, int sec, int node, double size, String behaviour) {

        ArrayList data = new ArrayList();

        data.add (hour);
        data.add (min);
        data.add (sec);
        data.add (node);
        data.add (size);
        data.add (behaviour);

        trace.add (data);

        updateBehaviour((Integer) data.get(3), size, behaviour, 1);
    }

    /*
     * Operation: -1 <=> Erase - 1 <=> Insert
     */
    private static void updateBehaviour (int senderId, double size, String behaviourPacket, int operation) {

        int pos = nodes.indexOf (senderId), value;

        if (behaviourPacket.equals ("collaborative")) value = 1;

        else if (behaviourPacket.equals("selfish")) value = -1;

        else value = 0;
        
        int behaviour = value * operation; 

        if (pos == -1) { // This node wasn't registered before

            nodes.add (senderId);

            rates.add (behaviour * size);

            counters.add (1);
        }

        else {

            double rate = rates.get(pos);

            double newRate = rate + (behaviour * size);

            if (newRate >= RANGE) newRate = RANGE;

            else if (newRate <= -RANGE) newRate = -RANGE;

            rates.set(pos, newRate);

            counters.set (pos, counters.get(pos) + 1);
        }
    }

    public static String getBehaviour (int node) {

        // First I must update the behaviours in the actual time

        Calendar cal = Calendar.getInstance();

        int h = cal.get(Calendar.HOUR_OF_DAY);
        int m = cal.get(Calendar.MINUTE);
        int s = cal.get(Calendar.SECOND);

        deleteOldRegisters(h, m, s);

        int pos = nodes.indexOf (node);

        if (pos == -1) return "Error";

        else {

            if (rates.get(pos) < 0)

                return "Selfish";

            else if (rates.get(pos) > 0)

                return "Collaborative";

            return "NoInformationYet";
        }
    }

    public static double getRate (int node) {

        // First I must update the rates in the actual time

        Calendar cal = Calendar.getInstance();

        int h = cal.get(Calendar.HOUR_OF_DAY);
        int m = cal.get(Calendar.MINUTE);
        int s = cal.get(Calendar.SECOND);

        deleteOldRegisters(h, m, s);

        int pos = nodes.indexOf (node);

        return (pos == -1) ? 0 : rates.get(pos);
    }

    private static void deleteOldRegisters (int h, int m, int s) {

        int i = 0;

        for (; i < trace.size(); i++) {
            
            if (((Integer) trace.get(i).get(0)) >= h - HOURS_TO_SAVE &&
                    ((Integer) trace.get(i).get(1)) >= m - MINUTES_TO_SAVE &&
                    ((Integer) trace.get(i).get(2)) >= s - SECONDS_TO_SAVE)

                break;
        }

        // Update behaviours

        for (int p = 0; p < i; p++) {

            updateBehaviour (((Integer) trace.get(p).get(3)), ((Double) trace.get(p).get(4)), ((String) trace.get(p).get(5)), -1);
        }

        // Delete registers from 0 to i
        
        int node;
        
        for (int k = i - 1, pos; k > 0; k--) {

            // If the node has only one apperance in the trace we have to delete it in the ArrayLists too

            node = ((Integer) trace.get(k).get(3));

            pos = nodes.indexOf (node);

            counters.set (pos, counters.get(pos) - 1);

            if (counters.get(pos) == 0) {

                nodes.remove (pos);
                rates.remove (pos);
                counters.remove (pos);
            }

            trace.remove (k);
        }
    }
}
