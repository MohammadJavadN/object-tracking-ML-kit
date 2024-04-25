package com.example.vehiclespeeddetection;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class CsvWriter {

    public static void saveHashMapToCsv(HashMap<Integer, HashMap<Integer, Float>> map, String filePath) {
        try {
            FileWriter writer = new FileWriter(filePath);

            for (Map.Entry<Integer, HashMap<Integer, Float>> entry : map.entrySet()) {
                Integer outerKey = entry.getKey();
                HashMap<Integer, Float> innerMap = entry.getValue();

                for (Map.Entry<Integer, Float> innerEntry : innerMap.entrySet()) {
                    Integer innerKey = innerEntry.getKey();
                    Float value = innerEntry.getValue();

                    writer.append(outerKey.toString())
                            .append(",")
                            .append(innerKey.toString())
                            .append(",")
                            .append(value.toString())
                            .append("\n");
                }
            }

//            for (Map.Entry<Integer, HashMap<Integer, Float>> entry : map.entrySet()) {
//                Integer outerKey = entry.getKey();
//                HashMap<Integer, Float> innerMap = entry.getValue();
//                writer.append(outerKey.toString())
//                        .append(",");
//                int cnt = 0;
//                for (Map.Entry<Integer, Float> innerEntry : innerMap.entrySet()) {
//                    Integer innerKey = innerEntry.getKey();
//                    Float value = innerEntry.getValue();
//                    while (cnt < innerKey) {
//                        writer.append(",");
//                        cnt++;
//                    }
//                    writer.append(value.toString());
////                            .append("\n");
//                }
//                writer.append("\n");
//
//            }

            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

//    public static void saveHashMapToCsv(HashMap<Integer, ArrayList<Float>> map, String filePath) {
//        try {
//            FileWriter writer = new FileWriter(filePath);
//
//            for (Map.Entry<Integer, ArrayList<Float>> entry : map.entrySet()) {
//                Integer key = entry.getKey();
//                ArrayList<Float> values = entry.getValue();
//
//                StringBuilder line = new StringBuilder();
//                line.append(key);
//                for (Float value : values) {
//                    line.append(",").append(value);
//                }
//                line.append("\n");
//
//                writer.append(line.toString());
//            }
//
//            writer.flush();
//            writer.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
}
