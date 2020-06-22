package uk.co.somestuff.WallDisplay.Driver;

import com.pi4j.io.spi.SpiChannel;
import com.pi4j.io.spi.SpiDevice;
import com.pi4j.io.spi.SpiFactory;

import java.lang.reflect.Array;
import java.util.Arrays;

/**
 * Last Edited 22nd June 2020 22:13
 * @author Stanley Julius-Sadler
 **/

public class Matrix {

    /** 'MATRIX_BLOCK_HW' is the height and width of each of the 8x8 matrix blocks
     *  'MATRIX_WIDTH' is defined when creating a Matrix instance and should be how many of the 8x8 matrix blocks is connected consecutively
     *  'MATRIX_BUFFER' is the byte array that is sent to the matrix display, this is set within each of the functions
     **/
    protected static final short MATRIX_BLOCK_HW = 8;
    protected int MATRIX_WIDTH;

    protected int orientation = 0;
    protected byte[] MATRIX_BUFFER;
    protected byte[] EXTRA_MATRIX_BUFFER;
    protected byte[] EXTRA_LEFT_BUFFER = {};

    protected SpiDevice spi;

    public static class Constants{
        public static byte MAX7219_REG_NOOP = 0x0;
        public static byte MAX7219_REG_DIGIT0 = 0x1;
        public static byte MAX7219_REG_DIGIT1 = 0x2;
        public static byte MAX7219_REG_DIGIT2 = 0x3;
        public static byte MAX7219_REG_DIGIT3 = 0x4;
        public static byte MAX7219_REG_DIGIT4 = 0x5;
        public static byte MAX7219_REG_DIGIT5 = 0x6;
        public static byte MAX7219_REG_DIGIT6 = 0x7;
        public static byte MAX7219_REG_DIGIT7 = 0x8;
        public static byte MAX7219_REG_DECODEMODE = 0x9;
        public static byte MAX7219_REG_INTENSITY = 0xA;
        public static byte MAX7219_REG_SCANLIMIT = 0xB;
        public static byte MAX7219_REG_SHUTDOWN = 0xC;
        public static byte MAX7219_REG_DISPLAYTEST = 0xF;
    }

    public Matrix(int MATRIX_WIDTH) {

        this.MATRIX_WIDTH = MATRIX_WIDTH;
        /** Once the 'MATRIX_WIDTH' is known a new byte array is created with that length **/
        this.MATRIX_BUFFER = new byte[MATRIX_BLOCK_HW * this.MATRIX_WIDTH];
        this.EXTRA_MATRIX_BUFFER = new byte[MATRIX_BLOCK_HW * this.MATRIX_WIDTH];

        try {
            this.spi = SpiFactory.getInstance(SpiChannel.CS0, SpiDevice.DEFAULT_SPI_SPEED,SpiDevice.DEFAULT_SPI_MODE);

            command(Constants.MAX7219_REG_SCANLIMIT, (byte) 0x7);
            command(Constants.MAX7219_REG_DECODEMODE, (byte) 0x0);
            command(Constants.MAX7219_REG_DISPLAYTEST, (byte) 0x0);
            //command(Constants.MAX7219_REG_SHUTDOWN, (byte) 0x1);

        } catch (Exception e) {
            e.printStackTrace ();
        }
    }

