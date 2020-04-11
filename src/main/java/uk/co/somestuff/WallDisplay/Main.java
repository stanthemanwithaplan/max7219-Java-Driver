package uk.co.somestuff.WallDisplay;

import org.json.JSONException;
import org.json.JSONObject;
import uk.co.somestuff.WallDisplay.Driver.Matrix;
import uk.co.somestuff.util.WebRequestException;
import uk.co.somestuff.util.WebRequests;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static uk.co.somestuff.WallDisplay.Driver.Font.LCD_FONT;

public class Main {

    public static void main(String[] args) {

        Runtime.getRuntime().addShutdownHook(new Thread("app-shutdown-hook") {
            @Override
            public void run() {
                System.out.println("[uk.co.somestuff.WallDisplay] Closing");
                matrix.clear();
                matrix.close();
            }
        });

        System.out.println("[uk.co.somestuff.WallDisplay] Starting");

        Matrix matrix = new Matrix(4);
        matrix.open();
        matrix.clear();
        matrix.brightness(0x0F);
        matrix.orientation(90);
        matrix.setLeft("|", LCD_FONT, 1, 1);
        matrix.scrollUp("Hello", LCD_FONT, matrix.getPadding("Hello", LCD_FONT));
        matrix.carrouselText("Hello", LCD_FONT, 40);
        matrix.text("Hello", LCD_FONT, matrix.getPadding("Hello", LCD_FONT));

    }
}
