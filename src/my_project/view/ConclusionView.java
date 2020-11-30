package my_project.view;

import KAGO_framework.control.ViewController;
import KAGO_framework.view.ProgramView;
import KAGO_framework.view.simple_gui.GIFPainter;
import my_project.control.ProgramController;

import java.awt.image.BufferedImage;

public class ConclusionView extends ProgramView {

    enum Conclusion {
        WIN, LOSE, KICK
    }

    private String playerName;
    private int playerPoints;
    private BufferedImage playerImage;

    private GIFPainter background;

    /**
     * Erzeugt ein Objekt der Klasse GameView
     *
     * @param viewController    das ViewController-Objekt des Frameworks
     * @param programController das ProgramController-Objekt des Frameworks
     */
    public ConclusionView(ViewController viewController, ProgramController programController, String playerName, int playerPoints, BufferedImage playerImage) {
        super(viewController, programController);
        this.playerImage = playerImage;
        this.playerName = playerName;
        this.playerPoints = playerPoints;
    }



}
