package com.sdg;

import com.sdg.controller.MainController;
import com.sdg.logging.LoggerUtil;

public class Main {
    public static void main(String[] args) throws Exception {
        LoggerUtil.info(Main.class, "Starting application");

        MainController.start();
    }
}
