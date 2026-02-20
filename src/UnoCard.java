
import processing.core.PApplet;

public class UnoCard extends Card {
    public UnoCard(String value, String suit) {
        super(value, suit);
    }

    @Override
    public void draw(PApplet sketch) {
        super.draw(sketch);
        // set card color based on suit
        switch (suit) {
            case "Red":
                sketch.fill(255, 0, 0);
                break;
            case "Yellow":
                sketch.fill(255, 255, 0);
                break;
            case "Green":
                sketch.fill(0, 255, 0);
                break;
            case "Blue":
                sketch.fill(40, 40, 210);
                break;
            default:
                sketch.fill(200);
                break;
        }
        sketch.rect(x, y, width, height);

        // text 
        if (suit == "Blue") {
            sketch.fill(255);
        } else {
            sketch.fill(0);
        }
        sketch.textSize(14);
        // put on the upper left corner
        if (value == "Skip") {
            // skip symbol can be represented as a circle with a line through it
            sketch.push();
            sketch.noFill();
            sketch.stroke(0);
            sketch.strokeWeight(3);
            sketch.ellipse(x + 15, y + 15, 20, 20);
            sketch.line(x + 5, y + 5, x + 25, y + 25);
            sketch.pop();
        } else if (value == "Reverse") {
            // reverse symbol can be represented as two arrows in a circle
            sketch.push();
            sketch.stroke(0);
            sketch.noFill();
            sketch.strokeWeight(3);
            // first arrow
            sketch.arc(x + 15, y + 15, 20, 20, sketch.PI * 1 / 6, sketch.PI);
            // at the end of the arc, draw an arrowhead
            sketch.line(x + 5, y + 15, x + 8, y + 18);
            sketch.line(x + 5, y + 15, x + 3, y + 18);
            // second arrow
            sketch.arc(x + 15, y + 15, 20, 20, sketch.PI * 7 / 6, sketch.TWO_PI);
            sketch.line(x + 25, y + 15, x + 22, y + 18);
            sketch.line(x + 25, y + 15, x + 28, y + 18);
            // line created
            sketch.pop();
        } else if (value == "Draw Two") {
            sketch.text("+2", x + 10, y + 10);
        } else if (value == "Wild") {
            // wild symbol can be represented as four quadrants of circle
            sketch.fill(255, 0, 0);
            sketch.arc(x + 15, y + 15, 20, 20, 0, sketch.HALF_PI);
            sketch.fill(0, 255, 0);
            sketch.arc(x + 15, y + 15, 20, 20,
                    sketch.HALF_PI, sketch.PI);
            sketch.fill(0, 0, 255);
            sketch.arc(x + 15, y + 15, 20, 20,
                    sketch.PI, 3 * sketch.HALF_PI);

            sketch.fill(255, 255, 0);
            sketch.arc(x + 15, y + 15, 20, 20,
                    3 * sketch.HALF_PI, sketch.TWO_PI);
        } else if (value == "Draw Four") {
            sketch.text("+4", x + 10, y + 10);
        } else {
            sketch.text(value, x + 10, y + 10);
        }

    }

}
