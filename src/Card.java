import processing.core.PImage;
import processing.core.PApplet;

public class Card extends ClickableRectangle {
    String value;
    String suit;
    PImage img;
    boolean turned = false;
    private int clickableWidth = 30; // Width of the left sliver that is clickable
    private boolean selected = false;
    private int baseY;
    private boolean hasBaseY = false;

    Card(String value, String suit) {
        this.value = value;
        this.suit = suit;
    }

    Card(String value, String suit, PImage img) {
        this.value = value;
        this.suit = suit;
        this.img = img;
    }

    public void setTurned(boolean turned) {
        this.turned = turned;
    }

    public void setClickableWidth(int width) {
        this.clickableWidth = width;
    }

    public void setSelected(boolean selected, int raiseAmount) {
        if (selected && !this.selected) {
            baseY = y;
            hasBaseY = true;
            y = baseY - raiseAmount;
        } else if (!selected && this.selected && hasBaseY) {
            y = baseY;
        }
        this.selected = selected;
    }

    public boolean isSelected() {
        return selected;
    }

    @Override
    public boolean isClicked(int mouseX, int mouseY) {
        // Only the left sliver of the card is clickable
        return mouseX >= x && mouseX <= x + clickableWidth &&
                mouseY >= y && mouseY <= y + height;
    }

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void setSize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public void setPosition(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    @Override
    public void draw(PApplet sketch) {
        if (turned) {
            sketch.fill(150);
            sketch.rect(x, y, width, height);
            return;
        }
        if (isSelected()) {
            sketch.stroke(0);
            sketch.strokeWeight(4);
        } else {
            sketch.stroke(0);
        }
        if (img != null) {
            sketch.image(img, x, y, width, height);
        } else {
            sketch.fill(255);
            sketch.rect(x, y, width, height);
            sketch.fill(0);
            sketch.text(value, x + 10, y + 10);
        }
        sketch.strokeWeight(1);
    }
}