    private Thread matrixUpdater = new Thread() {
        public void run() {
            while (true) {
                //System.out.println("[co.uk.somestuff.WallDisplay.Matrix] Updating display");
                Matrix.this.flush();
                try {
                    sleep(16);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    public void emptyLeftMargin() {
        this.EXTRA_LEFT_BUFFER = new byte[]{};
    }

    public void setLeftMargin(String msg, short[][] font, int fromLeft, int fromRight) {

        this.EXTRA_LEFT_BUFFER = new byte[]{};

        byte[] chars = new byte[MATRIX_BLOCK_HW * msg.length()];

        int constant = 0;
        for (int i = 0; i < msg.length(); i++) {
            /** Goes through each letter in the message and gets the byte array from the font profile, then sets it to
             * the variable 'chars' **/
            short[] letter = Font.value(font, msg.charAt(i));

            for (int u = 0; u < letter.length; u++) {
                chars[constant] = (byte) letter[u];
                constant++;
            }
        }

        if (constant + fromLeft + fromRight >= this.MATRIX_WIDTH * MATRIX_BLOCK_HW) {
            this.EXTRA_LEFT_BUFFER = null;
            System.out.println("[uk.co.somestuff.WallDisplay] Not showing extra left as it wouldn't fit");
            return;
        } else {
            this.EXTRA_LEFT_BUFFER = new byte[constant + fromLeft + fromRight];
        }

        /** Adding the left padding **/
        int i = 0;
        for (int u; i < fromLeft; i++) {
            this.EXTRA_LEFT_BUFFER[i] = 0;
        }

        /** Adding the characters **/
        while (i < EXTRA_LEFT_BUFFER.length-fromLeft-fromRight) {
            this.EXTRA_LEFT_BUFFER[i] = chars[i-fromLeft];
            i++;
        }

        /** Adding the right padding **/
        for (int u; i < fromRight; i++) {
            this.EXTRA_LEFT_BUFFER[i] = 0;
        }

        this.combineLeftMargin();
    }

    private void combineLeftMargin() {

        if (this.EXTRA_LEFT_BUFFER != null || this.EXTRA_LEFT_BUFFER.length > 0) {
            /** So we know the length of the Extra Buffer is less than the total width and that the matrix buffer is the
             * width of the display.
             **/

            /** Saves the existing matrix buffer to a new variable, it's a for loop because if you just do = it fucks up **/
            /*byte[] standingMatrixBuffer = new byte[this.MATRIX_BUFFER.length];
            for (int i = 0; i < standingMatrixBuffer.length; i++) {
                standingMatrixBuffer[i] = this.MATRIX_BUFFER[i];
            }*/

            int i = 0;
            /** First we add the extra on the left to the matrix buffer as we know it is less than the total width of
             * the display **/
            for (int u; i < this.EXTRA_LEFT_BUFFER.length; i++) {
                this.MATRIX_BUFFER[i] = this.EXTRA_LEFT_BUFFER[i];
            }

            /** Then add on the original message, or whats left that's visible onto the buffer **/
            for (int u; i < this.MATRIX_BUFFER.length; i++) {
                this.MATRIX_BUFFER[i] = this.EXTRA_MATRIX_BUFFER[i-this.EXTRA_LEFT_BUFFER.length];
            }
        }
    }

    public void text(String msg, short[][] font, int fromLeft) {

        byte[] chars = new byte[MATRIX_BLOCK_HW * this.MATRIX_WIDTH];

        int constant = 0;
        for (int i = 0; i < msg.length(); i++) {
            /** Goes through each letter in the message and gets the byte array from the font profile, then sets it to
             * the variable 'chars' **/
            short[] letter = Font.value(font, msg.charAt(i));

            for (int u = 0; u < letter.length; u++) {
                chars[constant] = (byte) letter[u];
                constant++;
            }
        }

        /** First we define 'i' which will be a constant for both loops, the first loop adds the white space to the
         * beginning of the 'MATRIX_BUFFER', the second loop adds the rest of the characters from the mesage to the buffer
         * upto the width of the buffer whilst adding 1 to i **/
        int i = 0;

        for (int u; i < fromLeft; i++) {
            this.EXTRA_MATRIX_BUFFER[i] = 0;
        }
        while (i < MATRIX_BLOCK_HW * this.MATRIX_WIDTH) {
            this.EXTRA_MATRIX_BUFFER[i] = chars[i-fromLeft];
            i++;
        }

        this.combineLeftMargin();
        this.EXTRA_MATRIX_BUFFER = new byte[MATRIX_BLOCK_HW * this.MATRIX_WIDTH];
    }

    public void scrollUp(String msg, short[][] font, int fromLeft, int delay) {

        /** We set the length of the the array 'orgChars' so that it can be the size of the matrix display or the full
         * message, which ever is bigger **/
        int width = MATRIX_BLOCK_HW * this.MATRIX_WIDTH;
        if (MATRIX_BLOCK_HW * msg.length() > MATRIX_BLOCK_HW * this.MATRIX_WIDTH) {
            width = MATRIX_BLOCK_HW * msg.length();
        }
        byte[] orgChars = new byte[width];

        int constant = 0;
        for (int u; constant < fromLeft; constant++) {
            orgChars[constant] = 0;
        }

        for (int i = 0; i < msg.length(); i++){
            /** Goes through each letter in the message and gets the byte array from the font profile, then sets it to
            * the variable 'orgChars' **/
            short[] letter = Font.value(font, msg.charAt(i));

            for (int u = 0; u < letter.length; u++) {
                orgChars[constant] = (byte) letter[u];
                constant++;
            }
        }

        String[] newChars = new String[MATRIX_BLOCK_HW * this.MATRIX_WIDTH];

        /** Makes sure that all of the binary characters is a byte long **/
        for (int i = 0; i < MATRIX_BLOCK_HW * this.MATRIX_WIDTH; i++) {
            StringBuilder charsBuilder = new StringBuilder(Integer.toBinaryString(orgChars[i]));
            while (charsBuilder.length() < 8) {
                charsBuilder.insert(0, "0");
            }
            orgChars[i] = (byte) Integer.parseInt(charsBuilder.toString(), 2);
            newChars[i] = charsBuilder.toString();
        }

        for (int i = 0; i < MATRIX_BLOCK_HW; i++) {
            /** Delays the thread (main program) by the delay value so that the message doesn't fly across the display **/
            try {
                Thread.sleep(delay);
            } catch(Exception ex) {
                ex.printStackTrace();
            }

            for (int y = 0; y < newChars.length; y++) {
                StringBuilder letterBuilder = new StringBuilder();
                /** Creates a 8 long string of 0s **/
                for (int u = 0; u < 8; u++) {
                    letterBuilder.insert(0, "0");
                }
                /** Sets the character at 'i-u' to the '7-u'th bit in the 'y'th byte of 'newChars' **/
                for (int u = 0; u <= i; u++) {
                    letterBuilder.setCharAt(i-u, newChars[y].charAt(7-u));
                }
                /** Assigns the 'y' value of the 'MATRIX_BUFFER' to the new binary **/
                this.EXTRA_MATRIX_BUFFER[y] = (byte) Integer.parseInt(letterBuilder.toString(), 2);
            }

            this.combineLeftMargin();
        }
    }

    public void carrouselText(String msg, short[][] font, int delay) {

        /** We set the length of the the array 'orgWhatsLeft' so that it can be the size of the matrix display or the full
         * message, which ever is bigger. We use this as a bigger then needed array as we don't know the actual width yet **/
        int width = MATRIX_BLOCK_HW * this.MATRIX_WIDTH;
        if (MATRIX_BLOCK_HW * msg.length() > MATRIX_BLOCK_HW * this.MATRIX_WIDTH) {
            width = MATRIX_BLOCK_HW * msg.length();
        }

        byte[] orgWhatsLeft = new byte[width];
        int actualW = 0;

        int constant = 0;
        for (int i = 0; i < msg.length(); i++){
            /** Goes through each letter in the message and gets the byte array from the font profile, then sets it to
             * the variable 'orgWhatsLeft' **/
            short[] letter = Font.value(font, msg.charAt(i));
            actualW += letter.length;
            for (int u = 0; u < letter.length; u++) {
                orgWhatsLeft[constant] = (byte) letter[u];
                constant += 1;
            }
        }

        /** Checks weather the length of the 'actualW' is less than the width of the matrix display **/
        if (actualW < MATRIX_BLOCK_HW * this.MATRIX_WIDTH) {
            actualW = MATRIX_BLOCK_HW * this.MATRIX_WIDTH;
        }

        /** Creates a new array with the now known length of the letters **/
        byte[] whatsLeft = new byte[actualW];
        /** Goes through 'orgWhatsLeft' and if it's gonna be visible on the display adds it to 'whatsLeft' **/
        for (int i = 0; i < whatsLeft.length; i++) {
            whatsLeft[i] = orgWhatsLeft[i];
        }

        /** If the length of the message is less than the length of the display, we add '0's to make sure it's the right length
         * Not sure if this is needed but i cba to check. **/
        int index = whatsLeft.length-1;
        while (whatsLeft.length < MATRIX_BLOCK_HW * this.MATRIX_WIDTH) {
            whatsLeft[index] = 0;
            index += 1;
        }

        /** We add the visible text to the 'MATRIX_BUFFER' so that it is visible
          don't think it's necessary as we do something similar later on **/
        /*for (int i = 0; i < MATRIX_BLOCK_HW * this.MATRIX_WIDTH; i++) {
            this.MATRIX_BUFFER[i] = whatsLeft[i];
        }*/

        for (int i = 0; i < actualW; i++) {
            /** Delays the thread (main program) by the delay value so that the message doesn't fly across the display **/
            try {
                Thread.sleep(delay);
            } catch(Exception ex) {
                ex.printStackTrace();
            }
            /** While there are still points in 'whatsLeft' we remove the first item in the array **/
            if (whatsLeft.length > 1) {
                whatsLeft = Arrays.copyOfRange(whatsLeft, 1, whatsLeft.length);
            }
            /** For the length of the display ether update the buffer with the new available message or set it to empty space **/
            for (int u = 0; u < MATRIX_BLOCK_HW * this.MATRIX_WIDTH; u++) {
                if (whatsLeft.length <= u) {
                    this.EXTRA_MATRIX_BUFFER[u] = 0;
                } else {
                    this.EXTRA_MATRIX_BUFFER[u] = whatsLeft[u];
                }
            }

            this.combineLeftMargin();
        }
        this.EXTRA_MATRIX_BUFFER = new byte[MATRIX_BLOCK_HW * this.MATRIX_WIDTH];
    }

    public int getPadding(String msg, short[][] font) {
        int messageWidth = 0;

        /** Gets the length of all the letters in the message **/
        for (int i = 0; i < msg.length(); i++){
            short[] letter = Font.value(font, msg.charAt(i));
            messageWidth += letter.length;
        }
        /** Get the width of the display add 1 of the extra space in the end of the message and add another 1 for tbh i cant remember
         * Divide by two to get each side and then round to the nearest whole number **/
        return Math.round(((MATRIX_BLOCK_HW * this.MATRIX_WIDTH + 2) - messageWidth) / 2);
    }

    /** All the code under this comment was written my a chinese man on GitHub, meaning that I don't know how it works properly :/
     * @author yancheng https://github.com/sharetop/max7219-java
     **/

    public void flush() {
        try {
            byte[] buf = this.MATRIX_BUFFER;

            /** I dont know how the orientation works yet **/
            if(this.orientation > 0){
                buf = this._rotate(buf);
            }

            for (short pos = 0; pos < MATRIX_BLOCK_HW; pos++) {
                this._write(this._values(pos, buf));
            }

        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    private void _write(byte[] buf) throws Exception {
        this.spi.write(buf);
    }

    public void command(byte register, byte data) throws Exception {

        int len = 2 * this.MATRIX_WIDTH;
        byte [] buf = new byte [len];

        for (int i = 0;i < len; i += 2) {
            buf[i] = register;
            buf[i+1] = data;
        }

        this._write(buf);
    }

    private byte[] _rotate(byte[] buf) {
        byte[] result = new byte[this.MATRIX_BUFFER.length];
        for (int i=0;i<this.MATRIX_WIDTH*MATRIX_BLOCK_HW;i+=MATRIX_BLOCK_HW) {
            byte[] tile = new byte[MATRIX_BLOCK_HW];
            for (int j=0;j<MATRIX_BLOCK_HW;j++) {
                tile[j]=buf[i+j];
            }
            int k = this.orientation/90;
            for (int j=0;j<k;j++) {
                tile = this._rotate_8_8(tile);
            }
            for (int j=0;j<MATRIX_BLOCK_HW;j++) {
                result[i+j]=tile[j];
            }

        }
        return result;
    }

    public void close(){
        try {
            this.clear();
            if (this.matrixUpdater.isAlive()) {
                this.matrixUpdater.interrupt();
            }
            this.command(Constants.MAX7219_REG_SHUTDOWN, (byte)0x0);
        } catch(Exception ex){
            ex.printStackTrace();
        }
    }

    public void open(){
        this.matrixUpdater.start();
        try {
            this.command(Constants.MAX7219_REG_SHUTDOWN, (byte)0x1);
            this.clear();
        } catch(Exception ex){
            ex.printStackTrace();
        }
    }

    private byte[] _rotate_8_8(byte[] buf) {
        byte[] result = new byte[8];
        for (int i = 0; i <8; i ++) {// Index of output result
            short b = 0;
            short t = (short) ((0x01 << i) & 0xff); // According to the index, calculate which bit of the source to take
            for (int j=0;j<8;j++) {
                int d = 7-i-j; // Calculate the number of shifts, which is related to i and may be negative
                if(d>0)
                    b +=(short)((buf[j]&t)<<d);
                else
                    b +=(short)((buf[j]&t)>>(-1*d));
            }
            result[i]=(byte)b;
        }

        return result;
    }

    public void clear(){

        try {
            for (int i=0;i<this.MATRIX_WIDTH;i++) {
                for (short j=0;j<MATRIX_BLOCK_HW;j++) {
                    this._setbyte(i, (short)(j+ Constants.MAX7219_REG_DIGIT0), (byte)0x00);
                }
            }
            this.flush();

        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    private void _setbyte(int deviceId,short position,byte value) {
        int offset = deviceId*MATRIX_BLOCK_HW+position- Constants.MAX7219_REG_DIGIT0;
        this.MATRIX_BUFFER[offset]=value;
    }

    public void orientation(int angle) {
        if(angle!=0 && angle!=90 && angle!=180 && angle!=270) return;

        this.orientation = angle;

        this.flush();
    }

    private byte[] _values(short position,byte[] buf) throws Exception {
        int len = 2*this.MATRIX_WIDTH;
        byte [] ret = new byte [len];

        for (int i=0;i<this.MATRIX_WIDTH;i++) {
            ret[2*i]=(byte)((position+ Constants.MAX7219_REG_DIGIT0)&0xff);
            ret[2 * i + 1] = buf[(i * MATRIX_BLOCK_HW) + position];

        }
        return ret;
    }

    public void brightness(int intensity) {
        try {
            if (intensity < 0 || intensity > 15) return;
            this.command(Constants.MAX7219_REG_INTENSITY, (byte) intensity);
        } catch (Exception e) {
            e.printStackTrace ();
        }
    }

}
