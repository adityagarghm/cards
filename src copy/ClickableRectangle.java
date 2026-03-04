
import processing.core.PApplet;

public class ClickableRectangle {
    int x;
    int y;
    int width;
    int height;

    boolean isClicked(int mouseX, int mouseY) {
        return mouseX >= x && mouseX <= x + width &&
               mouseY >= y && mouseY <= y + height;
    }

    public void draw(PApplet app) {
        app.rect(x, y, width, height);
    } 
}
