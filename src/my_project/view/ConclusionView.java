package my_project.view;

import KAGO_framework.control.ViewController;
import KAGO_framework.view.DrawTool;
import KAGO_framework.view.ProgramView;
import KAGO_framework.view.simple_gui.GIFPainter;
import my_project.control.ProgramController;

import java.awt.*;
import java.awt.image.BufferedImage;

public class ConclusionView extends ProgramView {

    public enum Conclusion {
        WIN, LOSE
    }

    private Conclusion conclusion;
    private BufferedImage playerImage;
    private GIFPainter background;
    private String[] ranking;

    /**
     * Erzeugt ein Objekt der Klasse GameView
     *
     * @param viewController    das ViewController-Objekt des Frameworks
     * @param programController das ProgramController-Objekt des Frameworks
     */
    public ConclusionView(ViewController viewController, ProgramController programController, BufferedImage playerImage, String[] ranking, Conclusion conclusion) {
        super(viewController, programController);
        this.playerImage = playerImage;
        this.ranking = ranking;
        this.conclusion = conclusion;
        viewController.getSoundController().loadSound("assets/sounds/sadness.mp3","lose",true);
        viewController.getSoundController().loadSound("assets/sounds/victory.mp3","win",true);
        if(conclusion == Conclusion.LOSE) viewController.getSoundController().playSound("lose");
        if(conclusion == Conclusion.WIN) viewController.getSoundController().playSound("win");
    }

    @Override
    public void draw(DrawTool drawTool) {
        drawTool.drawImage(playerImage,250,0);
        drawTool.formatText("Purisa", Font.BOLD, 24);
        drawTool.setCurrentColor(0, 0, 0, 255);
        drawTool.drawFilledRectangle(0, 0, 600, 600);
        drawTool.setCurrentColor(255, 255, 255, 255);
        if(conclusion == Conclusion.WIN){
            drawTool.drawText(235, 50, "You WIN!");
        } else if (conclusion == Conclusion.LOSE){
            drawTool.drawText(235, 50, "You LOSE!");
        }
        drawTool.formatText("Purisa", Font.BOLD, 18);
        drawTool.drawText(40, 200, "Ranking:");
        drawTool.formatText("Purisa", Font.PLAIN, 16);
        int rank = 1;
        for(int i = 1; i < ranking.length-1; i++){
            drawTool.drawText(40, 220+i*20, rank+". Platz: "+ranking[i]+" mit "+ranking[i+1]+" Punkten");
            i++;
        }
    }

    @Override
    public void disposeView(){
        super.disposeView();
        if(conclusion == Conclusion.LOSE) viewController.getSoundController().stopSound("lose");
        if(conclusion == Conclusion.WIN) viewController.getSoundController().stopSound("win");
    }

}
