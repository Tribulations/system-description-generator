package com.sdg;

import com.sdg.controller.InputController;
import com.sdg.graph.GraphDatabaseOperations;

public class Main {
    public static void main(String[] args) throws Exception {
        GraphDatabaseOperations dbOps = new GraphDatabaseOperations();
        dbOps.deleteAllData();
        dbOps.close();
        InputController.start();
    }
}
