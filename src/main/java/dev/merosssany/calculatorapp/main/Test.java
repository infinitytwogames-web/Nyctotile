package dev.merosssany.calculatorapp.main;

import dev.merosssany.calculatorapp.core.Core;
import dev.merosssany.calculatorapp.core.RGBA;
import dev.merosssany.calculatorapp.core.io.HoverEventRegister;
import dev.merosssany.calculatorapp.core.io.InputHandler;
import dev.merosssany.calculatorapp.core.logging.Logger;
import dev.merosssany.calculatorapp.core.logging.LoggingLevel;
import dev.merosssany.calculatorapp.core.position.UIVector2Df;
import dev.merosssany.calculatorapp.core.render.CleanupManager;
import dev.merosssany.calculatorapp.core.ui.Button;
import dev.merosssany.calculatorapp.core.ui.InteractableUI;
import dev.merosssany.calculatorapp.core.ui.UI;
import dev.merosssany.calculatorapp.core.ui.font.FontRenderer;
import org.joml.Matrix4f;

import java.io.IOException;

@Core
public class Test {
    private static Logger logger = new Logger("LoggerTest");
    private static Matrix4f textProj;
    private static InteractableUI t;
    private static UI topLeftHalfElement;
    private static FontRenderer test;

    public static void construct() {
        logger.info("Constructing....");
//        textProj = new Matrix4f().ortho2D(0, window.getWidth(), window.getHeight(), 0.5f ,new RGBA(0f, 0, 1f,1f));

        Button button = null;
//        try {
//            button = new Button("Hello",1f,new RGBA(1f,0f,0f,1f),new UIVector2Df(0,0),2,0.5f,0f,new RGBA(1f,1f,1f,1f),window,textProj);
//        } catch (IOException e) {
//            CleanupManager.createPopup("Failed to create button\n"+logger.formatStacktrace(e, LoggingLevel.FATAL));
//        }
    }

    public static void init() {
        topLeftHalfElement.init();
        t.init();
    }
}
