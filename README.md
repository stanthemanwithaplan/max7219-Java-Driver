# max7219 java driver
Mainly for use with a raspberry pi

The library is currently capable of:
- Displaying text
- Scrolling text up
- Carrouseling text along the display
- Accomidating a constant text on the left

## Usage

Setup
```
Matrix matrix = new Matrix(4);
matrix.open();
matrix.brightness(0x0F); /** 0x0F is hex for 15, the maximum brightness **/
matrix.orientation(90); /** Most bought displays come in at a 90 angle **/
```
Setting a constant left item
```
matrix.setLeftMargin(String msg, short[][] font, int fromLeft, int fromRight, boolean isFlush)
matrix.setLeftMargin("14:03", LCD_FONT, 1, 1, true);
```
Scroll text up
```
matrix.scrollUp(String msg, short[][] font, int fromLeft, int delay)
matrix.scrollUp("WOW", LCD_FONT, 0);
```
Carrouseling text along
```
matrix.carrousellText(String msg, short[][] font, int delay)
matrix.carrouselText("Oooo", LCD_FONT, 40);
```
Just displaying text
```
matrix.text(String msg, short[][] font, int fromLeft)
matrix.text("Hello", LCD_FONT, 0);
```

## Extra

Getting padding so that the text is centered
```
matrix.getPadding(String msg, short[][] font)
matrix.getPadding("Hello", LCD_FONT);
```
<br />
<br />
@yancheng https://github.com/sharetop/max7219-java for the base SPI interface
